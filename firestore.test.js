const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "citizen_alice_123";
const BOB_UID = "citizen_bob_456";
const ADMIN_UID = "admin_carol_789";
const BOOTSTRAP_ADMIN_UID = "bootstrap_admin_001";
const BOOTSTRAP_ADMIN_EMAIL = "yogamadha126@gmail.com";

const [emulatorHost, emulatorPortStr] = (
  process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085"
).split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validComplaintPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    complaintCode: "CC-2026-1042",
    userId: uid,
    citizenName: "Alice Green",
    citizenEmail: "alice@example.com",
    citizenPhone: "+1 555-0101",
    title: "Overflowing public bin on Main St",
    category: "Garbage Overflow",
    description: "Smart bin at the corner of Main St and 4th Ave is overflowing.",
    locationText: "Main St & 4th Ave, Ward 3",
    latitude: 12.9716,
    longitude: 77.5946,
    imageBase64: "",
    status: "Reported",
    adminRemarks: "",
    statusHistory: [
      {
        status: "Reported",
        remark: "Complaint submitted by citizen",
        updatedBy: "Alice Green",
        timestampMillis: Date.now() - 2000,
      },
    ],
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

function validUserPayload(uid, overrides = {}) {
  const now = new Date(Date.now() - 2000);
  return {
    userId: uid,
    name: "Alice Green",
    email: "alice@example.com",
    phone: "+1 555-0101",
    ward: "Ward 3 - Central",
    role: "citizen",
    createdAt: now,
    updatedAt: now,
    ...overrides,
  };
}

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated users cannot read or write any collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("complaints").get());
  await assertFails(
    unauthDb.collection("complaints").doc("cmp_1").set(validComplaintPayload(ALICE_UID))
  );
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("admins").doc(ADMIN_UID).get());
});

test("Citizen profile creation, self-escalation guard, and PII isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid citizen registration
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID))
  );

  // Reject self-assigned admin role
  await assertFails(
    bobDb
      .collection("users")
      .doc(BOB_UID)
      .set(validUserPayload(BOB_UID, { role: "admin" }))
  );

  // Partial update of mutable fields succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      name: "Alice M. Green",
      phone: "+1 555-0199",
      ward: "Ward 5 - East",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject role tampering on update
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      role: "admin",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Cross-user PII read & list must fail
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").get());
});

test("Citizen complaint creation, validation bounds, and scoped queries", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();

  // Valid create (no @DocumentId `id` field, optional lat/lon omitted also allowed)
  await assertSucceeds(
    aliceDb.collection("complaints").doc("cmp_alice_1").set(validComplaintPayload(ALICE_UID))
  );

  const withoutCoords = validComplaintPayload(ALICE_UID);
  delete withoutCoords.latitude;
  delete withoutCoords.longitude;
  await assertSucceeds(
    aliceDb.collection("complaints").doc("cmp_alice_2").set(withoutCoords)
  );

  // Reject `@DocumentId` id field inside document body
  await assertFails(
    aliceDb
      .collection("complaints")
      .doc("cmp_bad_id")
      .set(validComplaintPayload(ALICE_UID, { id: "cmp_bad_id" }))
  );

  // Reject status shortcutting on create
  await assertFails(
    aliceDb
      .collection("complaints")
      .doc("cmp_bad_status")
      .set(validComplaintPayload(ALICE_UID, { status: "Resolved" }))
  );

  // Reject invalid category
  await assertFails(
    aliceDb
      .collection("complaints")
      .doc("cmp_bad_cat")
      .set(validComplaintPayload(ALICE_UID, { category: "Noise" }))
  );

  // Reject future timestamp
  await assertFails(
    aliceDb
      .collection("complaints")
      .doc("cmp_future_ts")
      .set(
        validComplaintPayload(ALICE_UID, {
          createdAt: new Date(Date.now() + 60000),
          updatedAt: new Date(Date.now() + 60000),
        })
      )
  );

  // Filtered citizen query succeeds; unfiltered query fails; cross-user read fails
  await assertSucceeds(
    aliceDb.collection("complaints").where("userId", "==", ALICE_UID).get()
  );
  await assertFails(aliceDb.collection("complaints").get());
  await assertFails(bobDb.collection("complaints").doc("cmp_alice_1").get());

  // Citizen cannot update complaint status or remarks directly
  await assertFails(
    aliceDb.collection("complaints").doc("cmp_alice_1").update({
      status: "Resolved",
      adminRemarks: "Self-resolved",
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Admin role access, status/remarks updates, and email spoofing guard", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const db = context.firestore();
    await db.collection("admins").doc(ADMIN_UID).set({ email: "admin@cleancity.gov" });
    await db.collection("complaints").doc("cmp_alice_1").set(validComplaintPayload(ALICE_UID));
  });

  // Citizen cannot write to /admins
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(
    aliceDb.collection("admins").doc(ALICE_UID).set({ email: "hacker@example.com" })
  );

  // Admin via /admins/{uid} marker can list all complaints and update status + remarks
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(adminDb.collection("complaints").get());
  await assertSucceeds(
    adminDb.collection("complaints").doc("cmp_alice_1").update({
      status: "In Progress",
      adminRemarks: "Sanitation crew dispatched to Ward 3.",
      statusHistory: [
        {
          status: "Reported",
          remark: "Complaint submitted by citizen",
          updatedBy: "Alice Green",
          timestampMillis: Date.now() - 5000,
        },
        {
          status: "In Progress",
          remark: "Sanitation crew dispatched to Ward 3.",
          updatedBy: "City Admin",
          timestampMillis: Date.now() - 500,
        },
      ],
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Bootstrapped admin with verified email succeeds
  const verifiedBootstrapDb = testEnv
    .authenticatedContext(BOOTSTRAP_ADMIN_UID, {
      email: BOOTSTRAP_ADMIN_EMAIL,
      email_verified: true,
    })
    .firestore();
  await assertSucceeds(verifiedBootstrapDb.collection("complaints").get());
  await assertSucceeds(
    verifiedBootstrapDb.collection("complaints").doc("cmp_alice_1").update({
      status: "Resolved",
      adminRemarks: "Area cleaned and sanitized.",
      statusHistory: [
        {
          status: "Reported",
          remark: "Complaint submitted by citizen",
          updatedBy: "Alice Green",
          timestampMillis: Date.now() - 5000,
        },
        {
          status: "Resolved",
          remark: "Area cleaned and sanitized.",
          updatedBy: "City Admin",
          timestampMillis: Date.now() - 200,
        },
      ],
      updatedAt: new Date(Date.now() - 200),
    })
  );

  // Spoofed unverified admin email MUST fail
  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: BOOTSTRAP_ADMIN_EMAIL,
      email_verified: false,
    })
    .firestore();
  await assertFails(spoofedAdminDb.collection("complaints").get());
  await assertFails(
    spoofedAdminDb.collection("complaints").doc("cmp_alice_1").update({
      status: "Reported",
      updatedAt: new Date(Date.now() - 100),
    })
  );
});
