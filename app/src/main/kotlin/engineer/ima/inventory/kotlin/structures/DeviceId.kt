package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class DeviceId(
        @SerializedName("kernel_ids")
        var kernelID: KernelIds? = null,
        @SerializedName("hardware_ids")
        var hardwareIds: HardwareIds? = null,
        @SerializedName("user_ids")
        var userIds: UserIds? = null,
)