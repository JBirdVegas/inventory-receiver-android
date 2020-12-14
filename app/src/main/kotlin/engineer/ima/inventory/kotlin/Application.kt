package engineer.ima.inventory.kotlin

import android.app.Application


val configs: Preferences? = null


class Application : Application() {
    companion object {
        var preferences: Preferences? = null
        const val KEY_STATUS_CODE = "KEY_STATUS_CODE"
        const val KEY_RESPONSE_BODY = "KEY_RESPONSE_BODY"
        var runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    }

    override fun onCreate() {
        super.onCreate()
        preferences = Preferences(context = baseContext)
    }
}