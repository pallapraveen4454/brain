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

print(f"Initial counts -> GK: {len(gk_items)}, SCI: {len(sci_items)}, HIS: {len(his_items)}")

# Let's write helper to find duplicates and concept overlaps
def find_all_flags(items):
    flagged = set()
    reasons = {}
    
    # 1. Exact or normalized duplicate text
    norm_texts = {}
    for q in items:
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        q_clean = re.sub(r'[^a-z0-9]', '', question.lower().strip())
        if q_clean in norm_texts:
            flagged.add(q_id)
            reasons[q_id] = f"Exact duplicate of {norm_texts[q_clean]}"
        else:
            norm_texts[q_clean] = q_id

    # 2. Repeated answer fact (case-insensitive normalized)
    ans_map = defaultdict(list)
    for q in items:
        if q[0] in flagged: continue
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        ans_norm = re.sub(r'[^a-z0-9]', '', ans.lower().strip())
        ans_map[ans_norm].append(q_id)

    for ans_norm, q_ids in ans_map.items():
        if len(q_ids) > 1:
            for extra_id in q_ids[1:]:
                flagged.add(extra_id)
                reasons[extra_id] = f"Repeated answer fact '{ans_norm}' (kept {q_ids[0]})"

    # 3. High Jaccard similarity (> 0.38) or shared unique keywords
    stop_words = {'what', 'which', 'is', 'the', 'of', 'a', 'an', 'in', 'and', 'to', 'for', 'with', 'on', 'at', 'by', 'from', 'that', 'this', 'it', 'how', 'many', 'does', 'do', 'are', 'was', 'were', 'who', 'where', 'when', 'why', 'name', 'following', 'called', 'known', 'as', 'type', 'first', 'used', 'city', 'country', 'capital', 'element', 'chemical', 'symbol'}
    
    def tokenize(s):
        tokens = re.findall(r'\b[a-z0-9]+\b', s.lower())
        return set(t for t in tokens if t not in stop_words)

    for i in range(len(items)):
        q1_id = items[i][0]
        if q1_id in flagged: continue
        t1 = tokenize(items[i][2])
        if not t1: continue
        for j in range(i+1, len(items)):
            q2_id = items[j][0]
            if q2_id in flagged: continue
            t2 = tokenize(items[j][2])
            if not t2: continue
            
            intersection = t1.intersection(t2)
            union = t1.union(t2)
            sim = len(intersection) / len(union) if union else 0
            if sim > 0.38:
                flagged.add(q2_id)
                reasons[q2_id] = f"High similarity ({sim:.2f}) with {q1_id}"

    return flagged, reasons

gk_flagged, gk_reasons = find_all_flags(gk_items)
sci_flagged, sci_reasons = find_all_flags(sci_items)
his_flagged, his_reasons = find_all_flags(his_items)

print(f"\nGeneral Knowledge Flagged: {len(gk_flagged)}")
for q_id in sorted(gk_flagged):
    print(f"  {q_id}: {gk_reasons[q_id]}")

print(f"\nScience Flagged: {len(sci_flagged)}")
for q_id in sorted(sci_flagged):
    print(f"  {q_id}: {sci_reasons[q_id]}")

print(f"\nHistory Flagged: {len(his_flagged)}")
for q_id in sorted(his_flagged):
    print(f"  {q_id}: {his_reasons[q_id]}")

