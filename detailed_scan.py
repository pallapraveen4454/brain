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

def scan_cat(cat_name, prefix):
    items = get_category_data(prefix)
    print(f"\n==========================================")
    print(f"FULL DETAILED SCAN FOR: {cat_name} ({prefix}) - {len(items)} items")
    print(f"==========================================")

    # 1. Group by correct answer
    by_ans = defaultdict(list)
    for q in items:
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        by_ans[ans.lower().strip()].append(q)

    print("\n--- Repeated Answers / Same Fact ---")
    rep_count = 0
    for ans_val, list_q in sorted(by_ans.items(), key=lambda x: len(x[1]), reverse=True):
        if len(list_q) > 1:
            rep_count += len(list_q) - 1
            print(f"\nAnswer: '{ans_val}' ({len(list_q)} occurrences):")
            for q in list_q:
                print(f"  [{q[0]}] ({q[9]}): {q[2]}")

    # 2. Check for high semantic similarity in questions
    stop_words = {'what', 'which', 'is', 'the', 'of', 'a', 'an', 'in', 'and', 'to', 'for', 'with', 'on', 'at', 'by', 'from', 'that', 'this', 'it', 'how', 'many', 'does', 'do', 'are', 'was', 'were', 'who', 'where', 'when', 'why', 'name', 'following', 'called', 'known', 'as', 'type', 'first'}
    
    def tokenize(s):
        tokens = re.findall(r'\b[a-z0-9]+\b', s.lower())
        return set(t for t in tokens if t not in stop_words)

    print("\n--- Question Similarity Pairs (> 0.40 Jaccard) ---")
    sim_pairs = []
    for i in range(len(items)):
        q1_id, _, q1_txt, _, _, _, _, _, _, diff1 = items[i]
        t1 = tokenize(q1_txt)
        if not t1: continue
        for j in range(i+1, len(items)):
            q2_id, _, q2_txt, _, _, _, _, _, _, diff2 = items[j]
            t2 = tokenize(q2_txt)
            if not t2: continue
            
            intersection = t1.intersection(t2)
            union = t1.union(t2)
            sim = len(intersection) / len(union)
            if sim > 0.40:
                sim_pairs.append((sim, q1_id, diff1, q1_txt, q2_id, diff2, q2_txt))
                
    sim_pairs.sort(reverse=True, key=lambda x: x[0])
    for sim, q1_id, diff1, q1_txt, q2_id, diff2, q2_txt in sim_pairs:
        print(f"  Sim {sim:.2f}: [{q1_id}] ({diff1}) '{q1_txt}' <===> [{q2_id}] ({diff2}) '{q2_txt}'")

scan_cat("General Knowledge", "GK")
scan_cat("Science", "SCI")
scan_cat("History", "HIS")

