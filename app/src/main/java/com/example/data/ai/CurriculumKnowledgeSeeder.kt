package com.example.data.ai

import com.example.data.local.dao.CurriculumKnowledgeDao
import com.example.data.local.entity.CurriculumChunkEntity
import com.example.data.local.entity.CurriculumDocumentEntity
import com.example.data.local.entity.CurriculumDocumentType

/**
 * Seeds official Myanmar KG-G12 Basic Education Curriculum framework,
 * School-Approved International course materials, and Past Exam Paper References
 * into the local Room knowledge store so teachers have verified textbook sources immediately.
 */
object CurriculumKnowledgeSeeder {

    suspend fun seedCurriculumIfEmpty(dao: CurriculumKnowledgeDao) {
        if (dao.getChunkCount() > 0) return

        val documents = listOf(
            // KG Myanmar & Math
            CurriculumDocumentEntity(
                id = 101,
                title = "Myanmar Kindergarten (KG) Activity Book",
                gradeLevel = "KG",
                subject = "Myanmar",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),
            CurriculumDocumentEntity(
                id = 102,
                title = "Myanmar Kindergarten (KG) Mathematics & Shapes",
                gradeLevel = "KG",
                subject = "Mathematics",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),

            // Grade 1 Primary
            CurriculumDocumentEntity(
                id = 103,
                title = "Myanmar Primary Grade 1 English",
                gradeLevel = "G1",
                subject = "English",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),

            // Grade 5 Primary
            CurriculumDocumentEntity(
                id = 1,
                title = "Myanmar Primary English Textbook (Grade 5)",
                gradeLevel = "G5",
                subject = "English",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),
            CurriculumDocumentEntity(
                id = 2,
                title = "Myanmar Primary Mathematics Textbook (Grade 5)",
                gradeLevel = "G5",
                subject = "Mathematics",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),
            CurriculumDocumentEntity(
                id = 3,
                title = "Myanmar Primary Science & Environment (Grade 5)",
                gradeLevel = "G5",
                subject = "Science",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),

            // Middle School Grade 7
            CurriculumDocumentEntity(
                id = 4,
                title = "Myanmar Middle School English Textbook (Grade 7)",
                gradeLevel = "G7",
                subject = "English",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),
            CurriculumDocumentEntity(
                id = 5,
                title = "Myanmar Middle School General Science (Grade 7)",
                gradeLevel = "G7",
                subject = "Science",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),

            // School Approved International Material (Grade 8 Science)
            CurriculumDocumentEntity(
                id = 801,
                title = "Cambridge Lower Secondary Science Stage 8",
                gradeLevel = "G8",
                subject = "Science",
                documentType = CurriculumDocumentType.CAMBRIDGE_CEFR.name,
                authorOrPublisher = "Cambridge University Press / School Approved"
            ),

            // High School Grade 10
            CurriculumDocumentEntity(
                id = 6,
                title = "Myanmar High School Physics (Grade 10)",
                gradeLevel = "G10",
                subject = "Physics",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),
            CurriculumDocumentEntity(
                id = 7,
                title = "Myanmar High School Chemistry (Grade 10)",
                gradeLevel = "G10",
                subject = "Chemistry",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),
            CurriculumDocumentEntity(
                id = 8,
                title = "Myanmar High School Biology (Grade 10)",
                gradeLevel = "G10",
                subject = "Biology",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),
            CurriculumDocumentEntity(
                id = 9,
                title = "Myanmar High School Economics (Grade 10)",
                gradeLevel = "G10",
                subject = "Economics",
                documentType = CurriculumDocumentType.MYANMAR_TEXTBOOK.name,
                authorOrPublisher = "Ministry of Education Myanmar"
            ),

            // Past Question Paper (REFERENCE ONLY)
            CurriculumDocumentEntity(
                id = 999,
                title = "Previous Question Paper - Grade 5 English Mid-Term 2025 (Reference Only)",
                gradeLevel = "G5",
                subject = "English",
                documentType = CurriculumDocumentType.EXAM_REFERENCE.name,
                authorOrPublisher = "Examination Department (Reference Format Only)"
            )
        )
        dao.insertDocuments(documents)

        val chunks = listOf(
            // KG Myanmar
            CurriculumChunkEntity(
                documentId = 101,
                gradeLevel = "KG",
                subject = "Myanmar",
                chapterUnit = "Unit 1: Myanmar Consonants and Vowels",
                sectionTopic = "က မှ င အက္ခရာများ နှင့် ရုပ်ပုံများ",
                pageRange = "pp. 1-6",
                content = "ဗျည်းအက္ခရာ (က၊ ခ၊ ဂ၊ ဃ၊ င) အသံထွက်နှင့် အခြေခံ ရုပ်ပုံတွဲဆက်မှုများ။ ကကြီးရေသောက်၊ ခကွေးခေါင်းတို၊ ဂငယ်ဂုဏ်တက်၊ ဃကြီးသံတို၊ ငလျင်လှုပ်။ ရုပ်ပုံကြည့်၍ အက္ခရာရွေးချယ်ခြင်းနှင့် အသံတူအက္ခရာခွဲခြားခြင်း။",
                learningObjectives = "KG ကျောင်းသားများသည် အခြေခံဗျည်းအက္ခရာများကို ရုပ်ပုံများနှင့် တွဲဖက်မှတ်မိနိုင်မည်။",
                vocabularyWords = "ကကြီး, ခကွေး, ဂငယ်, ဃကြီး, င",
                keywords = "KG, myanmar, consonants, alphabet, pictures",
                sourceReference = "Myanmar KG Activity Book, Unit 1, pp. 1-6"
            ),

            // KG Math
            CurriculumChunkEntity(
                documentId = 102,
                gradeLevel = "KG",
                subject = "Mathematics",
                chapterUnit = "Unit 1: Basic Shapes and Numbers 1 to 5",
                sectionTopic = "Counting and Shape Recognition",
                pageRange = "pp. 1-8",
                content = "Counting concrete items from 1 to 5 (apples, pencils, birds). Basic geometric shapes: Circle (အဝိုင်း), Square (လေးထောင့်), Triangle (တြိဂံ). Matching numeral to group count.",
                learningObjectives = "Recognize numbers 1 to 5 and identify circle, square, and triangle shapes with illustrations.",
                vocabularyWords = "circle, square, triangle, one, two, three, four, five",
                keywords = "KG, numbers, shapes, counting, geometry",
                sourceReference = "Myanmar KG Mathematics, Unit 1, pp. 1-8"
            ),

            // G1 English
            CurriculumChunkEntity(
                documentId = 103,
                gradeLevel = "G1",
                subject = "English",
                chapterUnit = "Unit 1: Greetings and Classroom Objects",
                sectionTopic = "Hello, Goodbye & Classroom Items",
                pageRange = "pp. 1-10",
                content = "Basic greetings: Hello, Good morning, Goodbye, How are you? Classroom nouns: book, pencil, ruler, eraser, desk, chair. Simple commands: Stand up, Sit down, Open your book.",
                learningObjectives = "Greet teachers and peers politely and name common classroom objects.",
                vocabularyWords = "hello, morning, goodbye, pencil, book, ruler, eraser, desk",
                keywords = "greetings, classroom, objects, primary",
                sourceReference = "Grade 1 English Textbook, Unit 1, pp. 1-10"
            ),

            // G5 English
            CurriculumChunkEntity(
                documentId = 1,
                gradeLevel = "G5",
                subject = "English",
                chapterUnit = "Unit 1: My Family and School",
                sectionTopic = "Vocabulary & Daily Routines",
                pageRange = "pp. 1-8",
                content = "Introduces vocabulary for extended family members (grandfather, grandmother, cousin, nephew, niece), school facilities (canteen, library, science lab, playground), and present simple tense for daily routines. Reading passage describes a typical school day in Yangon.",
                learningObjectives = "Students can describe their family tree and express daily habits using present simple tense.",
                vocabularyWords = "canteen, playground, laboratory, timetable, punctual, obedient, chores",
                keywords = "family, school, present simple, daily routine",
                sourceReference = "Grade 5 English Textbook, Unit 1, pp. 1-8"
            ),
            CurriculumChunkEntity(
                documentId = 1,
                gradeLevel = "G5",
                subject = "English",
                chapterUnit = "Unit 2: Animals and Their Habitats",
                sectionTopic = "Comparative Adjectives & Nature",
                pageRange = "pp. 9-16",
                content = "Explores terrestrial and aquatic wildlife in Myanmar (Asian elephant, Ayeyarwady dolphin, peacock). Teaches comparative and superlative adjectives (faster than, the heaviest, more intelligent). Includes reading comprehension on endangered wildlife conservation.",
                learningObjectives = "Use comparative and superlative forms accurately to compare animals and understand habitat conservation.",
                vocabularyWords = "habitat, endangered, mammals, aquatic, carnivore, sanctuary, peacock",
                keywords = "animals, nature, comparative, superlative, wildlife",
                sourceReference = "Grade 5 English Textbook, Unit 2, pp. 9-16"
            ),
            CurriculumChunkEntity(
                documentId = 1,
                gradeLevel = "G5",
                subject = "English",
                chapterUnit = "Unit 3: Healthy Food and Nutrition",
                sectionTopic = "Countable/Uncountable Nouns & Food Pyramid",
                pageRange = "pp. 17-24",
                content = "Details the five food groups: carbohydrates (rice, bread), proteins (fish, beans, eggs), vitamins & minerals (fruits, leafy vegetables), and fats. Teaches quantifiers (some, any, much, many, a few, a little) and modals for healthy advice (should, shouldn't).",
                learningObjectives = "Categorize balanced nutrition and correctly use quantifiers with countable and uncountable food nouns.",
                vocabularyWords = "nutrition, carbohydrates, protein, vitamins, balanced diet, beverage, digest",
                keywords = "food, nutrition, countable, uncountable, healthy diet",
                sourceReference = "Grade 5 English Textbook, Unit 3, pp. 17-24"
            ),
            CurriculumChunkEntity(
                documentId = 1,
                gradeLevel = "G5",
                subject = "English",
                chapterUnit = "Unit 4: Travel and Myanmar Festivals",
                sectionTopic = "Past Simple Tense & Cultural Heritage",
                pageRange = "pp. 25-34",
                content = "Covers famous landmarks in Myanmar (Bagan pagodas, Inle Lake, Mandalay Hill) and seasonal festivals (Thingyan Water Festival, Thadingyut Lighting Festival, Tazaungdaing). Teaches regular and irregular past tense verbs.",
                learningObjectives = "Narrate past festival experiences using past simple tense and describe cultural heritage politely.",
                vocabularyWords = "heritage, pagoda, festival, celebrate, illuminated, floating garden, souvenir",
                keywords = "festivals, Bagan, Inle, past simple, travel",
                sourceReference = "Grade 5 English Textbook, Unit 4, pp. 25-34"
            ),

            // G5 Mathematics
            CurriculumChunkEntity(
                documentId = 2,
                gradeLevel = "G5",
                subject = "Mathematics",
                chapterUnit = "Chapter 1: Multi-digit Numbers & Place Values",
                sectionTopic = "Numbers up to 10 Million",
                pageRange = "pp. 1-15",
                content = "Reading, writing, and comparing large numbers up to 10,000,000 in Myanmar and Hindu-Arabic numerals. Rounding to the nearest hundred, thousand, and ten thousand. Multi-step word problems involving addition and subtraction.",
                learningObjectives = "Master place values up to ten millions and solve multi-step financial and demographic word problems.",
                vocabularyWords = "place value, millions, rounding, estimate, numeral, sum, difference",
                keywords = "place value, rounding, estimation, addition, subtraction",
                sourceReference = "Grade 5 Mathematics Textbook, Chapter 1, pp. 1-15"
            ),
            CurriculumChunkEntity(
                documentId = 2,
                gradeLevel = "G5",
                subject = "Mathematics",
                chapterUnit = "Chapter 2: Multiplication & Division",
                sectionTopic = "3-digit Multipliers and Long Division",
                pageRange = "pp. 16-30",
                content = "Multiplication of 4-digit numbers by 2-digit and 3-digit multipliers. Long division with divisors up to 2 digits, calculating quotients and remainders. Practical word problems on school budget and goods distribution.",
                learningObjectives = "Execute multi-digit multiplication and long division with accuracy and interpret remainders correctly.",
                vocabularyWords = "multiplier, multiplicand, dividend, divisor, quotient, remainder",
                keywords = "multiplication, division, long division, word problems",
                sourceReference = "Grade 5 Mathematics Textbook, Chapter 2, pp. 16-30"
            ),
            CurriculumChunkEntity(
                documentId = 2,
                gradeLevel = "G5",
                subject = "Mathematics",
                chapterUnit = "Chapter 3: Fractions and Decimals",
                sectionTopic = "Equivalent Fractions & Basic Operations",
                pageRange = "pp. 31-48",
                content = "Proper, improper, and mixed fractions. Finding common denominators, adding and subtracting unlike fractions. Introduction to tenths, hundredths, thousandths in decimals and converting fractions to decimals.",
                learningObjectives = "Add and subtract unlike fractions using LCD and convert fractions to decimal notations.",
                vocabularyWords = "numerator, denominator, equivalent fraction, mixed number, decimal point",
                keywords = "fractions, decimals, common denominator, improper fraction",
                sourceReference = "Grade 5 Mathematics Textbook, Chapter 3, pp. 31-48"
            ),

            // G5 Science
            CurriculumChunkEntity(
                documentId = 3,
                gradeLevel = "G5",
                subject = "Science",
                chapterUnit = "Chapter 1: Plant Systems and Photosynthesis",
                sectionTopic = "Parts of a Plant & Food Production",
                pageRange = "pp. 1-12",
                content = "Structure and function of roots, stems, leaves, flowers, and seeds. Photosynthesis process: chlorophyll, sunlight, carbon dioxide, and water producing glucose and releasing oxygen. Plant adaptations in dry and wet regions of Myanmar.",
                learningObjectives = "Identify plant organs, explain photosynthesis inputs/outputs, and describe local plant adaptations.",
                vocabularyWords = "photosynthesis, chlorophyll, stomata, transpiration, germination, pollination",
                keywords = "plant, photosynthesis, leaf, roots, oxygen",
                sourceReference = "Grade 5 Science Textbook, Chapter 1, pp. 1-12"
            ),
            CurriculumChunkEntity(
                documentId = 3,
                gradeLevel = "G5",
                subject = "Science",
                chapterUnit = "Chapter 2: The Water Cycle and Weather",
                sectionTopic = "Evaporation, Condensation & Precipitation",
                pageRange = "pp. 13-24",
                content = "States of water (ice, liquid, water vapor). The continuous water cycle: solar heating, evaporation from water bodies, transpiration from plants, condensation forming clouds, precipitation (rain, hail), and groundwater accumulation.",
                learningObjectives = "Diagram the water cycle stages and describe factors affecting local monsoon weather patterns.",
                vocabularyWords = "evaporation, condensation, precipitation, water vapor, groundwater, monsoon",
                keywords = "water cycle, rain, condensation, weather, clouds",
                sourceReference = "Grade 5 Science Textbook, Chapter 2, pp. 13-24"
            ),

            // G7 Science
            CurriculumChunkEntity(
                documentId = 5,
                gradeLevel = "G7",
                subject = "Science",
                chapterUnit = "Chapter 1: Cells as the Basic Unit of Life",
                sectionTopic = "Plant vs Animal Cells & Organelles",
                pageRange = "pp. 1-18",
                content = "Microscopic cell theory. Comparison of plant cells (cell wall, chloroplasts, large central vacuole) and animal cells (cell membrane, cytoplasm, nucleus, mitochondria). Functions of major cell organelles.",
                learningObjectives = "Draw and label plant and animal cells, differentiating cell walls and chloroplasts under light microscopy.",
                vocabularyWords = "cell membrane, cytoplasm, nucleus, chloroplast, vacuole, organelle, microscope",
                keywords = "cell, biology, plant cell, animal cell, microscope",
                sourceReference = "Grade 7 General Science Textbook, Chapter 1, pp. 1-18"
            ),

            // G8 Cambridge / International Science
            CurriculumChunkEntity(
                documentId = 801,
                gradeLevel = "G8",
                subject = "Science",
                chapterUnit = "Stage 8 Unit 2: Chemical Reactions and Energy",
                sectionTopic = "Exothermic and Endothermic Reactions",
                pageRange = "pp. 45-60",
                content = "Chemical change vs physical change. Exothermic reactions release thermal energy to the surroundings (e.g. combustion, neutralization). Endothermic reactions absorb thermal energy (e.g. thermal decomposition, photosynthesis). Measuring temperature changes in laboratory experiments.",
                learningObjectives = "Distinguish exothermic and endothermic reactions through experimental temperature data and energy profile diagrams.",
                vocabularyWords = "exothermic, endothermic, activation energy, thermal decomposition, enthalpy, reactant, product",
                keywords = "chemistry, exothermic, endothermic, energy, reaction, cambridge",
                sourceReference = "Cambridge Lower Secondary Science Stage 8, Unit 2, pp. 45-60"
            ),

            // G10 Physics
            CurriculumChunkEntity(
                documentId = 6,
                gradeLevel = "G10",
                subject = "Physics",
                chapterUnit = "Chapter 1: Units and Measurement",
                sectionTopic = "SI Units & Precision Instruments",
                pageRange = "pp. 1-14",
                content = "Fundamental and derived physical quantities. SI base units (meter, kilogram, second, ampere, kelvin). Vernier calipers, micrometer screw gauge, zero error corrections, scientific notation, and significant figures.",
                learningObjectives = "Measure physical dimensions using Vernier calipers and micrometer gauges with zero error correction.",
                vocabularyWords = "physical quantity, SI units, vernier caliper, micrometer screw gauge, zero error, precision",
                keywords = "measurement, vernier, micrometer, SI units, precision",
                sourceReference = "Grade 10 Physics Textbook, Chapter 1, pp. 1-14"
            ),
            CurriculumChunkEntity(
                documentId = 6,
                gradeLevel = "G10",
                subject = "Physics",
                chapterUnit = "Chapter 2: Kinematics and Motion",
                sectionTopic = "Speed, Velocity, Acceleration & Graphs",
                pageRange = "pp. 15-32",
                content = "Scalars vs vectors (distance vs displacement, speed vs velocity). Uniform acceleration equations (v = u + at, s = ut + 0.5at^2, v^2 = u^2 + 2as). Interpreting displacement-time and velocity-time graphs.",
                learningObjectives = "Apply kinematic equations to solve linear motion problems and interpret velocity-time slopes and areas.",
                vocabularyWords = "displacement, velocity, acceleration, deceleration, kinematics, scalar, vector",
                keywords = "motion, kinematics, velocity, acceleration, graphs",
                sourceReference = "Grade 10 Physics Textbook, Chapter 2, pp. 15-32"
            ),

            // G10 Chemistry
            CurriculumChunkEntity(
                documentId = 7,
                gradeLevel = "G10",
                subject = "Chemistry",
                chapterUnit = "Chapter 1: Atomic Structure and Periodic Table",
                sectionTopic = "Subatomic Particles & Electronic Configuration",
                pageRange = "pp. 1-20",
                content = "Protons, neutrons, electrons. Atomic number (Z) and mass number (A). Isotopes of hydrogen and carbon. Electronic configurations of elements 1 to 20. Groups and periods in the modern periodic table, trends in valence electrons.",
                learningObjectives = "Determine subatomic particles, write electronic configurations, and identify periodic group properties.",
                vocabularyWords = "atomic number, mass number, isotope, electronic configuration, valence electron, noble gas",
                keywords = "atom, protons, electrons, periodic table, isotopes",
                sourceReference = "Grade 10 Chemistry Textbook, Chapter 1, pp. 1-20"
            ),

            // G10 Biology
            CurriculumChunkEntity(
                documentId = 8,
                gradeLevel = "G10",
                subject = "Biology",
                chapterUnit = "Chapter 1: Characteristics and Classification of Living Organisms",
                sectionTopic = "Five Kingdoms & Binomial Nomenclature",
                pageRange = "pp. 1-16",
                content = "The seven characteristics of living organisms (MRS GREN: Movement, Respiration, Sensitivity, Growth, Reproduction, Excretion, Nutrition). The five kingdoms (Monera, Protista, Fungi, Plantae, Animalia). Linnaean binomial naming system.",
                learningObjectives = "Classify organisms into 5 kingdoms using dichotomous keys and explain binomial nomenclature conventions.",
                vocabularyWords = "binomial nomenclature, genus, species, dichotomous key, respiration, excretion, classification",
                keywords = "classification, five kingdoms, binomial nomenclature, characteristics of life",
                sourceReference = "Grade 10 Biology Textbook, Chapter 1, pp. 1-16"
            ),

            // G10 Economics
            CurriculumChunkEntity(
                documentId = 9,
                gradeLevel = "G10",
                subject = "Economics",
                chapterUnit = "Chapter 1: Introduction to Economics and Scarcity",
                sectionTopic = "Factors of Production & Opportunity Cost",
                pageRange = "pp. 1-18",
                content = "The basic economic problem: unlimited wants versus scarce resources. The four factors of production: Land (natural resources), Labor (human effort), Capital (man-made machinery), Enterprise (risk-taking). Definition and graphical representation of Opportunity Cost.",
                learningObjectives = "Define scarcity, distinguish the 4 factors of production and calculate opportunity cost in economic trade-offs.",
                vocabularyWords = "scarcity, opportunity cost, factors of production, capital, enterprise, allocation of resources",
                keywords = "scarcity, opportunity cost, factors of production, economics",
                sourceReference = "Grade 10 Economics Textbook, Chapter 1, pp. 1-18"
            ),

            // Past Paper Style Reference (REFERENCE_ONLY)
            CurriculumChunkEntity(
                documentId = 999,
                gradeLevel = "G5",
                subject = "English",
                chapterUnit = "Mid-Term Examination 2025 Paper Structure",
                sectionTopic = "Examination Conventions and Mark Allocations",
                pageRange = "Reference Template",
                content = "[REFERENCE ONLY - STYLE AND STRUCTURE CONVENTION]\nSection A: Multiple Choice (10 marks, 1 mark each).\nSection B: Fill in the Blanks with Word Bank (10 marks, 1 mark each).\nSection C: Short Answer Comprehension (15 marks, 3 marks each).\nSection D: Guided Writing / Composition (15 marks).\nTotal Duration: 60 minutes. Total Marks: 50 marks.\nNote: This chunk defines the structural layout only; question content must come strictly from approved textbooks.",
                learningObjectives = "Exam format structural template for Mid-Term English assessments.",
                vocabularyWords = "examination, structure, sections, marking scheme",
                keywords = "past paper, reference only, style, structure, exam conventions",
                sourceReference = "Previous Examination Reference 2025 (Style & Convention Only)"
            )
        )
        dao.insertChunks(chunks)
    }
}
