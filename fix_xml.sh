#!/bin/bash
layout_dir="app/src/main/res/layout"
fixed_count=0

for filepath in "$layout_dir"/*.xml; do
    filename=$(basename "$filepath")
    
    # Read file
    content=$(cat "$filepath")
    original="$content"
    
    # Fix 1: Remove duplicate consecutive </LinearLayout> with same indentation
    # This is tricky with sed, let's use a different approach - process line by line
    # For now, let's fix the specific pattern we know: extra </LinearLayout> before MaterialCardView close
    content=$(echo "$content" | sed -E ':a;N;$!ba;s/([[:space:]]*)<\/LinearLayout>\n\1<\/LinearLayout>\n(\1[[:space:]]*)<\/com\.google\.android\.material\.card\.MaterialCardView>/\1<\/LinearLayout>\n\2<\/com.google.android.material.card.MaterialCardView>/g')
    
    # Fix 2: Fix duplicate android:orientation
    content=$(echo "$content" | sed -E 's/(android:orientation="[^"]*")\s+\1/\1/g')
    
    # Fix 3: Fix missing = in android:text
    content=$(echo "$content" | sed -E 's/android:text ([^="[:space:]]*)"/android:text="\1"/g')
    
    # Fix 4: Ensure root LinearLayout is closed
    root_opens=$(echo "$content" | grep -c '^<LinearLayout')
    root_closes=$(echo "$content" | grep -c '^</LinearLayout>$')
    if [ "$root_opens" -gt "$root_closes" ]; then
        missing=$((root_opens - root_closes))
        for i in $(seq 1 $missing); do
            content="$content
</LinearLayout>"
        done
    fi
    
    if [ "$content" != "$original" ]; then
        echo "$content" > "$filepath"
        fixed_count=$((fixed_count + 1))
        echo "Fixed: $filename"
    fi
done

echo "Total fixed: $fixed_count"
