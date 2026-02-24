import os
import re

def fix_backup_serialization():
    root_dirs = [
        'app/src/main/java/com/shinku/reader/data/backup/models',
        'source-api/src/commonMain/kotlin/com/shinku/reader/source/model'
    ]
    
    for root_dir in root_dirs:
        if not os.path.exists(root_dir):
            continue
        for root, dirs, files in os.walk(root_dir):
            for file in files:
                if file.endswith('.kt'):
                    path = os.path.join(root, file)
                    with open(path, 'r', encoding='utf-8') as f:
                        content = f.read()
                    
                    if '@Serializable' not in content:
                        continue
                        
                    pkg_match = re.search(r'package (com\.shinku\.reader\.[^\s;]*)', content)
                    if not pkg_match:
                        continue
                    pkg = pkg_match.group(1)
                    old_pkg = pkg.replace('com.shinku.reader', 'com.shinku.reader.mihon').replace('..', '.')
                    
                    if 'import kotlinx.serialization.SerialName' not in content:
                        content = content.replace('import kotlinx.serialization.Serializable', 
                                                 'import kotlinx.serialization.SerialName\nimport kotlinx.serialization.Serializable')

                    def add_sn(match):
                        modifiers = match.group(1) or ""
                        kind = match.group(2)
                        name = match.group(3)
                        
                        start = match.start()
                        context_before = content[max(0, start-100):start]
                        if '@SerialName' in context_before:
                            return match.group(0)
                        
                        if name == "UpdateStrategy":
                            return f'@SerialName("com.shinku.reader.mihon.source.model.UpdateStrategy")\n{modifiers}{kind} {name}'
                        
                        return f'@SerialName("{old_pkg}.{name}")\n{modifiers}{kind} {name}'

                    new_content = re.sub(r'((?:data |sealed |enum )?)(class|object) (\w+)', add_sn, content)
                    
                    if new_content != content:
                        with open(path, 'w', encoding='utf-8') as f:
                            f.write(new_content)
                        print(f"Updated {path}")

if __name__ == "__main__":
    fix_backup_serialization()
