import os
import shutil
import re

def revert_source_api():
    src_root = 'source-api/src/commonMain/kotlin/com/shinku/reader/source'
    dest_root = 'source-api/src/commonMain/kotlin/eu/kanade/tachiyomi/source'
    
    if os.path.exists(src_root):
        os.makedirs(os.path.dirname(dest_root), exist_ok=True)
        if os.path.exists(dest_root):
            shutil.rmtree(dest_root)
        shutil.move(src_root, dest_root)
        print(f"Moved {src_root} to {dest_root}")

    src_net = 'core/common/src/main/kotlin/com/shinku/reader/network'
    dest_net = 'core/common/src/main/kotlin/eu/kanade/tachiyomi/network'
    
    if os.path.exists(src_net):
        os.makedirs(os.path.dirname(dest_net), exist_ok=True)
        if os.path.exists(dest_net):
            shutil.rmtree(dest_net)
        shutil.move(src_net, dest_net)
        print(f"Moved {src_net} to {dest_net}")

    extensions = ['.kt', '.java', '.xml', '.aidl', '.gradle.kts']
    replacements = [
        ('com.shinku.reader.source', 'eu.kanade.tachiyomi.source'),
        ('com.shinku.reader.network', 'eu.kanade.tachiyomi.network'),
    ]

    for root, dirs, files in os.walk('.'):
        if 'build' in root or '.git' in root:
            continue
        for file in files:
            if any(file.endswith(ext) for ext in extensions):
                path = os.path.join(root, file)
                try:
                    with open(path, 'r', encoding='utf-8') as f:
                        content = f.read()
                except: continue
                
                new_content = content
                for old, new in replacements:
                    new_content = new_content.replace(old, new)
                
                if new_content != content:
                    with open(path, 'w', encoding='utf-8') as f:
                        f.write(new_content)
                    print(f"Updated {path}")

if __name__ == "__main__":
    revert_source_api()
