package engineer.ima.inventory.kotlin.structures

import com.google.gson.annotations.SerializedName

data class Location(
        var name: String? = null,
        var address: String? = null,
        var locality: String? = null,
        @SerializedName("postal_code")
        var postalCode: String? = null,
        var country: String? = null,
        var region: String? = null,
        var latitude: String? = null,
        var longitude: String? = null,
        @SerializedName("iso_country_code")
        var isoCountryCode: String? = null,
        var altitude: String? = null,
        var timezone: String? = null,
        @SerializedName("thoroughfare")
        var thoroughFare: String? = null,
        @SerializedName("sub_thoroughfare")
        var subThoroughFare: String? = null,
        @SerializedName("administrative_area")
        var administrativeArea: String? = null,
        @SerializedName("sub_administrative_area")
        var subAdmininstrativeArea: String? = null,
        @SerializedName("h_accuracy")
        var hAccuracy: String? = null,
        @SerializedName("v_accuracy")
        var vAccuracy: String? = null,
        var direction: String? = null,
        var speed: String? = null,
        @SerializedName("map_url")
        var mapUrl: String? = null,
        @SerializedName("stored_path")
        var storedPath: String? = null,
)