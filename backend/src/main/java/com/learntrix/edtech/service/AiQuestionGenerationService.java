package com.learntrix.edtech.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learntrix.edtech.ai.AiProvider;
import com.learntrix.edtech.common.exception.BusinessException;
import com.learntrix.edtech.common.exception.CourseAccessDeniedException;
import com.learntrix.edtech.common.exception.ResourceNotFoundException;
import com.learntrix.edtech.common.util.SecurityUtil;
import com.learntrix.edtech.dto.ai.*;
import com.learntrix.edtech.entity.Batch;
import com.learntrix.edtech.entity.Course;
import com.learntrix.edtech.repository.BatchRepository;
import com.learntrix.edtech.repository.CourseRepository;

/**
 * Turns a teacher's request into reviewed-but-unsaved question drafts.
 *
 * <p>Three rules shape this class:</p>
 * <ul>
 *   <li><b>Identity is never taken from the client.</b> The teacher id is passed in from the
 *       token; authorization reuses {@link CourseAccessService}, the same authority quizzes
 *       and coding problems use, so the assistant cannot become a second access model.</li>
 *   <li><b>Nothing is persisted.</b> Drafts go back to the browser. Saving happens later,
 *       through the existing quiz and coding APIs, which validate again.</li>
 *   <li><b>Gemini is not trusted.</b> A schema-conformant reply can still be wrong - a
 *       correct-option index out of range, duplicate distractors, a sample output that
 *       contradicts the description. Everything is re-checked below.</li>
 * </ul>
 */
@Service
public class AiQuestionGenerationService {

    private static final Set<String> DIFFICULTIES = Set.of("EASY", "MEDIUM", "HARD");
    private static final int OPTIONS_PER_QUESTION = 4;

    private final AiProvider aiProvider;
    private final CourseAccessService courseAccessService;
    private final CourseRepository courseRepository;
    private final BatchRepository batchRepository;
    private final ObjectMapper objectMapper;

    public AiQuestionGenerationService(
            AiProvider aiProvider,
            CourseAccessService courseAccessService,
            CourseRepository courseRepository,
            BatchRepository batchRepository,
            ObjectMapper objectMapper) {
        this.aiProvider = aiProvider;
        this.courseAccessService = courseAccessService;
        this.courseRepository = courseRepository;
        this.batchRepository = batchRepository;
        this.objectMapper = objectMapper;
    }

    private boolean isAdmin() {
        return SecurityUtil.hasRole("ADMIN") || SecurityUtil.hasRole("SUPER_ADMIN");
    }

    // ==================================================================
    // Entry point
    // ==================================================================

    @Transactional(readOnly = true)
    public AiGenerationResponse generate(AiGenerationRequest request, UUID teacherId) {
        Course course = authorizeCourse(request.getCourseId(), teacherId);
        Batch batch = authorizeBatch(request.getBatchId(), course, teacherId);

        String type = request.getType() == null ? "" : request.getType().toUpperCase();
        String difficulty = normaliseDifficulty(request.getDifficulty());
        int count = request.getCount() == null ? 5 : request.getCount();

        AiGenerationResponse.AiGenerationResponseBuilder response = AiGenerationResponse.builder()
                .type(type)
                .courseId(course.getId())
                .courseTitle(course.getTitle())
                .batchId(batch != null ? batch.getId() : null)
                .batchName(batch != null ? batch.getName() : null);

        return switch (type) {
            case "QUIZ" -> generateQuiz(request, difficulty, count, response);
            case "CODING" -> generateCoding(request, difficulty, count, response);
            case "ASSIGNMENT" -> generateAssignments(request, difficulty, count, response);
            default -> throw new BusinessException("INVALID_CONTENT_TYPE",
                    "Content type must be QUIZ, CODING or ASSIGNMENT.", HttpStatus.BAD_REQUEST);
        };
    }

    // ==================================================================
    // Authorization - identical rules to quizzes and coding problems
    // ==================================================================

    private Course authorizeCourse(UUID courseId, UUID teacherId) {
        if (!isAdmin()) {
            courseAccessService.verifyTeacherCanManageCourse(teacherId, courseId);
        }
        return courseRepository.findById(courseId)
                .orElseThrow(() -> new ResourceNotFoundException("Course", "id", courseId));
    }

    /**
     * A batch must belong to the named course AND be taught by this teacher.
     *
     * <p>The second half is what keeps Morning and Evening apart: managing the course is
     * not enough to generate for a cohort someone else runs.</p>
     */
    private Batch authorizeBatch(UUID batchId, Course course, UUID teacherId) {
        if (batchId == null) return null;
        Batch batch = batchRepository.findById(batchId)
                .orElseThrow(() -> new ResourceNotFoundException("Batch", "id", batchId));
        if (batch.getCourse() == null || !batch.getCourse().getId().equals(course.getId())) {
            throw new BusinessException("BATCH_COURSE_MISMATCH",
                    "That batch does not belong to the selected course.", HttpStatus.BAD_REQUEST);
        }
        if (!isAdmin()
                && (batch.getTeacher() == null || !batch.getTeacher().getId().equals(teacherId))) {
            throw new CourseAccessDeniedException(
                    "You are not authorized to generate content for that batch");
        }
        return batch;
    }

    private String normaliseDifficulty(String raw) {
        String d = raw == null ? "MEDIUM" : raw.toUpperCase();
        return DIFFICULTIES.contains(d) ? d : "MEDIUM";
    }

    // ==================================================================
    // Quiz generation
    // ==================================================================

    private AiGenerationResponse generateQuiz(
            AiGenerationRequest request, String difficulty, int count,
            AiGenerationResponse.AiGenerationResponseBuilder response) {

        String raw = aiProvider.generateJson(
                quizSystemInstruction(), quizPrompt(request, difficulty, count), quizSchema());

        List<AiQuizQuestionDraft> drafts = parseQuizDrafts(raw);

        List<String> warnings = new ArrayList<>();
        List<AiQuizQuestionDraft> valid = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            String problem = quizDraftProblem(drafts.get(i));
            if (problem == null) {
                valid.add(drafts.get(i));
            } else {
                // Drop the bad one and say so, rather than failing the whole batch: eight
                // good questions are more useful to a teacher than an error.
                warnings.add("Discarded generated question " + (i + 1) + ": " + problem);
            }
        }

        if (valid.isEmpty()) {
            throw new BusinessException("AI_INVALID_OUTPUT",
                    "The AI returned no usable questions. Please try again or rephrase the topic.",
                    HttpStatus.BAD_GATEWAY);
        }
        if (valid.size() < count) {
            warnings.add("Asked for " + count + " questions, kept " + valid.size() + ".");
        }

        return response.questions(valid).warnings(warnings).build();
    }

    private List<AiQuizQuestionDraft> parseQuizDrafts(String raw) {
        try {
            Map<String, Object> root = objectMapper.readValue(raw, new TypeReference<>() {});
            Object questions = root.get("questions");
            if (questions == null) throw new IllegalStateException("no questions key");
            return objectMapper.convertValue(questions, new TypeReference<List<AiQuizQuestionDraft>>() {});
        } catch (Exception e) {
            // Schema-constrained output should not land here, but "should not" is not
            // "cannot" - a truncated reply is still malformed JSON.
            throw new BusinessException("AI_MALFORMED_RESPONSE",
                    "The AI returned a response this server could not read. Please try again.",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    /** @return null when the draft is usable, otherwise why it was rejected. */
    private String quizDraftProblem(AiQuizQuestionDraft q) {
        if (q == null) return "empty question";
        if (isBlank(q.getQuestion())) return "question text is empty";

        List<String> options = q.getOptions();
        if (options == null || options.size() != OPTIONS_PER_QUESTION) {
            return "expected " + OPTIONS_PER_QUESTION + " options, got "
                    + (options == null ? 0 : options.size());
        }
        if (options.stream().anyMatch(AiQuestionGenerationService::isBlank)) {
            return "an option is empty";
        }
        // Case- and whitespace-insensitive, because "True" and "true " are the same
        // distractor to a student even though they differ as strings.
        Set<String> seen = new HashSet<>();
        for (String o : options) {
            if (!seen.add(o.trim().toLowerCase())) return "duplicate options";
        }

        Integer idx = q.getCorrectOption();
        if (idx == null || idx < 0 || idx >= options.size()) {
            return "correct option index is out of range";
        }
        if (q.getMarks() == null || q.getMarks() <= 0 || q.getMarks() > 100) {
            return "marks are out of range";
        }
        if (q.getDifficulty() == null || !DIFFICULTIES.contains(q.getDifficulty().toUpperCase())) {
            return "difficulty is not EASY, MEDIUM or HARD";
        }
        if (isBlank(q.getExplanation())) return "explanation is empty";
        return null;
    }

    // ==================================================================
    // Coding generation
    // ==================================================================

    private AiGenerationResponse generateCoding(
            AiGenerationRequest request, String difficulty, int count,
            AiGenerationResponse.AiGenerationResponseBuilder response) {

        String raw = aiProvider.generateJson(
                codingSystemInstruction(), codingPrompt(request, difficulty, count), codingSchema());

        List<AiCodingProblemDraft> drafts = parseCodingDrafts(raw);

        List<String> warnings = new ArrayList<>();
        List<AiCodingProblemDraft> valid = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            String problem = codingDraftProblem(drafts.get(i));
            if (problem == null) {
                valid.add(drafts.get(i));
            } else {
                warnings.add("Discarded generated problem " + (i + 1) + ": " + problem);
            }
        }

        if (valid.isEmpty()) {
            throw new BusinessException("AI_INVALID_OUTPUT",
                    "The AI returned no usable problems. Please try again or rephrase the topic.",
                    HttpStatus.BAD_GATEWAY);
        }
        warnings.add("Sample outputs have not been executed. Verify them before publishing.");

        return response.problems(valid).warnings(warnings).build();
    }

    private List<AiCodingProblemDraft> parseCodingDrafts(String raw) {
        try {
            Map<String, Object> root = objectMapper.readValue(raw, new TypeReference<>() {});
            Object problems = root.get("problems");
            if (problems == null) throw new IllegalStateException("no problems key");
            return objectMapper.convertValue(problems, new TypeReference<List<AiCodingProblemDraft>>() {});
        } catch (Exception e) {
            throw new BusinessException("AI_MALFORMED_RESPONSE",
                    "The AI returned a response this server could not read. Please try again.",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    private String codingDraftProblem(AiCodingProblemDraft p) {
        if (p == null) return "empty problem";
        if (isBlank(p.getTitle())) return "title is empty";
        if (isBlank(p.getDescription())) return "description is empty";
        if (p.getDifficulty() == null || !DIFFICULTIES.contains(p.getDifficulty().toUpperCase())) {
            return "difficulty is not EASY, MEDIUM or HARD";
        }

        List<AiCodingTestCaseDraft> cases = p.getTestCases();
        if (cases == null || cases.isEmpty()) {
            // The existing publish rule requires at least one case; a problem that cannot
            // clear it is not worth showing the teacher.
            return "no test cases were generated";
        }
        for (int i = 0; i < cases.size(); i++) {
            AiCodingTestCaseDraft tc = cases.get(i);
            if (tc == null || isBlank(tc.getExpectedOutput())) {
                return "test case " + (i + 1) + " has no expected output";
            }
        }
        if (cases.stream().noneMatch(AiCodingTestCaseDraft::isSample)) {
            // Without a sample the student has nothing to Run against, so promote the first.
            cases.get(0).setSample(true);
        }
        return null;
    }

    // ==================================================================
    // Prompts
    // ==================================================================

    private String quizSystemInstruction() {
        return """
            You are an experienced computer-science examiner writing multiple-choice questions \
            for a technical training institute.

            Rules you must follow:
            - Every question must be factually correct and unambiguous.
            - Match the requested difficulty honestly. EASY = recall, MEDIUM = application, \
              HARD = analysis or subtle edge cases.
            - Provide exactly four options.
            - Exactly one option is correct. correctOption is its ZERO-BASED index.
            - Distractors must be plausible to someone who half-knows the topic, never filler.
            - No two options may be the same or trivially reworded.
            - The explanation must say why the correct answer is right, in one or two sentences.

            DO NOT return Markdown.
            DO NOT return explanatory text outside the schema.
            RETURN ONLY the requested structured object.
            """;
    }

    private String quizPrompt(AiGenerationRequest request, String difficulty, int count) {
        StringBuilder p = new StringBuilder();
        p.append("Generate ").append(count).append(' ').append(difficulty)
                .append(" difficulty multiple-choice questions about: ")
                .append(request.getTopic()).append('.');
        if (!isBlank(request.getInstructions())) {
            p.append("\n\nAdditional instructions from the teacher: ")
                    .append(request.getInstructions());
        }
        appendRegeneration(p, request, "questions");
        if (request.getExistingQuestions() != null && !request.getExistingQuestions().isEmpty()) {
            p.append("\n\nThe questions currently on screen are:\n")
                    .append(safeJson(request.getExistingQuestions()));
        }
        return p.toString();
    }

    private String codingSystemInstruction() {
        return """
            You are an experienced competitive-programming problem setter writing practice \
            problems for a technical training institute. Problems are judged by running the \
            submitted program against stdin and comparing stdout exactly.

            Rules you must follow:
            - The description must state the input format and the output format precisely.
            - sampleInput and sampleOutput must be consistent with the description, and \
              sampleOutput must be the genuinely correct output for sampleInput.
            - Test case input is fed to the program on stdin exactly as written.
            - expectedOutput must match the program's stdout exactly, with no extra prose.
            - Provide at least one test case with sample=true (shown to the student) and at \
              least two with sample=false (hidden, used only when judging).
            - Hidden cases must cover edge cases the sample does not.
            - Constraints must be consistent with the test cases you provide.

            DO NOT return Markdown.
            DO NOT return explanatory text outside the schema.
            RETURN ONLY the requested structured object.
            """;
    }

    private String codingPrompt(AiGenerationRequest request, String difficulty, int count) {
        StringBuilder p = new StringBuilder();
        p.append("Generate ").append(count).append(' ').append(difficulty)
                .append(" difficulty coding problem(s) about: ").append(request.getTopic())
                .append(".\nTarget language: ")
                .append(isBlank(request.getLanguage()) ? "JAVA" : request.getLanguage().toUpperCase())
                .append('.');
        if (!isBlank(request.getInstructions())) {
            p.append("\n\nAdditional instructions from the teacher: ")
                    .append(request.getInstructions());
        }
        appendRegeneration(p, request, "problems");
        if (request.getExistingProblems() != null && !request.getExistingProblems().isEmpty()) {
            p.append("\n\nThe problems currently on screen are:\n")
                    .append(safeJson(request.getExistingProblems()));
        }
        return p.toString();
    }

    private void appendRegeneration(StringBuilder p, AiGenerationRequest request, String noun) {
        if (!isBlank(request.getRegenerateInstruction())) {
            p.append("\n\nThis is a revision request. Apply this change to the ").append(noun)
                    .append(": ").append(request.getRegenerateInstruction());
        }
    }

    /** Serialises review context for the prompt; a failure here must not break generation. */
    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    // ==================================================================
    // Response schemas
    // ==================================================================

    private Map<String, Object> quizSchema() {
        Map<String, Object> question = new LinkedHashMap<>();
        question.put("type", "OBJECT");
        question.put("properties", new LinkedHashMap<>(Map.of(
                "question", Map.of("type", "STRING"),
                "options", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                "correctOption", Map.of("type", "INTEGER"),
                "marks", Map.of("type", "INTEGER"),
                "difficulty", Map.of("type", "STRING", "enum", List.of("EASY", "MEDIUM", "HARD")),
                "explanation", Map.of("type", "STRING"))));
        question.put("required",
                List.of("question", "options", "correctOption", "marks", "difficulty", "explanation"));

        return Map.of(
                "type", "OBJECT",
                "properties", Map.of("questions", Map.of("type", "ARRAY", "items", question)),
                "required", List.of("questions"));
    }

    private Map<String, Object> codingSchema() {
        Map<String, Object> testCase = new LinkedHashMap<>();
        testCase.put("type", "OBJECT");
        testCase.put("properties", new LinkedHashMap<>(Map.of(
                "input", Map.of("type", "STRING"),
                "expectedOutput", Map.of("type", "STRING"),
                "sample", Map.of("type", "BOOLEAN"))));
        testCase.put("required", List.of("input", "expectedOutput", "sample"));

        Map<String, Object> problem = new LinkedHashMap<>();
        problem.put("type", "OBJECT");
        problem.put("properties", new LinkedHashMap<>(Map.of(
                "title", Map.of("type", "STRING"),
                "description", Map.of("type", "STRING"),
                "language", Map.of("type", "STRING"),
                "difficulty", Map.of("type", "STRING", "enum", List.of("EASY", "MEDIUM", "HARD")),
                "constraints", Map.of("type", "ARRAY", "items", Map.of("type", "STRING")),
                "sampleInput", Map.of("type", "STRING"),
                "sampleOutput", Map.of("type", "STRING"),
                "testCases", Map.of("type", "ARRAY", "items", testCase))));
        problem.put("required", List.of("title", "description", "language", "difficulty",
                "sampleInput", "sampleOutput", "testCases"));

        return Map.of(
                "type", "OBJECT",
                "properties", Map.of("problems", Map.of("type", "ARRAY", "items", problem)),
                "required", List.of("problems"));
    }


    // ==================================================================
    // Assignment generation
    // ==================================================================

    private AiGenerationResponse generateAssignments(
            AiGenerationRequest request, String difficulty, int count,
            AiGenerationResponse.AiGenerationResponseBuilder response) {

        String raw = aiProvider.generateJson(
                assignmentSystemInstruction(), assignmentPrompt(request, difficulty, count),
                assignmentSchema());

        List<AiAssignmentDraft> drafts = parseAssignmentDrafts(raw);

        List<String> warnings = new ArrayList<>();
        List<AiAssignmentDraft> valid = new ArrayList<>();
        for (int i = 0; i < drafts.size(); i++) {
            String problem = assignmentDraftProblem(drafts.get(i));
            if (problem == null) {
                valid.add(drafts.get(i));
            } else {
                warnings.add("Discarded generated assignment " + (i + 1) + ": " + problem);
            }
        }

        if (valid.isEmpty()) {
            throw new BusinessException("AI_INVALID_OUTPUT",
                    "The AI returned no usable assignments. Please try again or rephrase the topic.",
                    HttpStatus.BAD_GATEWAY);
        }

        return response.assignments(valid).warnings(warnings).build();
    }

    private List<AiAssignmentDraft> parseAssignmentDrafts(String raw) {
        try {
            Map<String, Object> root = objectMapper.readValue(raw, new TypeReference<>() {});
            Object items = root.get("assignments");
            if (items == null) throw new IllegalStateException("no assignments key");
            return objectMapper.convertValue(items, new TypeReference<List<AiAssignmentDraft>>() {});
        } catch (Exception e) {
            throw new BusinessException("AI_MALFORMED_RESPONSE",
                    "The AI returned a response this server could not read. Please try again.",
                    HttpStatus.BAD_GATEWAY);
        }
    }

    /** @return null when usable, otherwise why it was rejected. */
    private String assignmentDraftProblem(AiAssignmentDraft a) {
        if (a == null) return "empty assignment";
        if (isBlank(a.getTitle())) return "title is empty";
        if (isBlank(a.getDescription())) return "instructions are empty";
        if (a.getPoints() == null || a.getPoints() <= 0 || a.getPoints() > 1000) {
            return "marks are out of range";
        }
        if (a.getDifficulty() == null || !DIFFICULTIES.contains(a.getDifficulty().toUpperCase())) {
            return "difficulty is not EASY, MEDIUM or HARD";
        }
        // A due date in the past would be unusable the moment it was created.
        if (a.getSuggestedDueInDays() != null
                && (a.getSuggestedDueInDays() < 1 || a.getSuggestedDueInDays() > 90)) {
            return "suggested due date is not between 1 and 90 days";
        }
        return null;
    }

    private String assignmentSystemInstruction() {
        return """
            You are an experienced computer-science trainer writing practical assignment \
            briefs for a technical training institute.

            Rules you must follow:
            - The brief must state exactly what the student has to build or produce.
            - State how the work should be submitted: a repository link or a hosted link.
            - Match the requested difficulty honestly.
            - Scope the work to what a student can finish in the suggested number of days.
            - Do not invent grading rubrics the platform cannot evaluate automatically.
            - Marks must be a whole number a trainer would plausibly use.

            DO NOT return Markdown.
            DO NOT return explanatory text outside the schema.
            RETURN ONLY the requested structured object.
            """;
    }

    private String assignmentPrompt(AiGenerationRequest request, String difficulty, int count) {
        StringBuilder p = new StringBuilder();
        p.append("Generate ").append(count).append(' ').append(difficulty)
                .append(" difficulty assignment brief(s) about: ").append(request.getTopic())
                .append('.');
        if (!isBlank(request.getInstructions())) {
            p.append("\n\nAdditional instructions from the teacher: ")
                    .append(request.getInstructions());
        }
        appendRegeneration(p, request, "assignments");
        if (request.getExistingAssignments() != null && !request.getExistingAssignments().isEmpty()) {
            p.append("\n\nThe assignments currently on screen are:\n")
                    .append(safeJson(request.getExistingAssignments()));
        }
        return p.toString();
    }

    private Map<String, Object> assignmentSchema() {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("type", "OBJECT");
        item.put("properties", new LinkedHashMap<>(Map.of(
                "title", Map.of("type", "STRING"),
                "description", Map.of("type", "STRING"),
                "points", Map.of("type", "INTEGER"),
                "difficulty", Map.of("type", "STRING", "enum", List.of("EASY", "MEDIUM", "HARD")),
                "suggestedDueInDays", Map.of("type", "INTEGER"))));
        item.put("required", List.of("title", "description", "points", "difficulty"));

        return Map.of(
                "type", "OBJECT",
                "properties", Map.of("assignments", Map.of("type", "ARRAY", "items", item)),
                "required", List.of("assignments"));
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
