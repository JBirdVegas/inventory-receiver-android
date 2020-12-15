package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class AccessPoint(
        var ssid: String? = null,
        var bssid: String? = null,
        @SerializedName("link_auth")
        var linkAuth: String? = null,
        var channels: List<String>? = null,
)