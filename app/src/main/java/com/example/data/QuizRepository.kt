package com.example.data

import com.example.data.model.QuizQuestion

class QuizRepository {

    private val questionBank = listOf(
        // --- GENERAL KNOWLEDGE ---
        QuizQuestion(
            id = "gk_1",
            categoryId = "gk",
            questionText = "Which famous playwright wrote 'Romeo and Juliet'?",
            options = listOf("Charles Dickens", "William Shakespeare", "Mark Twain", "Jane Austen"),
            correctOptionIndex = 1,
            explanation = "William Shakespeare wrote the tragic play 'Romeo and Juliet' in the late 16th century."
        ),
        QuizQuestion(
            id = "gk_2",
            categoryId = "gk",
            questionText = "What is the capital city of France?",
            options = listOf("Berlin", "Madrid", "Paris", "Rome"),
            correctOptionIndex = 2,
            explanation = "Paris is the capital and most populous city of France."
        ),
        QuizQuestion(
            id = "gk_3",
            categoryId = "gk",
            questionText = "Which currency is used in Japan?",
            options = listOf("Yuan", "Yen", "Won", "Baht"),
            correctOptionIndex = 1,
            explanation = "The official currency of Japan is the Japanese Yen (¥)."
        ),
        QuizQuestion(
            id = "gk_4",
            categoryId = "gk",
            questionText = "How many colors are in a standard rainbow?",
            options = listOf("5", "6", "7", "8"),
            correctOptionIndex = 2,
            explanation = "A rainbow has 7 distinct color bands: Red, Orange, Yellow, Green, Blue, Indigo, and Violet."
        ),
        QuizQuestion(
            id = "gk_5",
            categoryId = "gk",
            questionText = "What is the main ingredient in traditional Mexican guacamole?",
            options = listOf("Tomato", "Avocado", "Onion", "Pepper"),
            correctOptionIndex = 1,
            explanation = "Guacamole is an avocado-based dip originating from Mexico."
        ),
        QuizQuestion(
            id = "gk_6",
            categoryId = "gk",
            questionText = "Who painted the Mona Lisa?",
            options = listOf("Vincent van Gogh", "Pablo Picasso", "Leonardo da Vinci", "Michelangelo"),
            correctOptionIndex = 2,
            explanation = "The Mona Lisa was painted by the Italian Renaissance artist Leonardo da Vinci."
        ),
        QuizQuestion(
            id = "gk_7",
            categoryId = "gk",
            questionText = "What is the national flower of Japan?",
            options = listOf("Rose", "Tulip", "Cherry Blossom (Sakura)", "Lotus"),
            correctOptionIndex = 2,
            explanation = "The Cherry Blossom (Sakura) is widely regarded as the national flower of Japan."
        ),
        QuizQuestion(
            id = "gk_8",
            categoryId = "gk",
            questionText = "Which animal is known as the 'Ship of the Desert'?",
            options = listOf("Camel", "Elephant", "Horse", "Llama"),
            correctOptionIndex = 0,
            explanation = "Camels are suited for desert transport due to their endurance and hump energy reserves."
        ),
        QuizQuestion(
            id = "gk_9",
            categoryId = "gk",
            questionText = "Which is the smallest country in the world?",
            options = listOf("Monaco", "Vatican City", "San Marino", "Liechtenstein"),
            correctOptionIndex = 1,
            explanation = "Vatican City is an independent state surrounded by Rome, covering about 0.49 square kilometers."
        ),
        QuizQuestion(
            id = "gk_10",
            categoryId = "gk",
            questionText = "What is the official home and office of the US President?",
            options = listOf("The Capitol", "The White House", "Pentagon", "Empire State"),
            correctOptionIndex = 1,
            explanation = "The White House in Washington, D.C. is the official residence of the US President."
        ),

        // --- SCIENCE ---
        QuizQuestion(
            id = "sci_1",
            categoryId = "science",
            questionText = "What gas do plants absorb during photosynthesis?",
            options = listOf("Oxygen", "Carbon Dioxide", "Nitrogen", "Hydrogen"),
            correctOptionIndex = 1,
            explanation = "Plants absorb carbon dioxide (CO2) and water to produce glucose and oxygen."
        ),
        QuizQuestion(
            id = "sci_2",
            categoryId = "science",
            questionText = "What is the hardest natural substance on Earth?",
            options = listOf("Gold", "Iron", "Diamond", "Quartz"),
            correctOptionIndex = 2,
            explanation = "Diamond ranks 10 on the Mohs hardness scale, making it the hardest natural substance."
        ),
        QuizQuestion(
            id = "sci_3",
            categoryId = "science",
            questionText = "How many bones are in the adult human body?",
            options = listOf("186", "206", "226", "256"),
            correctOptionIndex = 1,
            explanation = "An adult human skeleton consists of 206 bones."
        ),
        QuizQuestion(
            id = "sci_4",
            categoryId = "science",
            questionText = "Which part of the cell is known as the powerhouse?",
            options = listOf("Nucleus", "Ribosome", "Mitochondria", "Golgi Apparatus"),
            correctOptionIndex = 2,
            explanation = "Mitochondria generate most of the chemical energy needed to power cellular reactions."
        ),
        QuizQuestion(
            id = "sci_5",
            categoryId = "science",
            questionText = "What speed does light travel at in a vacuum?",
            options = listOf("300,000 km/s", "150,000 km/s", "1,000,000 km/s", "30,000 km/s"),
            correctOptionIndex = 0,
            explanation = "Light travels at approximately 299,792 kilometers per second in a vacuum."
        ),
        QuizQuestion(
            id = "sci_6",
            categoryId = "science",
            questionText = "Which subatomic particle carries a negative charge?",
            options = listOf("Proton", "Neutron", "Electron", "Positron"),
            correctOptionIndex = 2,
            explanation = "Electrons orbit the nucleus and hold a negative electrical charge."
        ),
        QuizQuestion(
            id = "sci_7",
            categoryId = "science",
            questionText = "What is the chemical formula for water?",
            options = listOf("CO2", "H2O", "NaCl", "O2"),
            correctOptionIndex = 1,
            explanation = "Water consists of two hydrogen atoms bonded to one oxygen atom (H2O)."
        ),
        QuizQuestion(
            id = "sci_8",
            categoryId = "science",
            questionText = "Which organ filters blood and produces urine?",
            options = listOf("Liver", "Heart", "Kidney", "Lungs"),
            correctOptionIndex = 2,
            explanation = "The kidneys filter blood to remove wastes and excess fluids."
        ),
        QuizQuestion(
            id = "sci_9",
            categoryId = "science",
            questionText = "What force pulls objects toward the center of Earth?",
            options = listOf("Magnetism", "Gravity", "Friction", "Tension"),
            correctOptionIndex = 1,
            explanation = "Gravity is a fundamental force that attracts objects with mass toward each other."
        ),
        QuizQuestion(
            id = "sci_10",
            categoryId = "science",
            questionText = "What type of energy is stored in a battery?",
            options = listOf("Thermal Energy", "Kinetic Energy", "Chemical Energy", "Nuclear Energy"),
            correctOptionIndex = 2,
            explanation = "Batteries convert stored chemical energy into electrical energy."
        ),

        // --- HISTORY ---
        QuizQuestion(
            id = "hist_1",
            categoryId = "history",
            questionText = "In which year did World War II end?",
            options = listOf("1939", "1941", "1945", "1950"),
            correctOptionIndex = 2,
            explanation = "World War II officially ended in September 1945."
        ),
        QuizQuestion(
            id = "hist_2",
            categoryId = "history",
            questionText = "Who was the first President of the United States?",
            options = listOf("Thomas Jefferson", "George Washington", "Abraham Lincoln", "John Adams"),
            correctOptionIndex = 1,
            explanation = "George Washington served as the first US President from 1789 to 1797."
        ),
        QuizQuestion(
            id = "hist_3",
            categoryId = "history",
            questionText = "Which ancient civilization built the Pyramids of Giza?",
            options = listOf("Ancient Greeks", "Romans", "Ancient Egyptians", "Mayans"),
            correctOptionIndex = 2,
            explanation = "The Giza Pyramids were built by the Ancient Egyptians during the Old Kingdom period."
        ),
        QuizQuestion(
            id = "hist_4",
            categoryId = "history",
            questionText = "What was the name of the ship that brought the Pilgrims to America in 1620?",
            options = listOf("Santa Maria", "Mayflower", "Endeavour", "Beagle"),
            correctOptionIndex = 1,
            explanation = "The Mayflower carried 102 passengers to Plymouth, Massachusetts in 1620."
        ),
        QuizQuestion(
            id = "hist_5",
            categoryId = "history",
            questionText = "Who discovered gravity when an apple allegedly fell on his head?",
            options = listOf("Galileo Galilei", "Sir Isaac Newton", "Albert Einstein", "Nikola Tesla"),
            correctOptionIndex = 1,
            explanation = "Sir Isaac Newton formulated the laws of motion and universal gravitation."
        ),
        QuizQuestion(
            id = "hist_6",
            categoryId = "history",
            questionText = "Which empire was ruled by Julius Caesar?",
            options = listOf("Ottoman Empire", "Roman Republic/Empire", "Byzantine Empire", "British Empire"),
            correctOptionIndex = 1,
            explanation = "Julius Caesar was a Roman general and statesman who played a critical role in Rome."
        ),
        QuizQuestion(
            id = "hist_7",
            categoryId = "history",
            questionText = "In which country did the Industrial Revolution begin?",
            options = listOf("United States", "France", "Great Britain", "Germany"),
            correctOptionIndex = 2,
            explanation = "The Industrial Revolution started in Great Britain in the mid-18th century."
        ),
        QuizQuestion(
            id = "hist_8",
            categoryId = "history",
            questionText = "Who was the first person to walk on the Moon?",
            options = listOf("Buzz Aldrin", "Neil Armstrong", "Yuri Gagarin", "Michael Collins"),
            correctOptionIndex = 1,
            explanation = "Neil Armstrong stepped onto the Moon on July 20, 1969 during the Apollo 11 mission."
        ),
        QuizQuestion(
            id = "hist_9",
            categoryId = "history",
            questionText = "The Fall of the Berlin Wall occurred in which year?",
            options = listOf("1979", "1989", "1991", "1995"),
            correctOptionIndex = 1,
            explanation = "The Berlin Wall fell on November 9, 1989, marking the collapse of the Eastern Bloc."
        ),
        QuizQuestion(
            id = "hist_10",
            categoryId = "history",
            questionText = "Who wrote the 'I Have a Dream' speech?",
            options = listOf("Malcolm X", "Martin Luther King Jr.", "John F. Kennedy", "Barack Obama"),
            correctOptionIndex = 1,
            explanation = "Dr. Martin Luther King Jr. delivered the iconic speech in 1963 during the March on Washington."
        ),

        // --- GEOGRAPHY ---
        QuizQuestion(
            id = "geo_1",
            categoryId = "geo",
            questionText = "What is the longest river in the world?",
            options = listOf("Amazon River", "Nile River", "Yangtze River", "Mississippi River"),
            correctOptionIndex = 1,
            explanation = "The Nile River in Africa spans approximately 6,650 kilometers."
        ),
        QuizQuestion(
            id = "geo_2",
            categoryId = "geo",
            questionText = "Which mountain is the highest above sea level?",
            options = listOf("K2", "Mount Kilimanjaro", "Mount Everest", "Denali"),
            correctOptionIndex = 2,
            explanation = "Mount Everest stands at 8,848.86 meters in the Himalayas."
        ),
        QuizQuestion(
            id = "geo_3",
            categoryId = "geo",
            questionText = "Which country has the largest land area in the world?",
            options = listOf("Canada", "China", "United States", "Russia"),
            correctOptionIndex = 3,
            explanation = "Russia spans over 17 million square kilometers across Europe and Asia."
        ),
        QuizQuestion(
            id = "geo_4",
            categoryId = "geo",
            questionText = "What is the capital of Australia?",
            options = listOf("Sydney", "Melbourne", "Canberra", "Brisbane"),
            correctOptionIndex = 2,
            explanation = "Canberra was chosen as the capital of Australia in 1908."
        ),
        QuizQuestion(
            id = "geo_5",
            categoryId = "geo",
            questionText = "In which continent is the Sahara Desert located?",
            options = listOf("Asia", "South America", "Africa", "Australia"),
            correctOptionIndex = 2,
            explanation = "The Sahara is the world's largest hot desert, covering northern Africa."
        ),
        QuizQuestion(
            id = "geo_6",
            categoryId = "geo",
            questionText = "Which island is the largest in the world?",
            options = listOf("Madagascar", "Greenland", "Borneo", "New Guinea"),
            correctOptionIndex = 1,
            explanation = "Greenland is the world's largest non-continental island."
        ),
        QuizQuestion(
            id = "geo_7",
            categoryId = "geo",
            questionText = "What ocean lies between North America and Europe?",
            options = listOf("Pacific Ocean", "Atlantic Ocean", "Indian Ocean", "Southern Ocean"),
            correctOptionIndex = 1,
            explanation = "The Atlantic Ocean connects Europe and Africa to the Americas."
        ),
        QuizQuestion(
            id = "geo_8",
            categoryId = "geo",
            questionText = "Which country is shaped like a boot?",
            options = listOf("Greece", "Spain", "Italy", "Portugal"),
            correctOptionIndex = 2,
            explanation = "Italy's peninsula on the Mediterranean Sea famously resembles a boot."
        ),
        QuizQuestion(
            id = "geo_9",
            categoryId = "geo",
            questionText = "What is the capital city of Canada?",
            options = listOf("Toronto", "Vancouver", "Ottawa", "Montreal"),
            correctOptionIndex = 2,
            explanation = "Ottawa is Canada's national capital located in the province of Ontario."
        ),
        QuizQuestion(
            id = "geo_10",
            categoryId = "geo",
            questionText = "Which country has the largest population in the world?",
            options = listOf("India", "China", "United States", "Indonesia"),
            correctOptionIndex = 0,
            explanation = "India is the world's most populous nation with over 1.4 billion residents."
        ),

        // --- SPORTS ---
        QuizQuestion(
            id = "spo_1",
            categoryId = "sports",
            questionText = "How long is a standard soccer match?",
            options = listOf("80 minutes", "90 minutes", "100 minutes", "60 minutes"),
            correctOptionIndex = 1,
            explanation = "A regular soccer match consists of two 45-minute halves total 90 minutes."
        ),
        QuizQuestion(
            id = "spo_2",
            categoryId = "sports",
            questionText = "How many players are on a basketball team on the court at once?",
            options = listOf("4", "5", "6", "7"),
            correctOptionIndex = 1,
            explanation = "Each basketball team plays with 5 players on the court at a time."
        ),
        QuizQuestion(
            id = "spo_3",
            categoryId = "sports",
            questionText = "Which country hosted the 2020 Summer Olympics (held in 2021)?",
            options = listOf("China", "Brazil", "Japan", "United Kingdom"),
            correctOptionIndex = 2,
            explanation = "Tokyo, Japan hosted the 2020 Olympic Games."
        ),
        QuizQuestion(
            id = "spo_4",
            categoryId = "sports",
            questionText = "In tennis, what term is used for a score of zero?",
            options = listOf("Nil", "Zero", "Love", "Blank"),
            correctOptionIndex = 2,
            explanation = "'Love' in tennis represents a score of 0."
        ),
        QuizQuestion(
            id = "spo_5",
            categoryId = "sports",
            questionText = "Who has won the most FIFA World Cup titles in men's football?",
            options = listOf("Germany", "Argentina", "Italy", "Brazil"),
            correctOptionIndex = 3,
            explanation = "Brazil holds 5 FIFA World Cup trophies (1958, 1962, 1970, 1994, 2002)."
        ),
        QuizQuestion(
            id = "spo_6",
            categoryId = "sports",
            questionText = "Which sport uses terms like 'strike', 'spare', and 'turkey'?",
            options = listOf("Golf", "Bowling", "Baseball", "Cricket"),
            correctOptionIndex = 1,
            explanation = "Ten-pin bowling uses strike, spare, and turkey (three strikes in a row)."
        ),
        QuizQuestion(
            id = "spo_7",
            categoryId = "sports",
            questionText = "How many rings are in the official Olympic logo?",
            options = listOf("4", "5", "6", "7"),
            correctOptionIndex = 1,
            explanation = "The Olympic logo features 5 interlocking rings representing the five continents."
        ),
        QuizQuestion(
            id = "spo_8",
            categoryId = "sports",
            questionText = "Which athlete is known as 'The Lightning Bolt' and holds the 100m world record?",
            options = listOf("Carl Lewis", "Usain Bolt", "Tyson Gay", "Justin Gatlin"),
            correctOptionIndex = 1,
            explanation = "Usain Bolt set the world record for 100m at 9.58 seconds in 2009."
        ),
        QuizQuestion(
            id = "spo_9",
            categoryId = "sports",
            questionText = "What is the maximum score possible in a single frame of ten-pin bowling?",
            options = listOf("10", "20", "30", "100"),
            correctOptionIndex = 2,
            explanation = "A frame with strikes can yield up to 30 points towards the frame score."
        ),
        QuizQuestion(
            id = "spo_10",
            categoryId = "sports",
            questionText = "In golf, what is one stroke under par on a hole called?",
            options = listOf("Eagle", "Birdie", "Bogey", "Albatross"),
            correctOptionIndex = 1,
            explanation = "Scoring one stroke fewer than par is called a Birdie."
        ),

        // --- MOVIES ---
        QuizQuestion(
            id = "mov_1",
            categoryId = "movies",
            questionText = "Which movie won the Oscar for Best Picture in 1997 and featured Leonardo DiCaprio?",
            options = listOf("Avatar", "Titanic", "Inception", "The Aviator"),
            correctOptionIndex = 1,
            explanation = "James Cameron's 'Titanic' won 11 Academy Awards including Best Picture."
        ),
        QuizQuestion(
            id = "mov_2",
            categoryId = "movies",
            questionText = "Who directed the movie 'Jurassic Park' (1993)?",
            options = listOf("George Lucas", "Steven Spielberg", "Christopher Nolan", "James Cameron"),
            correctOptionIndex = 1,
            explanation = "Steven Spielberg directed the iconic dinosaur sci-fi blockbuster."
        ),
        QuizQuestion(
            id = "mov_3",
            categoryId = "movies",
            questionText = "What is the highest-grossing film of all time (unadjusted for inflation)?",
            options = listOf("Avengers: Endgame", "Titanic", "Avatar", "Star Wars: The Force Awakens"),
            correctOptionIndex = 2,
            explanation = "James Cameron's 'Avatar' holds the global box office record over $2.9 billion."
        ),
        QuizQuestion(
            id = "mov_4",
            categoryId = "movies",
            questionText = "Which superhero is also known as Bruce Wayne?",
            options = listOf("Superman", "Spider-Man", "Batman", "Iron Man"),
            correctOptionIndex = 2,
            explanation = "Bruce Wayne is the billionaire alter-ego of Batman."
        ),
        QuizQuestion(
            id = "mov_5",
            categoryId = "movies",
            questionText = "In 'The Lion King', what is the name of Simba's father?",
            options = listOf("Scar", "Mufasa", "Rafiki", "Kovu"),
            correctOptionIndex = 1,
            explanation = "Mufasa was the wise ruler of Pride Rock and father of Simba."
        ),
        QuizQuestion(
            id = "mov_6",
            categoryId = "movies",
            questionText = "What color is the pill Neo chooses in 'The Matrix'?",
            options = listOf("Blue", "Red", "Green", "Yellow"),
            correctOptionIndex = 1,
            explanation = "Neo takes the red pill to learn the truth about the Matrix."
        ),
        QuizQuestion(
            id = "mov_7",
            categoryId = "movies",
            questionText = "Which animated movie features the famous song 'Let It Go'?",
            options = listOf("Tangled", "Moana", "Frozen", "Brave"),
            correctOptionIndex = 2,
            explanation = "Elsa sings 'Let It Go' in Disney's 'Frozen' (2013)."
        ),
        QuizQuestion(
            id = "mov_8",
            categoryId = "movies",
            questionText = "Who played Tony Stark / Iron Man in the Marvel Cinematic Universe?",
            options = listOf("Chris Evans", "Robert Downey Jr.", "Chris Hemsworth", "Mark Ruffalo"),
            correctOptionIndex = 1,
            explanation = "Robert Downey Jr. portrayed Iron Man starting in 2008."
        ),
        QuizQuestion(
            id = "mov_9",
            categoryId = "movies",
            questionText = "What fictional planet is the home of Luke Skywalker in 'Star Wars'?",
            options = listOf("Coruscant", "Tatooine", "Alderaan", "Endor"),
            correctOptionIndex = 1,
            explanation = "Tatooine is a desert planet where Luke Skywalker grew up."
        ),
        QuizQuestion(
            id = "mov_10",
            categoryId = "movies",
            questionText = "Which Pixar movie is centered around a young trash-compacting robot?",
            options = listOf("WALL-E", "Robots", "Cars", "Up"),
            correctOptionIndex = 0,
            explanation = "WALL-E is a solitude robot cleaning up an abandoned Earth in 2805."
        ),

        // --- TECHNOLOGY ---
        QuizQuestion(
            id = "tech_1",
            categoryId = "tech",
            questionText = "What does 'CPU' stand for in computer science?",
            options = listOf(
                "Central Processing Unit",
                "Computer Personal Unit",
                "Central Process Utility",
                "Control Power Unit"
            ),
            correctOptionIndex = 0,
            explanation = "The Central Processing Unit performs fundamental arithmetic and logic operations."
        ),
        QuizQuestion(
            id = "tech_2",
            categoryId = "tech",
            questionText = "Which programming language was created by James Gosling at Sun Microsystems?",
            options = listOf("Python", "Java", "C++", "JavaScript"),
            correctOptionIndex = 1,
            explanation = "Java was developed by James Gosling and released in 1995."
        ),
        QuizQuestion(
            id = "tech_3",
            categoryId = "tech",
            questionText = "Who co-founded Microsoft alongside Paul Allen in 1975?",
            options = listOf("Steve Jobs", "Bill Gates", "Jeff Bezos", "Larry Page"),
            correctOptionIndex = 1,
            explanation = "Bill Gates and Paul Allen founded Microsoft in Albuquerque, New Mexico."
        ),
        QuizQuestion(
            id = "tech_4",
            categoryId = "tech",
            questionText = "What operating system is developed by Google for mobile devices?",
            options = listOf("iOS", "Windows Mobile", "Android", "Symbian"),
            correctOptionIndex = 2,
            explanation = "Android is an open-source Linux-based mobile OS developed by Google."
        ),
        QuizQuestion(
            id = "tech_5",
            categoryId = "tech",
            questionText = "What does 'RAM' stand for in hardware?",
            options = listOf(
                "Read Access Memory",
                "Random Access Memory",
                "Rapid Application Module",
                "Run Auto Memory"
            ),
            correctOptionIndex = 1,
            explanation = "Random Access Memory allows fast short-term data read and write operations."
        ),
        QuizQuestion(
            id = "tech_6",
            categoryId = "tech",
            questionText = "Which company acquired GitHub in 2018?",
            options = listOf("Google", "Facebook", "Microsoft", "Amazon"),
            correctOptionIndex = 2,
            explanation = "Microsoft acquired GitHub for $7.5 billion in 2018."
        ),
        QuizQuestion(
            id = "tech_7",
            categoryId = "tech",
            questionText = "What protocol is used to securely transfer web pages over the internet?",
            options = listOf("HTTP", "HTTPS", "FTP", "SMTP"),
            correctOptionIndex = 1,
            explanation = "HTTPS uses TLS encryption to secure communication between client and server."
        ),
        QuizQuestion(
            id = "tech_8",
            categoryId = "tech",
            questionText = "What is the mascot of the Linux operating system?",
            options = listOf("Gopher", "Tux the Penguin", "Octocat", "Android Bot"),
            correctOptionIndex = 1,
            explanation = "Tux is the official penguin mascot created in 1996 for Linux."
        ),
        QuizQuestion(
            id = "tech_9",
            categoryId = "tech",
            questionText = "In computer networking, what does 'IP' stand for?",
            options = listOf("Internet Protocol", "Internal Process", "Integrated Port", "Interface Program"),
            correctOptionIndex = 0,
            explanation = "Internet Protocol routes packets across network boundaries."
        ),
        QuizQuestion(
            id = "tech_10",
            categoryId = "tech",
            questionText = "Which company developed the Gemini AI model family?",
            options = listOf("OpenAI", "Google", "Meta", "Anthropic"),
            correctOptionIndex = 1,
            explanation = "Google DeepMind developed the multimodal Gemini AI family."
        ),
        // --- MATHEMATICS ---
        QuizQuestion(
            id = "math_1",
            categoryId = "math",
            questionText = "What is the square root of 144?",
            options = listOf("10", "11", "12", "14"),
            correctOptionIndex = 2,
            explanation = "12 × 12 = 144."
        ),
        QuizQuestion(
            id = "math_2",
            categoryId = "math",
            questionText = "What is the value of Pi (π) rounded to two decimal places?",
            options = listOf("3.12", "3.14", "3.16", "3.18"),
            correctOptionIndex = 1,
            explanation = "Pi is approximately 3.14159..."
        ),
        QuizQuestion(
            id = "math_3",
            categoryId = "math",
            questionText = "What is 15% of 200?",
            options = listOf("20", "25", "30", "35"),
            correctOptionIndex = 2,
            explanation = "0.15 × 200 = 30."
        ),
        QuizQuestion(
            id = "math_4",
            categoryId = "math",
            questionText = "What is the prime number immediately following 19?",
            options = listOf("21", "23", "25", "27"),
            correctOptionIndex = 1,
            explanation = "23 is the next prime number after 19."
        ),
        QuizQuestion(
            id = "math_5",
            categoryId = "math",
            questionText = "What is the sum of interior angles in a triangle?",
            options = listOf("90°", "180°", "270°", "360°"),
            correctOptionIndex = 1,
            explanation = "The interior angles of any triangle always add up to 180 degrees."
        ),
        QuizQuestion(
            id = "math_6",
            categoryId = "math",
            questionText = "If 3x + 5 = 20, what is the value of x?",
            options = listOf("3", "4", "5", "6"),
            correctOptionIndex = 2,
            explanation = "3x = 15, so x = 5."
        ),
        QuizQuestion(
            id = "math_7",
            categoryId = "math",
            questionText = "What is 7 cubed (7³)?",
            options = listOf("243", "343", "441", "512"),
            correctOptionIndex = 1,
            explanation = "7 × 7 × 7 = 343."
        ),
        QuizQuestion(
            id = "math_8",
            categoryId = "math",
            questionText = "What is the area of a circle with radius 5? (Area = πr²)",
            options = listOf("25π", "10π", "50π", "5π"),
            correctOptionIndex = 0,
            explanation = "Area = π × 5² = 25π."
        ),
        QuizQuestion(
            id = "math_9",
            categoryId = "math",
            questionText = "What is the least common multiple (LCM) of 4 and 6?",
            options = listOf("12", "24", "18", "8"),
            correctOptionIndex = 0,
            explanation = "12 is the smallest positive integer divisible by both 4 and 6."
        ),
        QuizQuestion(
            id = "math_10",
            categoryId = "math",
            questionText = "What is 2 to the power of 8 (2⁸)?",
            options = listOf("128", "256", "512", "1024"),
            correctOptionIndex = 1,
            explanation = "2⁸ = 256."
        )
    )

    private val selectionEngine by lazy { com.example.data.database.QuestionSelectionEngine() }

    fun getQuestionCountForCategory(categoryId: String): Int {
        return selectionEngine.getQuestionCountForCategory(categoryId)
    }

    fun getQuestionsForCategory(categoryId: String): List<QuizQuestion> {
        return selectionEngine.getQuestionsForCategorySync(categoryId)
    }

    suspend fun getQuestionsForCategoryAsync(categoryId: String): List<QuizQuestion> {
        return selectionEngine.getQuestionsForCategory(categoryId)
    }

    fun getCategoryTitle(categoryId: String): String {
        return when (categoryId.lowercase()) {
            "gk" -> "General Knowledge"
            "science" -> "Science"
            "history" -> "History"
            "geo" -> "Geography"
            "sports" -> "Sports"
            "movies" -> "Movies"
            "tech" -> "Technology"
            "math" -> "Mathematics"
            "quick" -> "Quick Play"
            "daily" -> "Daily Challenge"
            else -> "Brain Quiz"
        }
    }
}
