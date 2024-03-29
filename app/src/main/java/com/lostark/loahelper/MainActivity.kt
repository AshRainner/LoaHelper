package com.lostark.loahelper

import android.content.Intent
import android.net.Uri
import android.os.Build.VERSION.SDK_INT
import android.os.Bundle
import android.os.Parcelable
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.viewpager2.widget.ViewPager2
import com.lostark.adapter.EventViewPagerAdapter
import com.lostark.api.LoaRetrofitObj
import com.lostark.customview.HomeButtonView
import com.lostark.database.AppDatabase
import com.lostark.database.table.*
import com.lostark.dto.news.NoticeItem
import retrofit2.Response


class MainActivity : AppCompatActivity() {
    private val updateLink = "https://github.com/AshRainner/LoaHelper"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity)
        overridePendingTransition(0, 0)
        drawerSet()
        buttonSet()
        eventSet()

    }

    private fun drawerSet() {
        val mainActivity = findViewById<DrawerLayout>(R.id.main_activity)
        val overlayView = findViewById<LinearLayout>(R.id.drawer_view)
        val drawerButton = findViewById<ImageButton>(R.id.drawer_button)

        mainActivity.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)

        drawerButton.setOnClickListener {
            mainActivity.openDrawer(GravityCompat.END)
        }
        drawerLayoutButtonSet()

        overlayView.setOnTouchListener { _, event -> //터치이벤트 가로채서 뒤에 있는 버튼들 안눌리게
            true
        }

    }

    private fun apiKeyCheck(key:String,db:AppDatabase) {
        val accept = "application/json"
        val call = LoaRetrofitObj.getRetrofitService().getNotice(accept,"bearer "+key)
        val response: Response<MutableList<NoticeItem>> = call.execute()

        val errorCode = response.code()
        var text:String=""
        when (errorCode) {
            200 -> {
                Log.d("올바른 키입니다.", "errorCode: ")
                db.keyDao().deleteAllKey()
                db.keyDao().insertKey(Key("bearer "+key))
                text="올바른 코드입니다. 변경완료"

            }
            401 -> {
                Log.d("올바르지 않은 키입니다.", "errorCode: ")
                //db.keyDao().deleteAllKey()
                text="올바르지 않은 키입니다."
            }
            503 -> {
                Log.d("서버가 점검 중입니다.", "errorCode")
                text="서버가 점검 중입니다."
            }
        }
        runOnUiThread {
            Toast.makeText(
                this@MainActivity,
                text,
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun drawerLayoutButtonSet() {
        val insertButton = findViewById<Button>(R.id.key_insert_button)
        val updateButton = findViewById<Button>(R.id.update_button)
        val keyEditText = findViewById<EditText>(R.id.key_edit)

        val key = intent.getStringExtra("Key") ?: null
        keyEditText.setText(key?.substring(6, key.length))

        insertButton.setOnClickListener {
            val db = AppDatabase.getInstance(applicationContext)!!
            if (key != null) {
                Thread {
                    apiKeyCheck(keyEditText.text.toString(), db)
                }.start()
            }
            /*db.keyDao().deleteAllKey()
            db.keyDao().insertKey(Key("bearer " + keyEditText.text.toString()))*/
        }

        updateButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(updateLink)))
        }
    }


    private fun buttonSet() {
        val stoneList = intent.parcelableArrayList<Items>("StoneList")
        val destructionList = intent.parcelableArrayList<Items>("Destruction")

        val noticeList = intent.parcelableArrayList<Notice>("NoticeList")

        val mapItemList = intent.parcelableArrayList<Items>("MapItemList")
        val lv1Gem= intent.paracelableExtra<GemItems>("Lv1Gem")

        val raidButton = findViewById<HomeButtonView>(R.id.home_raid_button)
        val searchButton = findViewById<HomeButtonView>(R.id.char_search_button)
        val dailyButton = findViewById<HomeButtonView>(R.id.home_daily_button)
        val engravingButton = findViewById<HomeButtonView>(R.id.home_engraving_button)
        val noticeButton = findViewById<HomeButtonView>(R.id.home_notice_button)
        val calculatorButton = findViewById<HomeButtonView>(R.id.home_calculator_button)

        calculatorButton.ClickEvent(
            Intent(this, CalculatorActivity::class.java)
                .putExtra("MapItemList",mapItemList)
                .putExtra("Lv1Gem",lv1Gem)
                .apply {
            flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        })

        raidButton.ClickEvent(Intent(this, RaidActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        })

        noticeButton.ClickEvent(
            Intent(this, NoticeActivity::class.java)
                .putExtra("NoticeList", noticeList).apply {
                    flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
        )

        dailyButton.ClickEvent(
            Intent(this, DailyActivity::class.java)
                .putExtra("StoneList", stoneList)
                .putExtra("Destruction", destructionList).apply {
                    flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
                }
        )

        engravingButton.ClickEvent(Intent(this, EngravingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        })

        searchButton.ClickEvent(Intent(this, SearchActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NO_ANIMATION
        })
    }

    private fun eventSet() {
        val eventList = intent.parcelableArrayList<LoaEvents>("EventList")
        val eventPager = findViewById<ViewPager2>(R.id.event_image_slider)
        val eventAdapter = EventViewPagerAdapter(eventList!!, eventPager)
        eventPager.adapter = eventAdapter
    }

    inline fun <reified T : Parcelable> Intent.parcelableArrayList(key: String): ArrayList<T>? =
        when {
            SDK_INT >= 33 -> getParcelableArrayListExtra(key, T::class.java)
            else -> @Suppress("DEPRECATION") getParcelableArrayListExtra(key)
        }
    inline fun <reified T : Parcelable> Intent.paracelableExtra(key: String): T? =
        when {
            SDK_INT >= 33 -> getParcelableExtra(key, T::class.java)
            else -> @Suppress("DEPRECATION") getParcelableExtra(key)
        }

    override fun onBackPressed() {
        val mainActivity = findViewById<DrawerLayout>(R.id.main_activity)
        if (mainActivity.isDrawerOpen(GravityCompat.END)) {
            mainActivity.closeDrawers()
        } else {
            super.onBackPressed()
        }
    }
}