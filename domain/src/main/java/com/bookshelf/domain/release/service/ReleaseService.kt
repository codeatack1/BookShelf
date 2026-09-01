package com.bookshelf.domain.release.service

import com.bookshelf.domain.release.interactor.GetApplicationRelease
import com.bookshelf.domain.release.model.Release

interface ReleaseService {

    suspend fun latest(arguments: GetApplicationRelease.Arguments): Release?
}
