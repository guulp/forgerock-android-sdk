/*
 * Copyright (c) 2023 - 2024 ForgeRock. All rights reserved.
 *
 *  This software may be modified and distributed under the terms
 *  of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.webauthn

import android.content.Context
import org.forgerock.android.auth.RemoteWebAuthnRepository
import org.forgerock.android.auth.WebAuthnDataRepository
import org.forgerock.android.auth.exception.ApiException
import java.io.IOException

interface WebAuthnRepository {

    @Throws(ApiException::class, IOException::class)
    suspend fun delete(publicKeyCredentialSource: PublicKeyCredentialSource)

}

/**
 * Manage [PublicKeyCredentialSource] that generated by the SDK.
 * The [PublicKeyCredentialSource] only contains the reference to the actual key,
 * deleting the [PublicKeyCredentialSource] does not delete the actual key
 */
class FRWebAuthn @JvmOverloads constructor(private val context: Context,
                 private val webAuthnDataRepository: WebAuthnDataRepository =
                     WebAuthnDataRepository.WebAuthnDataRepositoryBuilder().context(context)
                         .build(),
                 private val remoteWebAuthnRepository: WebAuthnRepository? = RemoteWebAuthnRepository()) {

    /**
     * Delete the [PublicKeyCredentialSource] by Relying Party Id
     * @param rpId The relying party id to lookup from the internal storage
     */
    fun deleteCredentials(rpId: String) {
        webAuthnDataRepository.delete(rpId)
    }

    /**
     * Delete the provide [PublicKeyCredentialSource], the [PublicKeyCredentialSource.id] will
     * be used as the key to lookup from internal storage
     * @param publicKeyCredentialSource The [PublicKeyCredentialSource] to be deleted
     */
    fun deleteCredentials(publicKeyCredentialSource: PublicKeyCredentialSource) {
        webAuthnDataRepository.delete(publicKeyCredentialSource)
    }

    /**
     * Delete the provide [PublicKeyCredentialSource] from local storage and also remotely from Server.
     * By default, if failed to delete from server, local storage will not be deleted,
     * by providing [forceDelete] to true, it will also delete local keys if server call is failed.
     * @param publicKeyCredentialSource The [PublicKeyCredentialSource] to be deleted
     * @param forceDelete If true, it will also delete local keys if server call is failed.
     */
    suspend fun deleteCredentials(publicKeyCredentialSource: PublicKeyCredentialSource, forceDelete: Boolean = false) {
        try {
            remoteWebAuthnRepository?.delete(publicKeyCredentialSource)
        } catch (e: Exception) {
            if (forceDelete) {
                webAuthnDataRepository.delete(publicKeyCredentialSource)
            }
        }
    }

    /**
     * Load all the [PublicKeyCredentialSource] by the provided Relying Party Id
     * @param rpId The relying party id to lookup from the internal storage
     * @return All the [PublicKeyCredentialSource] which associate with the provide relying party id
     */
    fun loadAllCredentials(rpId: String): List<PublicKeyCredentialSource> {
        return webAuthnDataRepository.getPublicKeyCredentialSource(rpId)
    }
}