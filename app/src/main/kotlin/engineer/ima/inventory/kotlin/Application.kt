package engineer.ima.inventory.kotlin

import android.app.Application


class Application : Application() {
    companion object {
        const val KEY_STATUS_CODE = "KEY_STATUS_CODE"
        const val KEY_RESPONSE_BODY = "KEY_RESPONSE_BODY"
        var runningQOrLater = android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q
    }
}