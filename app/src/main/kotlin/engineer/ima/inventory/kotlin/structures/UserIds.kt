package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class UserIds(
        @SerializedName("user_id")
        var userId: String? = null,
)