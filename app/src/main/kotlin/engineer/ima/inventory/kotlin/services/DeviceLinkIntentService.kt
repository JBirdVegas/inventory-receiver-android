package engineer.ima.inventory.kotlin.services

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService

class DeviceLinkIntentService : JobIntentService() {
    override fun onHandleWork(intent: Intent) {
        val map = mapOf("" to "")
    }

    companion object {
        private const val UNIQUE_JOB_ID = 1000;

        @JvmStatic
        fun enqueueWork(context: Context, work: Intent) {
            enqueueWork(context, DeviceLinkIntentService::class.java, UNIQUE_JOB_ID, work)
        }
    }
}