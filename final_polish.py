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

final_replacements = {
    # Science Fixes
    "SCI240": ("SCI240", "science", "Which small pear-shaped organ located beneath the liver stores and concentrates bile?", "Gallbladder", "Pancreas", "Spleen", "Appendix", "Gallbladder", "The gallbladder stores and concentrates bile until released into the small intestine.", "Medium"),
    "SCI234": ("SCI234", "science", "Which membrane-bound organelle modifies, sorts, and packages proteins for transport or secretion?", "Golgi Apparatus", "Ribosome", "Lysosome", "Peroxisome", "Golgi Apparatus", "The Golgi apparatus processes and packages proteins synthesized in the endoplasmic reticulum.", "Medium"),
    "SCI177": ("SCI177", "science", "Which fluid mechanics principle states that pressure applied to a confined fluid is transmitted equally in all directions?", "Pascal's Law", "Bernoulli's Principle", "Archimedes' Principle", "Torricelli's Law", "Pascal's Law", "Pascal's law states that pressure changes in confined incompressible fluids are transmitted undiminished.", "Medium"),
    "SCI205": ("SCI205", "science", "Which fundamental physical force acts between electrically charged subatomic particles via photons?", "Electromagnetic Force", "Strong Nuclear Force", "Weak Nuclear Force", "Gravitational Force", "Electromagnetic Force", "The electromagnetic force governs electrostatic repulsion/attraction and magnetic phenomena.", "Medium"),

    # GK Fixes
    "GK295": ("GK295", "gk", "What is the official currency of the Scandinavian nation of Sweden?", "Swedish Krona", "Euro", "Krone", "Mark", "Swedish Krona", "The Swedish Krona (SEK) is the official currency of Sweden.", "Hard"),
    "GK287": ("GK287", "gk", "What is the official currency unit used in the Republic of South Africa?", "South African Rand", "Pula", "Cedi", "Kwacha", "South African Rand", "The South African Rand (ZAR) is the legal tender of South Africa.", "Hard"),
    "GK120": ("GK120", "gk", "What is the official national language spoken in the South American country of Brazil?", "Portuguese", "Spanish", "French", "Italian", "Portuguese", "Portuguese is the sole official language of Brazil.", "Easy"),

    # History Fixes (Difficulty Alignment)
    "HIS202": ("HIS202", "history", "Which conflict from 1861 to 1865 was fought between Northern Union and Southern Confederate states in America?", "American Civil War", "American Revolutionary War", "War of 1812", "Mexican-American War", "American Civil War", "The Civil War preserved the United States Union and abolished slavery across the nation.", "Medium"),
    "HIS205": ("HIS205", "history", "Which ancient city-state in Greece was famous for developing the world's first direct democracy?", "Athens", "Sparta", "Corinth", "Thebes", "Athens", "Classical Athens developed direct democracy under leaders like Cleisthenes and Pericles.", "Medium"),
    "HIS211": ("HIS211", "history", "Which Ottoman Sultan captured Constantinople in 1453, ending the Byzantine Empire?", "Mehmed II", "Suleiman the Magnificent", "Selim I", "Osman I", "Mehmed II", "Mehmed the Conqueror captured Constantinople at age 21, establishing Ottoman imperial power.", "Medium"),
    "HIS218": ("HIS218", "history", "Which Mauryan Emperor renounced military conquest after the Kalinga War and adopted Buddhism?", "Ashoka the Great", "Chandragupta Maurya", "Bindusara", "Brihadratha", "Ashoka the Great", "Emperor Ashoka embraced Dhamma, non-violence, and spread Buddhism across Asia.", "Medium"),
    "HIS219": ("HIS219", "history", "Which historic naval battle in 31 BCE saw Octavian defeat Mark Antony and Cleopatra off the coast of Greece?", "Battle of Actium", "Battle of Salamis", "Battle of Lepanto", "Battle of Trafalgar", "Battle of Actium", "Octavian's victory at Actium led directly to the end of the Roman Republic and start of Roman Empire.", "Medium"),
    "HIS230": ("HIS230", "history", "Which Roman Emperor constructed a massive 73-mile stone wall across northern Britain in 122 AD?", "Hadrian", "Antoninus Pius", "Trajan", "Marcus Aurelius", "Hadrian", "Hadrian's Wall was built to mark the northern boundary of Roman Britain.", "Medium"),
    "HIS232": ("HIS232", "history", "Which ancient Mesopotamian civilization created the world's earliest cuneiform writing system?", "Sumerians", "Babylonians", "Assyrians", "Akkadians", "Sumerians", "Sumerians in southern Mesopotamia invented cuneiform script around 3400 BCE.", "Medium"),
}

new_lines = []
for line in text.splitlines():
    if 'QuestionEntity("' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id = parts[0]
            if q_id in final_replacements:
                r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = final_replacements[q_id]
                indent = line[:line.find('QuestionEntity("')]
                line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write("\n".join(new_lines))

print("Applied final polish adjustments!")

