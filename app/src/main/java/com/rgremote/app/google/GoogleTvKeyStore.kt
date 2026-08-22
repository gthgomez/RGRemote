package com.rgremote.app.google

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.io.ByteArrayInputStream
import java.math.BigInteger
import java.net.Socket
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.MessageDigest
import java.security.Principal
import java.security.PrivateKey
import java.security.SecureRandom
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import java.util.Base64
import java.util.Date
import javax.net.ssl.KeyManager
import javax.net.ssl.SSLEngine
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509ExtendedKeyManager
import javax.net.ssl.X509TrustManager
import javax.security.auth.x500.X500Principal

class GoogleTvKeyStore(context: Context) {
    private val filesDir = context.applicationContext.filesDir

    fun ensureClientCredential(deviceId: String): ClientCredential {
        val alias = aliasFor(deviceId)
        val store = loadAndroidKeyStore()
        if (!store.containsAlias(alias)) {
            deleteLegacyPemFiles(alias)
            generateAndroidKeyStoreCredential(alias)
        }
        val certificate = loadCertificate(alias)
        return ClientCredential(
            alias = alias,
            certificate = certificate,
            certificatePem = certificate.toPem()
        )
    }

    fun pairingSslContext(alias: String): SSLContext {
        // Trust-on-first-use: the TV certificate is unknown before pairing, so identity
        // cannot be verified here. It is enforced post-pairing via SHA-256 pinning in
        // [pairedSslContext]; this stage only rejects structurally unusable server keys.
        Log.w(TAG, "Pairing TLS defers server identity validation to post-pairing certificate pinning")
        return sslContext(alias = alias, trustManagers = arrayOf(TOFU_TRUST_MANAGER))
    }

    fun pairedSslContext(alias: String, serverCertificateSha256: String): SSLContext =
        sslContext(alias = alias, trustManagers = arrayOf(pinnedServerTrustManager(serverCertificateSha256)))

    fun deleteClientCredential(deviceId: String) {
        val alias = aliasFor(deviceId)
        loadAndroidKeyStore().deleteEntry(alias)
        deleteLegacyPemFiles(alias)
    }

    fun certificateFromPem(pem: String): X509Certificate {
        val body = pem
            .replace("-----BEGIN CERTIFICATE-----", "")
            .replace("-----END CERTIFICATE-----", "")
            .replace("\\s".toRegex(), "")
        val bytes = Base64.getDecoder().decode(body)
        return CertificateFactory.getInstance("X.509")
            .generateCertificate(ByteArrayInputStream(bytes)) as X509Certificate
    }

    fun certificateSha256(certificate: X509Certificate): String =
        MessageDigest.getInstance("SHA-256")
            .digest(certificate.encoded)
            .joinToString(separator = "") { "%02x".format(it) }

    private fun sslContext(alias: String, trustManagers: Array<TrustManager>): SSLContext =
        SSLContext.getInstance("TLS").apply {
            init(arrayOf(clientKeyManager(alias)), trustManagers, SecureRandom())
        }

    private fun clientKeyManager(alias: String): KeyManager {
        val store = loadAndroidKeyStore()
        val privateKey = store.getKey(alias, null) as? PrivateKey
            ?: error("Google TV client credential is missing; pair the device again")
        val certificate = store.getCertificate(alias) as? X509Certificate
            ?: error("Google TV client certificate is missing; pair the device again")
        val chain = arrayOf(certificate)

        return object : X509ExtendedKeyManager() {
            override fun getClientAliases(keyType: String?, issuers: Array<out Principal>?): Array<String> =
                arrayOf(alias)

            override fun chooseClientAlias(
                keyType: Array<out String>?,
                issuers: Array<out Principal>?,
                socket: Socket?
            ): String = alias

            override fun getServerAliases(keyType: String?, issuers: Array<out Principal>?): Array<String>? = null

            override fun chooseServerAlias(
                keyType: String?,
                issuers: Array<out Principal>?,
                socket: Socket?
            ): String? = null

            override fun getCertificateChain(requestedAlias: String?): Array<X509Certificate>? =
                if (requestedAlias == alias) chain else null

            override fun getPrivateKey(requestedAlias: String?): PrivateKey? =
                if (requestedAlias == alias) privateKey else null

            override fun chooseEngineClientAlias(
                keyType: Array<out String>?,
                issuers: Array<out Principal>?,
                engine: SSLEngine?
            ): String = alias

            override fun chooseEngineServerAlias(
                keyType: String?,
                issuers: Array<out Principal>?,
                engine: SSLEngine?
            ): String? = null
        }
    }

    private fun pinnedServerTrustManager(expectedSha256: String): TrustManager =
        object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                val certificate = chain?.firstOrNull()
                    ?: throw CertificateException("Google TV did not present a server certificate")
                val actualSha256 = certificateSha256(certificate)
                if (!actualSha256.equals(expectedSha256, ignoreCase = true)) {
                    throw CertificateException("Google TV server certificate changed; re-pair the device")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }

    private fun generateAndroidKeyStoreCredential(alias: String) {
        val now = System.currentTimeMillis()
        val spec = KeyGenParameterSpec.Builder(
            alias,
            KeyProperties.PURPOSE_SIGN or KeyProperties.PURPOSE_VERIFY
        )
            .setKeySize(2048)
            .setDigests(KeyProperties.DIGEST_SHA256, KeyProperties.DIGEST_SHA512)
            .setSignaturePaddings(KeyProperties.SIGNATURE_PADDING_RSA_PKCS1)
            .setCertificateSubject(X500Principal("CN=RGRemote"))
            .setCertificateSerialNumber(BigInteger.valueOf(1000L + alias.hashCode().toUInt().toLong()))
            .setCertificateNotBefore(Date(now - ONE_DAY_MILLIS))
            .setCertificateNotAfter(Date(now + TEN_YEARS_MILLIS))
            .build()

        KeyPairGenerator.getInstance(KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEYSTORE)
            .apply { initialize(spec) }
            .generateKeyPair()
    }

    private fun loadCertificate(alias: String): X509Certificate =
        loadAndroidKeyStore().getCertificate(alias) as? X509Certificate
            ?: error("Google TV client certificate is missing; pair the device again")

    private fun loadAndroidKeyStore(): KeyStore =
        KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }

    private fun deleteLegacyPemFiles(alias: String) {
        filesDir.resolve("$alias-cert.pem").delete()
        filesDir.resolve("$alias-key.pem").delete()
    }

    private fun aliasFor(deviceId: String): String =
        "rgremote_google_tv_" + deviceId.hashCode().toUInt().toString(16)

    data class ClientCredential(
        val alias: String,
        val certificate: X509Certificate,
        val certificatePem: String
    )

    companion object {
        private const val TAG = "RGRemoteGooglePair"
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val MIN_SERVER_RSA_BITS = 2048
        private const val ONE_DAY_MILLIS = 24L * 60L * 60L * 1_000L
        private const val TEN_YEARS_MILLIS = 10L * 365L * ONE_DAY_MILLIS

        private val TOFU_TRUST_MANAGER: TrustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<out X509Certificate>?, authType: String?) = Unit

            override fun checkServerTrusted(chain: Array<out X509Certificate>?, authType: String?) {
                // TOFU protocol stage: identity is deferred to the post-pairing pin check,
                // but key material must still be strong enough to pin safely.
                val certificate = chain?.firstOrNull()
                    ?: throw CertificateException("Google TV did not present a server certificate during pairing")
                val publicKey = certificate.publicKey as? RSAPublicKey
                    ?: throw CertificateException("Google TV pairing requires an RSA server certificate")
                val bits = publicKey.modulus.bitLength()
                if (bits < MIN_SERVER_RSA_BITS) {
                    throw CertificateException("Google TV server RSA key is $bits bits; minimum is $MIN_SERVER_RSA_BITS")
                }
            }

            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
    }
}

private fun X509Certificate.toPem(): String {
    val body = Base64.getMimeEncoder(64, "\n".toByteArray())
        .encodeToString(encoded)
    return "-----BEGIN CERTIFICATE-----\n$body\n-----END CERTIFICATE-----\n"
}
