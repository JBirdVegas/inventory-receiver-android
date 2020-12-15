package engineer.ima.inventory.kotlin.activities

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import engineer.ima.inventory.R
import engineer.ima.inventory.kotlin.Preferences
import engineer.ima.inventory.kotlin.structures.DeviceStructure
import java.io.File


class DeviceHistoryActivity : AppCompatActivity() {
    companion object {
        const val DEVICE_ID = "DEVICE_ID"
    }

    private var listView: ListView? = null
    private var adapter: MyAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_history_activity)
        listView = findViewById(R.id.device_history_listview)
        val deviceId = intent.extras?.getString(DEVICE_ID)

        val arrayList = Preferences(applicationContext).getDeviceCheckins(deviceId!!)
        adapter = MyAdapter(applicationContext, arrayList)
        listView?.adapter = adapter
    }

    class MyAdapter(private val context: Context, private val arrayList: java.util.ArrayList<DeviceStructure>) : BaseAdapter() {
        private lateinit var address: TextView
        private lateinit var wifiAp: TextView
        private lateinit var date: TextView
        private lateinit var map: ImageView
        override fun getCount(): Int {
            return arrayList.size
        }

        override fun getItem(position: Int): Any {
            return position
        }

        override fun getItemId(position: Int): Long {
            return position.toLong()
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view: View? = convertView
                    ?: LayoutInflater.from(context).inflate(R.layout.device_history_listview_item, parent, false)
            address = view?.findViewById(R.id.list_item_address)!!
            wifiAp = view.findViewById(R.id.list_item_wifi_ap)!!
            date = view.findViewById(R.id.list_item_date)!!
            map = view.findViewById(R.id.list_item_map)!!
            address.text = arrayList[position].deviceCheckin?.location?.address
            wifiAp.text = arrayList[position].deviceCheckin?.currentAccessPoint?.ssid
            date.text = arrayList[position].deviceCheckin?.lastUpdated

            arrayList[position].deviceCheckin?.location?.mapUri?.let {
                val file = File(it)
                if (file.exists()) {
                    val readBytes = file.readBytes()
                    val b = BitmapFactory.decodeByteArray(readBytes, 0, readBytes.size, BitmapFactory.Options())
                    map.setImageBitmap(b)
                }
            }
            return view
        }
    }

}