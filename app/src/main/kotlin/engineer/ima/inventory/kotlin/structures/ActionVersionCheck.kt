package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class ActionVersionCheck(
        val current: String,
        @SerializedName("go_os")
        val goOs: String,
        @SerializedName("go_arch")
        val goArch: String,
        val latest: String,
)