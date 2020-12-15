package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class KernelIds(
        @SerializedName("kernel_host_name")
        var kernelHostname: String? = null,
        @SerializedName("kernel_uuid")
        var kernelUuid: String? = null,
)