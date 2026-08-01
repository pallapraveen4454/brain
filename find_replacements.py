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
sci_items = get_category_data("SCI")
his_items = get_category_data("HIS")

def audit_list(items, cat_name):
    # Find all duplicates / paraphrased / repeated answer facts
    by_ans = defaultdict(list)
    for q in items:
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        by_ans[ans.lower().strip()].append(q)

    flagged_ids = set()
    dup_details = []

    # 1. Repeated answers
    for ans_val, q_list in by_ans.items():
        if len(q_list) > 1:
            # Keep first, flag the rest
            first = q_list[0]
            for rest in q_list[1:]:
                flagged_ids.add(rest[0])
                dup_details.append((rest[0], rest[9], f"Repeated answer fact '{ans_val}' (First was {first[0]}): '{rest[2]}'"))

    # 2. Similarity / paraphrasing check
    stop_words = {'what', 'which', 'is', 'the', 'of', 'a', 'an', 'in', 'and', 'to', 'for', 'with', 'on', 'at', 'by', 'from', 'that', 'this', 'it', 'how', 'many', 'does', 'do', 'are', 'was', 'were', 'who', 'where', 'when', 'why', 'name', 'following', 'called', 'known', 'as', 'type', 'first'}
    
    def tokenize(s):
        tokens = re.findall(r'\b[a-z0-9]+\b', s.lower())
        return set(t for t in tokens if t not in stop_words)

    for i in range(len(items)):
        q1_id, _, q1_txt, _, _, _, _, _, _, diff1 = items[i]
        t1 = tokenize(q1_txt)
        if not t1: continue
        for j in range(i+1, len(items)):
            q2_id, _, q2_txt, _, _, _, _, _, _, diff2 = items[j]
            if q2_id in flagged_ids: continue
            t2 = tokenize(q2_txt)
            if not t2: continue
            
            intersection = t1.intersection(t2)
            union = t1.union(t2)
            sim = len(intersection) / len(union) if union else 0
            if sim > 0.40:
                flagged_ids.add(q2_id)
                dup_details.append((q2_id, diff2, f"High similarity ({sim:.2f}) to {q1_id}: '{q2_txt}' vs '{q1_txt}'"))

    print(f"\nFlagged in {cat_name}: {len(flagged_ids)} items")
    for f_id, diff, msg in sorted(dup_details, key=lambda x: x[0]):
        print(f"  [{f_id}] ({diff}) - {msg}")
        
    return flagged_ids

gk_flagged = audit_list(gk_items, "General Knowledge")
sci_flagged = audit_list(sci_items, "Science")
his_flagged = audit_list(his_items, "History")

