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

def audit_category(cat_name, prefix):
    items = get_category_data(prefix)
    print(f"\n==========================================")
    print(f"VERIFICATION AUDIT FOR: {cat_name} ({prefix})")
    print(f"==========================================")

    assert len(items) == 300, f"Expected 300 items, got {len(items)}"
    
    easy_cnt = 0
    med_cnt = 0
    hard_cnt = 0
    
    q_texts = set()
    dup_q = []
    by_ans = defaultdict(list)
    opt_errors = []
    ans_errors = []

    for i, q in enumerate(items, 1):
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        
        expected_id = f"{prefix}{i:03d}"
        assert q_id == expected_id, f"ID mismatch at line {i}: {q_id} vs {expected_id}"
        
        # Difficulty
        if diff == "Easy": easy_cnt += 1
        elif diff == "Medium": med_cnt += 1
        elif diff == "Hard": hard_cnt += 1
        else: raise ValueError(f"Unknown diff {diff} at {q_id}")

        # Unique options
        opts = [opA, opB, opC, opD]
        if len(set(opts)) != 4:
            opt_errors.append((q_id, opts))

        # Answer in options
        if ans not in opts:
            ans_errors.append((q_id, ans, opts))

        # Duplicate question text
        q_clean = re.sub(r'[^a-z0-9]', '', question.lower().strip())
        if q_clean in q_texts:
            dup_q.append((q_id, question))
        q_texts.add(q_clean)

        # Repeated answer fact
        ans_norm = re.sub(r'[^a-z0-9]', '', ans.lower().strip())
        by_ans[ans_norm].append((q_id, question, diff))

    print(f"✅ Total Questions: {len(items)} ({prefix}001 - {prefix}300)")
    print(f"✅ Difficulty Distribution:")
    print(f"   • Easy: {easy_cnt} (Expected 120)")
    print(f"   • Medium: {med_cnt} (Expected 120)")
    print(f"   • Hard: {hard_cnt} (Expected 60)")
    
    assert easy_cnt == 120, f"Expected 120 easy, got {easy_cnt}"
    assert med_cnt == 120, f"Expected 120 medium, got {med_cnt}"
    assert hard_cnt == 60, f"Expected 60 hard, got {hard_cnt}"

    assert len(opt_errors) == 0, f"Option uniqueness errors found: {opt_errors}"
    print(f"✅ Four Unique Options Per Question: PASS")

    assert len(ans_errors) == 0, f"Answer missing from options errors found: {ans_errors}"
    print(f"✅ Correct Answer Verified in Options: PASS")

    assert len(dup_q) == 0, f"Duplicate question text found: {dup_q}"
    print(f"✅ Zero Duplicate Question Texts: PASS")

    # Check repeated answers
    rep_ans = [(k, v) for k, v in by_ans.items() if len(v) > 1]
    if rep_ans:
        print(f"⚠️ Repeated answer facts found ({len(rep_ans)}):")
        for k, v in rep_ans:
            print(f"  Ans '{k}': {[x[0] for x in v]}")
    else:
        print(f"✅ Zero Repeated Answer Facts: PASS")

    # Check question similarity (> 0.40)
    stop_words = {'what', 'which', 'is', 'the', 'of', 'a', 'an', 'in', 'and', 'to', 'for', 'with', 'on', 'at', 'by', 'from', 'that', 'this', 'it', 'how', 'many', 'does', 'do', 'are', 'was', 'were', 'who', 'where', 'when', 'why', 'name', 'following', 'called', 'known', 'as', 'type', 'first', 'used', 'city', 'country', 'capital', 'element', 'chemical', 'symbol'}
    
    def tokenize(s):
        tokens = re.findall(r'\b[a-z0-9]+\b', s.lower())
        return set(t for t in tokens if t not in stop_words)

    sim_pairs = []
    for i in range(len(items)):
        q1_id, _, q1_txt, _, _, _, _, _, _, _ = items[i]
        t1 = tokenize(q1_txt)
        if not t1: continue
        for j in range(i+1, len(items)):
            q2_id, _, q2_txt, _, _, _, _, _, _, _ = items[j]
            t2 = tokenize(q2_txt)
            if not t2: continue
            
            intersection = t1.intersection(t2)
            union = t1.union(t2)
            sim = len(intersection) / len(union) if union else 0
            if sim > 0.40:
                sim_pairs.append((sim, q1_id, q1_txt, q2_id, q2_txt))

    if sim_pairs:
        print(f"⚠️ High similarity pairs found ({len(sim_pairs)}):")
        for sim, q1_id, q1_txt, q2_id, q2_txt in sim_pairs:
            print(f"  Sim {sim:.2f}: [{q1_id}] '{q1_txt}' <===> [{q2_id}] '{q2_txt}'")
    else:
        print(f"✅ Zero Paraphrased / High-Similarity Questions: PASS")

    return len(rep_ans) == 0 and len(sim_pairs) == 0

gk_ok = audit_category("General Knowledge", "GK")
sci_ok = audit_category("Science", "SCI")
his_ok = audit_category("History", "HIS")

if gk_ok and sci_ok and his_ok:
    print("\n🎉 ALL THREE CATEGORIES (GK, Science, History) PASSED 100% PERFECT AUDIT!")
else:
    print("\n❌ SOME CATEGORIES NEED FURTHER REFINEMENT")

