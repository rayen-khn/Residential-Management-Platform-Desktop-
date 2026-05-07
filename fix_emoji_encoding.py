#!/usr/bin/env python3
import os

os.chdir(r"C:\Users\amine\OneDrive\Desktop\ESPRIT-PIDEV-JAVA-3A53-2526-Syndicati")

# Files to fix
files_to_fix = [
    "src/main/java/com/syndicati/views/login/LoginView.java",
    "src/main/java/com/syndicati/views/frontend/login/LoginView.java"
]

# The corrupted string → correct emoji mappings
replacements = {
    'ðŸ"': '🔐',  # lock
    'ðŸ™': '🙏',  # pray hands
    'ðŸ"·': '📷',  # camera
    'âœ"': '✅',  # checkmark
    'èŠ•': '💬',  # speech bubble
    'ðŸ"„': '🔔',  # bell
    'ðŸ"': '🎫',  # ticket
    'âœ•': '✓',   # check
    'âŒ„': '➕'   # plus
}

for file_path in files_to_fix:
    if os.path.exists(file_path):
        # Try to read with different encodings
        content = None
        used_encoding = None
        
        for encoding in ['utf-8', 'iso-8859-1', 'cp1252', 'latin-1']:
            try:
                with open(file_path, 'r', encoding=encoding) as f:
                    content = f.read()
                used_encoding = encoding
                print(f"✓ Successfully read {file_path} with {encoding}")
                break
            except Exception:
                continue
        
        if content is None:
            print(f"✗ Could not read {file_path} with any encoding")
            continue
        
        # Apply replacements
        original_content = content
        for old, new in replacements.items():
            if old in content:
                count = content.count(old)
                print(f"  Found {count} occurrence(s) of {repr(old)}, replacing with {new}")
                content = content.replace(old, new)
        
        # Write back as UTF-8
        if content != original_content:
            with open(file_path, 'w', encoding='utf-8') as f:
                f.write(content)
            print(f"✓ Fixed {file_path}\n")
        else:
            print(f"ℹ No changes needed for {file_path}\n")
    else:
        print(f"✗ File not found: {file_path}")

print("Done!")
