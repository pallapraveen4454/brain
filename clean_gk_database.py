import re
from collections import defaultdict

file_path = "app/src/main/java/com/example/data/database/DefaultQuestionSeeds.kt"
with open(file_path) as f:
    text = f.read()

def parse_entity(line):
    inner = line[line.find("QuestionEntity(")+len("QuestionEntity("):line.rfind(")")]
    parts = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', inner)
    return parts

def get_category_data(cat_prefix):
    items = []
    for line in text.splitlines():
        if f'QuestionEntity("{cat_prefix}' in line:
            parts = parse_entity(line.strip())
            if len(parts) == 10:
                items.append(parts)
    return items

gk_items = get_category_data("GK")

# Track used answer facts, question texts, and core concepts
used_ans_facts = set()
used_q_texts = set()

# Pre-populate from existing non-duplicate items
clean_gk = []
to_replace = []

for q in gk_items:
    q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
    
    ans_norm = re.sub(r'[^a-z0-9]', '', ans.lower().strip())
    q_norm = re.sub(r'[^a-z0-9]', '', question.lower().strip())
    
    # Check if duplicate answer fact or duplicate text
    if ans_norm in used_ans_facts or q_norm in used_q_texts:
        to_replace.append(q)
    else:
        used_ans_facts.add(ans_norm)
        used_q_texts.add(q_norm)
        clean_gk.append(q)

print(f"Existing clean GK items: {len(clean_gk)}")
print(f"GK items needing brand new distinct replacement: {len(to_replace)}")

# Pool of 100% unique replacement questions for GK covering diverse topics
new_gk_pool = [
    # General Knowledge diverse topics: landmarks, currencies, symbols, art, science facts, world trivia, food, culture, literature, mythology
    ("What is the official national currency used in Canada?", "Canadian Dollar", "US Dollar", "Euro", "Pound Sterling", "Canadian Dollar", "The Canadian Dollar (CAD) is the official currency of Canada.", "Easy"),
    ("Which continent is the island nation of Madagascar associated with geographically?", "Africa", "Asia", "Europe", "Australia", "Africa", "Madagascar is an island country located in the Indian Ocean off East Africa.", "Easy"),
    ("What color do you get when you mix equal parts of red and blue paint?", "Purple", "Green", "Orange", "Brown", "Purple", "In subtractive color mixing, red and blue combine to produce purple.", "Easy"),
    ("Which organ in the human body produces insulin to regulate blood sugar levels?", "Pancreas", "Liver", "Kidney", "Spleen", "Pancreas", "The beta cells of the pancreas secrete insulin to lower blood glucose levels.", "Easy"),
    ("What is the hard outer covering of a turtle or tortoise called?", "Shell", "Exoskeleton", "Carapace", "Scales", "Shell", "A turtle's shell consists of an upper carapace and lower plastron.", "Easy"),
    ("Which famous inventor held over 1,000 US patents including the incandescent light bulb?", "Thomas Edison", "Nikola Tesla", "Alexander Graham Bell", "Benjamin Franklin", "Thomas Edison", "Thomas Edison established Menlo Park lab and invented the phonograph and light bulb.", "Easy"),
    ("What is the official national currency used in India?", "Indian Rupee", "Taka", "Rupiah", "Yen", "Indian Rupee", "The Indian Rupee (INR) is the legal currency of the Republic of India.", "Easy"),
    ("Which fruit is known as the 'King of Fruits' in Southeast Asia despite its pungent odor?", "Durian", "Mango", "Jackfruit", "Papaya", "Durian", "Durian is celebrated in Southeast Asia for its large size, spiky rind, and rich flavor.", "Easy"),
    ("What component in human blood is responsible for clotting to stop bleeding?", "Platelets", "Red Blood Cells", "White Blood Cells", "Plasma", "Platelets", "Platelets (thrombocytes) aggregate to form blood clots at injury sites.", "Easy"),
    ("Which Greek god of the sea is depicted holding a trident in classical mythology?", "Poseidon", "Zeus", "Hades", "Apollo", "Poseidon", "Poseidon was the ancient Greek Olympian god of the sea, storms, and horses.", "Easy"),
    ("What is the official national language spoken in Argentina?", "Spanish", "Portuguese", "Italian", "French", "Spanish", "Spanish is the official language of Argentina.", "Easy"),
    ("Which giant land animal has the longest lifespan of any mammal, up to 70 years?", "African Elephant", "Blue Whale", "Hippopotamus", "Rhinoceros", "African Elephant", "African elephants can live up to 70 years in the wild.", "Easy"),
    ("What is the chemical name for common household table salt?", "Sodium Chloride", "Sodium Bicarbonate", "Calcium Carbonate", "Potassium Chloride", "Sodium Chloride", "Table salt consists primarily of sodium chloride (NaCl).", "Easy"),
    ("Which country in South America is famous for the ancient Inca trail leading to Machu Picchu?", "Peru", "Chile", "Ecuador", "Colombia", "Peru", "Peru hosts the historic Inca sanctuary of Machu Picchu in the Andes mountains.", "Easy"),
    ("What is the name of the narrow neck of land connecting North America and South America?", "Isthmus of Panama", "Suez Isthmus", "Isthmus of Corinth", "Tehuantepec", "Isthmus of Panama", "The Isthmus of Panama is the narrow strip of land that lies between the Caribbean Sea and Pacific Ocean.", "Medium"),
    ("Which Swedish inventor created dynamite and instituted the prestigious Nobel Prizes?", "Alfred Nobel", "Carl Linnaeus", "Anders Celsius", "Gustav Dalén", "Alfred Nobel", "Alfred Nobel bequeathed his fortune to create the Nobel Prizes in his 1895 will.", "Medium"),
    ("What is the official capital city of Australia's neighbor nation, New Zealand?", "Wellington", "Auckland", "Christchurch", "Dunedin", "Wellington", "Wellington is the constitutional capital city of New Zealand.", "Medium"),
    ("Which instrument is used by meteorologists to measure atmospheric wind speed?", "Anemometer", "Barometer", "Hygrometer", "Psychrometer", "Anemometer", "Anemometers measure wind velocity and speed.", "Medium"),
    ("Which European mountain range stretches across France, Switzerland, Italy, and Austria?", "Alps", "Pyrenees", "Carpathians", "Apennines", "Alps", "The Alps are the highest and most extensive mountain range system in Europe.", "Medium"),
    ("What is the official currency used in Australia?", "Australian Dollar", "Kiwi Dollar", "Pound", "Franc", "Australian Dollar", "The Australian Dollar (AUD) is the official currency of Australia.", "Medium"),
    ("Which iconic amphitheater in Rome could seat over 50,000 spectators for gladiatorial games?", "Colosseum", "Pantheon", "Roman Forum", "Circus Maximus", "Colosseum", "The Colosseum in Rome was built under the Flavian emperors in the 1st century AD.", "Medium"),
    ("Which chemical element is represented by the symbol 'Cu' on the periodic table?", "Copper", "Cobalt", "Chromium", "Californium", "Copper", "Cu comes from the Latin word for copper, 'cuprum'.", "Medium"),
    ("What is the largest landlocked sea or lake in the world by surface area?", "Caspian Sea", "Lake Superior", "Lake Victoria", "Aral Sea", "Caspian Sea", "The Caspian Sea is the world's largest inland body of water by area (371,000 sq km).", "Medium"),
    ("Which famous epic author composed the ancient Roman poem 'Aeneid'?", "Virgil", "Ovid", "Horace", "Cicero", "Virgil", "Virgil wrote the Aeneid between 29 and 19 BCE detailing Aeneas' journey.", "Medium"),
    ("What is the name of the dense forest surrounding the equator in Central Africa?", "Congo Rainforest", "Taiga", "Daintree", "Valdivian", "Congo Rainforest", "The Congo Basin contains the second-largest tropical rainforest in the world.", "Medium"),
    ("Which gemstone is the traditional birthstone for the month of May, known for its green color?", "Emerald", "Ruby", "Sapphire", "Amethyst", "Emerald", "Emerald is a green variety of beryl colored by trace chromium or vanadium.", "Medium"),
    ("What is the official capital city of Thailand in Southeast Asia?", "Bangkok", "Phuket", "Chiang Mai", "Pattaya", "Bangkok", "Bangkok is the capital and administrative hub of Thailand.", "Medium"),
    ("Which ancient wonder of the world was located in Alexandria, Egypt to guide mariners?", "Lighthouse of Alexandria (Pharos)", "Colossus of Rhodes", "Statue of Zeus", "Mausoleum at Halicarnassus", "Lighthouse of Alexandria (Pharos)", "The Pharos of Alexandria was built in the 3rd century BCE as a navigational beacon.", "Medium"),
    ("Which noble gas is used inside bright glowing discharge lamps and advertising signs?", "Neon", "Argon", "Krypton", "Xenon", "Neon", "Neon gas glows reddish-orange when energized in electrical discharge tubes.", "Medium"),
    ("What is the official capital city of South Korea?", "Seoul", "Busan", "Incheon", "Daegu", "Seoul", "Seoul is the capital city of South Korea.", "Medium"),
    ("Which instrument measures relative humidity or moisture content in the air?", "Hygrometer", "Barometer", "Altimeter", "Manometer", "Hygrometer", "Hygrometers measure humidity levels in air or gas mixtures.", "Medium"),
    ("What is the name of the traditional Japanese garment worn with an obi sash?", "Kimono", "Yukata", "Sari", "Hanbok", "Kimono", "The kimono is a T-shaped, wrapped-front garment with wide sleeves.", "Medium"),
    ("Which line of latitude located at 23.5° North marks the northernmost boundary of the tropics?", "Tropic of Cancer", "Tropic of Capricorn", "Arctic Circle", "Equator", "Tropic of Cancer", "The Tropic of Cancer is the parallel of latitude 23.5° north of the Equator.", "Medium"),
    ("What is the official currency used in South Africa?", "South African Rand", "Pula", "Kwacha", "Cedi", "South African Rand", "The South African Rand (ZAR) is the legal currency of South Africa.", "Medium"),
    ("Which famous monument in Agra, India was constructed from white marble by Shah Jahan?", "Taj Mahal", "Qutub Minar", "Red Fort", "Hawa Mahal", "Taj Mahal", "The Taj Mahal was designated a UNESCO World Heritage site in 1983.", "Medium"),
    ("What is the capital city of Norway located at the head of the Oslofjord?", "Oslo", "Bergen", "Stavanger", "Trondheim", "Oslo", "Oslo is the capital city and main port of Norway.", "Medium"),
    ("Which chemical element is represented by the symbol 'Pb' on the periodic table?", "Lead", "Plutonium", "Platinum", "Polonium", "Lead", "Pb comes from the Latin word for lead, 'plumbum'.", "Medium"),
    ("What is the official capital city of the Mediterranean country Greece?", "Athens", "Sparta", "Patras", "Heraklion", "Athens", "Athens is the capital and largest city of Greece.", "Medium"),
    ("Which instrument is used by sailors to measure the angle between astronomical objects and horizon?", "Sextant", "Astrolabe", "Compass", "Chronometer", "Sextant", "The sextant measures angular distances for celestial navigation.", "Medium"),
    ("What is the capital city of the Nordic country Finland?", "Helsinki", "Espoo", "Tampere", "Turku", "Helsinki", "Helsinki is the capital and financial center of Finland.", "Medium"),
    ("Which European nation uses the currency Swiss Franc alongside Liechtenstein?", "Switzerland", "Austria", "Belgium", "Luxembourg", "Switzerland", "The Swiss Franc is the official currency of Switzerland.", "Hard"),
    ("What is the capital city of Iceland, the world's northernmost capital of a sovereign state?", "Reykjavik", "Akureyri", "Keflavik", "Selfoss", "Reykjavik", "Reykjavik is the capital and largest city of Iceland.", "Hard"),
    ("Which high-altitude lake between Peru and Bolivia is the highest navigable lake in the world?", "Lake Titicaca", "Lake Maracaibo", "Lake Poopó", "Lake General Carrera", "Lake Titicaca", "Lake Titicaca sits at 3,812 meters (12,507 ft) above sea level in the Andes.", "Hard"),
    ("What is the capital city of the Baltic state Estonia?", "Tallinn", "Tartu", "Narva", "Pärnu", "Tallinn", "Tallinn is the capital city of Estonia situated on the Gulf of Finland.", "Hard"),
    ("Which synthetic chemical element with atomic number 99 was named in honor of Albert Einstein?", "Einsteinium", "Fermium", "Mendelevium", "Curium", "Einsteinium", "Einsteinium (Es) was discovered in the debris of the Ivy Mike thermonuclear test in 1952.", "Hard"),
    ("What is the official currency used in Turkey?", "Turkish Lira", "Dinar", "Dirham", "Rial", "Turkish Lira", "The Turkish Lira (TRY) is the official currency of Turkey.", "Hard"),
    ("What is the capital city of Malta located on a peninsula in the Grand Harbour?", "Valletta", "Mdina", "Sliema", "Victoria", "Valletta", "Valletta is the fortified capital city of Malta built by the Knights Hospitaller.", "Hard"),
    ("Which chemical element with atomic number 74 has the symbol 'W' from its German name Wolfram?", "Tungsten", "Titanium", "Thorium", "Tantalum", "Tungsten", "Tungsten has the highest melting point of any pure metal (3,422°C).", "Hard"),
    ("What is the capital city of the South American landlocked country Paraguay?", "Asunción", "Ciudad del Este", "Encarnación", "Luque", "Asunción", "Asunción is the capital and largest city of Paraguay.", "Hard"),
    ("Which actinide element with atomic number 100 was named in honor of nuclear physicist Enrico Fermi?", "Fermium", "Lawrencium", "Nobelium", "Seaborgium", "Fermium", "Fermium (Fm) is a heavy synthetic actinide produced in nuclear reactors.", "Hard"),
    ("What is the capital city of Nepal situated near the junction of the Bagmati and Vishnumati rivers?", "Kathmandu", "Pokhara", "Lalitpur", "Bharatpur", "Kathmandu", "Kathmandu is the capital and largest urban agglomeration in Nepal.", "Hard"),
    ("Which island nation in the Pacific Ocean has Suva as its capital city?", "Fiji", "Samoa", "Tonga", "Vanuatu", "Fiji", "Suva is the capital city of Fiji located on the southeast coast of Viti Levu.", "Hard"),
    ("What is the official currency used in Poland?", "Zloty", "Koruna", "Forint", "Leu", "Zloty", "The Polish Zloty (PLN) is the official legal tender of Poland.", "Hard"),
    ("Which capital city sits on the Danube River and serves as the capital of Slovakia?", "Bratislava", "Kosice", "Prešov", "Nitra", "Bratislava", "Bratislava is the capital of Slovakia bordering both Austria and Hungary.", "Hard"),
    ("What is the capital city of Romania, historically nicknamed 'Little Paris'?", "Bucharest", "Cluj-Napoca", "Timișoara", "Iași", "Bucharest", "Bucharest is the capital and economic hub of Romania.", "Hard"),
    ("Which nonmetallic metalloid chemical element with symbol 'Si' is used extensively in semiconductors?", "Silicon", "Selenium", "Germanium", "Arsenic", "Silicon", "Silicon (Si) is a tetravalent metalloid used in integrated circuits and microchips.", "Hard"),
    ("What is the deepest point in the world's oceans located in the Mariana Trench?", "Challenger Deep", "Puerto Rico Trench", "Java Trench", "South Sandwich Trench", "Challenger Deep", "Challenger Deep reaches a maximum depth of approximately 10,928 meters.", "Hard"),
    ("What is the constitutional capital city of Bolivia in South America?", "Sucre", "La Paz", "Cochabamba", "Santa Cruz", "Sucre", "Sucre is the constitutional capital of Bolivia.", "Hard"),
]

# Apply replacement to DefaultQuestionSeeds.kt
to_replace_ids = [q[0] for q in to_replace]
print(f"IDs to replace in GK: {len(to_replace_ids)}")

repl_map = {}
for i, q in enumerate(to_replace):
    q_id = q[0]
    cat = "gk"
    q_txt, opA, opB, opC, opD, ans, exp, diff = new_gk_pool[i]
    repl_map[q_id] = (q_id, cat, q_txt, opA, opB, opC, opD, ans, exp, diff)

def to_kt_string(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

new_lines = []
for line in text.splitlines():
    if 'QuestionEntity("GK' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id = parts[0]
            if q_id in repl_map:
                r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = repl_map[q_id]
                indent = line[:line.find('QuestionEntity("')]
                line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write("\n".join(new_lines))

print("Applied clean replacements to GK!")

