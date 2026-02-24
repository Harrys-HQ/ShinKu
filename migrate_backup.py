import os
import gzip
import re

# This script performs a low-level byte-string replacement in the Protobuf backup
# to fix the package names without requiring the full app classpath.

def migrate_backup(input_path):
    output_path = input_path.replace(".tachibk", "_shinku_migrated.tachibk")
    
    print(f"Reading {input_path}...")
    with open(input_path, 'rb') as f:
        data = f.read()

    # Backups are gzipped
    is_gzipped = data.startswith(b'\x1f\x8b')
    if is_gzipped:
        print("Detected GZip compression, decompressing...")
        content = gzip.decompress(data)
    else:
        content = data

    # Package patterns to migrate
    # We replace the binary representation of the class names used as discriminators
    replacements = [
        (b'eu.kanade.tachiyomi', b'com.shinku.reader'),
        (b'eu.kanade.shinku', b'com.shinku.reader'),
        (b'com.shinku.reader.mihon', b'com.shinku.reader'),
    ]

    new_content = content
    for old, new in replacements:
        if old in new_content:
            print(f"Replacing {old.decode()} with {new.decode()}...")
            new_content = new_content.replace(old, new)

    if new_content == content:
        print("No matches found. The backup might already be using the new package names or a different structure.")
    
    if is_gzipped:
        print("Re-compressing...")
        final_data = gzip.compress(new_content)
    else:
        final_data = new_content

    with open(output_path, 'wb') as f:
        f.write(final_data)
    
    print(f"Migration complete! Saved to: {output_path}")
    print("You can now try restoring this file in the new ShinKu app.")

if __name__ == "__main__":
    import sys
    # Use the filename provided by the user
    target = "eu.kanade.shinku.debug_2026-02-23_13-14.tachibk"
    if os.path.exists(target):
        migrate_backup(target)
    else:
        print(f"Error: Could not find {target} in the current directory.")
        print("Please place the backup file in the same folder as this script.")
