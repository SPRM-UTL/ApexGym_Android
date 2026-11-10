import os
import re

layout_dir = "app/src/main/res/layout"

def fix_file(filepath):
    with open(filepath, 'r', encoding='utf-8') as f:
        content = f.read()
    
    original = content
    
    # Fix 1: Remove duplicate consecutive </LinearLayout> with same indentation
    content = re.sub(r'(^\s+)</LinearLayout>\n\1</LinearLayout>', r'\1</LinearLayout>', content, flags=re.MULTILINE)
    
    # Fix 2: Fix pattern where MaterialCardView has extra closing LinearLayout before its closing tag
    # Pattern: </LinearLayout>\n        </LinearLayout>\n        </com.google.android.material.card.MaterialCardView>
    # Should be: </LinearLayout>\n        </com.google.android.material.card.MaterialCardView>
    content = re.sub(
        r'(^\s+)</LinearLayout>\n(\s+)</LinearLayout>\n(\s+)</com\.google\.android\.material\.card\.MaterialCardView>',
        r'\1</LinearLayout>\n\3</com.google.android.material.card.MaterialCardView>',
        content,
        flags=re.MULTILINE
    )
    
    # Fix 3: Fix duplicate android:orientation
    content = re.sub(r'(android:orientation="[^"]+")\s+\1', r'\1', content)
    
    # Fix 4: Fix missing = in android:text
    content = re.sub(r'android:text\s+([^="\s]+)"', r'android:text="\1"', content)
    
    # Fix 5: Ensure root LinearLayout is closed (count opens vs closes at root level)
    root_opens = len(re.findall(r'^<LinearLayout\b', content, re.MULTILINE))
    root_closes = len(re.findall(r'^</LinearLayout>$', content, re.MULTILINE))
    if root_opens > root_closes:
        missing = root_opens - root_closes
        content = content.rstrip() + '\n' + '\n'.join(['</LinearLayout>'] * missing) + '\n'
    
    if content != original:
        with open(filepath, 'w', encoding='utf-8') as f:
            f.write(content)
        return True
    return False

fixed_count = 0
for filename in os.listdir(layout_dir):
    if filename.endswith(".xml"):
        filepath = os.path.join(layout_dir, filename)
        if fix_file(filepath):
            fixed_count += 1
            print(f"Fixed: {filename}")

print(f"Total fixed: {fixed_count}")
