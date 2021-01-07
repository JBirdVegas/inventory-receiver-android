package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

open class DeviceItem

open class DeviceItemStorable : DeviceItem() {
    @SerializedName("stored_path")
    var storedPath: String? = null
}
