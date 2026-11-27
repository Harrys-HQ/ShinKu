package com.shinku.reader.data.backup.restore.restorers

import com.shinku.reader.data.backup.models.BackupExtensionRepos
import com.shinku.reader.data.DatabaseHandler
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import uy.kohesive.injekt.Injekt
import uy.kohesive.injekt.api.get

class ExtensionRepoRestorer(
    private val handler: DatabaseHandler = Injekt.get(),
) {

    suspend operator fun invoke(
        backupRepo: BackupExtensionRepos,
    ) {
        if (backupRepo.baseUrl.toHttpUrlOrNull() == null) return

        handler.await(inTransaction = true) {
            val existing = extension_reposQueries.findOne(backupRepo.baseUrl).executeAsOneOrNull()
            if (existing != null) {
                check(existing.signing_key_fingerprint == backupRepo.signingKeyFingerprint) {
                    "Already exists with a different signing key fingerprint"
                }
                return@await
            }
            val sameKey = extension_reposQueries.findOneBySigningKeyFingerprint(backupRepo.signingKeyFingerprint)
                .executeAsOneOrNull()
            check(sameKey == null) { "${sameKey?.name} has the same signing key fingerprint" }
            extension_reposQueries.insert(
                backupRepo.baseUrl,
                backupRepo.name,
                backupRepo.shortName,
                backupRepo.website,
                backupRepo.signingKeyFingerprint,
            )
        }
    }
}
