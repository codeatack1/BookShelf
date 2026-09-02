package com.bookshelf.domain.extension.interactor

import android.content.pm.PackageInfo
import androidx.core.content.pm.PackageInfoCompat
import com.bookshelf.core.common.preference.getAndSet
import com.bookshelf.domain.extension.repository.ExtensionStoreRepository
import com.bookshelf.domain.source.service.SourcePreferences
import dev.zacsweers.metro.Inject

@Inject
class TrustExtension(
    private val repository: ExtensionStoreRepository,
    private val preferences: SourcePreferences,
) {

    suspend fun isTrusted(pkgInfo: PackageInfo, fingerprints: List<String>): Boolean {
        val trustedFingerprints = repository.getAll().map { it.signingKey }.toHashSet()
        val key = "${pkgInfo.packageName}:${PackageInfoCompat.getLongVersionCode(pkgInfo)}:${fingerprints.last()}"
        return trustedFingerprints.any { fingerprints.contains(it) } || key in preferences.trustedExtensions.get()
    }

    fun trust(pkgName: String, versionCode: Long, signatureHash: String) {
        preferences.trustedExtensions.getAndSet { exts ->
            // Remove previously trusted versions
            val removed = exts.filterNot { it.startsWith("$pkgName:") }.toMutableSet()

            removed.also { it += "$pkgName:$versionCode:$signatureHash" }
        }
    }

    fun revokeAll() {
        preferences.trustedExtensions.delete()
    }
}
