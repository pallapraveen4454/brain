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

perfect_replacements = {
    # GK Replacements
    "GK120": ("GK120", "gk", "What is the sole official national language spoken in the Federal Republic of Germany?", "German", "French", "Dutch", "Italian", "German", "German is the official language of Germany.", "Easy"),
    "GK287": ("GK287", "gk", "What is the official currency unit used in the Nordic nation of Denmark?", "Danish Krone", "Euro", "Krona", "Franc", "Danish Krone", "The Danish Krone (DKK) is the official currency of Denmark.", "Hard"),

    # Science Replacements
    "SCI176": ("SCI176", "science", "What universal physical constant (~3.00 × 10^8 m/s) defines the speed at which electromagnetic radiation travels in a vacuum?", "Speed of Light", "Speed of Sound", "Escape Velocity", "Terminal Velocity", "Speed of Light", "The speed of light in vacuum c is exactly 299,792,458 m/s.", "Medium"),
    "SCI191": ("SCI191", "science", "Which gaseous plant hormone regulates fruit ripening, flower senescence, and leaf abscission?", "Ethylene", "Auxin", "Gibberellin", "Cytokinin", "Ethylene", "Ethylene (C₂H₄) is a hydrocarbon gas acting as a plant ripening hormone.", "Medium"),
    "SCI234": ("SCI234", "science", "Which cellular organelle lacking ribosomes is involved in lipid synthesis, phospholipid production, and chemical detoxification?", "Smooth Endoplasmic Reticulum", "Rough Endoplasmic Reticulum", "Golgi Apparatus", "Lysosome", "Smooth Endoplasmic Reticulum", "Smooth ER synthesizes lipids and detoxifies metabolic metabolic compounds.", "Medium"),

    # History Replacements
    "HIS162": ("HIS162", "history", "Which series of peace treaties signed in 1648 ended the Thirty Years' War and established modern state sovereignty?", "Peace of Westphalia", "Treaty of Utrecht", "Treaty of Versailles", "Congress of Vienna", "Peace of Westphalia", "The Peace of Westphalia established Westphalian sovereignty in international law.", "Medium"),
    "HIS193": ("HIS193", "history", "Which Roman general famously crossed the Rubicon river in 49 BCE, initiating the Roman Civil War?", "Julius Caesar", "Mark Antony", "Pompey the Great", "Scipio Africanus", "Julius Caesar", "Caesar's crossing of the Rubicon led to his dictatorship and the downfall of the Republic.", "Medium"),
    "HIS202": ("HIS202", "history", "Which global conflict from 1914 to 1918 involved the Allied Powers and Central Powers?", "World War I", "World War II", "Seven Years' War", "Crimean War", "World War I", "World War I mobilised over 70 million military personnel across the globe.", "Medium"),
    "HIS205": ("HIS205", "history", "Which ancient ruler unified the Mesopotamian city-states in 2334 BCE to establish the Akkadian Empire?", "Sargon of Akkad", "Hammurabi", "Gilgamesh", "Ashurbanipal", "Sargon of Akkad", "Sargon of Akkad created the first historically verifiable multi-ethnic empire.", "Medium"),
    "HIS218": ("HIS218", "history", "Which Babylonian king enacted a famous ancient legal code featuring 282 inscribed laws?", "Hammurabi", "Nebuchadnezzar II", "Nabonidus", "Shulgi", "Hammurabi", "The Code of Hammurabi established legal standards in ancient Babylonia.", "Medium"),
    "HIS229": ("HIS229", "history", "Which historic 1815 engagement saw Napoleon Bonaparte defeated by the Duke of Wellington and Gebhard von Blücher?", "Battle of Waterloo", "Battle of Leipzig", "Battle of Austerlitz", "Battle of Borodino", "Battle of Waterloo", "The Battle of Waterloo concluded the Napoleonic Wars in modern Belgium.", "Medium"),
    "HIS232": ("HIS232", "history", "Which Mesoamerican civilization constructed the majestic step-pyramid temple of El Castillo at Chichen Itza?", "Maya Civilization", "Aztec Empire", "Inca Empire", "Olmec Civilization", "Maya Civilization", "Chichen Itza was a dominant Maya urban center in the Yucatan peninsula.", "Medium"),
}

new_lines = []
for line in text.splitlines():
    if 'QuestionEntity("' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id = parts[0]
            if q_id in perfect_replacements:
                r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = perfect_replacements[q_id]
                indent = line[:line.find('QuestionEntity("')]
                line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write("\n".join(new_lines))

print("Applied final perfection replacements!")

