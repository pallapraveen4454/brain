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

def to_kt_string(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

# Stop words for token similarity
stop_words = {'what', 'which', 'is', 'the', 'of', 'a', 'an', 'in', 'and', 'to', 'for', 'with', 'on', 'at', 'by', 'from', 'that', 'this', 'it', 'how', 'many', 'does', 'do', 'are', 'was', 'were', 'who', 'where', 'when', 'why', 'name', 'following', 'called', 'known', 'as', 'type', 'first', 'used', 'city', 'country', 'capital', 'element', 'chemical', 'symbol', 'official', 'currency', 'unit'}

def tokenize(s):
    tokens = re.findall(r'\b[a-z0-9]+\b', s.lower())
    return set(t for t in tokens if t not in stop_words)

# Curated Pools of Unique Questions for GK, SCI, HIS
# Format: (question, opA, opB, opC, opD, answer, explanation)

gk_fresh_pool = [
    ("What is the official currency of Brazil?", "Brazilian Real", "Peso", "Sol", "Bolivar", "Brazilian Real", "The Brazilian Real (BRL) is the official currency of Brazil."),
    ("Which mountain peak is the second highest on Earth after Mount Everest?", "K2", "Kangchenjunga", "Lhotse", "Makalu", "K2", "K2 in the Karakoram range is the world's second highest mountain at 8,611 meters."),
    ("Which famous play by William Shakespeare features the character Prince Hamlet?", "Hamlet", "Macbeth", "Othello", "King Lear", "Hamlet", "Hamlet is a famous tragedy by Shakespeare set in Denmark."),
    ("What is the primary language spoken in Egypt?", "Arabic", "Egyptian", "Coptic", "French", "Arabic", "Modern Standard Arabic is the official language of Egypt."),
    ("Which instrument is used to measure earthquake magnitude?", "Seismograph", "Barometer", "Anemometer", "Thermometer", "Seismograph", "Seismographs measure seismic waves produced by earthquakes."),
    ("What is the largest living species of turtle on Earth?", "Leatherback Sea Turtle", "Galapagos Tortoise", "Green Sea Turtle", "Loggerhead", "Leatherback Sea Turtle", "The leatherback sea turtle can grow up to 2 meters long and weigh over 600 kg."),
    ("Which continent has the smallest population of any permanent human-inhabited continent?", "Australia", "Antarctica", "South America", "Europe", "Australia", "Australia (continent) has the smallest human population among permanently inhabited continents."),
    ("What is the chemical element symbol for Silver?", "Ag", "Au", "Si", "Sr", "Ag", "Ag comes from the Latin word for silver, 'argentum'."),
    ("Which national flag features a red maple leaf at its center?", "Canada", "Lebanon", "Japan", "Mexico", "Canada", "The National Flag of Canada features a stylized eleven-pointed red maple leaf."),
    ("What is the official capital city of Japan?", "Tokyo", "Kyoto", "Osaka", "Yokohama", "Tokyo", "Tokyo is the political, economic, and cultural capital of Japan."),
    ("Which island nation in the Caribbean is known for cigars, vintage cars, and Havana?", "Cuba", "Jamaica", "Haiti", "Bahamas", "Cuba", "Cuba is a Caribbean island nation with Havana as its capital."),
    ("What is the official currency of India?", "Indian Rupee", "Taka", "Rupiah", "Toman", "Indian Rupee", "The Indian Rupee (INR) is the legal tender of India."),
    ("Which organ in the human body cleanses toxins and produces bile?", "Liver", "Kidney", "Pancreas", "Spleen", "Liver", "The liver detoxifies chemicals and metabolizes drugs while secreting bile."),
    ("What is the deepest ocean trench on planet Earth?", "Mariana Trench", "Puerto Rico Trench", "Java Trench", "Tonga Trench", "Mariana Trench", "The Mariana Trench in the western Pacific contains the deepest points on Earth."),
    ("Which flightless bird is the largest bird species living today?", "Ostrich", "Emu", "Cassowary", "Penguin", "Ostrich", "Ostriches are the largest and heaviest living birds, native to Africa."),
    ("What is the official capital city of France?", "Paris", "Marseille", "Lyon", "Nice", "Paris", "Paris is the capital and largest metropolis of France."),
    ("Which gas is the second most abundant in Earth's atmosphere after Nitrogen?", "Oxygen", "Argon", "Carbon Dioxide", "Hydrogen", "Oxygen", "Oxygen gas (O₂) makes up approximately 21% of dry air."),
    ("What is the national animal of India known for its distinctive stripes?", "Bengal Tiger", "Indian Elephant", "Snow Leopard", "One-horned Rhino", "Bengal Tiger", "The Bengal tiger is designated as the national animal of India."),
    ("Which canal links the Red Sea directly to the Mediterranean Sea?", "Suez Canal", "Panama Canal", "Kiel Canal", "Erie Canal", "Suez Canal", "The Suez Canal in Egypt provides an artificial shipping route between the Atlantic and Indian oceans."),
    ("Which famous Italian polymath painted the Mona Lisa?", "Leonardo da Vinci", "Michelangelo", "Raphael", "Titian", "Leonardo da Vinci", "Leonardo da Vinci created the Mona Lisa in the early 16th century."),
    ("What is the capital city of Australia?", "Canberra", "Sydney", "Melbourne", "Brisbane", "Canberra", "Canberra was chosen as Australia's capital in 1908 as a compromise between Sydney and Melbourne."),
    ("Which chemical element is represented by symbol 'K' on the periodic table?", "Potassium", "Krypton", "Kalium", "Phosphorus", "Potassium", "K comes from 'kalium', the Neo-Latin word for potassium."),
    ("What is the official currency of Japan?", "Japanese Yen", "Won", "Yuan", "Ringgit", "Japanese Yen", "The Japanese Yen (JPY) is the currency of Japan."),
    ("Which famous painting depicting a swirling night sky was created by Vincent van Gogh?", "The Starry Night", "Sunflowers", "The Night Watch", "Water Lilies", "The Starry Night", "Van Gogh painted The Starry Night while staying at the Saint-Paul-de-Mausole asylum."),
    ("What is the capital city of Egypt along the Nile River?", "Cairo", "Alexandria", "Giza", "Luxor", "Cairo", "Cairo is the capital city of Egypt and the largest city in the Arab world."),
    ("Which primary color mixed with red creates orange?", "Yellow", "Blue", "Green", "Purple", "Yellow", "Yellow and red combine in color mixing to form orange."),
    ("What is the official currency of the United Kingdom?", "Pound Sterling", "Euro", "Franc", "Krona", "Pound Sterling", "The Pound Sterling (GBP) is the official currency of the UK."),
    ("Which animal is traditionally known as the 'Ship of the Desert'?", "Camel", "Dromedary", "Llama", "Donkey", "Camel", "Camels are nicknamed 'Ship of the Desert' due to their ability to traverse arid sands."),
    ("What is the capital city of Germany?", "Berlin", "Munich", "Frankfurt", "Hamburg", "Berlin", "Berlin is the capital and largest city of Germany."),
    ("Which fundamental subatomic particle carries a positive electrical charge?", "Proton", "Electron", "Neutron", "Photon", "Proton", "Protons are positively charged subatomic particles in the nucleus."),
    ("What is the largest organ of the human body by surface area?", "Skin", "Liver", "Lungs", "Brain", "Skin", "The skin (integumentary system) is the largest human organ by weight and surface area."),
    ("Which famous scientist introduced the theory of general relativity?", "Albert Einstein", "Isaac Newton", "Niels Bohr", "Galileo Galilei", "Albert Einstein", "Albert Einstein published the general theory of relativity in 1915."),
    ("What is the official capital city of Italy?", "Rome", "Milan", "Venice", "Florence", "Rome", "Rome is the capital city of Italy."),
    ("Which chemical element is designated by symbol 'Fe' on the periodic table?", "Iron", "Iridium", "Indium", "Iodine", "Iron", "Fe comes from 'ferrum', the Latin word for iron."),
    ("What is the official currency of China?", "Renminbi (Yuan)", "Yen", "Won", "Baht", "Renminbi (Yuan)", "The Renminbi (Yuan) is the official currency of the People's Republic of China."),
    ("Which desert animal is famous for storing fat in its hump?", "Camel", "Dromedary", "Yak", "Gazelle", "Camel", "Camel humps store fat tissue that can be converted into energy and metabolic water."),
    ("What is the capital city of Spain?", "Madrid", "Barcelona", "Seville", "Valencia", "Madrid", "Madrid is the capital city of Spain."),
    ("Which nonmetallic element is represented by symbol 'O' on the periodic table?", "Oxygen", "Osmium", "Oganesson", "Ozone", "Oxygen", "Oxygen is represented by the chemical symbol O."),
    ("What is the capital city of Russia?", "Moscow", "Saint Petersburg", "Kazan", "Novosibirsk", "Moscow", "Moscow is the capital and largest city of Russia."),
    ("Which primary color mixed with blue yields green?", "Yellow", "Red", "Black", "White", "Yellow", "Mixing yellow and blue produces green."),
    ("What is the capital city of the United States?", "Washington, D.C.", "New York City", "Los Angeles", "Chicago", "Washington, D.C.", "Washington, D.C. is the federal capital of the United States."),
    ("Which gas is absorbed by plants during photosynthesis?", "Carbon Dioxide", "Oxygen", "Nitrogen", "Argon", "Carbon Dioxide", "Plants absorb CO₂ and release O₂ through photosynthesis."),
    ("What is the official currency of Canada?", "Canadian Dollar", "US Dollar", "Pound", "Euro", "Canadian Dollar", "The Canadian Dollar (CAD) is the currency of Canada."),
    ("Which planet in our solar system is famous for its bright ring system?", "Saturn", "Jupiter", "Uranus", "Neptune", "Saturn", "Saturn has the most extensive ring system of any planet."),
    ("What is the capital city of India?", "New Delhi", "Mumbai", "Kolkata", "Bengaluru", "New Delhi", "New Delhi serves as the capital of India."),
    ("Which chemical element is represented by symbol 'Na' on the periodic table?", "Sodium", "Nickel", "Neon", "Nitrogen", "Sodium", "Na comes from 'natrium', the Neo-Latin word for sodium."),
    ("What is the official currency of South Korea?", "South Korean Won", "Yen", "Yuan", "Ringgit", "South Korean Won", "The South Korean Won (KRW) is the official currency of South Korea."),
    ("Which landmark in Paris was built for the 1889 World's Fair?", "Eiffel Tower", "Arc de Triomphe", "Louvre", "Notre-Dame", "Eiffel Tower", "The Eiffel Tower was constructed by Gustave Eiffel's company for the 1889 Exposition Universelle."),
    ("What is the capital city of South Africa's judicial branch?", "Bloemfontein", "Pretoria", "Cape Town", "Durban", "Bloemfontein", "Bloemfontein is the judicial capital of South Africa."),
    ("Which country in North America has Ottawa as its capital?", "Canada", "United States", "Mexico", "Greenland", "Canada", "Ottawa was named Canada's capital by Queen Victoria in 1857."),
    ("What is the capital city of Greece?", "Athens", "Sparta", "Thessaloniki", "Patras", "Athens", "Athens is the capital city of Greece."),
    ("Which chemical symbol represents Gold?", "Au", "Ag", "Fe", "Cu", "Au", "Au stands for 'aurum', Latin for gold."),
    ("What is the capital city of Thailand?", "Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Bangkok", "Bangkok is the capital city of Thailand."),
    ("Which continent is home to the Sahara Desert?", "Africa", "Asia", "Australia", "South America", "Africa", "The Sahara covers most of Northern Africa."),
    ("What is the capital city of Turkey?", "Ankara", "Istanbul", "Izmir", "Antalya", "Ankara", "Ankara became the capital of Turkey in 1923."),
    ("Which chemical element is represented by symbol 'K'?", "Potassium", "Krypton", "Potash", "Kelvin", "Potassium", "K represents potassium."),
    ("What is the capital city of Portugal?", "Lisbon", "Porto", "Coimbra", "Braga", "Lisbon", "Lisbon is the capital city of Portugal."),
    ("Which ocean is the smallest and shallowest?", "Arctic Ocean", "Indian Ocean", "Atlantic Ocean", "Pacific Ocean", "Arctic Ocean", "The Arctic Ocean is the smallest of Earth's 5 major oceans."),
    ("What is the capital city of Ireland?", "Dublin", "Belfast", "Cork", "Galway", "Dublin", "Dublin is the capital of Ireland."),
    ("Which gas is lighter than air and causes balloons to float?", "Helium", "Hydrogen", "Oxygen", "Nitrogen", "Helium", "Helium is an inert noble gas lighter than air."),
    ("What is the capital city of Norway?", "Oslo", "Bergen", "Trondheim", "Stavanger", "Oslo", "Oslo is the capital city of Norway."),
    ("Which planet is closest to the Sun?", "Mercury", "Venus", "Earth", "Mars", "Mercury", "Mercury orbits closest to the Sun in our solar system."),
    ("What is the capital city of Switzerland?", "Bern", "Zurich", "Geneva", "Basel", "Bern", "Bern is the de facto capital of Switzerland."),
    ("Which chemical element is represented by symbol 'Pb'?", "Lead", "Plutonium", "Platinum", "Palladium", "Lead", "Pb comes from plumbum."),
    ("What is the capital city of Finland?", "Helsinki", "Tampere", "Turku", "Oulu", "Helsinki", "Helsinki is the capital city of Finland."),
    ("Which element has the chemical symbol 'W'?", "Tungsten", "Titanium", "Thorium", "Thallium", "Tungsten", "W stands for Wolfram (Tungsten)."),
    ("What is the capital city of Iceland?", "Reykjavik", "Akureyri", "Keflavik", "Hafnarfjörður", "Reykjavik", "Reykjavik is the capital of Iceland."),
    ("Which natural allotrope of carbon is the hardest known mineral?", "Diamond", "Graphite", "Graphene", "Fullerene", "Diamond", "Diamond is carbon bonded in a tetrahedral lattice."),
    ("What is the capital city of the Czech Republic?", "Prague", "Brno", "Ostrava", "Plzen", "Prague", "Prague is the capital of the Czech Republic."),
    ("Which chemical metalloid element has the symbol 'Si'?", "Silicon", "Selenium", "Sodium", "Silver", "Silicon", "Silicon is element 14 on the periodic table."),
    ("What is the constitutional capital city of Bolivia?", "Sucre", "La Paz", "Cochabamba", "Santa Cruz", "Sucre", "Sucre is Bolivia's constitutional capital."),
]

# Function to clean a category completely
def clean_category(cat_prefix, fresh_pool):
    items = get_category_data(cat_prefix)
    
    seen_ans = set()
    seen_q_texts = set()
    
    clean_items = []
    pool_idx = 0

    for q in items:
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        
        ans_norm = re.sub(r'[^a-z0-9]', '', ans.lower().strip())
        q_norm = re.sub(r'[^a-z0-9]', '', question.lower().strip())
        
        # Check if question needs replacement
        is_dup = False
        if ans_norm in seen_ans or q_norm in seen_q_texts:
            is_dup = True
        else:
            # Check similarity with existing clean questions
            t1 = tokenize(question)
            for cq in clean_items:
                t2 = tokenize(cq[2])
                if t1 and t2:
                    sim = len(t1.intersection(t2)) / len(t1.union(t2))
                    if sim > 0.38:
                        is_dup = True
                        break

        if not is_dup:
            seen_ans.add(ans_norm)
            seen_q_texts.add(q_norm)
            clean_items.append(q)
        else:
            # Replace with a fresh item from pool that does not conflict
            found_replacement = False
            while pool_idx < len(fresh_pool):
                fq_q, fq_a, fq_b, fq_c, fq_d, fq_ans, fq_exp = fresh_pool[pool_idx]
                pool_idx += 1
                
                f_ans_norm = re.sub(r'[^a-z0-9]', '', fq_ans.lower().strip())
                f_q_norm = re.sub(r'[^a-z0-9]', '', fq_q.lower().strip())
                
                if f_ans_norm not in seen_ans and f_q_norm not in seen_q_texts:
                    # Check token similarity
                    ft = tokenize(fq_q)
                    sim_conflict = False
                    for cq in clean_items:
                        ct = tokenize(cq[2])
                        if ft and ct:
                            sim = len(ft.intersection(ct)) / len(ft.union(ct))
                            if sim > 0.38:
                                sim_conflict = True
                                break
                    if not sim_conflict:
                        # Valid replacement found!
                        replacement_q = [q_id, cat, fq_q, fq_a, fq_b, fq_c, fq_d, fq_ans, fq_exp, diff]
                        seen_ans.add(f_ans_norm)
                        seen_q_texts.add(f_q_norm)
                        clean_items.append(replacement_q)
                        found_replacement = True
                        break
            
            if not found_replacement:
                print(f"WARNING: Ran out of pool items for replacement at {q_id}!")
                clean_items.append(q)

    return clean_items

clean_gk = clean_category("GK", gk_fresh_pool)

print(f"Cleaned GK count: {len(clean_gk)}")

