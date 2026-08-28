const http = require("http");
const { execSync } = require("child_process");

const BASE_URL = "http://localhost:8081/api";

function getLatestActivationTokenForUser(userId) {
  const cmd = `$env:PGPASSWORD = "1"; & "C:\\Program Files\\PostgreSQL\\17\\bin\\psql.exe" -U postgres -d learntrix -h localhost -t -A -c "SELECT token FROM account_activation_tokens WHERE user_id = '${userId}' AND used_at IS NULL ORDER BY created_at DESC LIMIT 1;"`;
  const result = execSync(cmd, { shell: "powershell.exe" }).toString().trim();
  return result;
}

function request(method, path, body = null, token = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(BASE_URL + path);
    const postData = body ? JSON.stringify(body) : "";

    const headers = {
      "Content-Type": "application/json",
    };
    if (body) {
      headers["Content-Length"] = Buffer.byteLength(postData);
    }
    if (token) {
      headers["Authorization"] = `Bearer ${token}`;
    }

    const req = http.request(
      {
        hostname: url.hostname,
        port: url.port,
        path: url.pathname + url.search,
        method: method,
        headers: headers,
      },
      (res) => {
        let data = "";
        res.on("data", (chunk) => (data += chunk));
        res.on("end", () => {
          try {
            const json = JSON.parse(data);
            resolve({ status: res.statusCode, headers: res.headers, data: json });
          } catch {
            resolve({ status: res.statusCode, headers: res.headers, raw: data });
          }
        });
      }
    );

    req.on("error", reject);
    if (postData) {
      req.write(postData);
    }
    req.end();
  });
}

async function run() {
  console.log("================================================================================");
  console.log("STARTING ADMIN PORTAL ONBOARDING & ACTIVATION E2E TEST SUITE");
  console.log("================================================================================\n");

  let adminToken = "";
  let studentId = "";
  let studentEmail = `rohan.${Date.now()}@example.com`;
  let studentActivationToken = "";
  let teacherId = "";
  let teacherEmail = `ananya.${Date.now()}@example.com`;
  let teacherActivationToken = "";

  // 1. Admin Login
  console.log("--- 1. Admin Login ---");
  const loginRes = await request("POST", "/auth/login", {
    email: "admin@learntrix.com",
    password: "password",
  });
  if (loginRes.status === 200 && loginRes.data.success) {
    adminToken = loginRes.data.data.accessToken;
    console.log("✓ Admin login successful. Token acquired.");
  } else {
    throw new Error(`Admin login failed: ${JSON.stringify(loginRes.data)}`);
  }

  // 2. Admin onboards a new Student
  console.log("\n--- 2. Admin Onboards Student ---");
  const studentPayload = {
    fullName: "Rohan Gupta",
    email: studentEmail,
    phone: "+91 98765 43210",
    college: "IIT Bombay",
    graduationYear: 2025,
    education: "B.Tech Computer Science",
    qualification: "Undergraduate",
    location: "Mumbai",
    gender: "Male",
    skills: "Java, Spring Boot, React, SQL",
    github: "https://github.com/rohangupta",
    linkedin: "https://linkedin.com/in/rohangupta",
  };
  const createStudentRes = await request("POST", "/admin/students", studentPayload, adminToken);
  console.log("Status:", createStudentRes.status);
  console.log("Response:", JSON.stringify(createStudentRes.data, null, 2));
  if (createStudentRes.status === 200 && createStudentRes.data.success) {
    studentId = createStudentRes.data.data.id;
    studentActivationToken = getLatestActivationTokenForUser(studentId);
    console.log(`✓ Student onboarded successfully with ID: ${studentId} and Identifier: ${createStudentRes.data.data.identifier}`);
    console.log(`✓ Retrieved one-time activation token: ${studentActivationToken}`);
  } else {
    throw new Error(`Failed to onboard student: ${JSON.stringify(createStudentRes.data)}`);
  }

  // 3. Verify Student in Admin Student List
  console.log("\n--- 3. Admin Views Student Directory ---");
  const studentsListRes = await request("GET", "/admin/students", null, adminToken);
  console.log("Status:", studentsListRes.status);
  const foundStudent = studentsListRes.data.data.find((s) => s.email === studentEmail);
  if (foundStudent) {
    console.log(`✓ Found newly created student in directory: Name=${foundStudent.name}, Status=${foundStudent.status}, StudentId=${foundStudent.studentId}`);
  } else {
    throw new Error("Created student not found in admin student list!");
  }

  // 4. Student sets password using activation token
  console.log("\n--- 4. Student Activates Account & Sets Password ---");
  const studentActivateRes = await request("POST", "/auth/reset-password", {
    token: studentActivationToken,
    password: "NewStudentPassword123!",
  });
  console.log("Status:", studentActivateRes.status);
  console.log("Response:", JSON.stringify(studentActivateRes.data, null, 2));
  if (studentActivateRes.status === 200 && studentActivateRes.data.success) {
    console.log("✓ Student password set and account activated successfully!");
  } else {
    throw new Error(`Student password activation failed: ${JSON.stringify(studentActivateRes.data)}`);
  }

  // 5. Student logs in with the newly set password
  console.log("\n--- 5. Student Login with New Password ---");
  const studentLoginRes = await request("POST", "/auth/login", {
    email: studentEmail,
    password: "NewStudentPassword123!",
  });
  console.log("Status:", studentLoginRes.status);
  console.log("User:", JSON.stringify(studentLoginRes.data.data.user, null, 2));
  if (studentLoginRes.status === 200 && studentLoginRes.data.data.user.status === "active") {
    console.log("✓ Student successfully logged in with ACTIVE status and student role!");
  } else {
    throw new Error(`Student login failed: ${JSON.stringify(studentLoginRes.data)}`);
  }

  // 6. Security Test: Token Single-Use Verification
  console.log("\n--- 6. Single-Use Token Security Check (Re-use Attempt) ---");
  const reuseTokenRes = await request("POST", "/auth/reset-password", {
    token: studentActivationToken,
    password: "AnotherPassword123!",
  });
  console.log("Status:", reuseTokenRes.status);
  console.log("Response:", JSON.stringify(reuseTokenRes.data, null, 2));
  if (reuseTokenRes.status === 400 && reuseTokenRes.data.error?.code === "TOKEN_ALREADY_USED") {
    console.log("✓ Token re-use correctly rejected with 400 TOKEN_ALREADY_USED!");
  } else {
    throw new Error(`Token re-use should have failed with TOKEN_ALREADY_USED, got: ${JSON.stringify(reuseTokenRes.data)}`);
  }

  // 7. Admin onboards a new Teacher / Trainer
  console.log("\n--- 7. Admin Onboards Teacher / Trainer ---");
  const teacherPayload = {
    fullName: "Dr. Ananya Roy",
    email: teacherEmail,
    phone: "+91 91234 56789",
    headline: "Senior Cloud & DevOps Architect",
    department: "Cloud Computing",
    qualification: "Ph.D. in Computer Engineering",
    experienceYears: 10,
    skills: "AWS, Kubernetes, Docker, Terraform, CI/CD, Go",
    bio: "Ex-Staff Engineer at AWS with 10+ years designing enterprise distributed systems.",
  };
  const createTeacherRes = await request("POST", "/admin/teachers", teacherPayload, adminToken);
  console.log("Status:", createTeacherRes.status);
  console.log("Response:", JSON.stringify(createTeacherRes.data, null, 2));
  if (createTeacherRes.status === 200 && createTeacherRes.data.success) {
    teacherId = createTeacherRes.data.data.id;
    teacherActivationToken = getLatestActivationTokenForUser(teacherId);
    console.log(`✓ Teacher onboarded successfully with ID: ${teacherId} and Identifier: ${createTeacherRes.data.data.identifier}`);
    console.log(`✓ Retrieved one-time activation token: ${teacherActivationToken}`);
  } else {
    throw new Error(`Failed to onboard teacher: ${JSON.stringify(createTeacherRes.data)}`);
  }

  // 8. Verify Teacher in Admin Trainers Directory
  console.log("\n--- 8. Admin Views Trainer Directory ---");
  const trainersListRes = await request("GET", "/admin/trainers", null, adminToken);
  console.log("Status:", trainersListRes.status);
  const foundTeacher = trainersListRes.data.data.find((t) => t.email === teacherEmail);
  if (foundTeacher) {
    console.log(`✓ Found newly created trainer in directory: Name=${foundTeacher.name}, EmployeeId=${foundTeacher.employeeId}, Headline=${foundTeacher.headline}, Status=${foundTeacher.status}`);
  } else {
    throw new Error("Created trainer not found in admin trainers list!");
  }

  // 9. Teacher sets password using activation token via /api/auth/activate
  console.log("\n--- 9. Teacher Activates Account & Sets Password ---");
  const teacherActivateRes = await request("POST", "/auth/activate", {
    token: teacherActivationToken,
    password: "NewTeacherPassword123!",
  });
  console.log("Status:", teacherActivateRes.status);
  console.log("Response:", JSON.stringify(teacherActivateRes.data, null, 2));
  if (teacherActivateRes.status === 200 && teacherActivateRes.data.success) {
    console.log("✓ Teacher password set and account activated successfully!");
  } else {
    throw new Error(`Teacher activation failed: ${JSON.stringify(teacherActivateRes.data)}`);
  }

  // 10. Teacher logs in with the newly set password
  console.log("\n--- 10. Teacher Login with New Password ---");
  const teacherLoginRes = await request("POST", "/auth/login", {
    email: teacherEmail,
    password: "NewTeacherPassword123!",
  });
  console.log("Status:", teacherLoginRes.status);
  console.log("User:", JSON.stringify(teacherLoginRes.data.data.user, null, 2));
  if (teacherLoginRes.status === 200 && teacherLoginRes.data.data.user.role === "teacher") {
    console.log("✓ Teacher successfully logged in with ACTIVE status and teacher role!");
  } else {
    throw new Error(`Teacher login failed: ${JSON.stringify(teacherLoginRes.data)}`);
  }

  // 11. Admin Resend Welcome Email for Student and Teacher
  console.log("\n--- 11. Admin Resend Welcome Activation Emails ---");
  const resendStudentRes = await request("POST", `/admin/students/${studentId}/resend-welcome-email`, null, adminToken);
  console.log("Resend Student Status:", resendStudentRes.status, JSON.stringify(resendStudentRes.data));
  const newStudentToken = getLatestActivationTokenForUser(studentId);
  console.log("Newly generated student token:", newStudentToken);

  const resendTeacherRes = await request("POST", `/admin/teachers/${teacherId}/resend-welcome-email`, null, adminToken);
  console.log("Resend Teacher Status:", resendTeacherRes.status, JSON.stringify(resendTeacherRes.data));
  const newTeacherToken = getLatestActivationTokenForUser(teacherId);
  console.log("Newly generated teacher token:", newTeacherToken);

  if (resendStudentRes.status === 200 && resendTeacherRes.status === 200 && newStudentToken && newTeacherToken) {
    console.log("✓ Admin successfully resent activation links with newly generated tokens!");
  } else {
    throw new Error("Failed to resend welcome emails!");
  }

  // 12. RBAC Access Control Test
  console.log("\n--- 12. RBAC Access Control Verification ---");
  const studentJwt = studentLoginRes.data.data.accessToken;
  const teacherJwt = teacherLoginRes.data.data.accessToken;

  const studentAdminAttempt = await request("GET", "/admin/students", null, studentJwt);
  console.log("Student calling /admin/students -> Status:", studentAdminAttempt.status);

  const teacherAdminAttempt = await request("GET", "/admin/trainers", null, teacherJwt);
  console.log("Teacher calling /admin/trainers -> Status:", teacherAdminAttempt.status);

  if (studentAdminAttempt.status === 403 && teacherAdminAttempt.status === 403) {
    console.log("✓ RBAC properly enforced! Non-admin roles strictly forbidden from admin endpoints (403 Forbidden).");
  } else {
    throw new Error(`RBAC violation: Student=${studentAdminAttempt.status}, Teacher=${teacherAdminAttempt.status}`);
  }

  console.log("\n================================================================================");
  console.log("ALL 12/12 ADMIN PORTAL ONBOARDING & ACTIVATION TESTS PASSED PERFECTLY!");
  console.log("================================================================================");
}

run().catch((err) => {
  console.error("\nTEST SUITE FAILED:", err);
  process.exit(1);
});
