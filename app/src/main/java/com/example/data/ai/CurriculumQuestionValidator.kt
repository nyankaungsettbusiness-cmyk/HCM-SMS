package com.example.data.ai

import com.example.data.local.entity.CurriculumChunkEntity

/**
 * Authoritative validation layer for generated Question Papers and Worksheets (Phase 3).
 * Enforces strict compliance with marks, question counts, answer keys,
 * curriculum scope boundaries, grade suitability, and non-duplication.
 */
object CurriculumQuestionValidator {

    /**
     * Validates an Official Question Paper or Classroom Quiz generation result.
     */
    fun validateQuestionPaper(
        request: QuestionPaperRequest,
        resultSections: List<QuestionSectionResult>,
        retrievedChunks: List<CurriculumChunkEntity>
    ): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val allQuestions = resultSections.flatMap { it.questions }

        // 1. Check if source chunks exist (Scope Verification)
        if (retrievedChunks.isEmpty()) {
            errors.add("Insufficient curriculum content: No approved curriculum chunks available for ${request.grade} ${request.subject} (${request.chapterUnit}).")
        }

        // 2. Question Count Check
        val totalQuestions = allQuestions.size
        val expectedCount = if (request.sections.isNotEmpty()) {
            request.sections.sumOf { it.questionCount }
        } else {
            request.questionCount
        }

        if (totalQuestions == 0) {
            errors.add("No questions were generated in the examination paper.")
        } else if (totalQuestions != expectedCount) {
            warnings.add("Question count mismatch: Generated $totalQuestions questions, expected $expectedCount.")
        }

        // 3. Total Marks & Section Mark Validation
        val calculatedTotalMarks = allQuestions.sumOf { it.marks }
        if (calculatedTotalMarks != request.totalMarks) {
            errors.add("Total mark mismatch: Total calculated marks ($calculatedTotalMarks) does not equal requested total marks (${request.totalMarks}).")
        }

        resultSections.forEach { section ->
            val sectionMarks = section.questions.sumOf { it.marks }
            if (sectionMarks != section.sectionMarks) {
                errors.add("Section '${section.sectionName}' mark mismatch: Questions sum to $sectionMarks marks, but section specifies ${section.sectionMarks} marks.")
            }
        }

        // 4. Answer Key & Grading Rubrics Completeness
        allQuestions.forEach { q ->
            when (q.questionType) {
                QuestionType.MCQ -> {
                    if (q.options.size < 2) {
                        errors.add("Q${q.questionNumber}: Multiple Choice Question must have at least 2 distinct answer options.")
                    }
                    if (q.correctAnswer.isBlank()) {
                        errors.add("Q${q.questionNumber}: Missing correct answer key for Multiple Choice Question.")
                    }
                }
                QuestionType.MATCHING -> {
                    if (q.matchingPairs.isEmpty() && q.correctAnswer.isBlank()) {
                        errors.add("Q${q.questionNumber}: Matching question must provide pair mappings or an answer key.")
                    }
                }
                QuestionType.FILL_IN_BLANKS -> {
                    if (q.correctAnswer.isBlank()) {
                        errors.add("Q${q.questionNumber}: Missing expected answer for Fill in the Blanks.")
                    }
                }
                QuestionType.TRUE_FALSE -> {
                    if (q.correctAnswer.isBlank()) {
                        errors.add("Q${q.questionNumber}: Missing True/False answer key.")
                    }
                }
                QuestionType.SHORT_ANSWER, QuestionType.STRUCTURED, QuestionType.DESCRIPTIVE, QuestionType.ESSAY -> {
                    if (q.markingGuide.isBlank() && q.correctAnswer.isBlank()) {
                        errors.add("Q${q.questionNumber}: Subjective question (${q.questionType.displayName}) must have a marking rubric or model answer.")
                    }
                }
                QuestionType.LABEL_DIAGRAM, QuestionType.MAP_BASED, QuestionType.PICTURE_BASED -> {
                    if (q.assetRequirement == null) {
                        warnings.add("Q${q.questionNumber}: Visual question type (${q.questionType.displayName}) is missing QuestionAssetRequirement metadata.")
                    }
                    if (q.correctAnswer.isBlank() && q.markingGuide.isBlank()) {
                        errors.add("Q${q.questionNumber}: Missing answer key / label guide for visual asset question.")
                    }
                }
            }
        }

        // 5. Duplicate Question Detection
        val seenTexts = mutableSetOf<String>()
        allQuestions.forEach { q ->
            val normalized = q.questionText.trim().lowercase().replace("\\s+".toRegex(), " ")
            if (normalized.isNotBlank()) {
                if (seenTexts.contains(normalized)) {
                    errors.add("Duplicate question detected (Q${q.questionNumber}): \"${q.questionText.take(50)}...\"")
                } else {
                    seenTexts.add(normalized)
                }
            }
        }

        // 6. Grade Suitability & Pedagogical Appropriateness
        val isKindergarten = request.grade.equals("KG", ignoreCase = true) || request.grade.equals("Kindergarten", ignoreCase = true)
        val isPrimary1 = request.grade.equals("G1", ignoreCase = true) || request.grade.equals("Grade 1", ignoreCase = true)

        if (isKindergarten || isPrimary1) {
            val inappropriateEssay = allQuestions.filter { it.questionType == QuestionType.ESSAY || it.questionType == QuestionType.DESCRIPTIVE }
            if (inappropriateEssay.isNotEmpty()) {
                errors.add("Pedagogical mismatch: Essay / Descriptive long-answer questions are inappropriate for ${request.grade}.")
            }
            val challengeCount = allQuestions.count { it.difficulty == DifficultyLevel.CHALLENGE }
            if (isKindergarten && challengeCount > 0) {
                warnings.add("Caution: Challenge-level abstract questions may not be age-appropriate for KG learners.")
            }
        }

        // 7. Source Scope Verification
        allQuestions.forEach { q ->
            if (q.sourceReference.documentTitle.isBlank() && q.sourceReference.chapterUnit.isBlank()) {
                errors.add("Q${q.questionNumber}: Question is missing curriculum source traceability reference.")
            }
        }

        return ValidationReport(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            totalCalculatedMarks = calculatedTotalMarks,
            targetMarks = request.totalMarks,
            validatedQuestionCount = totalQuestions
        )
    }

    /**
     * Validates a Differentiated Worksheet generation result.
     */
    fun validateWorksheet(
        request: WorksheetRequest,
        items: List<GeneratedQuestion>,
        retrievedChunks: List<CurriculumChunkEntity>
    ): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        if (retrievedChunks.isEmpty()) {
            errors.add("Insufficient curriculum content: No approved curriculum chunks available for ${request.grade} ${request.subject} (${request.chapterUnit}).")
        }

        val totalItems = items.size
        if (totalItems == 0) {
            errors.add("Worksheet generated 0 items.")
        }

        // Validate Difficulty Distribution
        val easyItems = items.count { it.difficulty == DifficultyLevel.EASY }
        val mediumItems = items.count { it.difficulty == DifficultyLevel.MEDIUM }
        val challengeItems = items.count { it.difficulty == DifficultyLevel.CHALLENGE }

        if (easyItems == 0 && request.difficultyDistribution.easyCount > 0) {
            warnings.add("Worksheet does not contain Easy difficulty items.")
        }

        // Validate Answers
        items.forEach { q ->
            if (q.correctAnswer.isBlank() && q.markingGuide.isBlank()) {
                errors.add("Item ${q.questionNumber}: Missing answer key / teacher solution in worksheet.")
            }
            if (q.sourceReference.chapterUnit.isBlank()) {
                errors.add("Item ${q.questionNumber}: Missing curriculum chapter/unit traceability.")
            }
        }

        // Duplicate Detection
        val seenTexts = mutableSetOf<String>()
        items.forEach { q ->
            val normalized = q.questionText.trim().lowercase().replace("\\s+".toRegex(), " ")
            if (normalized.isNotBlank()) {
                if (seenTexts.contains(normalized)) {
                    errors.add("Duplicate worksheet item detected (Item ${q.questionNumber}): \"${q.questionText.take(50)}...\"")
                } else {
                    seenTexts.add(normalized)
                }
            }
        }

        return ValidationReport(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            totalCalculatedMarks = items.sumOf { it.marks },
            targetMarks = items.sumOf { it.marks },
            validatedQuestionCount = totalItems
        )
    }

    /**
     * Validates a manually edited assessment result before applying changes (Phase 3B Step 3).
     */
    fun validateEditedStructuredResult(
        originalTotalMarks: Int,
        sections: List<com.example.ui.screens.ai.workspace.QuestionSectionUi>
    ): ValidationReport {
        val errors = mutableListOf<String>()
        val warnings = mutableListOf<String>()

        val allQuestions = sections.flatMap { it.questions }
        if (allQuestions.isEmpty()) {
            errors.add("Assessment paper contains no questions.")
        }

        // 1. Marks validation
        val calculatedTotalMarks = allQuestions.sumOf { it.marks }
        if (calculatedTotalMarks <= 0) {
            errors.add("Total marks must be greater than 0.")
        } else if (originalTotalMarks > 0 && calculatedTotalMarks != originalTotalMarks) {
            errors.add("Total marks mismatch: Questions sum to $calculatedTotalMarks marks, but paper total is $originalTotalMarks marks.")
        }

        // 2. Individual question validations
        val seenTexts = mutableSetOf<String>()
        allQuestions.forEach { q ->
            if (q.questionText.isBlank()) {
                errors.add("Question ${q.questionNumber}: Question text cannot be blank.")
            }

            if (q.marks <= 0) {
                errors.add("Question ${q.questionNumber}: Marks must be at least 1.")
            }

            if (q.questionType.contains("Multiple Choice", ignoreCase = true) || q.options.isNotEmpty()) {
                if (q.options.size < 2) {
                    errors.add("Question ${q.questionNumber}: Multiple choice question must have at least 2 options.")
                }
                if (q.options.any { it.isBlank() }) {
                    errors.add("Question ${q.questionNumber}: Multiple choice options cannot be empty.")
                }
                if (q.correctAnswer.isBlank()) {
                    errors.add("Question ${q.questionNumber}: Correct answer is required.")
                }
            } else {
                if (q.correctAnswer.isBlank() && q.markingGuide.isBlank()) {
                    errors.add("Question ${q.questionNumber}: Correct answer or marking rubric is required.")
                }
            }

            // Duplicate question detection
            val normalized = q.questionText.trim().lowercase().replace("\\s+".toRegex(), " ")
            if (normalized.isNotBlank()) {
                if (seenTexts.contains(normalized)) {
                    errors.add("Duplicate question detected (Question ${q.questionNumber}): \"${q.questionText.take(40)}...\"")
                } else {
                    seenTexts.add(normalized)
                }
            }

            // Traceability check: ensure source citation is retained
            if (q.sourceCitation == null || q.sourceCitation.chapterUnit.isBlank()) {
                warnings.add("Question ${q.questionNumber}: Source curriculum citation is missing.")
            }
        }

        return ValidationReport(
            isValid = errors.isEmpty(),
            errors = errors,
            warnings = warnings,
            totalCalculatedMarks = calculatedTotalMarks,
            targetMarks = originalTotalMarks,
            validatedQuestionCount = allQuestions.size
        )
    }
}
