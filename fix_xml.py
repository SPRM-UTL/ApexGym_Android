import os
import re

layout_dir = "app/src/main/res/layout"

for filename in os.listdir(layout_dir):
    if not filename.endswith(".xml"):
        continue
    
    filepath = os.path.join(layout_dir, filename)
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    # Fix 1: Remove extra </LinearLayout> at end of file (no indentation)
    content = re.sub(r'\n</LinearLayout>\s*$', '\n', content)
    
    # Fix 2: Remove duplicate consecutive </LinearLayout> with same indentation
    content = re.sub(r'(^\s+)</LinearLayout>\n\1</LinearLayout>', r'\1</LinearLayout>', content, flags=re.MULTILINE)
    
    # Fix 3: Fix duplicate android:orientation
    content = re.sub(r'(android:orientation="[^"]+")\s+\1', r'\1', content)
    
    # Fix 4: Fix missing = in android:text
    content = re.sub(r'android:text\s+([^="\s]+)"', r'android:text="\1"', content)
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        print(f"Fixed: {filename}")

print("Done!")
