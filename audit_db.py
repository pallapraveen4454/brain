import re

with open("app/src/main/java/com/example/data/database/DefaultQuestionSeeds.kt") as f:
    text = f.read()

def extract_category_block(cat_prefix):
    # Find all QuestionEntity lines for given category
    lines = []
    for line in text.splitlines():
        if f'QuestionEntity("{cat_prefix}' in line:
            lines.append(line.strip())
    return lines

gk_lines = extract_category_block("GK")
sci_lines = extract_category_block("SCI")
his_lines = extract_category_block("HIS")

print(f"Found GK questions: {len(gk_lines)}")
print(f"Found SCI questions: {len(sci_lines)}")
print(f"Found HIS questions: {len(his_lines)}")

def parse_entity(line):
    inner = line[line.find("QuestionEntity(")+len("QuestionEntity("):line.rfind(")")]
    parts = re.findall(r'"([^"\\]*(?:\\.[^"\\]*)*)"', inner)
    return parts

def audit_category(cat_name, lines, expected_prefix):
    print(f"\n--- Auditing {cat_name} ---")
    parsed = []
    for i, line in enumerate(lines, 1):
        p = parse_entity(line)
        if len(p) != 10:
            print(f"Parse error on line {i}: {line}")
            continue
        parsed.append(p)
    
    # 1. Check ID range and count
    print(f"Total parsed: {len(parsed)}")
    
    # Check duplicates in question text
    seen_texts = {}
    dups = []
    for p in parsed:
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = p
        q_clean = re.sub(r'[^a-z0-9]', '', question.lower().strip())
        if q_clean in seen_texts:
            dups.append((q_id, seen_texts[q_clean], question))
        else:
            seen_texts[q_clean] = q_id
            
    print(f"Exact/Normalized duplicate question texts found: {len(dups)}")
    for d in dups:
        print(f"  Duplicate: {d[0]} matches {d[1]}: '{d[2]}'")

audit_category("General Knowledge", gk_lines, "GK")
audit_category("Science", sci_lines, "SCI")
audit_category("History", his_lines, "HIS")

