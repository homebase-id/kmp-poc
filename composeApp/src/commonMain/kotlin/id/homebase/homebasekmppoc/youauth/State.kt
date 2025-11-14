package id.homebase.homebasekmppoc.youauth

import id.homebase.homebasekmppoc.crypto.EccFullKeyData
import id.homebase.homebasekmppoc.crypto.SensitiveByteArray

data class State(
    val identity: String,
    val privateKey: SensitiveByteArray,
    var keyPair: EccFullKeyData
)

