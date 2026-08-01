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

fix = {
    "HIS162": ("HIS162", "history", "Which landmark 1689 English constitutional act set limitative boundaries on royal power and established parliamentary sovereignty?", "Bill of Rights 1689", "Petition of Right", "Act of Settlement 1701", "Constitutions of Clarendon", "Bill of Rights 1689", "The English Bill of Rights 1689 limited royal authority and established statutory parliamentary rights.", "Medium"),
}

new_lines = []
for line in text.splitlines():
    if 'QuestionEntity("' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id = parts[0]
            if q_id in fix:
                r_id, r_cat, r_q, r_a, r_b, r_c, r_d, r_ans, r_exp, r_diff = fix[q_id]
                indent = line[:line.find('QuestionEntity("')]
                line = f'{indent}QuestionEntity("{r_id}", "{r_cat}", "{to_kt_string(r_q)}", "{to_kt_string(r_a)}", "{to_kt_string(r_b)}", "{to_kt_string(r_c)}", "{to_kt_string(r_d)}", "{to_kt_string(r_ans)}", "{to_kt_string(r_exp)}", "{r_diff}"),'
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write("\n".join(new_lines))

print("Applied fix for HIS162 Bill of Rights!")

