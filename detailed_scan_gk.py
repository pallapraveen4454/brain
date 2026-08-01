import re
from collections import defaultdict

with open("app/src/main/java/com/example/data/database/DefaultQuestionSeeds.kt") as f:
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

gk_items = get_category_data("GK")
print(f"GK items: {len(gk_items)}")

by_ans = defaultdict(list)
for q in gk_items:
    q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
    by_ans[ans.lower().strip()].append(q)

print("\n--- GK Repeated Answers ---")
for ans_val, list_q in sorted(by_ans.items(), key=lambda x: len(x[1]), reverse=True):
    if len(list_q) > 1:
        print(f"Answer: '{ans_val}' ({len(list_q)} occurrences):")
        for q in list_q:
            print(f"  [{q[0]}] ({q[9]}): {q[2]}")

