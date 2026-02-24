import os

def fix_sqldelight():
    root_dir = 'data/src/main/sqldelight'
    replacements = [
        ('com.shinku.reader.source', 'eu.kanade.tachiyomi.source'),
    ]

    for root, dirs, files in os.walk(root_dir):
        for file in files:
            if file.endswith('.sq') or file.endswith('.sqm'):
                path = os.path.join(root, file)
                with open(path, 'r', encoding='utf-8') as f:
                    content = f.read()
                
                new_content = content
                for old, new in replacements:
                    new_content = new_content.replace(old, new)
                
                if new_content != content:
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated SQLDelight file: {path}")

if __name__ == "__main__":
    fix_sqldelight()
