package id.homebase.homebasekmppoc.core

import id.homebase.homebasekmppoc.crypto.ByteArrayUtil
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * A wrapper for sensitive byte arrays (keys, secrets) that provides:
 * - Secure wiping of memory when no longer needed
 * - Cloning capability
 * - Safe accessors
 */
class SensitiveByteArray : AutoCloseable {
    private var _key: ByteArray? = null

    constructor()

    constructor(data: ByteArray) {
        setKey(data)
    }

    @OptIn(ExperimentalEncodingApi::class)
    constructor(data64: String) {
        setKey(Base64.decode(data64))
    }

    private constructor(other: SensitiveByteArray) {
        if (other._key != null) {
            setKey(other._key!!)
        }
    }

    override fun close() {
        wipe()
    }

    fun clone(): SensitiveByteArray {
        return SensitiveByteArray(this)
    }

    fun wipe() {
        _key?.let { key ->
            ByteArrayUtil.wipeByteArray(key)
        }
        _key = null
    }

    fun setKey(data: ByteArray) {
        require(data.isNotEmpty()) { "Can't set an empty key" }

        wipe()

        _key = ByteArray(data.size)
        data.copyInto(_key!!)
    }

    fun getKey(): ByteArray {
        return _key ?: throw IllegalStateException("No key set")
    }

    fun isEmpty(): Boolean {
        return _key == null
    }

    fun isSet(): Boolean {
        return _key != null
    }
}

/**
 * Extension function to convert a ByteArray to SensitiveByteArray
 */
fun ByteArray.toSensitiveByteArray(): SensitiveByteArray {
    return SensitiveByteArray(this)
}
