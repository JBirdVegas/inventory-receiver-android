package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName


data class PingReply(
        val ping: String? = null,
        @SerializedName("start_time")
        val startTime: String? = null,
        @SerializedName("end_time")
        val endTime: String? = null,
        @SerializedName("requested_action")
        val requestedAction: RequestedAction? = null
)