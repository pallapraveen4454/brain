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

flawless_fixes = {
    # Science Fix
    "SCI234": ("SCI234", "science", "Which dense sub-structure inside the eukaryotic cell nucleus synthesizes ribosomal RNA (rRNA) and assembles ribosome subunits?", "Nucleolus", "Centrosome", "Peroxisome", "Lysosome", "Nucleolus", "The nucleolus is the distinct site of ribosome RNA transcription and assembly inside the nucleus.", "Medium"),

    # History Fixes
    "HIS162": ("HIS162", "history", "Which 1598 edict issued by French King Henry IV granted Calvinist Protestants (Huguenots) religious tolerance?", "Edict of Nantes", "Edict of Milan", "Edict of Worms", "Edict of Fontainebleau", "Edict of Nantes", "The Edict of Nantes ended the French Wars of Religion by granting rights to Huguenots.", "Medium"),
    "HIS205": ("HIS205", "history", "Which Athenian statesman reformed the constitution in 508 BCE and is celebrated as the 'Father of Athenian Democracy'?", "Cleisthenes", "Pericles", "Solon", "Draco", "Cleisthenes", "Cleisthenes instituted democratic governance reforms in ancient Athens.", "Medium"),
    "HIS217": ("HIS217", "history", "Which historic 1789 pledge by members of the Third Estate declared they would not separate until a French constitution was written?", "Tennis Court Oath", "Declaration of the Rights of Man", "Civil Constitution of the Clergy", "September Massacres", "Tennis Court Oath", "The Tennis Court Oath was a pivotal event asserting popular sovereignty during the French Revolution.", "Medium"),
    "HIS218": ("HIS218", "history", "Which Persian monarch established the Achaemenid Empire and issued the Cyrus Cylinder declaring human rights and religious freedom?", "Cyrus the Great", "Darius the Great", "Xerxes I", "Artaxerxes I", "Cyrus the Great", "Cyrus II founded the first Persian Empire and conquered Babylon in 539 BCE.", "Medium"),
    "HIS229": ("HIS229", "history", "Which 1805 military engagement, also called the 'Battle of the Three Emperors', is considered Napoleon's tactical masterpiece?", "Battle of Austerlitz", "Battle of Jena-Auerstedt", "Battle of Wagram", "Battle of Borodino", "Battle of Austerlitz", "Napoleon decisively defeated a combined Russo-Austrian army at Austerlitz.", "Medium"),
    "HIS232": ("HIS232", "history", "Which ancient Mesoamerican civilization, famous for carving colossal basalt stone heads, is regarded as the mother culture of the region?", "Olmec Civilization", "Zapotec Civilization", "Mixtec Civilization", "Toltec Civilization", "Olmec Civilization", "The Olmecs flourished in lowland Mexico (c. 1200–400 BCE) and built early ritual centers.", "Medium"),
}

new_lines = []
for line in text.splitlines():
    if 'QuestionEntity("' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id = parts[0]
            if q_id in flawless_fixes:
                r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = flawless_fixes[q_id]
                indent = line[:line.find('QuestionEntity("')]
                line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write("\n".join(new_lines))

print("Applied flawless fixes!")

