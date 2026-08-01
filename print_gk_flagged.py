import re
from find_replacements import gk_items, gk_flagged

for item in gk_items:
    if item[0] in gk_flagged:
        print(f"[{item[0]}] ({item[9]}): {item[2]} -> Ans: {item[7]}")
