package com.learntrix.edtech.service;

import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.dto.quiz.*;
import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.common.exception.CourseAccessDeniedException;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.entity.QuizAttemptAnswer;
import com.learntrix.edtech.entity.QuizOption;
import com.learntrix.edtech.entity.QuizQuestion;
import com.learntrix.edtech.entity.Quiz;
import com.learntrix.edtech.entity.QuizAttempt;
import com.learntrix.edtech.entity.User;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;
import com.learntrix.edtech.repository.QuizAttemptAnswerRepository;
import com.learntrix.edtech.repository.QuizOptionRepository;
import com.learntrix.edtech.repository.QuizQuestionRepository;
import com.learntrix.edtech.repository.QuizAttemptRepository;
import com.learntrix.edtech.repository.QuizRepository;
import com.learntrix.edtech.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional
public class QuizService {

    private final QuizRepository quizRepository;
    private final QuizAttemptRepository quizAttemptRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseAccessService courseAccessService;
    private final QuizQuestionRepository questionRepository;
    private final QuizOptionRepository optionRepository;
    private final QuizAttemptAnswerRepository attemptAnswerRepository;
    private final BatchRepository batchRepository;

    public QuizService(
            QuizRepository quizRepository,
            QuizAttemptRepository quizAttemptRepository,
            CourseRepository courseRepository,
            UserRepository userRepository,
            CourseAccessService courseAccessService,
            QuizQuestionRepository questionRepository,
            QuizOptionRepository optionRepository,
            QuizAttemptAnswerRepository attemptAnswerRepository,
            BatchRepository batchRepository) {
        this.quizRepository = quizRepository;
        this.quizAttemptRepository = quizAttemptRepository;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseAccessService = courseAccessService;
        this.questionRepository = questionRepository;
        this.optionRepository = optionRepository;
        this.attemptAnswerRepository = attemptAnswerRepository;
        this.batchRepository = batchRepository;
    }

    // ------------------------------------------------------------------
    // Authorization helpers - the single place quiz access rules live.
    // Both delegate to CourseAccessService; neither reimplements it.
    // ------------------------------------------------------------------

    private boolean isAdmin() {
        return SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("SUPER_ADMIN");
    }

    /**
     * A teacher may manage a quiz when they are authorized for its course and, if the quiz is
     * pinned to a batch, when they own that batch. Course authority alone is not enough for a
     * batch quiz - otherwise a teacher on the same course could edit another cohort's quiz.
     */
    private void verifyTeacherCanManageQuiz(Quiz quiz, UUID teacherId) {
        if (isAdmin()) return;
        courseAccessService.verifyTeacherCanManageCourse(teacherId, quiz.getCourse().getId());
        Batch batch = quiz.getBatch();
        if (batch != null
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to manage quizzes for that batch");
        }
    }

    /** Resolves and validates a batch for a course, checking ownership. Null passes through. */
    private Batch resolveBatchForCourse(UUID batchId, UUID courseId, UUID teacherId) {
        if (batchId == null) return null;
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        if (batch.getCourse() == null || !batch.getCourse().getId().equals(courseId)) {
            throw new BusinessException("BATCH_COURSE_MISMATCH",
                    "That batch does not belong to the selected course.", HttpStatus.BAD_REQUEST);
        }
        if (!isAdmin()
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to create quizzes for that batch");
        }
        return batch;
    }

    /**
     * A student may open a quiz when it is published, they are enrolled in its course, and -
     * for a batch quiz - they are a member of that batch. This is what stops a Morning
     * student reaching an Evening quiz by editing the id in the URL.
     */
    private void verifyStudentCanAccessQuiz(Quiz quiz, UUID studentId) {
        if (isAdmin()) return;
        if (!"PUBLISHED".equalsIgnoreCase(quiz.getStatus())) {
            throw new CourseAccessDeniedException("This quiz is not available.");
        }
        courseAccessService.verifyStudentCanAccessCourse(studentId, quiz.getCourse().getId());
        Batch batch = quiz.getBatch();
        if (batch != null) {
            boolean member = batch.getStudents() != null
                    && batch.getStudents().stream().anyMatch(u -> u.getId().equals(studentId));
            if (!member) {
                throw new CourseAccessDeniedException("This quiz is not available.");
            }
        }
    }

    // ------------------------------------------------------------------

    public QuizResponse createQuiz(CreateQuizRequest request, UUID teacherId) {
        // Admins have full quiz management and are not required to teach the course.
        // Teachers still go through CourseAccessService exactly as before.
        if (!isAdmin()) {
            courseAccessService.verifyTeacherCanManageCourse(teacherId, request.getCourseId());
        }

        Course course = courseRepository.findById(request.getCourseId())
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", request.getCourseId()));
        User teacher = userRepository.findById(teacherId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", teacherId));

        Batch batch = resolveBatchForCourse(request.getBatchId(), course.getId(), teacherId);

        Quiz quiz = new Quiz();
        quiz.setCourse(course);
        quiz.setTeacher(teacher);
        quiz.setBatch(batch);
        quiz.setTitle(request.getTitle());
        quiz.setDescription(request.getDescription());
        quiz.setTimeLimitMinutes(request.getTimeLimitMinutes() != null ? request.getTimeLimitMinutes() : 30);
        quiz.setPassingScore(request.getPassingScore() != null ? request.getPassingScore() : 60);
        // Default DRAFT: a quiz with no questions yet must not be visible to students.
        quiz.setStatus("PUBLISHED".equalsIgnoreCase(request.getStatus()) ? "PUBLISHED" : "DRAFT");
        quiz.setCreatedAt(Instant.now());
        quiz.setUpdatedAt(Instant.now());

        Quiz saved = quizRepository.save(quiz);
        return mapToResponse(saved, null);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getTeacherQuizzes(UUID teacherId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForTeacher(teacherId);
        if (courseIds.isEmpty()) return List.of();

        return quizRepository.findByCourseIdIn(courseIds).stream()
                .map(q -> mapToResponse(q, null))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public QuizResponse getTeacherQuizById(UUID quizId, UUID teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        courseAccessService.verifyTeacherCanManageCourse(teacherId, quiz.getCourse().getId());
        return mapToResponse(quiz, null);
    }

    @Transactional(readOnly = true)
    public List<QuizResponse> getStudentQuizzes(UUID studentId) {
        List<UUID> courseIds = courseAccessService.getAuthorizedCourseIdsForStudent(studentId);
        if (courseIds.isEmpty()) return List.of();

        // Batch-aware and published-only. The previous call returned every quiz on the
        // course, including DRAFTs and other cohorts' quizzes.
        List<Quiz> quizzes = quizRepository.findAccessiblePublishedQuizzes(studentId, courseIds);
        return quizzes.stream()
                .map(q -> {
                    Optional<QuizAttempt> attempt = quizAttemptRepository.findByQuizIdAndStudentId(q.getId(), studentId);
                    return mapToResponse(q, attempt.orElse(null));
                })
                .collect(Collectors.toList());
    }

    // ==================================================================
    // Teacher: question and option management
    // ==================================================================

    /** Adds a question and its options to a quiz the caller is authorized to manage. */
    public TeacherQuizQuestionResponse addQuestion(UUID quizId, QuizQuestionRequest request, UUID teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyTeacherCanManageQuiz(quiz, teacherId);
        validateOptions(request);

        QuizQuestion question = new QuizQuestion();
        question.setQuiz(quiz);
        applyQuestion(question, request);
        QuizQuestion saved = questionRepository.save(question);
        replaceOptions(saved, request);
        return mapQuestionForTeacher(saved);
    }

    /** Updates a question and replaces its option set. */
    public TeacherQuizQuestionResponse updateQuestion(
            UUID quizId, UUID questionId, QuizQuestionRequest request, UUID teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyTeacherCanManageQuiz(quiz, teacherId);
        validateOptions(request);

        QuizQuestion question = loadQuestionOfQuiz(questionId, quizId);
        applyQuestion(question, request);
        question.setUpdatedAt(Instant.now());
        QuizQuestion saved = questionRepository.save(question);
        replaceOptions(saved, request);
        return mapQuestionForTeacher(saved);
    }

    public void deleteQuestion(UUID quizId, UUID questionId, UUID teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyTeacherCanManageQuiz(quiz, teacherId);
        questionRepository.delete(loadQuestionOfQuiz(questionId, quizId));
    }

    /** Teacher view of a quiz's questions, answer key included. */
    @Transactional(readOnly = true)
    public List<TeacherQuizQuestionResponse> getQuestionsForTeacher(UUID quizId, UUID teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyTeacherCanManageQuiz(quiz, teacherId);
        return questionRepository.findByQuizIdOrderBySequenceNumberAsc(quizId).stream()
                .map(this::mapQuestionForTeacher)
                .collect(Collectors.toList());
    }

    /**
     * Publishing is the last point at which a quiz can be rejected, so everything a
     * student depends on is re-checked here rather than trusted from creation time.
     *
     * <p>Questions can be edited after they were first validated, and options are stored
     * separately from their question, so a quiz that was valid when built may not be valid
     * now. Re-reading the persisted rows is the only way to be sure.</p>
     */
    public QuizResponse setPublished(UUID quizId, boolean publish, UUID teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyTeacherCanManageQuiz(quiz, teacherId);

        if (publish) {
            validateQuizIsPublishable(quiz);
        }
        quiz.setStatus(publish ? "PUBLISHED" : "DRAFT");
        quiz.setUpdatedAt(Instant.now());
        return mapToResponse(quizRepository.save(quiz), null);
    }

    /**
     * Every rule a published quiz must satisfy.
     *
     * <p>Messages name the offending question by position, because a teacher looking at a
     * ten-question quiz cannot act on "a question is invalid".</p>
     */
    private void validateQuizIsPublishable(Quiz quiz) {
        List<QuizQuestion> questions =
                questionRepository.findByQuizIdOrderBySequenceNumberAsc(quiz.getId());

        if (questions.isEmpty()) {
            throw new BusinessException("QUIZ_HAS_NO_QUESTIONS",
                    "Add at least one question before publishing.", HttpStatus.CONFLICT);
        }

        for (int i = 0; i < questions.size(); i++) {
            QuizQuestion q = questions.get(i);
            int number = i + 1;

            if (q.getQuestionText() == null || q.getQuestionText().isBlank()) {
                throw new BusinessException("INVALID_QUESTION",
                        "Question " + number + " has no question text.", HttpStatus.CONFLICT);
            }
            if (q.getPoints() == null || q.getPoints() <= 0) {
                throw new BusinessException("INVALID_QUESTION",
                        "Question " + number + " must carry more than zero marks.",
                        HttpStatus.CONFLICT);
            }

            List<QuizOption> options = optionRepository.findByQuestionIdOrderBySequenceNumberAsc(q.getId());
            if (options.size() < 2) {
                throw new BusinessException("INVALID_QUESTION",
                        "Question " + number + " needs at least two options.", HttpStatus.CONFLICT);
            }
            boolean blankOption = options.stream()
                    .anyMatch(o -> o.getOptionText() == null || o.getOptionText().isBlank());
            if (blankOption) {
                throw new BusinessException("INVALID_QUESTION",
                        "Question " + number + " has an empty option.", HttpStatus.CONFLICT);
            }
            long correct = options.stream().filter(QuizOption::isCorrect).count();
            if (correct != 1) {
                throw new BusinessException("INVALID_QUESTION",
                        "Question " + number + " must have exactly one correct option.",
                        HttpStatus.CONFLICT);
            }
        }
    }

    // ==================================================================
    // Student: questions without the answer key
    // ==================================================================

    /**
     * The quiz as a student attempting it should see. Options are mapped through
     * StudentQuizOptionResponse, which has no `correct` field, so the answer key cannot
     * leak even by accident.
     */
    @Transactional(readOnly = true)
    public StudentQuizDetailResponse getStudentQuizDetail(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyStudentCanAccessQuiz(quiz, studentId);

        List<StudentQuizQuestionResponse> questions =
                questionRepository.findByQuizIdOrderBySequenceNumberAsc(quizId).stream()
                        .map(q -> StudentQuizQuestionResponse.builder()
                                .id(q.getId())
                                .questionText(q.getQuestionText())
                                .questionType(q.getQuestionType())
                                .points(q.getPoints())
                                .sequenceNumber(q.getSequenceNumber())
                                .options(optionRepository
                                        .findByQuestionIdOrderBySequenceNumberAsc(q.getId()).stream()
                                        .map(o -> StudentQuizOptionResponse.builder()
                                                .id(o.getId())
                                                .optionText(o.getOptionText())
                                                .sequenceNumber(o.getSequenceNumber())
                                                .build())
                                        .collect(Collectors.toList()))
                                .build())
                        .collect(Collectors.toList());

        Optional<QuizAttempt> attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId);
        return StudentQuizDetailResponse.builder()
                .quiz(mapToResponse(quiz, attempt.orElse(null)))
                .questions(questions)
                .build();
    }

    /** A student's own attempts. Scoped by the authenticated id, never by a request parameter. */
    @Transactional(readOnly = true)
    public List<QuizAttemptResultResponse> getMyAttempts(UUID studentId) {
        return quizAttemptRepository.findByStudentId(studentId).stream()
                .map(a -> buildResult(a, a.getSubmittedAt() != null))
                .collect(Collectors.toList());
    }

    // ==================================================================
    // Teacher: performance
    // ==================================================================

    /** Attempts on one quiz. Only reachable by a teacher authorized for that quiz. */
    @Transactional(readOnly = true)
    public List<TeacherQuizAttemptResponse> getQuizPerformance(UUID quizId, UUID teacherId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyTeacherCanManageQuiz(quiz, teacherId);

        return quizAttemptRepository.findByQuizId(quizId).stream()
                .map(a -> TeacherQuizAttemptResponse.builder()
                        .attemptId(a.getId())
                        .quizId(quiz.getId())
                        .quizTitle(quiz.getTitle())
                        .studentId(a.getStudent().getId())
                        .studentName(a.getStudent().getName())
                        .studentEmail(a.getStudent().getEmail())
                        .score(a.getScore())
                        .passed(Boolean.TRUE.equals(a.getPassed()))
                        .correctAnswers(a.getCorrectAnswers())
                        .totalQuestions(a.getTotalQuestions())
                        .status(a.getStatus())
                        .submittedAt(a.getSubmittedAt())
                        .build())
                .collect(Collectors.toList());
    }

    // ==================================================================
    // Internals
    // ==================================================================

    /** A question id from the client must belong to the quiz in the path. */
    private QuizQuestion loadQuestionOfQuiz(UUID questionId, UUID quizId) {
        QuizQuestion question = questionRepository.findById(questionId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizQuestion", "id", questionId));
        if (question.getQuiz() == null || !question.getQuiz().getId().equals(quizId)) {
            throw new BusinessException("QUESTION_NOT_IN_QUIZ",
                    "That question does not belong to this quiz.", HttpStatus.BAD_REQUEST);
        }
        return question;
    }

    /** A single-choice question needs at least two options and exactly one correct answer. */
    private void validateOptions(QuizQuestionRequest request) {
        List<QuizOptionRequest> options = request.getOptions();
        if (options == null || options.size() < 2) {
            throw new BusinessException("INVALID_OPTIONS",
                    "A question needs at least two options.", HttpStatus.BAD_REQUEST);
        }
        long correct = options.stream().filter(QuizOptionRequest::isCorrect).count();
        if (correct != 1) {
            throw new BusinessException("INVALID_OPTIONS",
                    "Mark exactly one option as correct.", HttpStatus.BAD_REQUEST);
        }
    }

    private void applyQuestion(QuizQuestion question, QuizQuestionRequest request) {
        question.setQuestionText(request.getQuestionText());
        question.setQuestionType(request.getQuestionType() != null
                ? request.getQuestionType() : "SINGLE_CHOICE");
        question.setPoints(request.getPoints() != null ? request.getPoints() : 1);
        question.setSequenceNumber(request.getSequenceNumber() != null ? request.getSequenceNumber() : 0);
        question.setExplanation(request.getExplanation());
    }

    /** Options are replaced wholesale so the stored set always matches what the teacher sent. */
    private void replaceOptions(QuizQuestion question, QuizQuestionRequest request) {
        optionRepository.deleteAll(
                optionRepository.findByQuestionIdOrderBySequenceNumberAsc(question.getId()));
        int seq = 0;
        for (QuizOptionRequest o : request.getOptions()) {
            QuizOption option = new QuizOption();
            option.setQuestion(question);
            option.setOptionText(o.getOptionText());
            option.setCorrect(o.isCorrect());
            option.setSequenceNumber(o.getSequenceNumber() != null ? o.getSequenceNumber() : seq);
            optionRepository.save(option);
            seq++;
        }
    }

    private TeacherQuizQuestionResponse mapQuestionForTeacher(QuizQuestion q) {
        return TeacherQuizQuestionResponse.builder()
                .id(q.getId())
                .questionText(q.getQuestionText())
                .questionType(q.getQuestionType())
                .points(q.getPoints())
                .sequenceNumber(q.getSequenceNumber())
                .explanation(q.getExplanation())
                .options(optionRepository.findByQuestionIdOrderBySequenceNumberAsc(q.getId()).stream()
                        .map(o -> TeacherQuizOptionResponse.builder()
                                .id(o.getId())
                                .optionText(o.getOptionText())
                                .correct(o.isCorrect())
                                .sequenceNumber(o.getSequenceNumber())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }

    @Transactional(readOnly = true)
    public QuizResponse getStudentQuizById(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, quiz.getCourse().getId());

        Optional<QuizAttempt> attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId);
        return mapToResponse(quiz, attempt.orElse(null));
    }

    public QuizAttemptResponse attemptQuiz(UUID quizId, UUID studentId, QuizAttemptRequest request) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, quiz.getCourse().getId());

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        // Legacy shim. This endpoint used to take the score straight from the request body,
        // which let a student post {"score":100} and pass anything. It now ignores any score
        // in the payload and marks the attempt server-side, exactly like submitAttempt.
        SubmitQuizAttemptRequest converted = new SubmitQuizAttemptRequest();
        converted.setAnswers(List.of());
        QuizAttemptResultResponse result = submitAttempt(quizId, studentId, converted);

        QuizAttempt saved = quizAttemptRepository.findById(result.getAttemptId())
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt", "id", result.getAttemptId()));
        return mapToAttemptResponse(saved);
    }

    /**
     * Starts (or resumes) an attempt. Refuses a quiz the student has already submitted.
     */
    public QuizAttemptResultResponse startAttempt(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyStudentCanAccessQuiz(quiz, studentId);

        QuizAttempt existing = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId)
                .orElse(null);
        if (existing != null && existing.getSubmittedAt() != null) {
            throw new BusinessException("ATTEMPT_ALREADY_SUBMITTED",
                    "You have already submitted this quiz.", HttpStatus.CONFLICT);
        }
        if (existing != null) {
            return buildResult(existing, false);
        }

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        QuizAttempt attempt = new QuizAttempt();
        attempt.setQuiz(quiz);
        attempt.setStudent(student);
        attempt.setStartedAt(Instant.now());
        attempt.setStatus("IN_PROGRESS");
        attempt.setTotalQuestions((int) questionRepository.countByQuizId(quizId));
        attempt.setCorrectAnswers(0);
        attempt.setScore(0);
        attempt.setPassed(false);
        return buildResult(quizAttemptRepository.save(attempt), false);
    }

    /**
     * Marks a submission entirely server-side.
     *
     * <p>The request carries only questionId/selectedOptionId pairs. Correctness comes from
     * QuizOption.isCorrect, which never leaves the server, so nothing the client sends can
     * influence the score. Every id is re-checked against this quiz: a question must belong
     * to the quiz and an option must belong to that question, which blocks cross-quiz and
     * cross-question option injection.</p>
     */
    public QuizAttemptResultResponse submitAttempt(
            UUID quizId, UUID studentId, SubmitQuizAttemptRequest request) {

        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));
        verifyStudentCanAccessQuiz(quiz, studentId);

        User student = userRepository.findById(studentId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", studentId));

        QuizAttempt attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId)
                .orElseGet(() -> {
                    QuizAttempt a = new QuizAttempt();
                    a.setQuiz(quiz);
                    a.setStudent(student);
                    a.setStartedAt(Instant.now());
                    return a;
                });

        // Ownership: the attempt row must belong to the caller.
        if (attempt.getStudent() != null && !attempt.getStudent().getId().equals(studentId)) {
            throw new CourseAccessDeniedException("This attempt does not belong to you.");
        }
        if (attempt.getSubmittedAt() != null) {
            throw new BusinessException("ATTEMPT_ALREADY_SUBMITTED",
                    "You have already submitted this quiz.", HttpStatus.CONFLICT);
        }

        List<QuizQuestion> questions = questionRepository.findByQuizIdOrderBySequenceNumberAsc(quizId);
        if (questions.isEmpty()) {
            throw new BusinessException("QUIZ_HAS_NO_QUESTIONS",
                    "This quiz has no questions yet.", HttpStatus.CONFLICT);
        }

        // Index submitted answers by question. Anything not part of this quiz is ignored.
        Map<UUID, UUID> submitted = new HashMap<>();
        if (request != null && request.getAnswers() != null) {
            for (SubmitQuizAttemptRequest.Answer a : request.getAnswers()) {
                if (a == null || a.getQuestionId() == null) continue;
                // Last write wins; the DB unique constraint also guarantees one row per question.
                submitted.put(a.getQuestionId(), a.getSelectedOptionId());
            }
        }

        QuizAttempt persisted = quizAttemptRepository.save(attempt);
        attemptAnswerRepository.deleteAll(attemptAnswerRepository.findByAttemptId(persisted.getId()));

        int totalPoints = 0;
        int earnedPoints = 0;
        int correctCount = 0;

        for (QuizQuestion question : questions) {
            int worth = question.getPoints() != null ? question.getPoints() : 1;
            totalPoints += worth;

            UUID chosenId = submitted.get(question.getId());
            QuizOption chosen = null;
            if (chosenId != null) {
                // The option must belong to THIS question. An id from another question or
                // another quiz is rejected rather than silently accepted.
                chosen = optionRepository.findById(chosenId)
                        .filter(o -> o.getQuestion() != null
                                && o.getQuestion().getId().equals(question.getId()))
                        .orElseThrow(() -> new BusinessException("INVALID_OPTION",
                                "An answer referenced an option that does not belong to its question.",
                                HttpStatus.BAD_REQUEST));
            }

            boolean isCorrect = chosen != null && chosen.isCorrect();
            if (isCorrect) {
                correctCount++;
                earnedPoints += worth;
            }

            QuizAttemptAnswer answer = new QuizAttemptAnswer();
            answer.setAttempt(persisted);
            answer.setQuestion(question);
            answer.setSelectedOption(chosen);
            answer.setCorrect(isCorrect);
            answer.setPointsAwarded(isCorrect ? worth : 0);
            answer.setAnsweredAt(Instant.now());
            attemptAnswerRepository.save(answer);
        }

        int score = totalPoints == 0 ? 0 : (int) Math.round((earnedPoints * 100.0) / totalPoints);
        int passingScore = quiz.getPassingScore() != null ? quiz.getPassingScore() : 60;

        persisted.setScore(score);
        persisted.setPassed(score >= passingScore);
        persisted.setTotalQuestions(questions.size());
        persisted.setCorrectAnswers(correctCount);
        persisted.setSubmittedAt(Instant.now());
        persisted.setStatus("COMPLETED");

        return buildResult(quizAttemptRepository.save(persisted), true);
    }

    /** Builds the result view. Answer detail is attached only once the attempt is submitted. */
    private QuizAttemptResultResponse buildResult(QuizAttempt attempt, boolean includeAnswers) {
        List<QuizAnswerResultResponse> answers = List.of();
        if (includeAnswers && attempt.getSubmittedAt() != null) {
            answers = attemptAnswerRepository.findByAttemptId(attempt.getId()).stream()
                    .map(ans -> {
                        UUID correctId = optionRepository
                                .findByQuestionIdAndCorrectTrue(ans.getQuestion().getId()).stream()
                                .findFirst().map(QuizOption::getId).orElse(null);
                        return QuizAnswerResultResponse.builder()
                                .questionId(ans.getQuestion().getId())
                                .questionText(ans.getQuestion().getQuestionText())
                                .selectedOptionId(ans.getSelectedOption() != null
                                        ? ans.getSelectedOption().getId() : null)
                                .correctOptionId(correctId)
                                .correct(ans.isCorrect())
                                .pointsAwarded(ans.getPointsAwarded())
                                .explanation(ans.getQuestion().getExplanation())
                                .build();
                    })
                    .collect(Collectors.toList());
        }

        return QuizAttemptResultResponse.builder()
                .attemptId(attempt.getId())
                .quizId(attempt.getQuiz().getId())
                .quizTitle(attempt.getQuiz().getTitle())
                .score(attempt.getScore())
                .passingScore(attempt.getQuiz().getPassingScore())
                .passed(Boolean.TRUE.equals(attempt.getPassed()))
                .totalQuestions(attempt.getTotalQuestions())
                .correctAnswers(attempt.getCorrectAnswers())
                .status(attempt.getStatus())
                .startedAt(attempt.getStartedAt())
                .submittedAt(attempt.getSubmittedAt())
                .answers(answers)
                .build();
    }

    @Transactional(readOnly = true)
    public QuizAttemptResponse getStudentQuizResults(UUID quizId, UUID studentId) {
        Quiz quiz = quizRepository.findById(quizId)
                .orElseThrow(() -> new ResourceNotFoundException("Quiz", "id", quizId));

        courseAccessService.verifyStudentCanAccessCourse(studentId, quiz.getCourse().getId());

        QuizAttempt attempt = quizAttemptRepository.findByQuizIdAndStudentId(quizId, studentId)
                .orElseThrow(() -> new ResourceNotFoundException("QuizAttempt", "quizId", quizId));

        return mapToAttemptResponse(attempt);
    }

    private QuizResponse mapToResponse(Quiz quiz, QuizAttempt attempt) {
        String teacherName = quiz.getTeacher() != null
                ? quiz.getTeacher().getName()
                : (quiz.getCourse().getInstructor() != null ? quiz.getCourse().getInstructor().getName() : "Instructor");

        return QuizResponse.builder()
                .id(quiz.getId())
                .batchId(quiz.getBatch() != null ? quiz.getBatch().getId() : null)
                .batchName(quiz.getBatch() != null ? quiz.getBatch().getName() : null)
                .questionCount((int) questionRepository.countByQuizId(quiz.getId()))
                .courseId(quiz.getCourse().getId())
                .courseTitle(quiz.getCourse().getTitle())
                .teacherId(quiz.getTeacher() != null ? quiz.getTeacher().getId() : null)
                .teacherName(teacherName)
                .title(quiz.getTitle())
                .description(quiz.getDescription())
                .timeLimitMinutes(quiz.getTimeLimitMinutes())
                .passingScore(quiz.getPassingScore())
                .status(quiz.getStatus())
                .createdAt(quiz.getCreatedAt())
                // An IN_PROGRESS row means the student started the quiz, not that they
                // submitted it. Treating every row as "attempted" made the frontend show
                // the result screen (0%, failed) and prevented the student from resuming.
                .attempted(attempt != null && attempt.getSubmittedAt() != null)
                .score(attempt != null ? attempt.getScore() : null)
                .attemptStatus(attempt == null ? "NOT_ATTEMPTED"
                        : attempt.getSubmittedAt() == null ? "IN_PROGRESS"
                        : Boolean.TRUE.equals(attempt.getPassed()) ? "PASSED" : "FAILED")
                .build();
    }

    private QuizAttemptResponse mapToAttemptResponse(QuizAttempt a) {
        return QuizAttemptResponse.builder()
                .id(a.getId())
                .quizId(a.getQuiz().getId())
                .quizTitle(a.getQuiz().getTitle())
                .studentId(a.getStudent().getId())
                .studentName(a.getStudent().getName())
                .score(a.getScore())
                .passingScore(a.getQuiz().getPassingScore())
                .passed(Boolean.TRUE.equals(a.getPassed()))
                .status(a.getStatus())
                .startedAt(a.getStartedAt())
                .submittedAt(a.getSubmittedAt())
                .build();
    }
}
