package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class HardwareIds(
        @SerializedName("hardware_model")
        var hardwareModel: String? = null,
        @SerializedName("cpu_id")
        var cpuId: String? = null,
)