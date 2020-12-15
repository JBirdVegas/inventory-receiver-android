package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class DeviceStructure(
        @SerializedName("device_ids")
        var deviceIds: DeviceId? = null,
        @SerializedName("device_ids_unique_hash")
        var deviceIdsUniqueHash: String? = null,
        @SerializedName("device_checkin")
        var deviceCheckin: DeviceCheckin? = null,
)