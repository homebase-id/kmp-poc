package id.homebase.homebasekmppoc.prototype.ui.app

import id.homebase.homebasekmppoc.lib.drives.TargetDrive
import id.homebase.homebasekmppoc.lib.serialization.OdinSystemSerializer
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthAppParameters
import id.homebase.homebasekmppoc.prototype.lib.youauth.YouAuthDriveParameters
import kotlin.uuid.Uuid

val exampleDriveAlias = "11111111111111111111111111111111"
val exampleDriveType = "22222222222222222222222222222222"

val feedTargetDrive: TargetDrive = TargetDrive(
    alias = Uuid.parse("4db49422ebad02e99ab96e9c477d1e08"),
    type = Uuid.parse ("a3227ffba87608beeb24fee9b70d92a6")
)

val appId = Uuid.parse("0cecc6fe033e48b19ee6a4f60318be02")

val exampleBadDriveAlias = "99999999999999999999999999999999"

fun getAppParams(): YouAuthAppParameters {
    val driveParams = listOf(
        YouAuthDriveParameters(
            driveAlias = feedTargetDrive.alias.toString(),
            driveType = feedTargetDrive.type.toString(),
            name = "Feed Drive",
            description = "Access to your feed drive",
            permission = 3
        )
    )

    val appParams = YouAuthAppParameters(
        appName = "KMP Prototype App",
        appOrigin = "dev.dotyou.cloud:3005",
        appId = "0cecc6fe033e48b19ee6a4f60318be02",
        clientFriendly = "KMP App",
        drivesParam = OdinSystemSerializer.serialize(driveParams),
        returnParam = "backend-will-decide"
    )

    return appParams
}
