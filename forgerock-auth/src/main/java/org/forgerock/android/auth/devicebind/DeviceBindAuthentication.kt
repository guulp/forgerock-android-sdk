/*
 * Copyright (c) 2022 ForgeRock. All rights reserved.
 *
 * This software may be modified and distributed under the terms
 * of the MIT license. See the LICENSE file for details.
 */

package org.forgerock.android.auth.devicebind

import android.security.keystore.KeyProperties
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.JWSHeader
import com.nimbusds.jose.crypto.RSASSASigner
import com.nimbusds.jose.jwk.JWK
import com.nimbusds.jose.jwk.KeyUse
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jwt.JWTClaimsSet
import com.nimbusds.jwt.SignedJWT
import org.forgerock.android.auth.callback.DeviceBindingAuthenticationType
import java.security.PrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * Interface to override keypair keys, biometric display, sign
 */
interface DeviceBindInterface {
    /**
     * generate the public and private keypair
     */
    fun generateKeys(): KeyPair
    /**
     * Display biometric prompt for authentication type
     * @param timeout Timeout for biometric prompt
     * @param statusResult Listener for receiving Biometric changes
     */
    fun authenticate(timeout: Int,  statusResult: (DeviceBindingStatus) -> Unit)
    /**
     * sign the challenge sent from the server and generate signed JWT
     * @param keyPair Public and private key
     * @param kid Generated kid from the Preference
     * @param userId userId received from server
     * @param challenge challenge received from server
     */
    fun sign(keyPair: KeyPair, kid: String, userId: String, challenge: String): String {
        val jwk: JWK = RSAKey.Builder(keyPair.publicKey)
            .keyUse(KeyUse.SIGNATURE)
            .keyID(kid)
            .algorithm(JWSAlgorithm.RS512)
            .build()
        val signedJWT = SignedJWT(
            JWSHeader.Builder(JWSAlgorithm.RS512)
                .keyID(kid)
                .jwk(jwk)
                .build(), JWTClaimsSet.Builder()
                .subject(userId)
                .claim("challenge", challenge)
                .build()
        )
        signedJWT.sign(RSASSASigner(keyPair.privateKey))
        return signedJWT.serialize()
    }
    /**
     * check biometric is supported
     */
    fun isSupported(): Boolean
}

data class KeyPair(
    val publicKey: RSAPublicKey,
    val privateKey: PrivateKey,
    var keyAlias: String
)

/**
 * Settings  for all the biometric authentication is configured
 */
class BiometricOnly(private val biometricInterface: BiometricInterface,
                    private val authentication: KeyAware): DeviceBindInterface {

    /**
     * generate the public and private keypair
     */
    override fun generateKeys(): KeyPair {
        val builder = authentication.keyBuilder()
        if (biometricInterface.isApi30AndAbove()) {
            builder.setUserAuthenticationParameters(authentication.timeout, KeyProperties.AUTH_BIOMETRIC_STRONG)
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(authentication.timeout)
        }
        builder.setUserAuthenticationRequired(true)
        return authentication.createKeyPair(builder)
    }

    /**
     * check biometric is supported
     */
    override fun isSupported(): Boolean {
        return biometricInterface.isSupportedForBiometricOnly()
    }

    /**
     * Display biometric prompt for authentication type
     * @param timeout Timeout for biometric prompt
     * @param statusResult Listener for receiving Biometric changes
     */
    override fun authenticate(timeout: Int,  statusResult: (DeviceBindingStatus) -> Unit) {
        val listener = biometricInterface.getBiometricListener(timeout, statusResult)
        biometricInterface.setListener(listener)
        biometricInterface.authenticate()
    }

}

/**
 * Settings for all the biometric authentication and device credential is configured
 */
class BiometricAndDeviceCredential(private val biometricInterface: BiometricInterface,
                                   private val authentication: KeyAware): DeviceBindInterface {

    /**
     * generate the public and private keypair
     */
    override fun generateKeys(): KeyPair {
        val builder = authentication.keyBuilder()
        if (biometricInterface.isApi30AndAbove()) {
            builder.setUserAuthenticationParameters(authentication.timeout, KeyProperties.AUTH_BIOMETRIC_STRONG or KeyProperties.AUTH_DEVICE_CREDENTIAL)
        } else {
            builder.setUserAuthenticationValidityDurationSeconds(authentication.timeout)
        }
        builder.setUserAuthenticationRequired(true)
        return authentication.createKeyPair(builder)
    }

    /**
     * check biometric is supported
     */
    override fun isSupported(): Boolean {
        return biometricInterface.isSupportedForBiometricAndDeviceCredential()
    }

    /**
     * Display biometric prompt for authentication type
     * @param timeout Timeout for biometric prompt
     * @param statusResult Listener for receiving Biometric changes
     */
    override fun authenticate(timeout: Int,  statusResult: (DeviceBindingStatus) -> Unit) {
        val listener = biometricInterface.getBiometricListener(timeout, statusResult)
        biometricInterface.setListener(listener)
        biometricInterface.authenticate()
    }
}

/**
 * Settings for all the none authentication is configured
 */
class None(private val authentication: KeyAware): DeviceBindInterface {
    /**
     * generate the public and private keypair
     */
    override fun generateKeys(): KeyPair {
        val builder = authentication.keyBuilder()
        return authentication.createKeyPair(builder)
    }

    /**
     * Default is true for None type
     */
    override fun isSupported(): Boolean {
        return true
    }

    /**
     * return success block for None type
     */
    override fun authenticate(
        timeout: Int,
        result: (DeviceBindingStatus) -> Unit) {
        result(Success)
    }
}


class BindingFactory {
    companion object {
        fun getType(
            userId: String,
            authentication: DeviceBindingAuthenticationType,
            title: String,
            subtitle: String,
            description: String,
            keyAware: KeyAware = KeyAware(userId)
        ): DeviceBindInterface {
            return when (authentication) {
                DeviceBindingAuthenticationType.BIOMETRIC_ONLY -> BiometricOnly(
                    biometricInterface = BiometricUtil(
                        title,
                        subtitle,
                        description,
                        deviceBindAuthenticationType = authentication
                    ), keyAware
                )
                DeviceBindingAuthenticationType.BIOMETRIC_ALLOW_FALLBACK -> BiometricAndDeviceCredential(
                    BiometricUtil(
                        title,
                        subtitle,
                        description,
                        deviceBindAuthenticationType = authentication
                    ),
                    keyAware
                )
                else -> None(keyAware)
            }
        }
    }
}

