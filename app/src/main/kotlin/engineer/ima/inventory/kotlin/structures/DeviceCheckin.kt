package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class DeviceCheckin(
        var local: String? = null,
        var public: String? = null,
        @SerializedName("last_updated")
        var lastUpdated: String? = null,
        @SerializedName("current_access_point")
        var currentAccessPoint: AccessPoint? = null,
        @SerializedName("location")
        var location: Location? = null,
)