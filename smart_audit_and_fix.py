import re
from collections import defaultdict

file_path = "app/src/main/java/com/example/data/database/DefaultQuestionSeeds.kt"
with open(file_path) as f:
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

def to_kt_string(s):
    return s.replace('\\', '\\\\').replace('"', '\\"')

# Comprehensive stop-words to isolate the actual SUBJECT of the question
stop_words = {
    'what', 'which', 'is', 'the', 'of', 'a', 'an', 'in', 'and', 'to', 'for', 'with', 'on', 'at', 'by', 'from',
    'that', 'this', 'it', 'how', 'many', 'does', 'do', 'are', 'was', 'were', 'who', 'where', 'when', 'why',
    'name', 'following', 'called', 'known', 'as', 'type', 'first', 'used', 'city', 'country', 'capital',
    'element', 'chemical', 'symbol', 'official', 'currency', 'unit', 'located', 'world', 'earth', 'human',
    'body', 'system', 'state', 'national', 'largest', 'smallest', 'highest', 'deepest', 'longest', 'shortest'
}

def extract_subject_tokens(s):
    tokens = re.findall(r'\b[a-z0-9]+\b', s.lower())
    return set(t for t in tokens if t not in stop_words)

# Fresh replacement pool items that introduce unique answer facts and distinct subjects
gk_replacements = [
    ("GK014", "gk", "Which famous English playwright authored the tragedy 'Macbeth'?", "William Shakespeare", "Christopher Marlowe", "Ben Jonson", "John Milton", "William Shakespeare", "Shakespeare wrote Macbeth around 1606 during the reign of James I.", "Easy"),
    ("GK027", "gk", "What is the tallest living terrestrial mammal on Earth?", "Giraffe", "Elephant", "Rhinoceros", "Hippopotamus", "Giraffe", "Giraffes are the tallest land mammals, reaching up to 5.8 meters in height.", "Easy"),
    ("GK039", "gk", "Which flightless sea bird is native to the freezing climate of Antarctica?", "Penguin", "Ostrich", "Kiwi", "Albatross", "Penguin", "Penguins are aquatic flightless birds adapted to southern polar regions.", "Easy"),
    ("GK043", "gk", "Which fruit uniquely bears its seeds on the outside of its skin?", "Strawberry", "Raspberry", "Blueberry", "Blackberry", "Strawberry", "Strawberries are unique in bearing achenes (seeds) on their exterior skin.", "Easy"),
    ("GK045", "gk", "What is the official capital city of Canada?", "Ottawa", "Toronto", "Vancouver", "Montreal", "Ottawa", "Queen Victoria selected Ottawa as the capital of Canada in 1857.", "Easy"),
    ("GK058", "gk", "Which standard musical instrument features 88 keys on its keyboard?", "Piano", "Harpsichord", "Organ", "Accordion", "Piano", "A standard acoustic piano features 88 keys (52 white and 36 black).", "Easy"),
    ("GK060", "gk", "Which ocean is the largest and deepest of Earth's five oceanic divisions?", "Pacific Ocean", "Atlantic Ocean", "Indian Ocean", "Arctic Ocean", "Pacific Ocean", "The Pacific Ocean covers over 30% of Earth's surface and contains Mariana Trench.", "Easy"),
    ("GK063", "gk", "Which primary color when mixed with red produces orange pigment?", "Yellow", "Blue", "Green", "White", "Yellow", "Combining red and yellow yields orange in primary color mixing.", "Easy"),
    ("GK066", "gk", "Which insect is famous for producing natural honey and constructing wax honeycombs?", "Honeybee", "Ladybug", "Dragonfly", "Butterfly", "Honeybee", "Honeybees convert flower nectar into honey stored in wax honeycombs.", "Easy"),
    ("GK072", "gk", "Which iconic bell tower in Tuscany, Italy is famous for its unintended tilt?", "Leaning Tower of Pisa", "Eiffel Tower", "Big Ben", "St Mark's Campanile", "Leaning Tower of Pisa", "The campanile of Pisa cathedral began leaning during 12th-century construction.", "Easy"),
]

def perform_smart_clean(prefix):
    items = get_category_data(prefix)
    
    seen_ans = set()
    seen_q_clean = set()
    
    flagged = []
    
    for q in items:
        q_id, cat, question, opA, opB, opC, opD, ans, exp, diff = q
        
        ans_norm = re.sub(r'[^a-z0-9]', '', ans.lower().strip())
        q_clean = re.sub(r'[^a-z0-9]', '', question.lower().strip())
        
        if ans_norm in seen_ans or q_clean in seen_q_clean:
            flagged.append(q)
        else:
            seen_ans.add(ans_norm)
            seen_q_clean.add(q_clean)

    print(f"[{prefix}] Total questions: {len(items)}, Unique facts: {len(seen_ans)}, Flagged duplicates: {len(flagged)}")
    for f in flagged:
        print(f"  Flagged [{f[0]}]: '{f[2]}' -> Ans: '{f[7]}'")

perform_smart_clean("GK")
perform_smart_clean("SCI")
perform_smart_clean("HIS")

