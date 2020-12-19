package engineer.ima.inventory.kotlin

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import engineer.ima.inventory.kotlin.helpers.INet
import engineer.ima.inventory.kotlin.structures.DeviceStructure
import java.io.File
import java.io.FileOutputStream
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.security.SecureRandom
import java.util.*

class Preferences(context: Context) {
    private val prefsFileName = "engineer.ima.inventory.kotlin.Preferences"
    private val fireBaseTokenKey = "firebase-token"
    private val associatedDevicesKey = "associated-devices"
    private val usernameTokenKey = "%s-username"
    private val notificationIdKey = "%s-notification-id"
    private val prefs: SharedPreferences = context.getSharedPreferences(prefsFileName, 0)
    private var mContext = context
    val gson = Gson()

    private fun readAsText(fileName: String): String = File(fileName).readText(Charsets.UTF_8)

    var firebaseToken: String?
        get() = prefs.getString(fireBaseTokenKey, null)
        set(value) = prefs.edit().putString(fireBaseTokenKey, value).apply()

    var associatedDevices: MutableSet<String>
        get() {
            return prefs.getStringSet(associatedDevicesKey, mutableSetOf())!!
        }
        private set(_) {}

    fun storeDeviceCheckin(checkin: DeviceStructure) {
        val deviceDir = File(mContext.filesDir, checkin.deviceIdsUniqueHash!!)
        deviceDir.mkdirs()
        val file = File(deviceDir, checkin.deviceCheckin?.lastUpdated!!)

        val png = File(deviceDir, "%s.png".format(checkin.deviceCheckin?.lastUpdated!!))
        val download = INet.download(checkin.deviceCheckin?.location?.mapUrl!!)
        png.writeBytes(download)
        checkin.deviceCheckin?.location?.mapUri = png.absolutePath

        FileOutputStream(file).use {
            it.write(gson.toJson(checkin).toByteArray(Charset.defaultCharset()))
        }
    }

    fun getDeviceCheckins(deviceId: String): ArrayList<DeviceStructure> {
        val deviceDir = File(mContext.filesDir, deviceId)
        Log.d(TAG, "Looking for checkins: %s".format(deviceId))
        val toList = deviceDir.walkTopDown().toList()
        val checkins = arrayListOf<DeviceStructure>()
        for (file in toList) {
            if (file.isFile && file.extension != "png") {
                val text = readAsText(file.absolutePath)
                val element = gson.fromJson(text, DeviceStructure::class.java)
                checkins.add(element)
            }
        }
        return checkins
    }

    fun getLatestDeviceCheckin(deviceId: String): DeviceStructure {
        val deviceDir = File(mContext.filesDir, deviceId)
        Log.d(TAG, "Looking for latest checkins in: %s".format(deviceDir.absolutePath))
        val latest = findLatest(deviceDir.absolutePath)
        val data = readFileAsTextUsingInputStream(latest?.toFile()?.absolutePath!!)
        return gson.fromJson(data, DeviceStructure::class.java)
    }

    private fun readFileAsTextUsingInputStream(fileName: String) =
            File(fileName).inputStream().readBytes().toString(Charsets.UTF_8)

    private fun findLatest(sdir: String?): Path? {
        val dir: Path = Paths.get(sdir)
        if (Files.isDirectory(dir)) {
            val opPath: Optional<Path> = Files.list(dir)
                    .filter { p -> !Files.isDirectory(p) && p.toFile().extension != "png" }
                    .sorted { p1, p2 ->
                        java.lang.Long.valueOf(p2.toFile().lastModified())
                                .compareTo(p1.toFile().lastModified())
                    }
                    .findFirst()
            if (opPath.isPresent) {
                return opPath.get()
            }
        }
        return null
    }

    fun addAssociatedDevice(deviceId: String): Boolean {
        if (!associatedDevices.contains(deviceId)) {
            val mutableSetOf = mutableSetOf(deviceId)
            for (dev: String in associatedDevices) {
                mutableSetOf.add(dev)
            }
            Log.d(TAG, "Storing device list: %s".format(mutableSetOf))
            prefs.edit().putStringSet(associatedDevicesKey, mutableSetOf).apply()
            return true
        }
        return false
    }

    fun storeDeviceUsername(deviceToken: String, username: String) {
        val format = usernameTokenKey.format(deviceToken)
        Log.d(TAG, "Storing username: $username, deviceToken: $format")
        prefs.edit().putString(format, username).apply()
    }

    fun getDeviceUsername(deviceToken: String): String? {
        return prefs.getString(usernameTokenKey.format(deviceToken), "NOT FOUND: %s".format(deviceToken))
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

    companion object {
        val TAG = Preferences::class.java.simpleName
    }
}