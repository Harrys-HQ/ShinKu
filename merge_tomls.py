import os, glob, re

toml_files = ['gradle/androidx.versions.toml', 'gradle/compose.versions.toml', 'gradle/kotlinx.versions.toml', 'gradle/sy.versions.toml', 'gradle/libs.versions.toml']

sections = {'[versions]': [], '[libraries]': [], '[bundles]': [], '[plugins]': []}

for file in toml_files:
    if not os.path.exists(file): continue
    current_section = None
    with open(file, 'r', encoding='utf-8') as f:
        for line in f:
            stripped = line.strip()
            if stripped in sections:
                current_section = stripped
            elif current_section and stripped:
                sections[current_section].append(line)

with open('gradle/libs.versions.toml', 'w', encoding='utf-8') as f:
    for section, lines in sections.items():
        f.write(section + chr(10))
        for line in lines:
            f.write(line)
        f.write(chr(10))

for file in toml_files[:-1]:
    if os.path.exists(file):
        os.remove(file)

kts_files = glob.glob('**/*.gradle.kts', recursive=True)

for kts in kts_files:
    with open(kts, 'r', encoding='utf-8') as f:
        content = f.read()
    
    parts = content.split(chr(34))
    for i in range(0, len(parts), 2):
        parts[i] = re.sub(r'\b(kotlinx|androidx|compose|sylibs)\.', 'libs.', parts[i])
    
    new_content = chr(34).join(parts)
    
    if 'settings.gradle.kts' in kts.replace('\\', '/'):
        new_content = re.sub(r'create\((?:chr(34)|[\'\"])(?:kotlinx|androidx|compose|sylibs)(?:chr(34)|[\'\"])\) \{[^\}]+\}\s*', '', new_content)
    
    if new_content != content:
        with open(kts, 'w', encoding='utf-8') as f:
            f.write(new_content)

print('Dependencies consolidated successfully.')