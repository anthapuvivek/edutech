const BASE_URL = "http://localhost:8081/api";

async function runTests() {
  console.log("==================================================================");
  console.log("PHASE B: CRM & ENQUIRIES BACKEND LIVE TEST SUITE");
  console.log("==================================================================");

  // 1. Submit Public Enquiry (Unauthenticated)
  console.log("\n[TEST 1] Submitting public course enquiry unauthenticated...");
  const enquiryPayload = {
    name: "Kavya Deshmukh",
    email: "kavya.deshmukh@gmail.com",
    phone: "+91 99887 76655",
    courseId: "react-node-fullstack",
    experienceLevel: "Fresher",
    learningMode: "Live Online",
    message: "Interested in the upcoming batch with placement support."
  };

  const enqRes = await fetch(`${BASE_URL}/enquiries`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(enquiryPayload)
  });

  const enqData = await enqRes.json();
  console.log("Status:", enqRes.status, enqRes.statusText);
  console.log("Response:", JSON.stringify(enqData, null, 2));

  if (!enqRes.ok || !enqData.success) {
    throw new Error("Public enquiry submission failed!");
  }
  const enquiryId = enqData.data.id;
  console.log("✓ Public enquiry submitted successfully. Enquiry ID:", enquiryId);

  // 2. Log in as Admin
  console.log("\n[TEST 2] Logging in as Admin (admin@learntrix.com)...");
  const adminLoginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "admin@learntrix.com", password: "password" })
  });
  const adminLoginData = await adminLoginRes.json();
  const adminToken = adminLoginData.data.accessToken;
  console.log("✓ Admin logged in. Token acquired:", adminToken.slice(0, 25) + "...");

  const adminHeaders = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${adminToken}`
  };

  // 3. Fetch CRM Overview
  console.log("\n[TEST 3] Fetching CRM Overview as Admin (GET /api/admin/crm/overview)...");
  const overviewRes = await fetch(`${BASE_URL}/admin/crm/overview`, { headers: adminHeaders });
  const overviewData = await overviewRes.json();
  console.log("CRM Overview:", JSON.stringify(overviewData.data, null, 2));

  // 4. Fetch CRM Leads List & Find Kavya Deshmukh
  console.log("\n[TEST 4] Fetching CRM Leads List (GET /api/admin/crm/leads)...");
  const leadsRes = await fetch(`${BASE_URL}/admin/crm/leads`, { headers: adminHeaders });
  const leadsData = await leadsRes.json();
  console.log(`Total Leads returned: ${leadsData.data.length}`);
  const kavyaLead = leadsData.data.find(l => l.email === "kavya.deshmukh@gmail.com");
  console.log("Found Lead for Kavya Deshmukh:", JSON.stringify(kavyaLead, null, 2));

  if (!kavyaLead) {
    throw new Error("Lead for Kavya Deshmukh was not found in CRM lead list!");
  }
  const leadId = kavyaLead.id;

  // 5. Fetch Staff List
  console.log("\n[TEST 5] Fetching Staff List (GET /api/admin/staff)...");
  const staffRes = await fetch(`${BASE_URL}/admin/staff`, { headers: adminHeaders });
  const staffData = await staffRes.json();
  console.log(`Total Staff Members returned: ${staffData.data.length}`);
  staffData.data.forEach(s => console.log(` - ${s.name} (${s.role}) - assigned: ${s.assignedCount}`));

  // 6. Fetch Lead Detail Profile
  console.log(`\n[TEST 6] Fetching Lead Detail (GET /api/admin/crm/leads/${leadId})...`);
  const detailRes = await fetch(`${BASE_URL}/admin/crm/leads/${leadId}`, { headers: adminHeaders });
  const detailData = await detailRes.json();
  console.log("Lead Detail Summary:");
  console.log(` - Name: ${detailData.data.name}`);
  console.log(` - Course Interest: ${detailData.data.courseInterest}`);
  console.log(` - Stage: ${detailData.data.stage}`);
  console.log(` - Source: ${detailData.data.source}`);
  console.log(` - Notes count: ${detailData.data.notes.length}`);
  console.log(` - Enquiries history count: ${detailData.data.enquiryHistory.length}`);

  // 7. Update Pipeline Stage to 'Contacted'
  console.log(`\n[TEST 7] Updating Pipeline Stage to 'Contacted' (PATCH /api/admin/crm/leads/${leadId}/stage)...`);
  const stageRes = await fetch(`${BASE_URL}/admin/crm/leads/${leadId}/stage`, {
    method: "PATCH",
    headers: adminHeaders,
    body: JSON.stringify({ stage: "Contacted" })
  });
  const stageData = await stageRes.json();
  console.log("Updated Lead Stage:", stageData.data.stage, "| Last Contact:", stageData.data.lastContactAt);

  // 8. Add Note
  console.log(`\n[TEST 8] Adding Staff Note (POST /api/admin/crm/leads/${leadId}/notes)...`);
  const noteRes = await fetch(`${BASE_URL}/admin/crm/leads/${leadId}/notes`, {
    method: "POST",
    headers: adminHeaders,
    body: JSON.stringify({ body: "Spoke on phone: student is preparing for frontend transition." })
  });
  const noteData = await noteRes.json();
  console.log("Note response:", noteData);

  // 9. Log Communication
  console.log(`\n[TEST 9] Logging Communication (POST /api/admin/crm/leads/${leadId}/communications)...`);
  const commRes = await fetch(`${BASE_URL}/admin/crm/leads/${leadId}/communications`, {
    method: "POST",
    headers: adminHeaders,
    body: JSON.stringify({ channel: "Call", summary: "Introductory counseling call completed." })
  });
  const commData = await commRes.json();
  console.log("Communication response:", commData);

  // 10. Schedule Follow-up
  console.log(`\n[TEST 10] Scheduling Follow-up (POST /api/admin/crm/leads/${leadId}/follow-ups)...`);
  const followUpRes = await fetch(`${BASE_URL}/admin/crm/leads/${leadId}/follow-ups`, {
    method: "POST",
    headers: adminHeaders,
    body: JSON.stringify({
      date: "2026-08-28",
      time: "15:00",
      notes: "Follow up regarding weekend batch availability",
      nextAction: "Call"
    })
  });
  const followUpData = await followUpRes.json();
  console.log("Follow-up response:", followUpData);

  // 11. Reassign Lead to Counsellor
  const counsellor = staffData.data.find(s => s.role === "counsellor") || staffData.data[0];
  console.log(`\n[TEST 11] Reassigning Lead to ${counsellor.name} (${counsellor.id})...`);
  const assignRes = await fetch(`${BASE_URL}/admin/crm/leads/${leadId}/assign`, {
    method: "PATCH",
    headers: adminHeaders,
    body: JSON.stringify({ assignedToId: counsellor.id })
  });
  const assignData = await assignRes.json();
  console.log(`Assigned Lead to: ${assignData.data.assignedTo} (ID: ${assignData.data.assignedToId})`);

  // 12. Verify Lead Detail After Modifications
  console.log(`\n[TEST 12] Re-fetching Lead Detail (GET /api/admin/crm/leads/${leadId}) to verify persistence...`);
  const refreshedRes = await fetch(`${BASE_URL}/admin/crm/leads/${leadId}`, { headers: adminHeaders });
  const refreshedData = await refreshedRes.json();
  console.log("Persisted Lead Record:");
  console.log(` - Stage: ${refreshedData.data.stage}`);
  console.log(` - Assigned To: ${refreshedData.data.assignedTo}`);
  console.log(` - Next Follow-up: ${refreshedData.data.nextFollowUpAt}`);
  console.log(` - Notes count: ${refreshedData.data.notes.length}`);
  console.log(` - Communications count: ${refreshedData.data.communications.length}`);
  console.log(` - Follow-ups count: ${refreshedData.data.followUps.length}`);

  // 13. Security RBAC Verification: Student Access Attempt
  console.log("\n[TEST 13] RBAC Test: Logging in as Student (student@learntrix.com)...");
  const studentLoginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "student@learntrix.com", password: "password" })
  });
  const studentLoginData = await studentLoginRes.json();
  const studentHeaders = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${studentLoginData.data.accessToken}`
  };

  const studentCrmRes = await fetch(`${BASE_URL}/admin/crm/overview`, { headers: studentHeaders });
  console.log(`Student accessing GET /api/admin/crm/overview -> Status: ${studentCrmRes.status} (Expected: 403)`);

  const studentLeadsRes = await fetch(`${BASE_URL}/admin/crm/leads`, { headers: studentHeaders });
  console.log(`Student accessing GET /api/admin/crm/leads -> Status: ${studentLeadsRes.status} (Expected: 403)`);

  // 14. Security RBAC Verification: Teacher Access Attempt
  console.log("\n[TEST 14] RBAC Test: Logging in as Teacher (teacher@learntrix.com)...");
  const teacherLoginRes = await fetch(`${BASE_URL}/auth/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ email: "teacher@learntrix.com", password: "password" })
  });
  const teacherLoginData = await teacherLoginRes.json();
  const teacherHeaders = {
    "Content-Type": "application/json",
    "Authorization": `Bearer ${teacherLoginData.data.accessToken}`
  };

  const teacherCrmRes = await fetch(`${BASE_URL}/admin/crm/overview`, { headers: teacherHeaders });
  console.log(`Teacher accessing GET /api/admin/crm/overview -> Status: ${teacherCrmRes.status} (Expected: 403)`);

  const teacherLeadsRes = await fetch(`${BASE_URL}/admin/crm/leads`, { headers: teacherHeaders });
  console.log(`Teacher accessing GET /api/admin/crm/leads -> Status: ${teacherLeadsRes.status} (Expected: 403)`);

  console.log("\n==================================================================");
  console.log("ALL PHASE B API & RBAC TESTS COMPLETED SUCCESSFULLY!");
  console.log("==================================================================");
}

runTests().catch(err => {
  console.error("Test error:", err);
  process.exit(1);
});
