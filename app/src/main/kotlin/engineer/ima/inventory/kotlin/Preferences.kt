package engineer.ima.inventory.kotlin

import android.content.Context
import android.content.SharedPreferences
import java.security.SecureRandom

class Preferences(context: Context) {
    private val PREFS_FILENAME = "engineer.ima.inventory.kotlin.Preferences"
    private val FIREBASE_TOKEN = "firebase-token"
    private val ASSOCIATED_DEVICES = "associated-devices"
    private val usernameTokenKey = "%s-username"
    private val notificationIdKey = "%s-notification-id"
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_FILENAME, 0);

    var firebaseToken: String?
        get() = prefs.getString(FIREBASE_TOKEN, null)
        set(value) = prefs.edit().putString(FIREBASE_TOKEN, value).apply()

    var associatedDevices: MutableSet<String>
        get() {
            return prefs.getStringSet(ASSOCIATED_DEVICES, mutableSetOf())!!
        }
        private set(v) {}


    fun addAssociatedDevice(deviceId: String): Boolean {
        if (!associatedDevices.contains(deviceId)) {
            val mutableSetOf = mutableSetOf(deviceId)
            for (dev: String in associatedDevices) {
                mutableSetOf.add(dev)
            }
            prefs.edit().putStringSet(ASSOCIATED_DEVICES, mutableSetOf).apply()
            return true
        }
        return false
    }

    fun storeDeviceUsername(deviceToken: String, username: String) {
        prefs.edit().putString(usernameTokenKey.format(deviceToken), username).apply()
    }

    fun getDeviceUsername(deviceToken: String): String? {
        return prefs.getString(usernameTokenKey.format(deviceToken), null)
    }

    fun getNotificationId(deviceToken: String): Int {
        val int = prefs.getInt(notificationIdKey.format(deviceToken), 0)
        val instanceStrong = SecureRandom.getInstanceStrong()
        val nextInt = instanceStrong.nextInt()
        if (int == 0) {
            prefs.edit()
                    .putInt(notificationIdKey.format(deviceToken), nextInt)
                    .apply()
            return nextInt
        }
        return int
    }

}