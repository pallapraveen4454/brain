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

def analyze_category(cat_name, items):
    print(f"\n==========================================")
    print(f"DEEP AUDIT FOR: {cat_name} ({len(items)} questions)")
    print(f"==========================================")
    
    # Check option uniqueness and answer validity
    invalid_opts = []
    invalid_ans = []
    for q in items:
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        opts = [opA, opB, opC, opD]
        if len(set(opts)) != 4:
            invalid_opts.append((q_id, opts))
        if ans not in opts:
            invalid_ans.append((q_id, ans, opts))
            
    print(f"Option Uniqueness Failures: {len(invalid_opts)}")
    if invalid_opts:
        for x in invalid_opts:
            print(f"  {x[0]}: {x[1]}")
    print(f"Answer Not In Options Failures: {len(invalid_ans)}")
    if invalid_ans:
        for x in invalid_ans:
            print(f"  {x[0]}: ans '{x[1]}' in {x[2]}")

    # Check for repeated correct answers or repeated core subject in questions
    # Group by correct answer
    by_ans = defaultdict(list)
    for q in items:
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        by_ans[ans.lower().strip()].append((q_id, question, diff))
        
    print(f"\nAnswers appearing 2 or more times as correct answer:")
    repeated_ans_count = 0
    for ans_val, list_q in sorted(by_ans.items(), key=lambda x: len(x[1]), reverse=True):
        if len(list_q) > 1:
            repeated_ans_count += 1
            print(f"  Answer '{ans_val}' ({len(list_q)} times):")
            for q_id, q_txt, d in list_q:
                print(f"    - {q_id} [{d}]: {q_txt}")
                
    # Check word similarity / jaccard similarity between questions
    print(f"\nHigh similarity question pairs (Jaccard > 0.45):")
    stop_words = {'what', 'which', 'is', 'the', 'of', 'a', 'an', 'in', 'and', 'to', 'for', 'with', 'on', 'at', 'by', 'from', 'that', 'this', 'it', 'how', 'many', 'does', 'do', 'are', 'was', 'were', 'who', 'where', 'when', 'why', 'name', 'following', 'which', 'called', 'known', 'as'}
    
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
            sim = len(intersection) / len(union)
            if sim > 0.45:
                sim_pairs.append((sim, q1_id, q1_txt, q2_id, q2_txt))
                
    sim_pairs.sort(reverse=True, key=lambda x: x[0])
    for sim, q1_id, q1_txt, q2_id, q2_txt in sim_pairs:
        print(f"  Sim {sim:.2f}: [{q1_id}] '{q1_txt}' <===> [{q2_id}] '{q2_txt}'")

analyze_category("General Knowledge", get_category_data("GK"))
analyze_category("Science", get_category_data("SCI"))
analyze_category("History", get_category_data("HIS"))

