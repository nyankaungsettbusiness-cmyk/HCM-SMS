package com.example.data.policy

import com.example.data.local.entity.EducationLevel
import com.example.data.local.entity.SubjectCategory
import com.example.data.local.entity.SubjectEntity

/**
 * High School Stream enumeration representing STEAMS tracks and their mapped elective subjects.
 */
enum class StreamType(val code: String, val displayName: String, val electiveSubject: String) {
    STEAMS_1("STEAMS-1", "STEAMS-1 (Biology Track)", "Biology"),
    STEAMS_2("STEAMS-2", "STEAMS-2 (Economics Track)", "Economics");

    companion object {
        fun from(studentStream: String): StreamType {
            val norm = studentStream.trim().uppercase()
            return if (norm.contains("2") || norm.contains("ECON")) STEAMS_2 else STEAMS_1
        }
    }
}

/**
 * Sealed class hierarchy for grade level subject policy mapping based on student's grade and assigned stream.
 */
sealed class GradeSubjectPolicy {
    abstract val grade: String
    abstract val studentStream: String
    abstract fun getSubjects(): List<SubjectEntity>
    abstract fun getSubjectNames(): List<String>

    data class Kindergarten(
        override val grade: String,
        override val studentStream: String = ""
    ) : GradeSubjectPolicy() {
        override fun getSubjects(): List<SubjectEntity> = listOf(
            SubjectEntity(name = "Myanmar", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.KINDERGARTEN),
            SubjectEntity(name = "English (Phonics)", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.KINDERGARTEN),
            SubjectEntity(name = "English (Language)", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.KINDERGARTEN),
            SubjectEntity(name = "Mathematics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.KINDERGARTEN),
            SubjectEntity(name = "Physical Education (PE)", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.KINDERGARTEN),
            SubjectEntity(name = "Music & Performing Arts", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.KINDERGARTEN)
        )
        override fun getSubjectNames(): List<String> = getSubjects().map { it.name }
    }

    data class Primary(
        override val grade: String,
        override val studentStream: String = ""
    ) : GradeSubjectPolicy() {
        override fun getSubjects(): List<SubjectEntity> = listOf(
            SubjectEntity(name = "Myanmar", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
            SubjectEntity(name = "English", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
            SubjectEntity(name = "Mathematics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
            SubjectEntity(name = "Science", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
            SubjectEntity(name = "Social Studies", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.PRIMARY),
            SubjectEntity(name = "English (International)", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.PRIMARY),
            SubjectEntity(name = "Physical Education (PE)", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.PRIMARY),
            SubjectEntity(name = "Music & Performing Arts", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.PRIMARY)
        )
        override fun getSubjectNames(): List<String> = getSubjects().map { it.name }
    }

    data class Secondary(
        override val grade: String,
        override val studentStream: String = ""
    ) : GradeSubjectPolicy() {
        override fun getSubjects(): List<SubjectEntity> = listOf(
            SubjectEntity(name = "Myanmar", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
            SubjectEntity(name = "English", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
            SubjectEntity(name = "Mathematics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
            SubjectEntity(name = "Science", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
            SubjectEntity(name = "History", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
            SubjectEntity(name = "Geography", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.SECONDARY),
            SubjectEntity(name = "Coding & Robotics", category = SubjectCategory.ADDITIONAL, educationLevel = EducationLevel.SECONDARY)
        )
        override fun getSubjectNames(): List<String> = getSubjects().map { it.name }
    }

    data class HighSchool(
        override val grade: String,
        override val studentStream: String
    ) : GradeSubjectPolicy() {
        val streamType: StreamType = StreamType.from(studentStream)

        override fun getSubjects(): List<SubjectEntity> {
            val common = listOf(
                SubjectEntity(name = "Myanmar", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL),
                SubjectEntity(name = "English", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL),
                SubjectEntity(name = "Mathematics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL),
                SubjectEntity(name = "Physics", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL),
                SubjectEntity(name = "Chemistry", category = SubjectCategory.ACADEMIC, educationLevel = EducationLevel.HIGH_SCHOOL)
            )
            return when (streamType) {
                StreamType.STEAMS_1 -> common + SubjectEntity(
                    name = "Biology",
                    category = SubjectCategory.ACADEMIC,
                    educationLevel = EducationLevel.HIGH_SCHOOL,
                    subTrack = StreamType.STEAMS_1.code
                )
                StreamType.STEAMS_2 -> common + SubjectEntity(
                    name = "Economics",
                    category = SubjectCategory.ACADEMIC,
                    educationLevel = EducationLevel.HIGH_SCHOOL,
                    subTrack = StreamType.STEAMS_2.code
                )
            }
        }

        override fun getSubjectNames(): List<String> = getSubjects().map { it.name }
    }

    companion object {
        fun create(grade: String, studentStream: String = ""): GradeSubjectPolicy {
            val norm = grade.trim().uppercase()
            return when {
                norm == "KG" || norm.contains("KINDERGARTEN") -> Kindergarten(grade, studentStream)
                norm in listOf("G1", "G2", "G3", "G4", "G5", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4", "GRADE 5") -> Primary(grade, studentStream)
                norm in listOf("G6", "G7", "G8", "G9", "GRADE 6", "GRADE 7", "GRADE 8", "GRADE 9") -> Secondary(grade, studentStream)
                norm in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12") -> HighSchool(grade, studentStream)
                else -> Primary(grade, studentStream)
            }
        }
    }
}

/**
 * Centralized School Policy mapping grade levels to their EducationLevel and Subject structures.
 */
object SchoolPolicy {

    val VALID_GRADES = listOf("KG", "G1", "G2", "G3", "G4", "G5", "G6", "G7", "G8", "G9", "G10", "G11", "G12")
    val VALID_STREAMS = listOf("STEAMS-1", "STEAMS-2")

    fun isHighSchool(grade: String): Boolean {
        val norm = grade.trim().uppercase()
        return norm in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12")
    }

    fun getEducationLevel(grade: String): EducationLevel {
        val norm = grade.trim().uppercase()
        return when {
            norm == "KG" || norm.contains("KINDERGARTEN") -> EducationLevel.KINDERGARTEN
            norm in listOf("G1", "G2", "G3", "G4", "G5", "GRADE 1", "GRADE 2", "GRADE 3", "GRADE 4", "GRADE 5") -> EducationLevel.PRIMARY
            norm in listOf("G6", "G7", "G8", "G9", "GRADE 6", "GRADE 7", "GRADE 8", "GRADE 9") -> EducationLevel.SECONDARY
            norm in listOf("G10", "G11", "G12", "GRADE 10", "GRADE 11", "GRADE 12") -> EducationLevel.HIGH_SCHOOL
            else -> EducationLevel.PRIMARY
        }
    }

    fun getPolicyForGradeAndStream(grade: String, studentStream: String = ""): GradeSubjectPolicy {
        return GradeSubjectPolicy.create(grade, studentStream)
    }

    fun getDefaultSubjectsForLevel(level: EducationLevel, studentStream: String = ""): List<SubjectEntity> {
        val dummyGrade = when (level) {
            EducationLevel.KINDERGARTEN -> "KG"
            EducationLevel.PRIMARY -> "G1"
            EducationLevel.SECONDARY -> "G6"
            EducationLevel.HIGH_SCHOOL -> "G10"
        }
        return GradeSubjectPolicy.create(dummyGrade, studentStream).getSubjects()
    }

    fun getDefaultSubjectsForGrade(grade: String, studentStream: String = ""): List<SubjectEntity> {
        return GradeSubjectPolicy.create(grade, studentStream).getSubjects()
    }

    fun getDefaultSubjectNamesForGrade(grade: String, studentStream: String = ""): List<String> {
        return GradeSubjectPolicy.create(grade, studentStream).getSubjectNames()
    }

    fun filterSubjectsForGrade(allSubjectsFromDb: List<SubjectEntity>, grade: String, studentStream: String = ""): List<SubjectEntity> {
        val policy = GradeSubjectPolicy.create(grade, studentStream)
        val level = getEducationLevel(grade)
        val hasDbData = allSubjectsFromDb.isNotEmpty()
        val levelSubjects = allSubjectsFromDb.filter { !it.isDeleted && it.educationLevel == level }
        val activeLevelSubjects = levelSubjects.filter { it.isEnabled }
        
        val baseList = if (hasDbData) {
            activeLevelSubjects
        } else {
            policy.getSubjects()
        }

        if (policy is GradeSubjectPolicy.HighSchool) {
            val result = baseList.filter { subj ->
                val nameUpper = subj.name.uppercase()
                if (policy.streamType == StreamType.STEAMS_1) {
                    !nameUpper.contains("ECONOMICS") && !nameUpper.contains("ECON")
                } else if (policy.streamType == StreamType.STEAMS_2) {
                    !nameUpper.contains("BIOLOGY") && !nameUpper.contains("BIO")
                } else {
                    true
                }
            }
            return result.distinctBy { it.name.trim().lowercase() }
        }
        return baseList.distinctBy { it.name.trim().lowercase() }
    }

    fun getSubjectNamesForGrade(allSubjectsFromDb: List<SubjectEntity>, grade: String, studentStream: String = ""): List<String> {
        return filterSubjectsForGrade(allSubjectsFromDb, grade, studentStream).map { it.name }.distinct()
    }
}

