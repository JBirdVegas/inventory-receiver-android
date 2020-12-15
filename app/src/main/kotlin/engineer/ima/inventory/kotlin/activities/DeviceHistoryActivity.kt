package engineer.ima.inventory.kotlin.activities

import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import engineer.ima.inventory.R
import engineer.ima.inventory.kotlin.Preferences
import engineer.ima.inventory.kotlin.helpers.TimeHelper
import engineer.ima.inventory.kotlin.structures.DeviceStructure
import xyz.sangcomz.stickytimelineview.TimeLineRecyclerView
import xyz.sangcomz.stickytimelineview.callback.SectionCallback
import xyz.sangcomz.stickytimelineview.model.SectionInfo
import java.io.File
import java.util.*


class DeviceHistoryActivity : AppCompatActivity() {
    companion object {
        const val DEVICE_ID = "DEVICE_ID"
    }

    private var listView: TimeLineRecyclerView? = null
    private var adapter: MyAdapter? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.device_history_activity)
        listView = findViewById(R.id.device_history_listview)
        val deviceId = intent.extras?.getString(DEVICE_ID)

        val arrayList = Preferences(applicationContext).getDeviceCheckins(deviceId!!)
        adapter = MyAdapter(applicationContext, arrayList)
        listView?.adapter = SingerAdapter(arrayList)

        listView?.layoutManager = LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL,
                false)
        listView?.addItemDecoration(getSectionCallback(arrayList))

    }

    private fun getSectionCallback(arrayList: ArrayList<DeviceStructure>): SectionCallback {
        return object : SectionCallback {
            //In your data, implement a method to determine if this is a section.
            override fun isSection(position: Int): Boolean = true

            //Implement a method that returns a SectionHeader.
            override fun getSectionHeader(position: Int): SectionInfo? {
                val item = arrayList[position]
                val title = "${item.deviceCheckin?.location?.name} @ ${item.deviceCheckin?.lastUpdated}"
                return SectionInfo(title, item.deviceIds?.userIds?.userId)

            }

        }
    }

    class SingerAdapter(private val ourList: ArrayList<DeviceStructure>) : RecyclerView.Adapter<SingerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val v = LayoutInflater.from(parent.context).inflate(
                    R.layout.device_history_listview_item,
                    parent,
                    false)
            return ViewHolder(v)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val item = ourList[position]

            holder.address.text = item.deviceCheckin?.location?.address
            holder.wifiAp.text = item.deviceCheckin?.currentAccessPoint?.ssid
            holder.date.text = TimeHelper.format(item.deviceCheckin?.lastUpdated!!)

            item.deviceCheckin?.location?.mapUri?.let {
                val file = File(it)
                if (file.exists()) {
                    val readBytes = file.readBytes()
                    val b = BitmapFactory.decodeByteArray(readBytes, 0, readBytes.size, BitmapFactory.Options())
                    holder.map.setImageBitmap(b)
                }
            }
        }

        override fun getItemCount(): Int = ourList.size

        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val address: TextView = view.findViewById<View>(R.id.list_item_address) as TextView
            val wifiAp: TextView = view.findViewById<View>(R.id.list_item_wifi_ap) as TextView
            val date: TextView = view.findViewById<View>(R.id.list_item_date) as TextView
            val map: ImageView = view.findViewById<View>(R.id.list_item_map) as ImageView
        }
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
            date.text = TimeHelper.format(arrayList[position].deviceCheckin?.lastUpdated!!)

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