package id.homebase.homebasekmppoc.youauth

import id.homebase.homebasekmppoc.generateUuidBytes

fun buildAuthorizeUrl(identity: String): String {
    var url = "";


    //
    // YouAuth [010]
    //
    val privateKey = generateUuidBytes()


    //    val keyPair: EccFullKeyData = EccFullKeyData(privateKey, EccKeySize.P384, 1)


    return url;
}