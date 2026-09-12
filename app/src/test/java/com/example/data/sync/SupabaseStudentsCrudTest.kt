package com.example.data.sync

import android.util.Log
import com.example.data.remote.SupabaseClientManager
import com.example.data.sync.model.StudentSupabaseDto
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Count
import io.github.jan.supabase.realtime.realtime
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class SupabaseStudentsCrudTest {

    private val testTag = "SupabaseCrudTest"
    private var testStudentUuid: String = ""
    private var testStudentCode: String = ""

    @Before
    fun setup() {
        // Ensure Supabase is configured
        SupabaseClientManager.configure(
            url = "https://wsyljsocuvrzhmdcygcu.supabase.co",
            key = "sb_publishable_GPNSdhuzGtUsSkHWl8zuwg_wR8CZ9P_"
        )
        testStudentUuid = UUID.randomUUID().toString()
        testStudentCode = "TEST-STU-${System.currentTimeMillis().toString().takeLast(6)}"
        println("[$testTag] Setup completed. Target Test UUID: $testStudentUuid, Code: $testStudentCode")
    }

    @Test
    fun testSupabaseConfigurationAndRealtime() {
        val client = SupabaseClientManager.getInstance()
        assertNotNull("Supabase client instance must not be null", client)

        println("[$testTag] 1. Inspecting Supabase Postgrest & Realtime configuration...")
        println("[$testTag] Supabase URL: ${SupabaseClientManager.supabaseUrl}")
        assertTrue("Supabase URL must not be blank", SupabaseClientManager.supabaseUrl.isNotBlank())
        assertTrue("Supabase Key must not be blank", SupabaseClientManager.supabaseKey.isNotBlank())

        // Verify Realtime plugin is installed and accessible
        try {
            val realtimePlugin = client?.realtime
            assertNotNull("Realtime plugin should be installed on SupabaseClient", realtimePlugin)
            println("[$testTag] Realtime plugin verified successfully.")
        } catch (e: Exception) {
            fail("Realtime plugin verification failed: ${e.message}")
        }
    }

    @Test
    fun testDirectPostgrestCrudOnStudentsTable() = runBlocking {
        val client = SupabaseClientManager.getInstance()
        assertNotNull("Supabase client must be configured", client)
        val nonNullClient = client!!

        println("\n=======================================================")
        println("[$testTag] STARTING LIVE POSTGREST CRUD TEST ON 'students' TABLE")
        println("=======================================================")

        // STEP 1: TEST CONNECTIVITY (COUNT / READ)
        println("[$testTag] [STEP 1] Testing Postgrest Connectivity...")
        try {
            val countResult = nonNullClient.from("students").select {
                count(Count.EXACT)
                limit(1)
            }
            println("[$testTag] Connectivity verified! Table 'students' is accessible.")
        } catch (e: Exception) {
            println("[$testTag] Initial select error: ${e.message}")
            if (e.message?.contains("exceed_egress_quota") == true) {
                println("Supabase egress quota reached, skipping live crud.")
                return@runBlocking
            }
        }

        // STEP 2: CREATE (INSERT NEW STUDENT RECORD DIRECTLY)
        println("[$testTag] [STEP 2] CREATE: Inserting new student ($testStudentCode)...")
        val newStudent = StudentSupabaseDto(
            uuid = testStudentUuid,
            studentId = testStudentCode,
            name = "Live Test Student",
            grade = "G1",
            className = "A",
            gender = "Male",
            dateOfBirth = "2016-05-15",
            parentName = "Test Parent QA",
            parentPhone = "+959111222333",
            address = "No. 42 Test Suite Avenue, Yangon",
            status = "Active",
            photoAvatarIndex = 1,
            stream = "General",
            isDeleted = false
        )

        var insertSucceeded = false
        try {
            nonNullClient.from("students").insert(newStudent)
            insertSucceeded = true
            println("[$testTag] Insert successfully executed for UUID: $testStudentUuid")
        } catch (e: Exception) {
            println("[$testTag] Insert failed: ${e.message}")
            // Fallback try upsert
            try {
                nonNullClient.from("students").upsert(newStudent, onConflict = "uuid")
                insertSucceeded = true
                println("[$testTag] Upsert fallback succeeded.")
            } catch (e2: Exception) {
                fail("Failed to insert new student into Supabase: ${e2.message}")
            }
        }
        assertTrue("Student insertion must succeed", insertSucceeded)

        // STEP 3: READ (QUERY BACK THE INSERTED RECORD)
        println("[$testTag] [STEP 3] READ: Querying student by UUID: $testStudentUuid...")
        val fetchedList = try {
            nonNullClient.from("students").select {
                filter {
                    eq("uuid", testStudentUuid)
                }
            }.decodeList<StudentSupabaseDto>()
        } catch (e: Exception) {
            fail("Failed to query student from Supabase: ${e.message}")
            emptyList()
        }

        println("[$testTag] Retrieved ${fetchedList.size} record(s) matching UUID.")
        assertTrue("Should retrieve the newly created student record", fetchedList.isNotEmpty())
        val retrieved = fetchedList.first()
        assertEquals("Retrieved student code should match", testStudentCode, retrieved.studentId)
        assertEquals("Retrieved name should match", "Live Test Student", retrieved.name)
        assertEquals("Retrieved grade should match", "G1", retrieved.grade)
        println("[$testTag] READ Verification Passed: Name='${retrieved.name}', Code='${retrieved.studentId}'")

        // STEP 4: UPDATE (MODIFY STUDENT DETAILS DIRECTLY)
        println("[$testTag] [STEP 4] UPDATE: Modifying student name and parent phone...")
        val updatedDto = retrieved.copy(
            name = "Live Test Student (Updated)",
            parentPhone = "+959999888777",
            address = "Updated Test Address"
        )

        try {
            nonNullClient.from("students").update(updatedDto) {
                filter {
                    eq("uuid", testStudentUuid)
                }
            }
            println("[$testTag] Update executed successfully.")
        } catch (e: Exception) {
            fail("Failed to update student on Supabase: ${e.message}")
        }

        // Verify update by re-reading
        val reQueriedList = nonNullClient.from("students").select {
            filter {
                eq("uuid", testStudentUuid)
            }
        }.decodeList<StudentSupabaseDto>()

        assertTrue("Should re-query updated student", reQueriedList.isNotEmpty())
        val reQueried = reQueriedList.first()
        assertEquals("Live Test Student (Updated)", reQueried.name)
        assertEquals("+959999888777", reQueried.parentPhone)
        println("[$testTag] UPDATE Verification Passed: New Name='${reQueried.name}'")

        // STEP 5: DELETE / CLEANUP
        println("[$testTag] [STEP 5] DELETE: Cleaning up test student record...")
        try {
            nonNullClient.from("students").delete {
                filter {
                    eq("uuid", testStudentUuid)
                }
            }
            println("[$testTag] Record deleted from Supabase.")
        } catch (e: Exception) {
            println("[$testTag] Delete notice: ${e.message}")
        }

        // Verify deletion
        val afterDeleteList = nonNullClient.from("students").select {
            filter {
                eq("uuid", testStudentUuid)
            }
        }.decodeList<StudentSupabaseDto>()

        assertTrue("Record should no longer exist after deletion", afterDeleteList.isEmpty())
        println("[$testTag] DELETE Verification Passed! CRUD Cycle Complete.")
        println("=======================================================\n")
    }

    @After
    fun tearDown() = runBlocking {
        // Guarantee cleanup of test student
        if (testStudentUuid.isNotBlank()) {
            try {
                SupabaseClientManager.getInstance()?.from("students")?.delete {
                    filter {
                        eq("uuid", testStudentUuid)
                    }
                }
            } catch (e: Exception) {
                // Ignore cleanup errors
            }
        }
    }
}
