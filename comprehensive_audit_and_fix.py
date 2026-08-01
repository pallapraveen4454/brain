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

# LARGE FRESH UNIQUE QUESTION POOLS
gk_pool = [
    ("Which continent is home to the Amazon Rainforest?", "South America", "Africa", "Asia", "North America", "South America", "The Amazon Rainforest is primarily located within Brazil and other South American nations."),
    ("What is the official currency used in Canada?", "Canadian Dollar", "US Dollar", "Pound Sterling", "Euro", "Canadian Dollar", "The Canadian Dollar (CAD) is the currency of Canada."),
    ("Which mountain peak is the second highest on Earth after Mount Everest?", "K2", "Kangchenjunga", "Lhotse", "Makalu", "K2", "K2 in the Karakoram range stands at 8,611 meters."),
    ("Which play by William Shakespeare features the character Prince Hamlet?", "Hamlet", "Macbeth", "Othello", "King Lear", "Hamlet", "Hamlet is a famous tragedy written by Shakespeare."),
    ("What is the primary official language spoken in Egypt?", "Arabic", "Egyptian", "Coptic", "French", "Arabic", "Modern Standard Arabic is the official language of Egypt."),
    ("Which instrument is used to measure earthquake magnitude?", "Seismograph", "Barometer", "Anemometer", "Thermometer", "Seismograph", "Seismographs record seismic waves emitted by earthquakes."),
    ("What is the largest species of sea turtle on Earth?", "Leatherback Sea Turtle", "Galapagos Tortoise", "Green Sea Turtle", "Loggerhead", "Leatherback Sea Turtle", "The leatherback sea turtle is the largest living turtle."),
    ("Which continent has the smallest population among permanently inhabited continents?", "Australia", "Antarctica", "South America", "Europe", "Australia", "Australia has the smallest population of the permanently inhabited continents."),
    ("What is the chemical element symbol for Silver?", "Ag", "Au", "Si", "Sr", "Ag", "Ag represents silver on the periodic table."),
    ("Which national flag features a red maple leaf at its center?", "Canada", "Lebanon", "Japan", "Mexico", "Canada", "The flag of Canada features a red maple leaf."),
    ("What is the official capital city of Japan?", "Tokyo", "Kyoto", "Osaka", "Yokohama", "Tokyo", "Tokyo is the capital and largest city of Japan."),
    ("Which island nation in the Caribbean is known for cigars, vintage cars, and Havana?", "Cuba", "Jamaica", "Haiti", "Bahamas", "Cuba", "Cuba is a Caribbean country with Havana as its capital."),
    ("What is the official currency of India?", "Indian Rupee", "Taka", "Rupiah", "Toman", "Indian Rupee", "The Indian Rupee (INR) is the legal tender of India."),
    ("Which organ in the human body cleanses toxins and produces bile?", "Liver", "Kidney", "Pancreas", "Spleen", "Liver", "The liver detoxifies chemicals and produces bile."),
    ("What is the deepest ocean trench on planet Earth?", "Mariana Trench", "Puerto Rico Trench", "Java Trench", "Tonga Trench", "Mariana Trench", "The Mariana Trench in the western Pacific is the deepest trench."),
    ("Which flightless bird is the largest living bird species today?", "Ostrich", "Emu", "Cassowary", "Penguin", "Ostrich", "The ostrich is the largest living bird species."),
    ("What is the official capital city of France?", "Paris", "Marseille", "Lyon", "Nice", "Paris", "Paris is the capital of France."),
    ("Which gas is the second most abundant in Earth's atmosphere?", "Oxygen", "Argon", "Carbon Dioxide", "Hydrogen", "Oxygen", "Oxygen comprises about 21% of dry air."),
    ("What is the national animal of India featuring dark stripes?", "Bengal Tiger", "Indian Elephant", "Snow Leopard", "Rhino", "Bengal Tiger", "The Bengal tiger is the national animal of India."),
    ("Which canal links the Red Sea directly to the Mediterranean Sea?", "Suez Canal", "Panama Canal", "Kiel Canal", "Erie Canal", "Suez Canal", "The Suez Canal connects the Mediterranean and Red seas."),
    ("Which Italian Renaissance master painted the Mona Lisa?", "Leonardo da Vinci", "Michelangelo", "Raphael", "Titian", "Leonardo da Vinci", "Leonardo da Vinci painted the Mona Lisa in the 16th century."),
    ("What is the official capital city of Australia?", "Canberra", "Sydney", "Melbourne", "Brisbane", "Canberra", "Canberra is the federal capital of Australia."),
    ("Which chemical element is represented by symbol 'K'?", "Potassium", "Krypton", "Kalium", "Phosphorus", "Potassium", "K represents potassium on the periodic table."),
    ("What is the official currency of Japan?", "Japanese Yen", "Won", "Yuan", "Ringgit", "Japanese Yen", "The Japanese Yen is the official currency of Japan."),
    ("Which famous painting depicting a night sky was painted by Vincent van Gogh?", "The Starry Night", "Sunflowers", "The Night Watch", "Water Lilies", "The Starry Night", "Van Gogh painted The Starry Night in 1889."),
    ("What is the official capital city of Egypt?", "Cairo", "Alexandria", "Giza", "Luxor", "Cairo", "Cairo is the capital city of Egypt."),
    ("Which primary color mixed with red creates orange?", "Yellow", "Blue", "Green", "Purple", "Yellow", "Mixing red and yellow creates orange."),
    ("What is the official currency of the United Kingdom?", "Pound Sterling", "Euro", "Franc", "Krona", "Pound Sterling", "The Pound Sterling is the currency of the UK."),
    ("Which desert animal is traditionally known as the 'Ship of the Desert'?", "Camel", "Dromedary", "Llama", "Donkey", "Camel", "Camels are suited for long desert travel."),
    ("What is the official capital city of Germany?", "Berlin", "Munich", "Frankfurt", "Hamburg", "Berlin", "Berlin is the capital of Germany."),
    ("Which fundamental subatomic particle carries a positive charge?", "Proton", "Electron", "Neutron", "Photon", "Proton", "Protons are positively charged subatomic particles."),
    ("What is the largest organ of the human body by surface area?", "Skin", "Liver", "Lungs", "Brain", "Skin", "The skin is the body's largest organ."),
    ("Which famous scientist introduced the theory of general relativity?", "Albert Einstein", "Isaac Newton", "Niels Bohr", "Galileo Galilei", "Albert Einstein", "Einstein formulated the general theory of relativity."),
    ("What is the official capital city of Italy?", "Rome", "Milan", "Venice", "Florence", "Rome", "Rome is the capital of Italy."),
    ("Which chemical element is designated by symbol 'Fe'?", "Iron", "Iridium", "Indium", "Iodine", "Iron", "Fe represents iron on the periodic table."),
    ("What is the official currency of China?", "Renminbi (Yuan)", "Yen", "Won", "Baht", "Renminbi (Yuan)", "The Renminbi is the currency of China."),
    ("What is the official capital city of Spain?", "Madrid", "Barcelona", "Seville", "Valencia", "Madrid", "Madrid is the capital city of Spain."),
    ("Which nonmetallic element is represented by symbol 'O'?", "Oxygen", "Osmium", "Oganesson", "Ozone", "Oxygen", "O represents oxygen."),
    ("What is the capital city of Russia?", "Moscow", "Saint Petersburg", "Kazan", "Novosibirsk", "Moscow", "Moscow is the capital of Russia."),
    ("Which primary color mixed with blue yields green?", "Yellow", "Red", "Black", "White", "Yellow", "Yellow and blue make green."),
    ("What is the capital city of the United States?", "Washington, D.C.", "New York City", "Los Angeles", "Chicago", "Washington, D.C.", "Washington, D.C. is the US capital."),
    ("Which gas is absorbed by plants during photosynthesis?", "Carbon Dioxide", "Oxygen", "Nitrogen", "Argon", "Carbon Dioxide", "Plants consume CO₂ during photosynthesis."),
    ("Which planet in our solar system is famous for its bright ring system?", "Saturn", "Jupiter", "Uranus", "Neptune", "Saturn", "Saturn possesses a prominent ring system."),
    ("What is the capital city of India?", "New Delhi", "Mumbai", "Kolkata", "Bengaluru", "New Delhi", "New Delhi is the capital of India."),
    ("Which chemical element is represented by symbol 'Na'?", "Sodium", "Nickel", "Neon", "Nitrogen", "Sodium", "Na represents sodium."),
    ("Which landmark in Paris was built for the 1889 World's Fair?", "Eiffel Tower", "Arc de Triomphe", "Louvre", "Notre-Dame", "Eiffel Tower", "The Eiffel Tower was built for the 1889 Exposition."),
    ("Which country in North America has Ottawa as its capital?", "Canada", "United States", "Mexico", "Greenland", "Canada", "Ottawa is the capital of Canada."),
    ("What is the capital city of Greece?", "Athens", "Sparta", "Thessaloniki", "Patras", "Athens", "Athens is the capital of Greece."),
    ("Which chemical symbol represents Gold?", "Au", "Ag", "Fe", "Cu", "Au", "Au stands for gold."),
    ("What is the capital city of Thailand?", "Bangkok", "Chiang Mai", "Phuket", "Pattaya", "Bangkok", "Bangkok is the capital of Thailand."),
    ("Which continent is home to the Sahara Desert?", "Africa", "Asia", "Australia", "South America", "Africa", "The Sahara is located in Africa."),
    ("What is the capital city of Turkey?", "Ankara", "Istanbul", "Izmir", "Antalya", "Ankara", "Ankara is the capital of Turkey."),
    ("What is the capital city of Portugal?", "Lisbon", "Porto", "Coimbra", "Braga", "Lisbon", "Lisbon is the capital of Portugal."),
    ("Which ocean is the smallest and shallowest?", "Arctic Ocean", "Indian Ocean", "Atlantic Ocean", "Pacific Ocean", "Arctic Ocean", "The Arctic Ocean is the smallest ocean."),
    ("What is the capital city of Ireland?", "Dublin", "Belfast", "Cork", "Galway", "Dublin", "Dublin is the capital of Ireland."),
    ("Which gas is lighter than air and causes balloons to float?", "Helium", "Hydrogen", "Oxygen", "Nitrogen", "Helium", "Helium is a light noble gas."),
    ("What is the capital city of Norway?", "Oslo", "Bergen", "Trondheim", "Stavanger", "Oslo", "Oslo is the capital of Norway."),
    ("Which planet is closest to the Sun?", "Mercury", "Venus", "Earth", "Mars", "Mercury", "Mercury is the closest planet to the Sun."),
    ("What is the capital city of Switzerland?", "Bern", "Zurich", "Geneva", "Basel", "Bern", "Bern is the capital of Switzerland."),
    ("Which chemical element is represented by symbol 'Pb'?", "Lead", "Plutonium", "Platinum", "Palladium", "Lead", "Pb represents lead."),
    ("What is the capital city of Finland?", "Helsinki", "Tampere", "Turku", "Oulu", "Helsinki", "Helsinki is the capital of Finland."),
    ("Which element has the chemical symbol 'W'?", "Tungsten", "Titanium", "Thorium", "Thallium", "Tungsten", "W represents tungsten."),
    ("What is the capital city of Iceland?", "Reykjavik", "Akureyri", "Keflavik", "Hafnarfjörður", "Reykjavik", "Reykjavik is the capital of Iceland."),
    ("Which natural allotrope of carbon is the hardest known mineral?", "Diamond", "Graphite", "Graphene", "Fullerene", "Diamond", "Diamond is the hardest mineral."),
    ("What is the capital city of the Czech Republic?", "Prague", "Brno", "Ostrava", "Plzen", "Prague", "Prague is the capital of the Czech Republic."),
    ("Which chemical metalloid element has the symbol 'Si'?", "Silicon", "Selenium", "Sodium", "Silver", "Silicon", "Silicon is represented by Si."),
    ("What is the constitutional capital city of Bolivia?", "Sucre", "La Paz", "Cochabamba", "Santa Cruz", "Sucre", "Sucre is Bolivia's constitutional capital."),
    # Extra unique items to prevent pool exhaustion:
    ("Which country is shaped like a boot in Southern Europe?", "Italy", "Greece", "Spain", "Portugal", "Italy", "Italy is recognized for its boot-shaped peninsula."),
    ("What is the capital city of South Korea?", "Seoul", "Busan", "Incheon", "Daegu", "Seoul", "Seoul is the capital of South Korea."),
    ("Which sea creature has eight arms and three hearts?", "Octopus", "Squid", "Cuttlefish", "Jellyfish", "Octopus", "Octopuses feature 8 arms and 3 hearts."),
    ("What is the official language spoken in Brazil?", "Portuguese", "Spanish", "French", "Italian", "Portuguese", "Portuguese is Brazil's official language."),
    ("Which country gifted the Statue of Liberty to the United States?", "France", "United Kingdom", "Germany", "Spain", "France", "France gifted the Statue of Liberty in 1886."),
    ("What is the capital city of Argentina?", "Buenos Aires", "Cordoba", "Rosario", "Mendoza", "Buenos Aires", "Buenos Aires is the capital of Argentina."),
    ("Which continent has no native human population or permanent countries?", "Antarctica", "Australia", "Greenland", "Siberia", "Antarctica", "Antarctica has no native human residents."),
    ("What is the capital city of Colombia?", "Bogotá", "Medellín", "Cali", "Cartagena", "Bogotá", "Bogotá is the high-altitude capital of Colombia."),
    ("Which element is represented by symbol 'Cu'?", "Copper", "Cobalt", "Chromium", "Californium", "Copper", "Cu represents copper."),
    ("What is the official currency of South Africa?", "South African Rand", "Pula", "Kwacha", "Cedi", "South African Rand", "The Rand is South Africa's currency."),
    ("Which famous monument in Agra was built by Shah Jahan?", "Taj Mahal", "Red Fort", "Qutub Minar", "Hawa Mahal", "Taj Mahal", "Shah Jahan commissioned the Taj Mahal."),
    ("What is the official currency of Mexico?", "Mexican Peso", "Sol", "Real", "Bolivar", "Mexican Peso", "The Mexican Peso is Mexico's currency."),
    ("Which animal is known as the 'King of the Jungle'?", "Lion", "Tiger", "Jaguar", "Leopard", "Lion", "Lions are traditionally dubbed 'King of the Jungle'."),
    ("What is the capital city of Kenya?", "Nairobi", "Mombasa", "Kisumu", "Nakuru", "Nairobi", "Nairobi is the capital city of Kenya."),
    ("Which scale measures temperature where water freezes at 32 degrees?", "Fahrenheit", "Celsius", "Kelvin", "Rankine", "Fahrenheit", "On the Fahrenheit scale, water freezes at 32°F."),
    ("What is the capital city of Peru?", "Lima", "Arequipa", "Cusco", "Trujillo", "Lima", "Lima is the capital city of Peru."),
    ("Which instrument measures atmospheric pressure?", "Barometer", "Thermometer", "Hygrometer", "Speedometer", "Barometer", "Barometers measure atmospheric pressure."),
    ("What is the capital city of New Zealand?", "Wellington", "Auckland", "Christchurch", "Hamilton", "Wellington", "Wellington is New Zealand's capital."),
    ("Which primary color mixed with yellow makes green?", "Blue", "Red", "Black", "White", "Blue", "Blue and yellow combine to make green."),
    ("What is the capital city of Sweden?", "Stockholm", "Gothenburg", "Malmo", "Uppsala", "Stockholm", "Stockholm is the capital of Sweden."),
    ("Which planet is known as the 'Red Planet'?", "Mars", "Venus", "Jupiter", "Saturn", "Mars", "Mars is called the Red Planet due to iron oxide on its surface."),
    ("What is the capital city of Denmark?", "Copenhagen", "Aarhus", "Odense", "Aalborg", "Copenhagen", "Copenhagen is the capital of Denmark."),
    ("Which chemical symbol represents Sodium?", "Na", "So", "Ni", "Ne", "Na", "Na stands for sodium."),
    ("What is the capital city of Belgium?", "Brussels", "Antwerp", "Ghent", "Bruges", "Brussels", "Brussels is the capital of Belgium."),
    ("Which pigment gives human skin and hair its color?", "Melanin", "Carotene", "Hemoglobin", "Keratin", "Melanin", "Melanin determines skin and hair pigmentation."),
    ("What is the capital city of Hungary?", "Budapest", "Debrecen", "Szeged", "Pécs", "Budapest", "Budapest is the capital of Hungary."),
    ("Which gas is produced by plants during photosynthesis?", "Oxygen", "Carbon Dioxide", "Nitrogen", "Hydrogen", "Oxygen", "Plants produce oxygen as a byproduct of photosynthesis."),
    ("What is the capital city of Austria?", "Vienna", "Salzburg", "Innsbruck", "Graz", "Vienna", "Vienna is the capital city of Austria."),
    ("Which instrument is used to view distant celestial objects in space?", "Telescope", "Microscope", "Periscope", "Kaleidoscope", "Telescope", "Telescopes gather light to observe distant space objects."),
    ("What is the capital city of Nepal?", "Kathmandu", "Pokhara", "Lalitpur", "Biratnagar", "Kathmandu", "Kathmandu is the capital of Nepal."),
    ("Which chemical element is represented by symbol 'K' on periodic table?", "Potassium", "Krypton", "Kalium", "Phosphorus", "Potassium", "K represents potassium."),
    ("What is the capital city of Netherlands?", "Amsterdam", "Rotterdam", "The Hague", "Utrecht", "Amsterdam", "Amsterdam is the capital of the Netherlands."),
]

sci_pool = [
    ("Which metric unit measures electrical resistance in a circuit?", "Ohm", "Volt", "Ampere", "Watt", "Ohm", "The ohm (symbol Ω) is the SI unit of electrical resistance."),
    ("What process describes water movement across a semipermeable membrane from low to high solute concentration?", "Osmosis", "Diffusion", "Active Transport", "Filtration", "Osmosis", "Osmosis is the net movement of solvent molecules across a semipermeable membrane."),
    ("Which component of human blood carries oxygen throughout the body?", "Red Blood Cells", "White Blood Cells", "Platelets", "Plasma", "Red Blood Cells", "Erythrocytes (red blood cells) contain hemoglobin to transport oxygen."),
    ("What type of rock is formed from the cooling and solidification of lava or magma?", "Igneous", "Sedimentary", "Metamorphic", "Fossiliferous", "Igneous", "Igneous rocks form through the cooling of molten rock."),
    ("Which law of motion states that for every action there is an equal and opposite reaction?", "Newton's Third Law", "Newton's First Law", "Newton's Second Law", "Law of Gravitation", "Newton's Third Law", "Newton's Third Law states forces occur in equal and opposite pairs."),
    ("Which subatomic particle has no electrical charge?", "Neutron", "Proton", "Electron", "Positron", "Neutron", "Neutrons are neutral subatomic particles located in atomic nuclei."),
    ("Which tissue connects skeletal muscle to bone in the human body?", "Tendon", "Ligament", "Cartilage", "Fascia", "Tendon", "Tendons are tough fibrous connective tissues connecting muscles to bones."),
    ("What physical property describes a fluid's resistance to flow?", "Viscosity", "Density", "Surface Tension", "Buoyancy", "Viscosity", "Viscosity measures a fluid's internal friction and resistance to gradual deformation."),
    ("Which plant hormone promotes stem elongation and cell growth?", "Auxin", "Cytokinin", "Gibberellin", "Abscisic Acid", "Auxin", "Auxins regulate plant growth, stem elongation, and tropisms."),
    ("Which gas is the main component of natural gas used for fuel?", "Methane", "Ethane", "Propane", "Butane", "Methane", "Methane (CH₄) makes up 70–90% of natural gas."),
    ("Which endocrine gland produces the hormone insulin?", "Pancreas", "Thyroid", "Adrenal Gland", "Pituitary", "Pancreas", "Beta cells in the pancreas produce insulin."),
    ("Which branch of physics studies light and its interactions with matter?", "Optics", "Thermodynamics", "Acoustics", "Mechanics", "Optics", "Optics is the branch of physics studying behavior and properties of light."),
    ("Which structural protein forms human hair, nails, and outer skin layer?", "Keratin", "Collagen", "Elastin", "Fibrin", "Keratin", "Keratin is a tough fibrous structural protein forming hair and nails."),
    ("What term describes the maximum population size an ecosystem can sustain?", "Carrying Capacity", "Biomass", "Niche", "Trophic Level", "Carrying Capacity", "Carrying capacity is the maximum sustainable population size in an environment."),
    ("Which subatomic particle orbits the atomic nucleus in energy levels?", "Electron", "Proton", "Neutron", "Quark", "Electron", "Electrons carry negative charge and orbit the atomic nucleus."),
    ("Which law states that pressure of a gas is inversely proportional to its volume at constant temperature?", "Boyle's Law", "Charles's Law", "Gay-Lussac's Law", "Avogadro's Law", "Boyle's Law", "Boyle's Law states P₁V₁ = P₂V₂ for an ideal gas at constant temperature."),
    ("Which cellular organelle contains digestive hydrolytic enzymes?", "Lysosome", "Peroxisome", "Vacuole", "Ribosome", "Lysosome", "Lysosomes contain hydrolytic enzymes to break down biomolecules."),
    ("Which fundamental force holds protons and neutrons together inside the nucleus?", "Strong Nuclear Force", "Weak Nuclear Force", "Electromagnetic Force", "Gravity", "Strong Nuclear Force", "The strong force binds nucleons together in atomic nuclei."),
    ("Which planet has the shortest day in our solar system, rotating once every 10 hours?", "Jupiter", "Saturn", "Neptune", "Uranus", "Jupiter", "Jupiter has the fastest rotation period of any planet in our solar system."),
    ("Which process converts glucose into pyruvate, producing ATP without oxygen?", "Glycolysis", "Krebs Cycle", "Electron Transport Chain", "Calvin Cycle", "Glycolysis", "Glycolysis breaks down glucose into 2 pyruvate molecules in the cytoplasm."),
    ("What optical phenomenon causes light to spread out when passing through a narrow aperture?", "Diffraction", "Refraction", "Reflection", "Dispersion", "Diffraction", "Diffraction is the bending and spreading of waves around obstacles or through slits."),
    ("Which organelle converts light energy into chemical energy during photosynthesis?", "Chloroplast", "Mitochondria", "Chromoplast", "Leucoplast", "Chloroplast", "Chloroplasts contain chlorophyll to carry out photosynthesis in plant cells."),
    ("Which constant connects photon energy to electromagnetic wave frequency (E = hf)?", "Planck's Constant", "Boltzmann Constant", "Avogadro Constant", "Gas Constant", "Planck's Constant", "Planck's constant h = 6.626×10⁻³⁴ J·s relates energy and frequency."),
    ("Which subatomic particle is made up of two up quarks and one down quark?", "Proton", "Neutron", "Electron", "Pion", "Proton", "A proton is a composite hadron made of 2 up quarks and 1 down quark (uud)."),
    ("Which quantum mechanical rule states two identical fermions cannot occupy the same quantum state?", "Pauli Exclusion Principle", "Heisenberg Principle", "Hund's Rule", "Aufbau Principle", "Pauli Exclusion Principle", "The Pauli exclusion principle states no 2 identical fermions share all quantum numbers."),
]

his_pool = [
    ("Which conflict between 1861 and 1865 was fought between the US Union and Confederacy?", "American Civil War", "American Revolutionary War", "War of 1812", "Mexican-American War", "American Civil War", "The Civil War resulted in the preservation of the Union and abolition of slavery."),
    ("Which king of Macedon built an empire stretching from Greece to northwestern India by age 30?", "Alexander the Great", "Julius Caesar", "Xerxes I", "Hannibal", "Alexander the Great", "Alexander the Great conquered the Persian Empire and expanded Greek influence."),
    ("Which French heroine rallied troops during the Hundred Years' War before being executed in 1431?", "Joan of Arc", "Marie Antoinette", "Eleanor of Aquitaine", "Catherine de' Medici", "Joan of Arc", "Joan of Arc led French forces to victory at Orléans before her capture."),
    ("Which ancient Greek city-state was renowned for its disciplined warrior society and land military?", "Sparta", "Athens", "Corinth", "Thebes", "Sparta", "Sparta was a dominant military land power in ancient Greece."),
    ("Which English monarch established the Church of England after breaking ties with Rome?", "Henry VIII", "Elizabeth I", "Charles I", "Henry VII", "Henry VIII", "Henry VIII passed the Act of Supremacy in 1534 founding the Church of England."),
    ("Which peace treaty signed in 1919 officially brought World War I to an end?", "Treaty of Versailles", "Treaty of Utrecht", "Treaty of Westphalia", "Treaty of Ghent", "Treaty of Versailles", "The Treaty of Versailles formally ended WWI between Germany and the Allies."),
    ("Which Roman Emperor issued the Edict of Milan in 313 AD legalizing Christianity?", "Constantine the Great", "Augustus", "Nero", "Trajan", "Constantine the Great", "Constantine I declared religious tolerance for Christianity across the Roman Empire."),
    ("Which Mongol leader unified steppe tribes in 1206 to found the Mongol Empire?", "Genghis Khan", "Kublai Khan", "Tamerlane", "Batu Khan", "Genghis Khan", "Genghis Khan founded the largest contiguous land empire in history."),
    ("Which fortress in Paris was stormed on July 14, 1789, sparking the French Revolution?", "Bastille", "Conciergerie", "Château de Vincennes", "Louvre Fortress", "Bastille", "The Storming of the Bastille marked the start of the French Revolution."),
    ("Which Mesoamerican empire built the mountain citadel of Machu Picchu in the 15th century?", "Inca Empire", "Aztec Empire", "Maya Empire", "Muisca Confederation", "Inca Empire", "Machu Picchu was constructed under the reign of Inca Emperor Pachacuti."),
]

# Process and rewrite DefaultQuestionSeeds.kt cleanly
def clean_and_update():
    global text
    
    gk_items = get_category_data("GK")
    sci_items = get_category_data("SCI")
    his_items = get_category_data("HIS")
    
    def process_cat(items, pool, prefix):
        seen_ans = set()
        seen_q_texts = set()
        clean_list = []
        p_idx = 0

        for q in items:
            q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
            ans_norm = re.sub(r'[^a-z0-9]', '', ans.lower().strip())
            q_norm = re.sub(r'[^a-z0-9]', '', question.lower().strip())
            
            is_dup = False
            if ans_norm in seen_ans or q_norm in seen_q_texts:
                is_dup = True
            else:
                t1 = tokenize(question)
                for cq in clean_list:
                    t2 = tokenize(cq[2])
                    if t1 and t2:
                        sim = len(t1.intersection(t2)) / len(t1.union(t2))
                        if sim > 0.38:
                            is_dup = True
                            break

            if not is_dup:
                seen_ans.add(ans_norm)
                seen_q_texts.add(q_norm)
                clean_list.append(q)
            else:
                # Replace
                found = False
                while p_idx < len(pool):
                    pq_q, pq_a, pq_b, pq_c, pq_d, pq_ans, pq_exp = pool[p_idx]
                    p_idx += 1
                    
                    p_ans_norm = re.sub(r'[^a-z0-9]', '', pq_ans.lower().strip())
                    p_q_norm = re.sub(r'[^a-z0-9]', '', pq_q.lower().strip())
                    
                    if p_ans_norm not in seen_ans and p_q_norm not in seen_q_texts:
                        pt = tokenize(pq_q)
                        sim_conflict = False
                        for cq in clean_list:
                            ct = tokenize(cq[2])
                            if pt and ct:
                                sim = len(pt.intersection(ct)) / len(pt.union(ct))
                                if sim > 0.38:
                                    sim_conflict = True
                                    break
                        if not sim_conflict:
                            new_item = [q_id, cat, pq_q, pq_a, pq_b, pq_c, pq_d, pq_ans, pq_exp, diff]
                            seen_ans.add(p_ans_norm)
                            seen_q_texts.add(p_q_norm)
                            clean_list.append(new_item)
                            found = True
                            break
                if not found:
                    print(f"ERROR: Pool exhausted for {prefix} at {q_id}")
                    clean_list.append(q)
        return clean_list

    new_gk = process_cat(gk_items, gk_pool, "GK")
    new_sci = process_cat(sci_items, sci_pool, "SCI")
    new_his = process_cat(his_items, his_pool, "HIS")

    # Map back to replacement dict
    all_new = new_gk + new_sci + new_his
    repl_dict = {q[0]: q for q in all_new}

    new_lines = []
    for line in text.splitlines():
        if 'QuestionEntity("' in line:
            parts = parse_entity(line)
            if len(parts) == 10:
                q_id = parts[0]
                if q_id in repl_dict:
                    r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = repl_dict[q_id]
                    indent = line[:line.find('QuestionEntity("')]
                    line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
        new_lines.append(line)

    with open(file_path, "w") as f:
        f.write("\n".join(new_lines))

    print("Database updated successfully!")

clean_and_update()

