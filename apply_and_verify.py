import re
from collections import defaultdict
from generate_replacements import gk_replacements
from generate_all_replacements import sci_replacements, his_replacements

# Build replacement maps
replacements_map = {}
for q in gk_replacements + sci_replacements + his_replacements:
    replacements_map[q[0]] = q

print(f"Total replacements staged: {len(replacements_map)}")
print(f"  GK replacements: {len(gk_replacements)}")
print(f"  SCI replacements: {len(sci_replacements)}")
print(f"  HIS replacements: {len(his_replacements)}")

file_path = "app/src/main/java/com/example/data/database/DefaultQuestionSeeds.kt"
with open(file_path, "r") as f:
    text = f.read()

def parse_entity(line):
    inner = line[line.find("QuestionEntity(")+len("QuestionEntity("):line.rfind(")")]
    parts = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', inner)
    return parts

def to_kt_string(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

new_lines = []
replaced_count = 0

for line in text.splitlines():
    if 'QuestionEntity("' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id = parts[0]
            if q_id in replacements_map:
                r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = replacements_map[q_id]
                # Format into Kotlin QuestionEntity line
                indent = line[:line.find('QuestionEntity("')]
                new_line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
                new_lines.append(new_line)
                replaced_count += 1
                continue
    new_lines.append(line)

new_text = "\n".join(new_lines)

with open(file_path, "w") as f:
    f.write(new_text)

print(f"Successfully replaced {replaced_count} question entity lines in DefaultQuestionSeeds.kt!")

