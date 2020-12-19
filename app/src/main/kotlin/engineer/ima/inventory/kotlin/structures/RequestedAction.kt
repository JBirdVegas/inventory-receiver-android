package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class RequestedAction(
        val action: String? = null,
        @SerializedName("action_uuid")
        val actionUuid: String? = null,
        val deviceId: String? = null
)