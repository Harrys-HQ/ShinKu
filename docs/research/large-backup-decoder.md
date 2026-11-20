# Experimental bounded-record backup decoding

Status: research draft, not a validated Android fix. Tracks https://github.com/Harrys-HQ/ShinKu/issues/40.

## Scope
Decode top-level protobuf records without retaining a complete decompressed byte array; preserve existing model serializers and known field numbers. Add a per-record cap and translated error. Avoid a second decode immediately after creating a backup.

## Evidence
Prior isolated JVM runs with real backup models and external stubs passed six parser tests (including an optional synthetic >150 MiB case) plus two wrapper checks. Those temporary harnesses are not part of this commit and are not a reproducible Android integration result. The checked-in parser tests are in app/src/test/java/com/shinku/reader/data/backup/BackupStreamDecoderTest.kt; the large test requires SHINKU_RUN_LARGE_BACKUP_STRESS=1.

Android compilation did not reach app compilation: the Gradle daemon disappeared during presentation-core compilation. No device reproduction, integrated test pass, heap profile or measured speedup is claimed. FlexibleAdapter remains unchanged in the dependency catalog; a temporary upstream substitution used in build attempts is not included here.

## Known limitations / review gates
- The full decoded Backup remains resident: this is not bounded-memory end-to-end restoration. Many chapter objects can still exhaust heap.
- Per-record cap (heap/16 clamped to 4–32 MiB) is heuristic and can reject otherwise valid entries; it is not an OOM guarantee.
- Recursive skipping of unknown protobuf groups needs an explicit nesting bound and adversarial tests before release.
- Audit protobuf tag bounds, duplicate-field semantics, malformed inputs and compatibility against the original decoder.
- Creator no longer reopens its just-written output for validation. Review integrity implications and replace with a bounded integrity check if needed; successful stream close is not a read-back integrity guarantee.
- English/Spanish error wording and pre-write failure guarantees require integration review.
- Require real Android build/tests, gzip/raw and chapter-heavy fixtures, and memory/time measurements on low-end hardware before merge.

## Separate feature request
Kotatsu/Usagi/Kototoro backup conversion and parser JAR execution are not implemented by this patch: https://github.com/Harrys-HQ/ShinKu/issues/41.
