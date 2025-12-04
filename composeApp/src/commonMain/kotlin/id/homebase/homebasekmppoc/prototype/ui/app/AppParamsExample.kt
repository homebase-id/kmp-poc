package id.homebase.homebasekmppoc.prototype.ui.app

import id.homebase.homebasekmppoc.prototype.lib.serialization.OdinSystemSerializer
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
