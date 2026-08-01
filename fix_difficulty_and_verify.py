import re

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
for line in text.splitlines():
    if 'QuestionEntity("' in line:
        parts = parse_entity(line)
        if len(parts) == 10:
            q_id, cat, q_text, opA, opB, opC, opD, ans, exp, diff = parts
            
            # Determine correct difficulty by ID index
            m = re.match(r'([A-Z]+)(\d+)', q_id)
            if m:
                prefix, num_str = m.groups()
                num = int(num_str)
                if num <= 120:
                    correct_diff = "Easy"
                elif num <= 240:
                    correct_diff = "Medium"
                else:
                    correct_diff = "Hard"
                
                if diff != correct_diff:
                    indent = line[:line.find('QuestionEntity("')]
                    line = f'{indent}QuestionEntity("{q_id}", "{cat}", "{to_kt_string(q_text)}", "{to_kt_string(opA)}", "{to_kt_string(opB)}", "{to_kt_string(opC)}", "{to_kt_string(opD)}", "{to_kt_string(ans)}", "{to_kt_string(exp)}", "{correct_diff}"),'
    new_lines.append(line)

with open(file_path, "w") as f:
    f.write("\n".join(new_lines))

print("Difficulty field alignment complete!")

