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

by_ans = defaultdict(list)
for q in gk_items:
    q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
    ans_norm = re.sub(r'[^a-z0-9]', '', ans.lower().strip())
    by_ans[ans_norm].append(q)

gk_second_flags = []
for ans_norm, list_q in by_ans.items():
    if len(list_q) > 1:
        print(f"\nAnswer '{ans_norm}':")
        for q in list_q:
            print(f"  [{q[0]}] ({q[9]}): {q[2]}")
        # Keep first, flag the rest
        for extra in list_q[1:]:
            gk_second_flags.append(extra)

print(f"\nRemaining GK items to replace: {len(gk_second_flags)}")
for q in gk_second_flags:
    print(f"Replace [{q[0]}] ({q[9]}): {q[2]} -> Ans: {q[7]}")

