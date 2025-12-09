package id.homebase.homebasekmppoc.prototype.ui.app

import id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthAppParameters
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthDriveParameters

val exampleDriveAlias = "11111111111111111111111111111111"
val exampleDriveType = "22222222222222222222222222222222"

val exampleBadDriveAlias = "99999999999999999999999999999999"

fun getAppParams(): YouAuthAppParameters {
    val driveParams = listOf(
        YouAuthDriveParameters(
            driveAlias = exampleDriveAlias,
            driveType = exampleDriveType,
            name = "Third Part Library",
            description = "Place for your third parties",
            permission = 3
        )
    )

    val appParams = YouAuthAppParameters(
        appName = "third party app",
        appOrigin = "dev.dotyou.cloud:3005",
        appId = "aaaaaaaa-bbbb-cccc-dddd-cccccccccccc",
        clientFriendly = "KMP App",
        drivesParam = OdinSystemSerializer.serialize(driveParams),
        returnParam = "backend-will-decide"
    )

    return appParams
}

fun getFeedAppParams(): YouAuthAppParameters {
    val driveParams = listOf(
        YouAuthDriveParameters(
            driveAlias = "e8475dc46cb4b6651c2d0dbd0f3aad5f",
            driveType = "8f448716-e34c-edf9-0141-45e043ca6612",
            name = "feed me library",
            description = "Place for your feeds",
            permission = 3
        )
    )

    val appParams = YouAuthAppParameters(
        appName = "feed me app",
        appOrigin = "dev.dotyou.cloud:3005",
        appId = "5f887d80-0132-4294-ba40-bda79155551d",
        clientFriendly = "KMP App",
        drivesParam = OdinSystemSerializer.serialize(driveParams),
        returnParam = "backend-will-decide"
    )

    return appParams
}


