import re

file_path = "app/src/main/java/com/example/data/database/DefaultQuestionSeeds.kt"
with open(file_path) as f:
    text = f.read()

def parse_entity(line):
    inner = line[line.find("QuestionEntity(")+len("QuestionEntity("):line.rfind(")")]
    parts = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', inner)
    return parts

def to_kt_string(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

# Targeted replacements for the 30 flagged IDs with brand new concepts/facts

exact_replacements = {
    # GK Replacements (4 items)
    "GK285": ("GK285", "gk", "What is the capital city of Bangladesh situated along the Buriganga River?", "Dhaka", "Chittagong", "Sylhet", "Rajshahi", "Dhaka", "Dhaka is the capital and largest city of Bangladesh.", "Hard"),
    "GK290": ("GK290", "gk", "What is the capital city of Ethiopia located in the Horn of Africa?", "Addis Ababa", "Dire Dawa", "Mekelle", "Gondar", "Addis Ababa", "Addis Ababa is the capital city of Ethiopia and headquarters of the African Union.", "Hard"),
    "GK293": ("GK293", "gk", "What is the capital city of Sri Lanka located on the western coast?", "Colombo", "Kandy", "Galle", "Jaffna", "Colombo", "Colombo is the executive and judicial capital and largest city of Sri Lanka.", "Hard"),
    "GK299": ("GK299", "gk", "What is the capital city of Morocco located on the Atlantic coast?", "Rabat", "Casablanca", "Marrakesh", "Fes", "Rabat", "Rabat is the official capital city of Morocco.", "Hard"),

    # Science Replacements (16 items)
    "SCI165": ("SCI165", "science", "Which metric unit measures magnetic field strength in the International System of Units?", "Tesla", "Weber", "Gauss", "Henry", "Tesla", "The tesla (symbol T) is the SI unit of magnetic flux density.", "Medium"),
    "SCI177": ("SCI177", "science", "Which law of fluid dynamics states that an increase in fluid speed occurs simultaneously with a decrease in pressure?", "Bernoulli's Principle", "Pascal's Law", "Archimedes' Principle", "Torricelli's Law", "Bernoulli's Principle", "Bernoulli's principle relates fluid flow velocity to static pressure.", "Medium"),
    "SCI178": ("SCI178", "science", "Which process converts light energy into chemical energy stored in glucose in photosynthetic organisms?", "Photosynthesis", "Chemiosmosis", "Photophosphorylation", "Transpiration", "Photosynthesis", "Photosynthesis synthesizes organic compounds from CO₂ and H₂O using light energy.", "Medium"),
    "SCI181": ("SCI181", "science", "Which rule in quantum mechanics predicts that electrons fill degenerate orbitals singly before pairing up?", "Hund's Rule", "Aufbau Principle", "Pauli Exclusion Principle", "Madelung Rule", "Hund's Rule", "Hund's rule of maximum multiplicity maximizes total electron spin in degenerate subshells.", "Medium"),
    "SCI201": ("SCI201", "science", "Which instrument is used to measure the specific gravity or relative density of liquids?", "Hydrometer", "Pycnometer", "Anemometer", "Manometer", "Hydrometer", "Hydrometers measure liquid relative density based on buoyancy principles.", "Medium"),
    "SCI205": ("SCI205", "science", "Which fundamental force governs radioactive decay and neutrino interactions in subatomic particles?", "Weak Nuclear Force", "Strong Nuclear Force", "Electromagnetic Force", "Gravitational Force", "Weak Nuclear Force", "The weak interaction causes beta decay by changing quark flavors.", "Medium"),
    "SCI216": ("SCI216", "science", "Which nucleic acid monomer consists of a nitrogenous base, a five-carbon sugar, and a phosphate group?", "Nucleotide", "Nucleoside", "Amino Acid", "Fatty Acid", "Nucleotide", "Nucleotides form the basic structural monomer units of DNA and RNA polymers.", "Medium"),
    "SCI219": ("SCI219", "science", "Which state of matter occurs near absolute zero when bosons collapse into a single quantum state?", "Bose-Einstein Condensate", "Plasma", "Supercritical Fluid", "Degenerate Matter", "Bose-Einstein Condensate", "BEC is a state of matter formed when low-density boson gas is cooled close to 0 K.", "Medium"),
    "SCI232": ("SCI232", "science", "What property of a lens or curved mirror measures its ability to bend or focus light rays?", "Focal Length", "Refractive Index", "Magnification", "Aperture", "Focal Length", "Focal length is the distance over which initially collimated rays are brought to a focus.", "Medium"),
    "SCI234": ("SCI234", "science", "Which double-membrane organelle produces ATP through oxidative phosphorylation during aerobic respiration?", "Mitochondrion", "Golgi Body", "Endoplasmic Reticulum", "Peroxisome", "Mitochondrion", "Mitochondria generate most of the cell's supply of ATP via cellular respiration.", "Medium"),
    "SCI240": ("SCI240", "science", "Which organ in the human body filters metabolic waste products from blood to produce urine?", "Kidney", "Spleen", "Pancreas", "Gallbladder", "Kidney", "Kidneys filter blood to remove urea, excess salts, and water as urine.", "Medium"),
    "SCI262": ("SCI262", "science", "Which class of elementary particles includes electrons, muons, taus, and their associated neutrinos?", "Leptons", "Quarks", "Baryons", "Mesons", "Leptons", "Leptons are half-integer spin fundamental particles that do not undergo strong interactions.", "Hard"),
    "SCI265": ("SCI265", "science", "Which law states that the total electric flux through a closed surface equals the enclosed charge divided by permittivity?", "Gauss's Law", "Ampere's Law", "Faraday's Law", "Coulomb's Law", "Gauss's Law", "Gauss's law relates electric charge distribution to resulting electric field.", "Hard"),
    "SCI283": ("SCI283", "science", "Which dark reaction pathway in plant photosynthesis fixes carbon dioxide into 3-phosphoglycerate?", "Calvin Cycle", "C4 Pathway", "CAM Pathway", "Glycolysis", "Calvin Cycle", "The Calvin cycle fixes CO₂ using RuBisCO enzyme in plant stroma.", "Hard"),
    "SCI286": ("SCI286", "science", "Which equation in quantum physics describes wave-particle duality by relating wavelength to momentum (λ = h/p)?", "de Broglie Relation", "Planck Postulate", "Einstein Photoelectric Equation", "Rydberg Formula", "de Broglie Relation", "Louis de Broglie proposed that matter particles possess wave properties with λ = h/p.", "Hard"),
    "SCI298": ("SCI298", "science", "Which thermodynamic state function combines internal energy, pressure, and volume (H = U + PV)?", "Enthalpy", "Entropy", "Gibbs Free Energy", "Helmholtz Energy", "Enthalpy", "Enthalpy is a thermodynamic property equal to internal energy plus pressure-volume product.", "Hard"),

    # History Replacements (10 items)
    "HIS162": ("HIS162", "history", "Which diplomatic conference in 1814–1815 reorganized Europe following the downfall of Napoleon Bonaparte?", "Congress of Vienna", "Congress of Berlin", "Peace of Westphalia", "Treaty of Utrecht", "Congress of Vienna", "The Congress of Vienna reconstituted the European balance of power after the Napoleonic Wars.", "Medium"),
    "HIS193": ("HIS193", "history", "Which Carthaginian military commander famously crossed the Alps with war elephants during the Second Punic War?", "Hannibal Barca", "Hamilcar Barca", "Hasdrubal", "Mago Barca", "Hannibal Barca", "Hannibal led Carthaginian forces against the Roman Republic across the Pyrenees and Alps.", "Medium"),
    "HIS202": ("HIS202", "history", "Which revolution in 1917 overthrew the Russian Tsarist autocracy and brought the Bolsheviks to power?", "Russian Revolution", "French Revolution", "Xinhai Revolution", "Glorious Revolution", "Russian Revolution", "The Russian Revolution abolished the monarchy and established the Soviet state under Lenin.", "Easy"),
    "HIS205": ("HIS205", "history", "Which ancient city-state in Greece was famous for developing the world's first direct democracy?", "Athens", "Sparta", "Corinth", "Thebes", "Athens", "Classical Athens developed direct democracy under leaders like Cleisthenes and Pericles.", "Easy"),
    "HIS211": ("HIS211", "history", "Which Ottoman Sultan captured Constantinople in 1453, ending the Byzantine Empire?", "Mehmed II", "Suleiman the Magnificent", "Selim I", "Osman I", "Mehmed II", "Mehmed the Conqueror captured Constantinople at age 21, establishing Ottoman imperial power.", "Easy"),
    "HIS218": ("HIS218", "history", "Which Mauryan Emperor renounced military conquest after the Kalinga War and adopted Buddhism?", "Ashoka the Great", "Chandragupta Maurya", "Bindusara", "Brihadratha", "Ashoka the Great", "Emperor Ashoka embraced Dhamma, non-violence, and spread Buddhism across Asia.", "Easy"),
    "HIS219": ("HIS219", "history", "Which historic naval battle in 31 BCE saw Octavian defeat Mark Antony and Cleopatra off the coast of Greece?", "Battle of Actium", "Battle of Salamis", "Battle of Lepanto", "Battle of Trafalgar", "Battle of Actium", "Octavian's victory at Actium led directly to the end of the Roman Republic and start of Roman Empire.", "Easy"),
    "HIS229": ("HIS229", "history", "Which 1805 naval battle off the Spanish coast saw Admiral Horatio Nelson defeat the Franco-Spanish fleet?", "Battle of Trafalgar", "Battle of the Nile", "Battle of Jutland", "Battle of Copenhagen", "Battle of Trafalgar", "The Battle of Trafalgar established British naval supremacy throughout the 19th century.", "Medium"),
    "HIS230": ("HIS230", "history", "Which Roman Emperor constructed a massive 73-mile stone wall across northern Britain in 122 AD?", "Hadrian", "Antoninus Pius", "Trajan", "Marcus Aurelius", "Hadrian", "Hadrian's Wall was built to mark the northern boundary of Roman Britain.", "Easy"),
    "HIS232": ("HIS232", "history", "Which ancient Mesopotamian civilization created the world's earliest cuneiform writing system?", "Sumerians", "Babylonians", "Assyrians", "Akkadians", "Sumerians", "Sumerians in southern Mesopotamia invented cuneiform script around 3400 BCE.", "Easy"),
}

new_lines = []
replaced_cnt = 0

for line in text.splitlines():
    if 'QuestionEntity("' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id = parts[0]
            if q_id in exact_replacements:
                r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = exact_replacements[q_id]
                indent = line[:line.find('QuestionEntity("')]
                line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
                replaced_cnt += 1
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write("\n".join(new_lines))

print(f"Applied targeted replacements for all {replaced_cnt} flagged questions!")

