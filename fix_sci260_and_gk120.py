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

fixes = {
    "GK120": ("GK120", "gk", "Which European country has Berlin as its federal capital and German as its primary official language?", "Germany", "Austria", "Switzerland", "Liechtenstein", "Germany", "Germany is situated in Central Europe with Berlin as its capital.", "Easy"),
    "SCI260": ("SCI260", "science", "Which solid-state physics theory classifies materials into conductors, semiconductors, and insulators based on energy gaps between valence and conduction bands?", "Band Theory of Solids", "Drude Model", "BCS Theory", "Sommerfeld Model", "Band Theory of Solids", "Band theory describes quantum energy states allowed for electrons in crystalline solids.", "Hard"),
}

new_lines = []
for line in text.splitlines():
    if 'QuestionEntity("' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id = parts[0]
            if q_id in fixes:
                r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = fixes[q_id]
                indent = line[:line.find('QuestionEntity("')]
                line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write("\n".join(new_lines))

print("Applied fix for SCI260 and GK120!")

