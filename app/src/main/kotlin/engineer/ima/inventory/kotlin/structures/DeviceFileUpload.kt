package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class DeviceFileUpload(
        @SerializedName("download_url")
        val downloadUrl: String? = null,
        @SerializedName("device_id")
        val deviceId: String? = null,
        @SerializedName("file_name")
        val fileName: String? = null,
        @SerializedName("upload_time")
        val uploadTime: String? = null,

        @SerializedName("stored_path")
        var storedPath: String? = null
) : DeviceItem()