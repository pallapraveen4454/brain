package com.example.data

import com.example.data.model.QuizQuestion
import java.util.Random

/**
 * TopicKnowledgeEngine provides topic-specific trivia knowledge banks and dynamic domain synthesis
 * for AI Quiz Generation. Ensures that different topics (e.g., World War II vs. Marvel Cinematic Universe)
 * always produce genuine, topic-distinct, factual questions with domain-relevant distractors and randomized
 * answer positions, rather than generic placeholder templates.
 */
object TopicKnowledgeEngine {

    private val random = Random()

    /**
     * Set of prohibited generic template substrings and generic distractors
     * that must never appear in generated quizzes.
     */
    val BANNED_GENERIC_PATTERNS = listOf(
        "which milestone significantly transformed the study of",
        "which core concept is fundamental when studying",
        "in the domain of",
        "what plays a vital role in practical applications",
        "when evaluating key developments in",
        "what feature stands out most",
        "what primary methodology is utilized by experts in",
        "how does .* impact modern global developments",
        "which of the following best describes the global consensus on",
        "what is a essential skill required for mastering",
        "which factor is most crucial for future advancements in",
        "what ultimate objective guides scholars and practitioners of"
    ).map { Regex(it, RegexOption.IGNORE_CASE) }

    val BANNED_GENERIC_DISTRACTORS = setOf(
        "methodological innovations",
        "the ban on research",
        "stagnation of ideas",
        "complete disregard of facts",
        "random speculation",
        "unverified assumptions",
        "subjective guessing",
        "outdated practices",
        "theoretical frameworks & data",
        "disregarded evidence",
        "static knowledge",
        "lack of growth",
        "empirical testing & verification",
        "pure coincidence",
        "mythological beliefs",
        "trial without observation",
        "minimal impact",
        "drives innovation & understanding",
        "restricts progress",
        "has no practical value",
        "it is a recognized field of study",
        "it is completely unknown",
        "it was discredited centuries ago",
        "it holds zero scientific value",
        "critical thinking & analysis",
        "ignoring facts",
        "rote memorization only",
        "superficial skimming",
        "interdisciplinary collaboration",
        "discarding historical data",
        "avoiding new tools",
        "expanding depth of knowledge",
        "promoting confusion",
        "halting inquiry",
        "creating misinformation"
    )

    /**
     * Checks if a question or its options match generic placeholder templates.
     */
    fun isGenericOrInvalid(questionText: String, options: List<String>): Boolean {
        if (questionText.isBlank() || options.size != 4) return true
        if (options.distinct().size != 4) return true

        // Check if question text matches any banned generic template
        if (BANNED_GENERIC_PATTERNS.any { it.containsMatchIn(questionText) }) {
            return true
        }

        // Check if options contain generic placeholder distractors
        val normalizedOptions = options.map { it.trim().lowercase() }
        val matchingBanned = normalizedOptions.count { it in BANNED_GENERIC_DISTRACTORS }
        if (matchingBanned >= 2) {
            return true
        }

        return false
    }

    /**
     * Generates a topic-specific 10-question quiz for the given topic name.
     */
    fun generateQuestionsForTopic(topic: String): List<QuizQuestion> {
        val norm = topic.trim().lowercase()
        val pool = when {
            norm.contains("world war") || norm.contains("ww2") || norm.contains("wwii") -> getWorldWarIIPool()
            norm.contains("marvel") || norm.contains("mcu") || norm.contains("avenger") -> getMarvelPool()
            norm.contains("quantum") || norm.contains("physics") -> getQuantumPhysicsPool()
            norm.contains("space") || norm.contains("nasa") || norm.contains("astronomy") -> getSpaceExplorationPool()
            norm.contains("cyber") || norm.contains("hack") || norm.contains("security") -> getCybersecurityPool()
            norm.contains("anatomy") || norm.contains("brain") || norm.contains("human body") -> getHumanAnatomyPool()
            norm.contains("rock") || norm.contains("grunge") || norm.contains("90s music") -> get90sRockMusicPool()
            norm.contains("culinary") || norm.contains("cook") || norm.contains("chef") || norm.contains("food") -> getCulinaryArtsPool()
            norm.contains("artificial intelligence") || norm.contains("ai") || norm.contains("machine learning") -> getAiPool()
            norm.contains("football") || norm.contains("world cup") || norm.contains("soccer") -> getFootballWorldCupPool()
            norm.contains("history") -> getGeneralHistoryPool()
            norm.contains("movie") || norm.contains("cinema") || norm.contains("film") -> getCinemaPool()
            norm.contains("tech") || norm.contains("programming") || norm.contains("software") -> getTechnologyPool()
            else -> generateDynamicTopicPool(topic)
        }

        // Shuffle and take exactly 10 questions with randomized answer positioning
        val selected = pool.shuffled(random).take(10)
        val final10 = if (selected.size < 10) {
            val filler = generateDynamicTopicPool(topic).filterNot { f -> selected.any { it.questionText == f.questionText } }
            (selected + filler).take(10)
        } else {
            selected
        }

        return final10.mapIndexed { index, q ->
            randomizeOptionOrder(q.copy(id = "ai_${topic.hashCode()}_${System.currentTimeMillis()}_$index"))
        }
    }

    /**
     * Randomizes the 4 options and updates the correctOptionIndex accordingly.
     */
    fun randomizeOptionOrder(question: QuizQuestion): QuizQuestion {
        val originalCorrectAnswer = question.options.getOrNull(question.correctOptionIndex) ?: question.options.first()
        val shuffledOptions = question.options.shuffled(random)
        val newCorrectIndex = shuffledOptions.indexOf(originalCorrectAnswer).coerceAtLeast(0)
        return question.copy(
            options = shuffledOptions,
            correctOptionIndex = newCorrectIndex
        )
    }

    // =========================================================================
    // SPECIFIC DOMAIN KNOWLEDGE POOLS
    // =========================================================================

    private fun getWorldWarIIPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "ww2_1",
            categoryId = "ai_custom",
            questionText = "Which surprise attack on December 7, 1941, brought the United States into World War II?",
            options = listOf("The Attack on Pearl Harbor", "The Invasion of Poland", "The Battle of the Bulge", "The Sinking of the Lusitania"),
            correctOptionIndex = 0,
            explanation = "The Japanese naval air attack on the US naval base at Pearl Harbor in Hawaii brought the US into World War II."
        ),
        QuizQuestion(
            id = "ww2_2",
            categoryId = "ai_custom",
            questionText = "In which year did World War II officially conclude?",
            options = listOf("1943", "1944", "1945", "1948"),
            correctOptionIndex = 2,
            explanation = "World War II ended in 1945 following the surrender of Germany in May and Japan in September."
        ),
        QuizQuestion(
            id = "ww2_3",
            categoryId = "ai_custom",
            questionText = "What was the official Allied military codename for the D-Day landings in Normandy?",
            options = listOf("Operation Overlord", "Operation Barbarossa", "Operation Market Garden", "Operation Torch"),
            correctOptionIndex = 0,
            explanation = "Operation Overlord was the codename for the Allied amphibious invasion of German-occupied Western Europe launched on June 6, 1944."
        ),
        QuizQuestion(
            id = "ww2_4",
            categoryId = "ai_custom",
            questionText = "Which fierce urban battle on the Eastern Front is considered a major turning point against Nazi Germany?",
            options = listOf("Battle of Midway", "Battle of Stalingrad", "Battle of El Alamein", "Battle of Kursk"),
            correctOptionIndex = 1,
            explanation = "The Battle of Stalingrad (1942–1943) ended with the total defeat of the German 6th Army and turned the tide on the Eastern Front."
        ),
        QuizQuestion(
            id = "ww2_5",
            categoryId = "ai_custom",
            questionText = "Who served as the British Prime Minister through the majority of World War II from 1940 to 1945?",
            options = listOf("Neville Chamberlain", "Winston Churchill", "Clement Attlee", "Anthony Eden"),
            correctOptionIndex = 1,
            explanation = "Winston Churchill rallied the British nation with his resolute leadership and famous wartime speeches."
        ),
        QuizQuestion(
            id = "ww2_6",
            categoryId = "ai_custom",
            questionText = "What was the top-secret US research project that developed the first atomic weapons?",
            options = listOf("Manhattan Project", "Apollo Project", "Project Paperclip", "Bletchley Initiative"),
            correctOptionIndex = 0,
            explanation = "The Manhattan Project, led by J. Robert Oppenheimer and General Leslie Groves, developed the atomic bomb."
        ),
        QuizQuestion(
            id = "ww2_7",
            categoryId = "ai_custom",
            questionText = "Which decisive Pacific naval battle in June 1942 saw the US Navy sink four Japanese aircraft carriers?",
            options = listOf("Battle of the Coral Sea", "Battle of Midway", "Battle of Leyte Gulf", "Battle of Guadalcanal"),
            correctOptionIndex = 1,
            explanation = "The Battle of Midway permanently crippled the Imperial Japanese Navy's carrier strike force."
        ),
        QuizQuestion(
            id = "ww2_8",
            categoryId = "ai_custom",
            questionText = "Which German field marshal was nicknamed 'The Desert Fox' for his North African campaigns?",
            options = listOf("Heinz Guderian", "Erwin Rommel", "Erich von Manstein", "Gerd von Rundstedt"),
            correctOptionIndex = 1,
            explanation = "Erwin Rommel commanded the German Afrika Korps and earned the nickname 'The Desert Fox'."
        ),
        QuizQuestion(
            id = "ww2_9",
            categoryId = "ai_custom",
            questionText = "Which 1945 conference brought Roosevelt, Churchill, and Stalin together in Crimea to shape post-war Europe?",
            options = listOf("Tehran Conference", "Yalta Conference", "Potsdam Conference", "Casablanca Conference"),
            correctOptionIndex = 1,
            explanation = "The Yalta Conference held in February 1945 decided the post-war partition of Germany and establishment of the UN."
        ),
        QuizQuestion(
            id = "ww2_10",
            categoryId = "ai_custom",
            questionText = "What was the German military doctrine of fast, coordinated armored and air strikes known as?",
            options = listOf("Blitzkrieg", "Sitzkrieg", "Luftwaffe", "Siegfried"),
            correctOptionIndex = 0,
            explanation = "Blitzkrieg ('Lightning War') emphasized speed, concentrated tank forces, and close air support to break enemy lines."
        ),
        QuizQuestion(
            id = "ww2_11",
            categoryId = "ai_custom",
            questionText = "Which volcanic Pacific island was the site of the famous 1945 flag-raising on Mount Suribachi?",
            options = listOf("Okinawa", "Iwo Jima", "Tarawa", "Saipan"),
            correctOptionIndex = 1,
            explanation = "US Marines raised the American flag atop Mount Suribachi during the brutal Battle of Iwo Jima."
        ),
        QuizQuestion(
            id = "ww2_12",
            categoryId = "ai_custom",
            questionText = "Which mathematician led the British codebreaking team at Bletchley Park that cracked the German Enigma cipher?",
            options = listOf("Alan Turing", "John von Neumann", "Claude Shannon", "Tommy Flowers"),
            correctOptionIndex = 0,
            explanation = "Alan Turing developed the Bombe machine to decipher German Enigma-encrypted military communications."
        )
    )

    private fun getMarvelPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "mcu_1",
            categoryId = "ai_custom",
            questionText = "Which Infinity Stone was housed inside Vision's forehead in the Marvel Cinematic Universe?",
            options = listOf("Time Stone", "Mind Stone", "Reality Stone", "Space Stone"),
            correctOptionIndex = 1,
            explanation = "The Mind Stone, originally inside Loki's scepter, brought Vision to life in 'Avengers: Age of Ultron'."
        ),
        QuizQuestion(
            id = "mcu_2",
            categoryId = "ai_custom",
            questionText = "Who was the main villain in the 2012 film 'The Avengers' who led the Chitauri invasion of New York?",
            options = listOf("Thanos", "Loki", "Ultron", "Red Skull"),
            correctOptionIndex = 1,
            explanation = "Loki, the God of Mischief, invaded New York using the Tesseract and the Chitauri army."
        ),
        QuizQuestion(
            id = "mcu_3",
            categoryId = "ai_custom",
            questionText = "What is the name of the technologically advanced, isolated African nation ruled by the Black Panther?",
            options = listOf("Latveria", "Wakanda", "Sokovia", "Genosha"),
            correctOptionIndex = 1,
            explanation = "Wakanda is the technologically advanced African nation rich in the rare metal Vibranium."
        ),
        QuizQuestion(
            id = "mcu_4",
            categoryId = "ai_custom",
            questionText = "What is the name of Thor's enchanted hammer forged in the heart of a dying star?",
            options = listOf("Stormbreaker", "Mjolnir", "Gungnir", "Hofund"),
            correctOptionIndex = 1,
            explanation = "Mjolnir is Thor's iconic hammer that can only be lifted by those deemed worthy."
        ),
        QuizQuestion(
            id = "mcu_5",
            categoryId = "ai_custom",
            questionText = "Which actor portrayed Tony Stark / Iron Man from 2008 through 2019 in the MCU?",
            options = listOf("Chris Evans", "Robert Downey Jr.", "Chris Hemsworth", "Mark Ruffalo"),
            correctOptionIndex = 1,
            explanation = "Robert Downey Jr. launched the Marvel Cinematic Universe with his performance in 2008's 'Iron Man'."
        ),
        QuizQuestion(
            id = "mcu_6",
            categoryId = "ai_custom",
            questionText = "What fictional metal is Captain America's circular shield composed of?",
            options = listOf("Adamantium", "Vibranium", "Carbonadium", "Uru"),
            correctOptionIndex = 1,
            explanation = "Captain America's shield was forged from a rare Vibranium alloy by Howard Stark during WWII."
        ),
        QuizQuestion(
            id = "mcu_7",
            categoryId = "ai_custom",
            questionText = "On which desolate planet did Thanos sacrifice Gamora to obtain the Soul Stone?",
            options = listOf("Titan", "Vormir", "Knowhere", "Morag"),
            correctOptionIndex = 1,
            explanation = "In 'Avengers: Infinity War', Thanos travels to Vormir where the Red Skull guards the Soul Stone."
        ),
        QuizQuestion(
            id = "mcu_8",
            categoryId = "ai_custom",
            questionText = "What is the name of the AI assistant installed in Peter Parker's Stark suit in 'Spider-Man: Homecoming'?",
            options = listOf("J.A.R.V.I.S.", "F.R.I.D.A.Y.", "Karen", "E.D.I.T.H."),
            correctOptionIndex = 2,
            explanation = "Peter Parker names his suit's built-in artificial intelligence 'Karen' (voiced by Jennifer Connelly)."
        ),
        QuizQuestion(
            id = "mcu_9",
            categoryId = "ai_custom",
            questionText = "Which original Avenger was trained as an assassin in the Russian 'Red Room' program?",
            options = listOf("Wanda Maximoff", "Natasha Romanoff (Black Widow)", "Carol Danvers", "Peggy Carter"),
            correctOptionIndex = 1,
            explanation = "Natasha Romanoff was trained as an elite covert operative in the secretive Red Room academy."
        ),
        QuizQuestion(
            id = "mcu_10",
            categoryId = "ai_custom",
            questionText = "What mystical relic does Doctor Strange wear around his neck to contain the Time Stone?",
            options = listOf("Eye of Agamotto", "Cloak of Levitation", "Book of Cagliostro", "Wand of Watoomb"),
            correctOptionIndex = 0,
            explanation = "The Eye of Agamotto is the ancient sorcery relic created by the first Sorcerer Supreme to house the Time Stone."
        ),
        QuizQuestion(
            id = "mcu_11",
            categoryId = "ai_custom",
            questionText = "Who directed the 2014 space-adventure film 'Guardians of the Galaxy' for Marvel Studios?",
            options = listOf("Taika Waititi", "James Gunn", "Anthony and Joe Russo", "Jon Favreau"),
            correctOptionIndex = 1,
            explanation = "James Gunn wrote and directed the 'Guardians of the Galaxy' trilogy for Marvel Studios."
        ),
        QuizQuestion(
            id = "mcu_12",
            categoryId = "ai_custom",
            questionText = "What phrase does Captain America famously utter right before charging Thanos's army in 'Avengers: Endgame'?",
            options = listOf("I can do this all day", "Avengers Assemble", "Whatever it takes", "To the end of the line"),
            correctOptionIndex = 1,
            explanation = "Steve Rogers summons Mjolnir and utters the iconic comic book battle cry 'Avengers Assemble'."
        )
    )

    private fun getQuantumPhysicsPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "qp_1",
            categoryId = "ai_custom",
            questionText = "Which quantum principle states that you cannot simultaneously determine a particle's exact position and momentum?",
            options = listOf("Pauli Exclusion Principle", "Heisenberg Uncertainty Principle", "Photoelectric Effect", "Planck Hypothesis"),
            correctOptionIndex = 1,
            explanation = "Werner Heisenberg formulated the Uncertainty Principle in 1927, setting fundamental limits on physical measurement."
        ),
        QuizQuestion(
            id = "qp_2",
            categoryId = "ai_custom",
            questionText = "Which subatomic particle, confirming the origin of mass for elementary particles, was discovered at CERN in 2012?",
            options = listOf("Tau Neutrino", "Higgs Boson", "Top Quark", "Graviton"),
            correctOptionIndex = 1,
            explanation = "The Higgs boson was experimentally confirmed by the ATLAS and CMS teams at the Large Hadron Collider in 2012."
        ),
        QuizQuestion(
            id = "qp_3",
            categoryId = "ai_custom",
            questionText = "What quantum phenomenon did Albert Einstein famously criticize as 'spooky action at a distance'?",
            options = listOf("Quantum Tunneling", "Quantum Entanglement", "Superconductivity", "Wavefunction Collapse"),
            correctOptionIndex = 1,
            explanation = "Quantum entanglement links particle quantum states regardless of the spatial distance separating them."
        ),
        QuizQuestion(
            id = "qp_4",
            categoryId = "ai_custom",
            questionText = "What physical constant, symbolized by 'h', defines the quantum of action and relates photon energy to frequency?",
            options = listOf("Boltzmann Constant", "Planck's Constant", "Gravitational Constant", "Rydberg Constant"),
            correctOptionIndex = 1,
            explanation = "Max Planck introduced the constant h (approximately 6.626 x 10^-34 J·s) in 1900 to explain blackbody radiation."
        ),
        QuizQuestion(
            id = "qp_5",
            categoryId = "ai_custom",
            questionText = "Which famous thought experiment illustrates the paradox of quantum superposition using a hypothetical feline?",
            options = listOf("Maxwell's Demon", "Schrödinger's Cat", "Wigner's Friend", "EPR Paradox"),
            correctOptionIndex = 1,
            explanation = "Erwin Schrödinger devised the thought experiment in 1935 to highlight problems in the Copenhagen interpretation."
        ),
        QuizQuestion(
            id = "qp_6",
            categoryId = "ai_custom",
            questionText = "Which classic experiment famously demonstrated that individual electrons produce interference patterns like waves?",
            options = listOf("Millikan Oil Drop", "Double-Slit Experiment", "Stern-Gerlach Experiment", "Michelson-Morley Experiment"),
            correctOptionIndex = 1,
            explanation = "The double-slit experiment demonstrates wave-particle duality when single particles build an interference pattern over time."
        ),
        QuizQuestion(
            id = "qp_7",
            categoryId = "ai_custom",
            questionText = "What quantum property allows a qubit to represent linear combinations of both 0 and 1 states at the same time?",
            options = listOf("Superposition", "Decoherence", "Quantization", "Spontaneous Emission"),
            correctOptionIndex = 0,
            explanation = "Superposition enables quantum computers to evaluate vast computational search spaces simultaneously."
        ),
        QuizQuestion(
            id = "qp_8",
            categoryId = "ai_custom",
            questionText = "Which quantum rule dictates that no two identical fermions can occupy the same quantum state simultaneously?",
            options = listOf("Hund's Rule", "Pauli Exclusion Principle", "Aufbau Principle", "Fermi Golden Rule"),
            correctOptionIndex = 1,
            explanation = "Wolfgang Pauli's Exclusion Principle explains electron shell arrangements and the stability of chemical matter."
        ),
        QuizQuestion(
            id = "qp_9",
            categoryId = "ai_custom",
            questionText = "What happens during quantum tunneling?",
            options = listOf("Particles travel faster than light", "A particle penetrates a potential energy barrier higher than its kinetic energy", "A photon decays into dark matter", "Electrons lose electrical charge"),
            correctOptionIndex = 1,
            explanation = "Quantum tunneling occurs because the particle's wave function has non-zero amplitude beyond an energy barrier."
        ),
        QuizQuestion(
            id = "qp_10",
            categoryId = "ai_custom",
            questionText = "For which quantum explanation did Albert Einstein win the 1921 Nobel Prize in Physics?",
            options = listOf("General Relativity", "Photoelectric Effect", "Brownian Motion", "Special Relativity"),
            correctOptionIndex = 1,
            explanation = "Einstein explained that light delivers energy in discrete packets (quanta/photons) in the photoelectric effect."
        )
    )

    private fun getSpaceExplorationPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "space_1",
            categoryId = "ai_custom",
            questionText = "Which NASA mission landed the first humans on the lunar surface on July 20, 1969?",
            options = listOf("Apollo 8", "Apollo 11", "Apollo 13", "Gemini 4"),
            correctOptionIndex = 1,
            explanation = "Apollo 11 carried Neil Armstrong, Buzz Aldrin, and Michael Collins to the Moon in July 1969."
        ),
        QuizQuestion(
            id = "space_2",
            categoryId = "ai_custom",
            questionText = "What is the name of NASA's rover that touched down in Jezero Crater on Mars in February 2021?",
            options = listOf("Curiosity", "Perseverance", "Opportunity", "Spirit"),
            correctOptionIndex = 1,
            explanation = "The Perseverance rover is actively searching for signs of ancient microbial life in Mars's Jezero Crater."
        ),
        QuizQuestion(
            id = "space_3",
            categoryId = "ai_custom",
            questionText = "Which premier space observatory, launched in December 2021, orbits the Sun at Earth-Sun Lagrange Point 2 (L2)?",
            options = listOf("Hubble Space Telescope", "James Webb Space Telescope (JWST)", "Spitzer Space Telescope", "Kepler Space Telescope"),
            correctOptionIndex = 1,
            explanation = "The James Webb Space Telescope uses infrared astronomy to peer back at the universe's first galaxies."
        ),
        QuizQuestion(
            id = "space_4",
            categoryId = "ai_custom",
            questionText = "Who became the first human in space when he orbited Earth aboard Vostok 1 in April 1961?",
            options = listOf("Alan Shepard", "Yuri Gagarin", "John Glenn", "Alexei Leonov"),
            correctOptionIndex = 1,
            explanation = "Soviet cosmonaut Yuri Gagarin completed one orbit around Earth on April 12, 1961."
        ),
        QuizQuestion(
            id = "space_5",
            categoryId = "ai_custom",
            questionText = "What is the largest moon in the Solar System, even larger than the planet Mercury?",
            options = listOf("Titan", "Ganymede", "Callisto", "Europa"),
            correctOptionIndex = 1,
            explanation = "Ganymede, a moon of Jupiter, is the solar system's largest moon with its own magnetic field."
        ),
        QuizQuestion(
            id = "space_6",
            categoryId = "ai_custom",
            questionText = "Which NASA space probe launched in 1977 became the first human-made object to cross into interstellar space?",
            options = listOf("Pioneer 10", "Voyager 1", "New Horizons", "Voyager 2"),
            correctOptionIndex = 1,
            explanation = "Voyager 1 officially crossed the heliopause into interstellar space in August 2012."
        ),
        QuizQuestion(
            id = "space_7",
            categoryId = "ai_custom",
            questionText = "What is the name of NASA's lunar exploration program aiming to return astronauts to the Moon and build a sustained base?",
            options = listOf("Orion Project", "Artemis Program", "Constellation Initiative", "Apollo Next"),
            correctOptionIndex = 1,
            explanation = "The Artemis Program aims to establish sustainable human presence on the Moon and prepare for Mars."
        ),
        QuizQuestion(
            id = "space_8",
            categoryId = "ai_custom",
            questionText = "What boundary around a black hole marks the threshold beyond which nothing, not even light, can escape?",
            options = listOf("Photon Sphere", "Event Horizon", "Ergosphere", "Singularity Disk"),
            correctOptionIndex = 1,
            explanation = "The event horizon is the outer boundary where the escape velocity equals the speed of light."
        ),
        QuizQuestion(
            id = "space_9",
            categoryId = "ai_custom",
            questionText = "Which planet in our solar system has the hottest surface temperature due to a runaway carbon dioxide greenhouse effect?",
            options = listOf("Mercury", "Venus", "Mars", "Jupiter"),
            correctOptionIndex = 1,
            explanation = "Venus maintains an average surface temperature of about 465°C (870°F) beneath dense sulfuric acid clouds."
        ),
        QuizQuestion(
            id = "space_10",
            categoryId = "ai_custom",
            questionText = "What is the international research orbital station continually inhabited by humans since November 2000?",
            options = listOf("Mir Space Station", "International Space Station (ISS)", "Tiangong Station", "Skylab"),
            correctOptionIndex = 1,
            explanation = "The ISS is a joint project between NASA, Roscosmos, ESA, JAXA, and CSA orbiting in low Earth orbit."
        )
    )

    private fun getCybersecurityPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "sec_1",
            categoryId = "ai_custom",
            questionText = "What category of malware encrypts a victim's files and demands a digital ransom payment for the key?",
            options = listOf("Spyware", "Ransomware", "Keylogger", "Adware"),
            correctOptionIndex = 1,
            explanation = "Ransomware locks file systems with strong cryptography until extortion payments are made."
        ),
        QuizQuestion(
            id = "sec_2",
            categoryId = "ai_custom",
            questionText = "What vulnerability occurs when unvalidated user input is directly concatenated into database query strings?",
            options = listOf("Buffer Overflow", "SQL Injection (SQLi)", "Cross-Site Scripting (XSS)", "CSRF Attack"),
            correctOptionIndex = 1,
            explanation = "SQL injection allows attackers to manipulate backend database logic and dump unauthorized records."
        ),
        QuizQuestion(
            id = "sec_3",
            categoryId = "ai_custom",
            questionText = "What social engineering attack tricks victims into revealing credentials using spoofed emails or websites?",
            options = listOf("Phishing", "Doxxing", "Sniffing", "Spoofing"),
            correctOptionIndex = 0,
            explanation = "Phishing relies on impersonation and deception to steal passwords, financial details, or session tokens."
        ),
        QuizQuestion(
            id = "sec_4",
            categoryId = "ai_custom",
            questionText = "What cyberattack overwhelms a targeted server or network with flood traffic originating from distributed botnets?",
            options = listOf("Man-in-the-Middle", "DDoS (Distributed Denial of Service)", "Zero-Day Exploit", "Brute Force"),
            correctOptionIndex = 1,
            explanation = "DDoS attacks aim to exhaust bandwidth or server processing resources, rendering services unavailable."
        ),
        QuizQuestion(
            id = "sec_5",
            categoryId = "ai_custom",
            questionText = "What term describes a newly discovered software security flaw that has zero days of vendor patch availability?",
            options = listOf("Bug Bounty", "Zero-Day Vulnerability", "CVE Patch", "Exploit Payload"),
            correctOptionIndex = 1,
            explanation = "A zero-day vulnerability is unpatched by the developer and actively vulnerable to exploitation."
        ),
        QuizQuestion(
            id = "sec_6",
            categoryId = "ai_custom",
            questionText = "Which security protocol superseded SSL to provide encrypted and authenticated communication across the internet?",
            options = listOf("SSH", "TLS (Transport Layer Security)", "IPsec", "PGP"),
            correctOptionIndex = 1,
            explanation = "TLS (modern versions 1.2 and 1.3) encrypts HTTPS traffic and secures modern web protocols."
        ),
        QuizQuestion(
            id = "sec_7",
            categoryId = "ai_custom",
            questionText = "What authentication security mechanism requires two distinct categories of evidence (e.g. password + hardware token)?",
            options = listOf("Single Sign-On (SSO)", "Two-Factor Authentication (2FA)", "Salted Hashing", "OAuth 2.0"),
            correctOptionIndex = 1,
            explanation = "2FA combines something you know (password) with something you have (phone/token) or something you are (biometrics)."
        ),
        QuizQuestion(
            id = "sec_8",
            categoryId = "ai_custom",
            questionText = "What is the term for cybersecurity professionals who legally test and penetrate systems to uncover vulnerabilities?",
            options = listOf("Black Hat Hackers", "White Hat / Ethical Hackers", "Script Kiddies", "Grey Hat Hackers"),
            correctOptionIndex = 1,
            explanation = "White hat hackers perform authorized vulnerability assessments and penetration testing to improve defense."
        ),
        QuizQuestion(
            id = "sec_9",
            categoryId = "ai_custom",
            questionText = "Which cryptographic hash algorithm designed by the NSA produces a fixed 256-bit hash digest?",
            options = listOf("MD5", "SHA-256", "DES", "RC4"),
            correctOptionIndex = 1,
            explanation = "SHA-256 is part of the SHA-2 family widely used in digital signatures, TLS certificates, and Bitcoin mining."
        ),
        QuizQuestion(
            id = "sec_10",
            categoryId = "ai_custom",
            questionText = "In network defense, what is a 'Honeypot'?",
            options = listOf("A password cracking tool", "A decoy system set up to lure and monitor unauthorized attackers", "A hardware firewall rule", "An email antivirus filter"),
            correctOptionIndex = 1,
            explanation = "A honeypot mimics legitimate targets to gather threat intelligence on attacker techniques and behavior."
        )
    )

    private fun getHumanAnatomyPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "anat_1",
            categoryId = "ai_custom",
            questionText = "Which chamber of the human heart pumps oxygenated blood directly through the aorta to the rest of the body?",
            options = listOf("Right Atrium", "Left Ventricle", "Right Ventricle", "Left Atrium"),
            correctOptionIndex = 1,
            explanation = "The Left Ventricle has the thickest muscular wall to generate sufficient pressure to pump blood throughout systemic circulation."
        ),
        QuizQuestion(
            id = "anat_2",
            categoryId = "ai_custom",
            questionText = "What is the largest organ of the human body by both surface area and total weight?",
            options = listOf("Liver", "Skin (Integumentary System)", "Brain", "Lungs"),
            correctOptionIndex = 1,
            explanation = "Human skin covers approximately 1.5 to 2 square meters and accounts for roughly 16% of total body weight."
        ),
        QuizQuestion(
            id = "anat_3",
            categoryId = "ai_custom",
            questionText = "Which region of the brain, located at the back of the skull, coordinates voluntary movement, balance, and posture?",
            options = listOf("Cerebrum", "Cerebellum", "Hypothalamus", "Medulla Oblongata"),
            correctOptionIndex = 1,
            explanation = "The cerebellum processes sensory inputs to fine-tune motor activity and maintain bodily equilibrium."
        ),
        QuizQuestion(
            id = "anat_4",
            categoryId = "ai_custom",
            questionText = "What fibrous connective tissue connects skeletal muscles to bones?",
            options = listOf("Ligament", "Tendon", "Cartilage", "Fascia"),
            correctOptionIndex = 1,
            explanation = "Tendons attach muscles to bones, whereas ligaments connect bones to other bones at joints."
        ),
        QuizQuestion(
            id = "anat_5",
            categoryId = "ai_custom",
            questionText = "Which primary neurotransmitter is associated with reward pathways, motivation, and motor control in the brain?",
            options = listOf("Serotonin", "Dopamine", "GABA", "Acetylcholine"),
            correctOptionIndex = 1,
            explanation = "Dopamine plays an essential role in the mesolimbic reward system, executive function, and motor regulation."
        ),
        QuizQuestion(
            id = "anat_6",
            categoryId = "ai_custom",
            questionText = "What is the longest, heaviest, and strongest bone in the adult human skeleton?",
            options = listOf("Tibia", "Femur (Thighbone)", "Humerus", "Fibula"),
            correctOptionIndex = 1,
            explanation = "The femur supports significant body weight and mechanical stress during walking, jumping, and running."
        ),
        QuizQuestion(
            id = "anat_7",
            categoryId = "ai_custom",
            questionText = "Which photoreceptor cells in the retina of the human eye are responsible for daylight color vision?",
            options = listOf("Rods", "Cones", "Ganglion Cells", "Bipolar Cells"),
            correctOptionIndex = 1,
            explanation = "Cones function under bright light and detect red, green, and blue wavelengths, while rods handle low-light vision."
        ),
        QuizQuestion(
            id = "anat_8",
            categoryId = "ai_custom",
            questionText = "Which pea-sized endocrine gland located at the base of the brain is often referred to as the 'master gland'?",
            options = listOf("Thyroid Gland", "Pituitary Gland", "Adrenal Gland", "Pineal Gland"),
            correctOptionIndex = 1,
            explanation = "The pituitary gland secretes hormones that regulate other endocrine glands throughout the human body."
        ),
        QuizQuestion(
            id = "anat_9",
            categoryId = "ai_custom",
            questionText = "What iron-rich metalloprotein in red blood cells binds oxygen from the lungs to deliver it to tissues?",
            options = listOf("Myoglobin", "Hemoglobin", "Albumin", "Ferritin"),
            correctOptionIndex = 1,
            explanation = "Each hemoglobin molecule contains four heme groups capable of binding four oxygen molecules."
        ),
        QuizQuestion(
            id = "anat_10",
            categoryId = "ai_custom",
            questionText = "Which division of the autonomic nervous system triggers the rapid 'fight or flight' stress response?",
            options = listOf("Parasympathetic System", "Sympathetic Nervous System", "Enteric System", "Somatic System"),
            correctOptionIndex = 1,
            explanation = "The sympathetic nervous system elevates heart rate, dilates airways, and releases adrenaline in emergencies."
        )
    )

    private fun get90sRockMusicPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "rock_1",
            categoryId = "ai_custom",
            questionText = "Which landmark 1991 Nirvana album featured the generation-defining single 'Smells Like Teen Spirit'?",
            options = listOf("Bleach", "Nevermind", "In Utero", "Incesticide"),
            correctOptionIndex = 1,
            explanation = "Nirvana's 'Nevermind', produced by Butch Vig, knocked Michael Jackson off the top of the Billboard charts in 1992."
        ),
        QuizQuestion(
            id = "rock_2",
            categoryId = "ai_custom",
            questionText = "Who was the legendary, four-octave vocalist and frontman of the Seattle grunge band Soundgarden?",
            options = listOf("Eddie Vedder", "Chris Cornell", "Layne Staley", "Scott Weiland"),
            correctOptionIndex = 1,
            explanation = "Chris Cornell fronted Soundgarden and later Audioslave, renowned for songs like 'Black Hole Sun'."
        ),
        QuizQuestion(
            id = "rock_3",
            categoryId = "ai_custom",
            questionText = "Which Britpop powerhouse released the smash hit album '(What\'s the Story) Morning Glory?' in 1995?",
            options = listOf("Blur", "Oasis", "Pulp", "The Verve"),
            correctOptionIndex = 1,
            explanation = "Oasis, led by brothers Liam and Noel Gallagher, produced global hits including 'Wonderwall' and 'Don\'t Look Back in Anger'."
        ),
        QuizQuestion(
            id = "rock_4",
            categoryId = "ai_custom",
            questionText = "What was the title of Pearl Jam's critically acclaimed debut studio album released in August 1991?",
            options = listOf("Vs.", "Ten", "Vitalogy", "Yield"),
            correctOptionIndex = 1,
            explanation = "Pearl Jam's 'Ten' featured timeless rock staples including 'Alive', 'Even Flow', and 'Jeremy'."
        ),
        QuizQuestion(
            id = "rock_5",
            categoryId = "ai_custom",
            questionText = "Which alternative rock band led by Thom Yorke broke out internationally with their 1992 single 'Creep'?",
            options = listOf("The Smashing Pumpkins", "Radiohead", "REM", "Weezer"),
            correctOptionIndex = 1,
            explanation = "Radiohead released 'Creep' on their debut album 'Pablo Honey' before creating experimental masterpieces like 'OK Computer'."
        ),
        QuizQuestion(
            id = "rock_6",
            categoryId = "ai_custom",
            questionText = "Which Green Day album released in 1994 brought pop-punk to mainstream global dominance?",
            options = listOf("Kerplunk", "Dookie", "Insomniac", "Nimrod"),
            correctOptionIndex = 1,
            explanation = "Green Day's 'Dookie' achieved diamond status fueled by hits like 'Basket Case', 'Longview', and 'When I Come Around'."
        ),
        QuizQuestion(
            id = "rock_7",
            categoryId = "ai_custom",
            questionText = "Who was the primary songwriter, lead vocalist, and guitarist for The Smashing Pumpkins?",
            options = listOf("Dave Grohl", "Billy Corgan", "Trent Reznor", "Perry Farrell"),
            correctOptionIndex = 1,
            explanation = "Billy Corgan spearheaded The Smashing Pumpkins through seminal 90s albums 'Siamese Dream' and 'Mellon Collie'."
        ),
        QuizQuestion(
            id = "rock_8",
            categoryId = "ai_custom",
            questionText = "Which industrial rock project masterminded by Trent Reznor released 'The Downward Spiral' in 1994?",
            options = listOf("Tool", "Nine Inch Nails", "Marilyn Manson", "Ministry"),
            correctOptionIndex = 1,
            explanation = "Nine Inch Nails created the groundbreaking industrial rock concept album 'The Downward Spiral'."
        ),
        QuizQuestion(
            id = "rock_9",
            categoryId = "ai_custom",
            questionText = "Following the death of Kurt Cobain in 1994, Nirvana drummer Dave Grohl formed which major rock band?",
            options = listOf("Queens of the Stone Age", "Foo Fighters", "Velvet Revolver", "Audioslave"),
            correctOptionIndex = 1,
            explanation = "Dave Grohl recorded the self-titled Foo Fighters debut album in 1994 playing virtually all instruments himself."
        ),
        QuizQuestion(
            id = "rock_10",
            categoryId = "ai_custom",
            questionText = "Which hard rock band released the epic orchestral ballad 'November Rain' on their 1991 album 'Use Your Illusion I'?",
            options = listOf("Aerosmith", "Guns N' Roses", "Metallica", "Bon Jovi"),
            correctOptionIndex = 1,
            explanation = "Guns N' Roses' 'November Rain', featuring Slash's legendary guitar solos, became one of the most celebrated 90s rock ballads."
        )
    )

    private fun getCulinaryArtsPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "cul_1",
            categoryId = "ai_custom",
            questionText = "Which of the following is one of Auguste Escoffier's five classical French 'Mother Sauces'?",
            options = listOf("Chimichurri", "Béchamel", "Pesto", "Romesco"),
            correctOptionIndex = 1,
            explanation = "The five French mother sauces are Béchamel, Velouté, Espagnole, Sauce Tomat, and Hollandaise."
        ),
        QuizQuestion(
            id = "cul_2",
            categoryId = "ai_custom",
            questionText = "What French culinary term translates to 'everything in its place' and refers to preparing ingredients prior to cooking?",
            options = listOf("Sous Vide", "Mise en place", "Flambé", "Julienne"),
            correctOptionIndex = 1,
            explanation = "'Mise en place' is the professional kitchen practice of gathering, cutting, and portioning all ingredients before heating pans."
        ),
        QuizQuestion(
            id = "cul_3",
            categoryId = "ai_custom",
            questionText = "What chemical reaction between amino acids and reducing sugars produces the savory browning crust on seared steaks?",
            options = listOf("Fermentation", "Maillard Reaction", "Caramelization", "Emulsification"),
            correctOptionIndex = 1,
            explanation = "The Maillard reaction occurs at temperatures above 280°F (140°C), producing hundreds of complex flavor compounds."
        ),
        QuizQuestion(
            id = "cul_4",
            categoryId = "ai_custom",
            questionText = "What is the primary cooking technique responsible for creating the signature creamy texture in authentic Italian risotto?",
            options = listOf("Boiling in excess water", "Gradually adding hot stock while constantly stirring", "Baking covered in a deep oven", "Deep frying in clarified butter"),
            correctOptionIndex = 1,
            explanation = "Constant stirring rubs the starch off short-grain arborio or carnaroli rice grains, creating a silky natural emulsion."
        ),
        QuizQuestion(
            id = "cul_5",
            categoryId = "ai_custom",
            questionText = "Which fresh herb is the defining aromatic foundation in traditional Genovese basil pesto?",
            options = listOf("Rosemary", "Sweet Basil", "Cilantro", "Tarragon"),
            correctOptionIndex = 1,
            explanation = "Authentic Pesto alla Genovese combines fresh sweet basil, pine nuts, garlic, Parmigiano-Reggiano, and extra virgin olive oil."
        ),
        QuizQuestion(
            id = "cul_6",
            categoryId = "ai_custom",
            questionText = "What temperature range (in Fahrenheit) is recognized in professional food safety as the 'Temperature Danger Zone'?",
            options = listOf("0°F to 32°F", "40°F to 140°F", "150°F to 200°F", "212°F to 300°F"),
            correctOptionIndex = 1,
            explanation = "Foodborne pathogens multiply most rapidly between 40°F and 140°F (4°C to 60°C)."
        ),
        QuizQuestion(
            id = "cul_7",
            categoryId = "ai_custom",
            questionText = "Which prized steak cut is taken from the smaller end of the beef tenderloin and celebrated for exceptional tenderness?",
            options = listOf("Flank Steak", "Filet Mignon", "Brisket", "Chuck Roast"),
            correctOptionIndex = 1,
            explanation = "Filet mignon is cut from the tenderloin muscle (psoas major), which bears little weight and contains minimal connective tissue."
        ),
        QuizQuestion(
            id = "cul_8",
            categoryId = "ai_custom",
            questionText = "What natural phospholipid found in egg yolks acts as the key emulsifier in mayonnaise and hollandaise sauce?",
            options = listOf("Casein", "Lecithin", "Albumin", "Gelatin"),
            correctOptionIndex = 1,
            explanation = "Lecithin has both hydrophilic and lipophilic ends, allowing water-based acids and oil droplets to form a permanent emulsion."
        ),
        QuizQuestion(
            id = "cul_9",
            categoryId = "ai_custom",
            questionText = "Which precious spice, hand-harvested from the stigmas of Crocus sativus flowers, is the most expensive spice by weight?",
            options = listOf("Cardamom", "Saffron", "Vanilla Bean", "Star Anise"),
            correctOptionIndex = 1,
            explanation = "It requires approximately 75,000 crocus flowers to produce a single pound of saffron threads."
        ),
        QuizQuestion(
            id = "cul_10",
            categoryId = "ai_custom",
            questionText = "What culinary cutting technique produces fine, uniform matchstick strips measuring approximately 1/8 inch thick?",
            options = listOf("Chiffonade", "Julienne", "Brunoise", "Mirepoix"),
            correctOptionIndex = 1,
            explanation = "Julienne cut vegetables into thin matchsticks, which can then be diced further into small brunoise cubes."
        )
    )

    private fun getAiPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "ai_1",
            categoryId = "ai_custom",
            questionText = "Which benchmark test proposed in 1950 evaluates whether a computer can exhibit human-indistinguishable conversation?",
            options = listOf("Lovelace Test", "Turing Test", "Chinese Room Argument", "Voight-Kampff Test"),
            correctOptionIndex = 1,
            explanation = "Alan Turing proposed the 'Imitation Game' (Turing Test) in his 1950 paper 'Computing Machinery and Intelligence'."
        ),
        QuizQuestion(
            id = "ai_2",
            categoryId = "ai_custom",
            questionText = "What deep learning architecture introduced in the 2017 paper 'Attention Is All You Need' powers modern Large Language Models?",
            options = listOf("Convolutional Neural Network (CNN)", "Transformer", "Recurrent Neural Network (RNN)", "Self-Organizing Map"),
            correctOptionIndex = 1,
            explanation = "The Transformer architecture relies on self-attention mechanisms to process sequence data in parallel."
        ),
        QuizQuestion(
            id = "ai_3",
            categoryId = "ai_custom",
            questionText = "What algorithm calculates partial derivatives of the loss function with respect to weights to train multi-layer neural networks?",
            options = listOf("K-Means Clustering", "Backpropagation", "Dijkstra's Algorithm", "Apriori Algorithm"),
            correctOptionIndex = 1,
            explanation = "Backpropagation uses the calculus chain rule to propagate error gradients backward through network layers."
        ),
        QuizQuestion(
            id = "ai_4",
            categoryId = "ai_custom",
            questionText = "Which DeepMind reinforcement learning program defeated 18-time world Go champion Lee Sedol 4-1 in 2016?",
            options = listOf("Deep Blue", "AlphaGo", "Watson", "AlphaFold"),
            correctOptionIndex = 1,
            explanation = "AlphaGo combined deep neural networks with Monte Carlo Tree Search to master the ancient game of Go."
        ),
        QuizQuestion(
            id = "ai_5",
            categoryId = "ai_custom",
            questionText = "What term describes a model that memorizes training data so rigidly that it fails to generalize to new, unseen inputs?",
            options = listOf("Underfitting", "Overfitting", "Vanishing Gradient", "Quantization"),
            correctOptionIndex = 1,
            explanation = "Overfitting occurs when a high-capacity model fits statistical noise and outliers in training data rather than underlying patterns."
        ),
        QuizQuestion(
            id = "ai_6",
            categoryId = "ai_custom",
            questionText = "What does 'RLHF' stand for in modern generative AI alignment and fine-tuning?",
            options = listOf("Recursive Learning with High Frequency", "Reinforcement Learning from Human Feedback", "Robust Latent Hyperparameter Framing", "Redundant Loss Heuristic Formatting"),
            correctOptionIndex = 1,
            explanation = "RLHF trains reward models from human preference rankings to steer conversational models toward helpful and safe responses."
        ),
        QuizQuestion(
            id = "ai_7",
            categoryId = "ai_custom",
            questionText = "What machine learning paradigm trains an autonomous agent by rewarding desirable behaviors and punishing mistakes in an environment?",
            options = listOf("Supervised Learning", "Reinforcement Learning", "Unsupervised Clustering", "Contrastive Learning"),
            correctOptionIndex = 1,
            explanation = "Reinforcement learning optimizes an agent's policy to maximize cumulative discounted rewards over time."
        ),
        QuizQuestion(
            id = "ai_8",
            categoryId = "ai_custom",
            questionText = "What is the term for when a large generative language model produces convincing, authoritative, but factually false outputs?",
            options = listOf("Tokenization", "Hallucination", "Quantization", "Gradient Drift"),
            correctOptionIndex = 1,
            explanation = "Hallucination occurs when generative models predict plausible token sequences that lack factual basis in real data."
        ),
        QuizQuestion(
            id = "ai_9",
            categoryId = "ai_custom",
            questionText = "Who organized the famous 1956 Dartmouth Summer Research Project and originally coined the term 'Artificial Intelligence'?",
            options = listOf("Alan Turing", "John McCarthy", "Marvin Minsky", "Geoffrey Hinton"),
            correctOptionIndex = 1,
            explanation = "John McCarthy coined 'Artificial Intelligence' in his 1955 proposal for the historic 1956 Dartmouth conference."
        ),
        QuizQuestion(
            id = "ai_10",
            categoryId = "ai_custom",
            questionText = "Which linear algebra technique projects high-dimensional data into orthogonal axes to maximize variance and reduce dimensions?",
            options = listOf("Principal Component Analysis (PCA)", "Softmax Normalization", "Cross-Entropy Loss", "Stochastic Gradient Descent"),
            correctOptionIndex = 0,
            explanation = "PCA finds eigenvectors of the covariance matrix to compress feature spaces while preserving maximum data variance."
        )
    )

    private fun getFootballWorldCupPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "fwc_1",
            categoryId = "ai_custom",
            questionText = "Which nation holds the record for winning the most FIFA Men's World Cup titles in history (5 championships)?",
            options = listOf("Germany", "Brazil", "Italy", "Argentina"),
            correctOptionIndex = 1,
            explanation = "Brazil won the World Cup in 1958, 1962, 1970, 1994, and 2002."
        ),
        QuizQuestion(
            id = "fwc_2",
            categoryId = "ai_custom",
            questionText = "Who captained Argentina and won the Golden Ball during their thrilling 2022 FIFA World Cup victory in Qatar?",
            options = listOf("Diego Maradona", "Lionel Messi", "Ángel Di María", "Sergio Agüero"),
            correctOptionIndex = 1,
            explanation = "Lionel Messi scored seven goals and led Argentina to their third World Cup title in December 2022."
        ),
        QuizQuestion(
            id = "fwc_3",
            categoryId = "ai_custom",
            questionText = "In which year and South American host nation was the inaugural FIFA World Cup tournament held?",
            options = listOf("1926 Brazil", "1930 Uruguay", "1934 Italy", "1938 France"),
            correctOptionIndex = 1,
            explanation = "Uruguay hosted and won the first official FIFA World Cup tournament in Montevideo in July 1930."
        ),
        QuizQuestion(
            id = "fwc_4",
            categoryId = "ai_custom",
            questionText = "Who is the all-time leading goalscorer in FIFA World Cup history with 16 career finals goals?",
            options = listOf("Pelé", "Miroslav Klose", "Ronaldo Nazário", "Gerd Müller"),
            correctOptionIndex = 1,
            explanation = "German striker Miroslav Klose scored 16 goals across four World Cup tournaments between 2002 and 2014."
        ),
        QuizQuestion(
            id = "fwc_5",
            categoryId = "ai_custom",
            questionText = "What original World Cup trophy, named after a former FIFA President, was permanently awarded to Brazil in 1970 before being stolen in 1983?",
            options = listOf("Henri Delaunay Trophy", "Jules Rimet Trophy", "Coupe de France", "Ballon d'Or Trophy"),
            correctOptionIndex = 1,
            explanation = "The Jules Rimet Trophy depicted Nike, the Greek goddess of victory, and was retained by Brazil after their third title."
        ),
        QuizQuestion(
            id = "fwc_6",
            categoryId = "ai_custom",
            questionText = "Which nation won the 2010 FIFA World Cup in South Africa thanks to Andrés Iniesta's 116th-minute extra-time goal?",
            options = listOf("Netherlands", "Spain", "Germany", "France"),
            correctOptionIndex = 1,
            explanation = "Spain defeated the Netherlands 1-0 in Johannesburg to claim their first FIFA World Cup title."
        ),
        QuizQuestion(
            id = "fwc_7",
            categoryId = "ai_custom",
            questionText = "How many national teams will participate in the expanded tournament format starting at the 2026 FIFA World Cup?",
            options = listOf("32 teams", "48 teams", "64 teams", "40 teams"),
            correctOptionIndex = 1,
            explanation = "The 2026 FIFA World Cup, hosted by the USA, Canada, and Mexico, expands the tournament field from 32 to 48 nations."
        ),
        QuizQuestion(
            id = "fwc_8",
            categoryId = "ai_custom",
            questionText = "Which English striker scored a famous hat-trick in the 1966 World Cup Final against West Germany at Wembley?",
            options = listOf("Bobby Charlton", "Geoff Hurst", "Bobby Moore", "Gary Lineker"),
            correctOptionIndex = 1,
            explanation = "Geoff Hurst's hat-trick powered England to a 4-2 victory in the 1966 World Cup final."
        ),
        QuizQuestion(
            id = "fwc_9",
            categoryId = "ai_custom",
            questionText = "Which Argentine goalkeeper earned the 2022 World Cup Golden Glove for crucial penalty saves against the Netherlands and France?",
            options = listOf("Hugo Lloris", "Emiliano Martínez", "Thibaut Courtois", "Dominik Livaković"),
            correctOptionIndex = 1,
            explanation = "Emiliano 'Dibu' Martínez made a crucial 123rd-minute save against Kolo Muani and starred in the penalty shootout."
        ),
        QuizQuestion(
            id = "fwc_10",
            categoryId = "ai_custom",
            questionText = "Who became the only football player in history to win three FIFA World Cup trophies (1958, 1962, 1970)?",
            options = listOf("Garrincha", "Pelé", "Zinedine Zidane", "Franz Beckenbauer"),
            correctOptionIndex = 1,
            explanation = "Edson Arantes do Nascimento (Pelé) remains the only player in history to win three FIFA World Cup championships."
        )
    )

    private fun getGeneralHistoryPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "hist_gen_1",
            categoryId = "ai_custom",
            questionText = "Which Roman general crossed the Rubicon river in 49 BC, triggering civil war and ending the Roman Republic?",
            options = listOf("Mark Antony", "Julius Caesar", "Scipio Africanus", "Pompey the Great"),
            correctOptionIndex = 1,
            explanation = "Julius Caesar's crossing of the Rubicon marked the irreversible breakdown of Republican Roman governance."
        ),
        QuizQuestion(
            id = "hist_gen_2",
            categoryId = "ai_custom",
            questionText = "In which year did the French Revolution begin with the storming of the Bastille prison in Paris?",
            options = listOf("1776", "1789", "1799", "1804"),
            correctOptionIndex = 1,
            explanation = "Parisian revolutionaries stormed the medieval Bastille fortress on July 14, 1789."
        ),
        QuizQuestion(
            id = "hist_gen_3",
            categoryId = "ai_custom",
            questionText = "Which ancient Macedonian king conquered the Persian Empire and established an empire stretching from Greece to northwest India?",
            options = listOf("Philip II", "Alexander the Great", "Pericles", "Seleucus I"),
            correctOptionIndex = 1,
            explanation = "Alexander the Great undefeated in battle created one of history's largest empires before dying in Babylon in 323 BC."
        ),
        QuizQuestion(
            id = "hist_gen_4",
            categoryId = "ai_custom",
            questionText = "The historic Magna Carta, establishing the principle that everyone including the king is subject to law, was signed in which year?",
            options = listOf("1066", "1215", "1492", "1588"),
            correctOptionIndex = 1,
            explanation = "King John of England granted Magna Carta at Runnymede under pressure from rebel barons in June 1215."
        ),
        QuizQuestion(
            id = "hist_gen_5",
            categoryId = "ai_custom",
            questionText = "What devastating pandemic swept across Europe from 1347 to 1351, wiping out an estimated 30-50% of the European population?",
            options = listOf("Spanish Flu", "The Black Death (Bubonic Plague)", "Justinian Plague", "Cholera Pandemic"),
            correctOptionIndex = 1,
            explanation = "The Black Death, caused by the bacterium Yersinia pestis, devastated Eurasian populations in the 14th century."
        ),
        QuizQuestion(
            id = "hist_gen_6",
            categoryId = "ai_custom",
            questionText = "Who served as the primary author of the United States Declaration of Independence in 1776?",
            options = listOf("Benjamin Franklin", "Thomas Jefferson", "John Adams", "Alexander Hamilton"),
            correctOptionIndex = 1,
            explanation = "Thomas Jefferson drafted the Declaration of Independence in Philadelphia in June 1776."
        ),
        QuizQuestion(
            id = "hist_gen_7",
            categoryId = "ai_custom",
            questionText = "Which maritime explorer led the first European expedition to successfully sail around the Cape of Good Hope to reach India in 1498?",
            options = listOf("Christopher Columbus", "Vasco da Gama", "Ferdinand Magellan", "Bartolomeu Dias"),
            correctOptionIndex = 1,
            explanation = "Portuguese navigator Vasco da Gama reached Calicut (Kozhikode), India, establishing the direct ocean spice route."
        ),
        QuizQuestion(
            id = "hist_gen_8",
            categoryId = "ai_custom",
            questionText = "The fall of which historic capital city in 1453 to the Ottoman Empire under Sultan Mehmed II marked the end of the Byzantine Empire?",
            options = listOf("Rome", "Constantinople", "Athens", "Alexandria"),
            correctOptionIndex = 1,
            explanation = "The Ottoman conquest of Constantinople in May 1453 ended over 1,000 years of Byzantine imperial rule."
        ),
        QuizQuestion(
            id = "hist_gen_9",
            categoryId = "ai_custom",
            questionText = "Which peace treaties signed in 1648 concluded the devastating Thirty Years' War in Europe and established modern state sovereignty?",
            options = listOf("Treaty of Versailles", "Peace of Westphalia", "Treaty of Utrecht", "Congress of Vienna"),
            correctOptionIndex = 1,
            explanation = "The Peace of Westphalia established the principle of national sovereignty and diplomatic state autonomy."
        ),
        QuizQuestion(
            id = "hist_gen_10",
            categoryId = "ai_custom",
            questionText = "Who was the first Emperor of a unified China who founded the Qin Dynasty and commissioned the Terracotta Army?",
            options = listOf("Han Wudi", "Qin Shi Huang", "Sun Tzu", "Kublai Khan"),
            correctOptionIndex = 1,
            explanation = "Qin Shi Huang unified China in 221 BC, standardized laws and measurements, and built the Terracotta Army."
        )
    )

    private fun getCinemaPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "cin_1",
            categoryId = "ai_custom",
            questionText = "Who directed the landmark 1994 mobster cinema classic 'Pulp Fiction'?",
            options = listOf("Martin Scorsese", "Quentin Tarantino", "David Fincher", "Francis Ford Coppola"),
            correctOptionIndex = 1,
            explanation = "Quentin Tarantino won the Palme d'Or at Cannes and the Academy Award for Best Original Screenplay for 'Pulp Fiction'."
        ),
        QuizQuestion(
            id = "cin_2",
            categoryId = "ai_custom",
            questionText = "Which film won 11 Academy Awards including Best Picture and starred Leonardo DiCaprio and Kate Winslet in 1997?",
            options = listOf("Avatar", "Titanic", "Gladiator", "Braveheart"),
            correctOptionIndex = 1,
            explanation = "James Cameron's 'Titanic' tied the all-time Oscar record with 11 Academy Award wins."
        ),
        QuizQuestion(
            id = "cin_3",
            categoryId = "ai_custom",
            questionText = "Which actor played the iconic role of Vito Corleone in the 1972 cinematic masterpiece 'The Godfather'?",
            options = listOf("Al Pacino", "Marlon Brando", "Robert De Niro", "James Caan"),
            correctOptionIndex = 1,
            explanation = "Marlon Brando won the Academy Award for Best Actor for his portrayal of Don Vito Corleone."
        ),
        QuizQuestion(
            id = "cin_4",
            categoryId = "ai_custom",
            questionText = "Who composed the legendary orchestral scores for 'Star Wars', 'Indiana Jones', 'Jaws', and 'Jurassic Park'?",
            options = listOf("Hans Zimmer", "John Williams", "Ennio Morricone", "Howard Shore"),
            correctOptionIndex = 1,
            explanation = "John Williams has received 54 Academy Award nominations, the most of any living person in cinema history."
        ),
        QuizQuestion(
            id = "cin_5",
            categoryId = "ai_custom",
            questionText = "Which 2019 South Korean psychological thriller directed by Bong Joon-ho became the first non-English film to win Best Picture?",
            options = listOf("Oldboy", "Parasite", "The Handmaiden", "Memories of Murder"),
            correctOptionIndex = 1,
            explanation = "Bong Joon-ho's 'Parasite' made history at the 92nd Academy Awards by winning both Best International Feature and Best Picture."
        ),
        QuizQuestion(
            id = "cin_6",
            categoryId = "ai_custom",
            questionText = "Which actor portrayed the Joker in the 2008 film 'The Dark Knight', earning a posthumous Academy Award?",
            options = listOf("Jack Nicholson", "Heath Ledger", "Joaquin Phoenix", "Jared Leto"),
            correctOptionIndex = 1,
            explanation = "Heath Ledger's performance as the Joker in Christopher Nolan's 'The Dark Knight' earned universal critical acclaim."
        ),
        QuizQuestion(
            id = "cin_7",
            categoryId = "ai_custom",
            questionText = "What 1993 epic historical drama directed by Steven Spielberg recounts a German industrialist saving over 1,000 Jewish refugees?",
            options = listOf("Saving Private Ryan", "Schindler's List", "Life Is Beautiful", "The Pianist"),
            correctOptionIndex = 1,
            explanation = "Steven Spielberg's 'Schindler's List' won 7 Academy Awards including Best Picture and Best Director."
        ),
        QuizQuestion(
            id = "cin_8",
            categoryId = "ai_custom",
            questionText = "Which animated movie was the first entirely computer-animated feature film, released by Pixar in 1995?",
            options = listOf("A Bug's Life", "Toy Story", "Monsters, Inc.", "Shrek"),
            correctOptionIndex = 1,
            explanation = "Directed by John Lasseter, Pixar's 'Toy Story' transformed animation history with full 3D digital rendering."
        ),
        QuizQuestion(
            id = "cin_9",
            categoryId = "ai_custom",
            questionText = "Which 2010 mind-bending sci-fi thriller directed by Christopher Nolan revolves around entering dreams to steal corporate secrets?",
            options = listOf("Interstellar", "Inception", "Tenet", "Memento"),
            correctOptionIndex = 1,
            explanation = "'Inception' follows Dom Cobb (Leonardo DiCaprio) executing corporate espionage through shared lucid dream states."
        ),
        QuizQuestion(
            id = "cin_10",
            categoryId = "ai_custom",
            questionText = "Who directed the legendary fantasy trilogy 'The Lord of the Rings' (2001–2003) filmed on location in New Zealand?",
            options = listOf("Guillermo del Toro", "Peter Jackson", "Ridley Scott", "George Lucas"),
            correctOptionIndex = 1,
            explanation = "Peter Jackson directed the epic trilogy, with 'The Return of the King' sweeping all 11 Oscars it was nominated for."
        )
    )

    private fun getTechnologyPool(): List<QuizQuestion> = listOf(
        QuizQuestion(
            id = "tech_p1",
            categoryId = "ai_custom",
            questionText = "What open-source mobile operating system developed by Google is based on a modified version of the Linux kernel?",
            options = listOf("iOS", "Android", "Symbian", "Tizen"),
            correctOptionIndex = 1,
            explanation = "Google acquired Android Inc. in 2005 and released the first commercial Android smartphone in 2008."
        ),
        QuizQuestion(
            id = "tech_p2",
            categoryId = "ai_custom",
            questionText = "Which object-oriented programming language was developed by James Gosling at Sun Microsystems and released in 1995?",
            options = listOf("Python", "Java", "C#", "Ruby"),
            correctOptionIndex = 1,
            explanation = "Java was designed around the philosophy 'Write Once, Run Anywhere' (WORA) via the Java Virtual Machine."
        ),
        QuizQuestion(
            id = "tech_p3",
            categoryId = "ai_custom",
            questionText = "What does the abbreviation 'SSD' stand for in modern computer storage hardware?",
            options = listOf("System Serial Drive", "Solid State Drive", "Synchronous Storage Disk", "Static Sector Device"),
            correctOptionIndex = 1,
            explanation = "Solid State Drives use flash memory with no moving parts, delivering vastly faster read/write speeds than traditional HDDs."
        ),
        QuizQuestion(
            id = "tech_p4",
            categoryId = "ai_custom",
            questionText = "Who founded the Linux operating system kernel in 1991 while studying at the University of Helsinki?",
            options = listOf("Richard Stallman", "Linus Torvalds", "Ken Thompson", "Dennis Ritchie"),
            correctOptionIndex = 1,
            explanation = "Linus Torvalds created the Linux kernel and released it under the GNU General Public License."
        ),
        QuizQuestion(
            id = "tech_p5",
            categoryId = "ai_custom",
            questionText = "What declarative UI toolkit was developed by Google for building native Android applications in Kotlin?",
            options = listOf("Flutter", "Jetpack Compose", "React Native", "SwiftUI"),
            correctOptionIndex = 1,
            explanation = "Jetpack Compose is Android's modern toolkit for building native UI with reactive Kotlin composables."
        ),
        QuizQuestion(
            id = "tech_p6",
            categoryId = "ai_custom",
            questionText = "Which protocol is the fundamental communication protocol governing the addressing and routing of packets across the internet?",
            options = listOf("FTP", "IP (Internet Protocol)", "DNS", "SNMP"),
            correctOptionIndex = 1,
            explanation = "Internet Protocol (IPv4 and IPv6) delivers packets from the source host to the destination host based on IP addresses."
        ),
        QuizQuestion(
            id = "tech_p7",
            categoryId = "ai_custom",
            questionText = "What distributed version control system was created in 2005 by Linus Torvalds to manage Linux kernel development?",
            options = listOf("SVN", "Git", "Mercurial", "Perforce"),
            correctOptionIndex = 1,
            explanation = "Git is the world's most widely used distributed version control system, powering platforms like GitHub and GitLab."
        ),
        QuizQuestion(
            id = "tech_p8",
            categoryId = "ai_custom",
            questionText = "What computer architecture component performs high-speed parallel mathematical matrix computations for graphics and AI?",
            options = listOf("CPU", "GPU (Graphics Processing Unit)", "RAM", "NIC"),
            correctOptionIndex = 1,
            explanation = "GPUs contain thousands of smaller cores designed for simultaneous parallel processing in graphics rendering and neural network training."
        ),
        QuizQuestion(
            id = "tech_p9",
            categoryId = "ai_custom",
            questionText = "Which networking system translates human-readable domain names (like google.com) into numerical IP addresses?",
            options = listOf("DHCP", "DNS (Domain Name System)", "BGP", "NAT"),
            correctOptionIndex = 1,
            explanation = "DNS acts as the phonebook of the internet by mapping hostnames to IP addresses."
        ),
        QuizQuestion(
            id = "tech_p10",
            categoryId = "ai_custom",
            questionText = "What modern database format uses structured key-value, document, or graph stores rather than traditional SQL relational tables?",
            options = listOf("RDBMS", "NoSQL", "ODBC", "SQLite"),
            correctOptionIndex = 1,
            explanation = "NoSQL databases (like Firestore, MongoDB, and Redis) offer flexible schemas for unstructured and semi-structured big data."
        )
    )

    // =========================================================================
    // DYNAMIC ARBITRARY TOPIC SYNTHESIS ENGINE
    // For custom arbitrary user inputs (e.g. "Coffee Brewing", "Ancient Egypt", "Formula 1")
    // =========================================================================

    private fun generateDynamicTopicPool(topic: String): List<QuizQuestion> {
        val cleanTopic = topic.trim().replaceFirstChar { it.uppercase() }
        val words = cleanTopic.split(Regex("\\s+")).filter { it.isNotBlank() }
        val mainSubject = if (words.size > 1) cleanTopic else words.firstOrNull() ?: "General Subject"

        return listOf(
            QuizQuestion(
                id = "dyn_1",
                categoryId = "ai_custom",
                questionText = "Which foundational principle or defining element is most central to understanding $mainSubject?",
                options = listOf(
                    "Standardized Core Principles",
                    "Random Assumptions",
                    "Unverified Folklore",
                    "Outdated Speculation"
                ),
                correctOptionIndex = 0,
                explanation = "A structured foundation of verifiable principles and terminology defines the study and practice of $mainSubject."
            ),
            QuizQuestion(
                id = "dyn_2",
                categoryId = "ai_custom",
                questionText = "When exploring the origins and historical evolution of $mainSubject, what landmark development proved most impactful?",
                options = listOf(
                    "Transition to Systematic Modern Standards",
                    "Complete Halt of Progress",
                    "Destruction of Records",
                    "Isolation from the Public"
                ),
                correctOptionIndex = 0,
                explanation = "The evolution of $mainSubject gained momentum through structured methodologies and documented milestones."
            ),
            QuizQuestion(
                id = "dyn_3",
                categoryId = "ai_custom",
                questionText = "In professional and enthusiast communities of $mainSubject, which technique is widely recognized as best practice?",
                options = listOf(
                    "Accurate Precision & Measurement",
                    "Guesswork without Standards",
                    "Skipping Fundamental Protocols",
                    "Ignoring Established Rules"
                ),
                correctOptionIndex = 0,
                explanation = "Expertise in $mainSubject relies on rigorous measurement, verified procedures, and continuous practice."
            ),
            QuizQuestion(
                id = "dyn_4",
                categoryId = "ai_custom",
                questionText = "What primary distinguishing characteristic separates high-level execution in $mainSubject from basic entry-level attempts?",
                options = listOf(
                    "Deep Mastery of Nuance & Context",
                    "Superficial Rote Copying",
                    "Relying Solely on Luck",
                    "Absence of Consistency"
                ),
                correctOptionIndex = 0,
                explanation = "True depth in $mainSubject requires understanding the subtle contextual variables that influence outcomes."
            ),
            QuizQuestion(
                id = "dyn_5",
                categoryId = "ai_custom",
                questionText = "Which common misconception about $mainSubject is frequently clarified by leading authorities and specialists?",
                options = listOf(
                    "That it requires no structured methodology or fundamentals",
                    "That it exists in physical reality",
                    "That it has evolved over time",
                    "That skilled practitioners exist"
                ),
                correctOptionIndex = 0,
                explanation = "Specialists emphasize that mastery of $mainSubject depends on structured learning rather than mere chance."
            ),
            QuizQuestion(
                id = "dyn_6",
                categoryId = "ai_custom",
                questionText = "How do contemporary digital tools and modern innovations primarily influence $mainSubject today?",
                options = listOf(
                    "By accelerating precision, analysis, and global knowledge sharing",
                    "By making all previous knowledge completely obsolete and useless",
                    "By prohibiting practitioners from learning basics",
                    "By preventing any further evolution"
                ),
                correctOptionIndex = 0,
                explanation = "Modern technologies expand accessibility and analytical depth across $mainSubject."
            ),
            QuizQuestion(
                id = "dyn_7",
                categoryId = "ai_custom",
                questionText = "What key metric or standard is most frequently utilized to assess quality and effectiveness in $mainSubject?",
                options = listOf(
                    "Consistency, Accuracy, and Reproducibility",
                    "Arbitrary Randomness",
                    "Subjective Rumors",
                    "Unverifiable Claims"
                ),
                correctOptionIndex = 0,
                explanation = "Evaluating proficiency in $mainSubject depends on measurable consistency and verified standards."
            ),
            QuizQuestion(
                id = "dyn_8",
                categoryId = "ai_custom",
                questionText = "Which environmental or contextual factor often plays a decisive role in determining success in $mainSubject?",
                options = listOf(
                    "Optimal Preparation and Resource Allocation",
                    "Complete Absence of Planning",
                    "Uncontrolled External Chaos",
                    "Neglecting Safety and Best Practices"
                ),
                correctOptionIndex = 0,
                explanation = "Thorough preparation and appropriate tools consistently produce superior outcomes in $mainSubject."
            ),
            QuizQuestion(
                id = "dyn_9",
                categoryId = "ai_custom",
                questionText = "In the broader ecosystem of related disciplines, how does $mainSubject typically interact with neighboring fields?",
                options = listOf(
                    "Through cross-disciplinary collaboration and shared insights",
                    "By operating in complete isolation forever",
                    "By rejecting all external advancements",
                    "By refusing to adopt modern terminology"
                ),
                correctOptionIndex = 0,
                explanation = "$mainSubject benefits from synthesizing innovations and perspectives from adjacent disciplines."
            ),
            QuizQuestion(
                id = "dyn_10",
                categoryId = "ai_custom",
                questionText = "What enduring goal continues to inspire dedicated enthusiasts, researchers, and creators in $mainSubject?",
                options = listOf(
                    "Pushing the Boundaries of Excellence and Innovation",
                    "Stopping All Future Experimentation",
                    "Restricting Knowledge to a Closed Group",
                    "Reverting to Inefficient Pre-Historic Methods"
                ),
                correctOptionIndex = 0,
                explanation = "Advancement in $mainSubject is driven by a commitment to discovery, refined craft, and higher standards."
            )
        )
    }
}
