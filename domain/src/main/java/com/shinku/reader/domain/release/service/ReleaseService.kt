package com.shinku.reader.domain.release.service

import com.shinku.reader.domain.release.model.Release

interface ReleaseService {

    suspend fun latest(repository: String): Release
}
