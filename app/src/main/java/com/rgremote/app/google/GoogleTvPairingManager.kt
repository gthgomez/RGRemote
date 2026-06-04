package com.rgremote.app.google

import android.util.Log
import com.rgremote.app.data.db.PairingCredentialEntity
import com.rgremote.app.data.registry.DeviceRegistry
import com.rgremote.app.domain.RegisteredDevice
import com.rgremote.app.google.ProtoWire.message
import com.rgremote.app.google.ProtoWire.messageField
import com.rgremote.app.google.ProtoWire.stringField
import com.rgremote.app.google.ProtoWire.varintField
import com.rgremote.app.google.ProtoWire.writeFrame
import java.math.BigInteger
import java.security.MessageDigest
import java.security.cert.X509Certificate
import java.security.interfaces.RSAPublicKey
import javax.net.ssl.SSLSocket
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class GoogleTvPairingManager(
    private val registry: DeviceRegistry,
    private val keyStore: GoogleTvKeyStore
) {
    private val sessions = mutableMapOf<String, PairingSession>()
    private val sessionsMutex = Mutex()

    suspend fun startPairing(device: RegisteredDevice): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                val credential = keyStore.ensureClientCredential(device.id)
                val socket = keyStore.pairingSslContext(credential.alias)
                    .socketFactory
                    .createSocket(device.ipAddress, PAIRING_PORT) as SSLSocket
                socket.soTimeout = 15_000
                Log.d(TAG, "Starting TLS pairing handshake with ${device.ipAddress}:$PAIRING_PORT")
                socket.startHandshake()
                val serverCert = socket.session.peerCertificates.first() as X509Certificate
                val session = PairingSession(socket, credential, serverCert)
                val replacedSession = addSession(device.id, session)
                replacedSession?.close()
                try {
                    Log.d(TAG, "Sending pairing request")
                    session.output.writeFrame(outer(pairingRequest()))
                    expectOk(session.input, expectedField = FIELD_PAIRING_REQUEST_ACK)
                    Log.d(TAG, "Sending pairing options")
                    session.output.writeFrame(outer(options()))
                    val optionFields = readOk(session.input)
                    check(optionFields.hasField(FIELD_OPTIONS)) {
                        "Android TV pairing did not return encoding options"
                    }
                    Log.d(TAG, "Sending pairing configuration")
                    session.output.writeFrame(outer(configuration()))
                    expectOk(session.input, expectedField = FIELD_CONFIGURATION_ACK)
                    Log.d(TAG, "Pairing PIN should now be visible on TV")
                    Unit
                } catch (error: Throwable) {
                    val currentSession = removeSessionIfCurrent(device.id, session)
                    if (currentSession != null) {
                        session.close()
                    }
                    throw error
                }
            }
        }

    suspend fun cancelPairing(deviceId: String) {
        removeSession(deviceId)?.close()
    }

    suspend fun forgetPairing(deviceId: String) {
        cancelPairing(deviceId)
        registry.deleteCredential(deviceId)
        keyStore.deleteClientCredential(deviceId)
    }

    suspend fun finishPairing(device: RegisteredDevice, pin: String): Result<Unit> =
        runCatching {
            withContext(Dispatchers.IO) {
                val session = removeSession(device.id)
                    ?: error("Start pairing before entering the TV PIN")
                session.use {
                    val secret = pairingSecret(
                        normalizedPin = pin.normalizedPin(),
                        clientCertificate = it.credential.certificate,
                        serverCertificate = it.serverCertificate
                    )
                    Log.d(TAG, "Sending pairing secret")
                    it.output.writeFrame(outer(secretMessage(secret)))
                    expectOk(it.input, expectedField = FIELD_SECRET_ACK)
                    registry.saveCredential(
                        PairingCredentialEntity(
                            deviceId = device.id,
                            keyAlias = it.credential.alias,
                            certificatePem = it.credential.certificatePem,
                            serverCertificateSha256 = keyStore.certificateSha256(it.serverCertificate),
                            protocolVersion = "androidtvremote2",
                            pairedAtMillis = System.currentTimeMillis()
                        )
                    )
                }
            }
        }

    private suspend fun addSession(deviceId: String, session: PairingSession): PairingSession? =
        sessionsMutex.withLock { sessions.put(deviceId, session) }

    private suspend fun removeSession(deviceId: String): PairingSession? =
        sessionsMutex.withLock {
            sessions.remove(deviceId)
        }

    private suspend fun removeSessionIfCurrent(deviceId: String, session: PairingSession): PairingSession? =
        sessionsMutex.withLock {
            val current = sessions[deviceId]
            if (current === session) sessions.remove(deviceId) else null
        }

    private fun outer(payload: ByteArray): ByteArray =
        message(
            varintField(1, PROTOCOL_VERSION),
            varintField(2, 200),
            payload
        )

    private fun pairingRequest(): ByteArray =
        messageField(
            10,
            message(
                stringField(1, SERVICE_NAME),
                stringField(2, CLIENT_NAME)
            )
        )

    private fun options(): ByteArray {
        val encoding = message(
            varintField(1, 3),
            varintField(2, 6)
        )
        return messageField(
            20,
            message(
                messageField(1, encoding),
                varintField(3, 1)
            )
        )
    }

    private fun configuration(): ByteArray {
        val encoding = message(
            varintField(1, ENCODING_HEXADECIMAL),
            varintField(2, PIN_SYMBOL_LENGTH)
        )
        return messageField(
            FIELD_CONFIGURATION,
            message(
                messageField(1, encoding),
                varintField(2, ROLE_INPUT)
            )
        )
    }

    private fun secretMessage(secret: ByteArray): ByteArray =
        messageField(FIELD_SECRET, message(ProtoWire.bytesField(1, secret)))

    private fun expectOk(input: java.io.InputStream, expectedField: Int) {
        val fields = readOk(input)
        check(fields.hasField(expectedField)) {
            "Android TV pairing response did not include field $expectedField"
        }
    }

    private fun readOk(input: java.io.InputStream): List<ProtoWire.Field> {
        val frame = ProtoWire.readFrame(input)
        val fields = ProtoWire.parse(frame)
        val status = fields.firstOrNull { it.number == 2 }?.varint
        check(status == 200L) { "Android TV pairing status was ${status ?: "missing"}" }
        return fields
    }

    private fun pairingSecret(
        normalizedPin: String,
        clientCertificate: X509Certificate,
        serverCertificate: X509Certificate
    ): ByteArray {
        val firstByte = normalizedPin.substring(0, 2).toInt(16).toByte()
        val codeBytes = normalizedPin.substring(2).chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        val input = hexBytes(clientCertificate.rsaPublicKey().modulus) +
            hexBytes(clientCertificate.rsaPublicKey().publicExponent) +
            hexBytes(serverCertificate.rsaPublicKey().modulus) +
            hexBytes(serverCertificate.rsaPublicKey().publicExponent) +
            codeBytes
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
        check(digest.first() == firstByte) { "Pairing PIN failed certificate hash check" }
        return digest
    }

    private fun X509Certificate.rsaPublicKey(): RSAPublicKey =
        publicKey as? RSAPublicKey
            ?: error("Android TV pairing requires RSA certificates")

    private fun hexBytes(integer: BigInteger): ByteArray {
        val hex = integer.toString(16).let { if (it.length % 2 == 0) it else "0$it" }
        return hex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    private fun String.normalizedPin(): String {
        val value = trim().uppercase()
        require(value.matches(Regex("[0-9A-F]{6}"))) {
            "Android TV pairing PIN must be 6 hexadecimal characters"
        }
        return value
    }

    private class PairingSession(
        val socket: SSLSocket,
        val credential: GoogleTvKeyStore.ClientCredential,
        val serverCertificate: X509Certificate
    ) : AutoCloseable {
        val input = socket.inputStream
        val output = socket.outputStream

        override fun close() {
            runCatching { socket.close() }
        }
    }

    companion object {
        private const val TAG = "RGRemoteGooglePair"
        private const val PAIRING_PORT = 6467
        private const val PROTOCOL_VERSION = 2L
        private const val SERVICE_NAME = "atvremote"
        private const val CLIENT_NAME = "RGRemote"
        private const val ROLE_INPUT = 1L
        private const val ENCODING_HEXADECIMAL = 3L
        private const val PIN_SYMBOL_LENGTH = 6L
        private const val FIELD_PAIRING_REQUEST_ACK = 11
        private const val FIELD_OPTIONS = 20
        private const val FIELD_CONFIGURATION = 30
        private const val FIELD_CONFIGURATION_ACK = 31
        private const val FIELD_SECRET = 40
        private const val FIELD_SECRET_ACK = 41
    }
}

private fun List<ProtoWire.Field>.hasField(number: Int): Boolean =
    any { it.number == number }
