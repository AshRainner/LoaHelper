package com.lostark.loahelper

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.lostark.api.LoaRetrofitObj
import com.lostark.database.AppDatabase
import com.lostark.database.dao.EventsDAO
import com.lostark.database.dao.ItemsDAO
import com.lostark.database.dao.NoticeDAO
import com.lostark.database.table.*
import com.lostark.dto.auctions.AuctionsBody
import com.lostark.dto.markets.MarketsBody
import com.lostark.dto.news.NoticeItem
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class StartActivity() : AppCompatActivity() {
    val ACCEPT = "application/json"
    val CONTENTTYPE = "application/json"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.start_activity)

        val intent = Intent(this@StartActivity, MainActivity::class.java)
        val db = AppDatabase.getInstance(applicationContext)!!
        if (db.keyDao().getKey() != null) {//키가 비어 있지 않다면
            val key = db.keyDao().getKey()
            val time = LoaTimeCheck()

            val recentTimeList = db.updateDAO().getRecentUpdate()!!
            var recentTime:String?=null
            if (recentTimeList.isNotEmpty()) {
                recentTime = db.updateDAO().getRecentUpdate()!!.get(0).recentUpdate
            }
            else{
                db.updateDAO().insertUpdate(UpdateT(time.split('T')[0]))
                val recentTimeList = db.updateDAO().getRecentUpdate()!!
                recentTime = recentTimeList.get(0).recentUpdate
            }

            if (compareToDate(time, recentTime)) {
                Log.d("time : ", time)
                Log.d("recentTime : ", recentTime)
                db.eventsDao().deleteAllEvents()
                Thread {
                    val code = apiKeyCheck(key)
                    if(code==200) {
                        //insertCraftItem(db.itemsDAO(), key)
                        Log.d("맵 아이템",insertMapItems(db.itemsDAO(),key).toString())
                        intent.putExtra("MapItemList",insertMapItems(db.itemsDAO(),key))
                        intent.putExtra("Lv1Gem", insertMapGemItems(db.itemsDAO(),key))
                        intent.putExtra("EventList", insertEvents(db.eventsDao(), key))
                        intent.putExtra("StoneList", insertStoneItems(db.itemsDAO(), key))
                        intent.putExtra(
                            "Destruction",
                            insertDestructionStone(db.itemsDAO(), key)
                        )
                        intent.putExtra("NoticeList", insertNotice(db.noticeDAO(), key))
                        intent.putExtra("Key", key)
                        db.updateDAO().deleteUpdateAll()
                        db.updateDAO().insertUpdate(UpdateT(time.split('T')[0]))
                        startActivity(intent)
                        finish()
                    }
                    else if(code==503){
                        //서버점검
                    }
                    else if(code== 401){
                        val intent = Intent(this,ApiKeyInputActivity::class.java)
                        db.keyDao().deleteAllKey()
                        startActivity(intent)
                        finish()
                    }
                }.start()
            }
        } else {
            //처음에 key가 없을 시 사용자에게 입력 받아야함
            val intent = Intent(this,ApiKeyInputActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun apiKeyCheck(key:String):Int {
        val accept = "application/json"
        val call = LoaRetrofitObj.getRetrofitService().getNotice(accept, key)
        val response: Response<MutableList<NoticeItem>> = call.execute()
        val errorCode = response.code()
        return errorCode
    }

    private fun insertCraftItem(itemsDao: ItemsDAO,key:String):ArrayList<CraftItems>{
        //배틀아이템 60000
        var pageNo=1
        itemsDao.deleteAllCraftItems()
        do {
            val itemBody = MarketsBody("GRADE", 60000, null, null, null, null, pageNo, "ASC")
            val call =
                LoaRetrofitObj.getRetrofitService().getItemsInfo(ACCEPT, key, CONTENTTYPE, itemBody)
            var itemList = call.execute().body()!!
            itemList.items?.forEach{
                var item = CraftItems(it.id,it.name,it.iconUrl,it.bundleCount,it.yDayAvgPrice)
                itemsDao.insertCraftItems(item)
            }
            pageNo++
        }while(itemList.totalCount>itemList.pageNo*itemList.pageSize)
        return ArrayList(itemsDao.getCraftItemList())
    }

    private fun insertNotice(noticeDao:NoticeDAO,key:String):ArrayList<Notice>{
        val call = LoaRetrofitObj.getRetrofitService().getNotice(ACCEPT, key)
        val noticeList = call.execute().body()!!
        noticeList.forEach{
            var notice = Notice(it.link,it.title,it.date,it.type)
            noticeDao.insertNotice(notice)
        }
        return ArrayList(noticeDao.getNoticeList())
    }

    private fun insertMapItems(itemsDao: ItemsDAO,key: String):ArrayList<Items>{
        val mapItemsBody = MarketsBody("GRADE",50000,null,3,null,"태양의",0,"ASC")
        val call = LoaRetrofitObj.getRetrofitService().getItemsInfo(ACCEPT,key,CONTENTTYPE,mapItemsBody)
        val marketsList = call.execute().body()!!
        marketsList.items?.forEach(){
            var item = Items(it.id,it.name,it.iconUrl,it.yDayAvgPrice)
            itemsDao.insertItems(item)
        }
        val returnList = ArrayList<Items>()
        returnList.addAll(itemsDao.getSelectItemList("태양의").sortedBy { it.id })
        returnList.add(insertMapFragmentsItems(itemsDao, key))
        return returnList
    }
    private fun insertMapFragmentsItems(itemsDao: ItemsDAO,key:String):Items{
        val mapItemsBody = MarketsBody("GRADE",50000,null,3,null,"명예의 파편 주머니(대)",0,"ASC")
        val call = LoaRetrofitObj.getRetrofitService().getItemsInfo(ACCEPT,key,CONTENTTYPE,mapItemsBody)
        val marketsList = call.execute().body()!!
        marketsList.items?.forEach(){
            var item = Items(it.id,it.name,it.iconUrl,it.yDayAvgPrice)
            itemsDao.insertItems(item)
        }
        Log.d("명파 주머니", itemsDao.getSelectItem("명예의 파편 주머니(대)").toString())
        return itemsDao.getSelectItem("명예의 파편 주머니(대)")
    }

    private fun insertMapGemItems(itemsDao: ItemsDAO, key: String): GemItems{
        val searchBody = AuctionsBody("BUY_PRICE",210000,null,3,null,"1레벨",0,"ASC")
        //stoneBody == 돌파석 검색용 바디
        val call = LoaRetrofitObj.getRetrofitService().getAuctionItemsInfo(ACCEPT,key,CONTENTTYPE,searchBody)
        val marketsList = call.execute().body()!!
        Log.d("!@#!@#", marketsList.items.toString())
        val gemData = marketsList.items.get(0)
        val gem = GemItems(gemData.name,gemData.grade,gemData.auctionInfo.buyPrice!!,gemData.auctionInfo.tradeAllowCount)
        itemsDao.deleteAllGemItems()
        itemsDao.insertGemItems(gem)
        return itemsDao.getSelectGemItemList("1레벨")
    }


    private fun insertStoneItems(itemsDao: ItemsDAO, key: String): ArrayList<Items>{
        val stoneBody = MarketsBody("GRADE",50000,null,3,"희귀"," 명예의 돌파석",0,"ASC")
        //stoneBody == 돌파석 검색용 바디
        val call = LoaRetrofitObj.getRetrofitService().getItemsInfo(ACCEPT,key,CONTENTTYPE,stoneBody)
        val marketsList = call.execute().body()!!
        marketsList.items?.forEach {
            var item = Items(it.id, it.name, it.iconUrl, it.yDayAvgPrice)
            itemsDao.insertItems(item)
        }
        return ArrayList(itemsDao.getSelectItemList("돌파석").sortedBy { it.id })
    }

    private fun insertDestructionStone(itemsDao: ItemsDAO, key: String): ArrayList<Items>{
        var destructionBody = MarketsBody("GRADE",50000,null,3,null,"파괴석 결정",0,"ASC")
        var call = LoaRetrofitObj.getRetrofitService().getItemsInfo(ACCEPT,key,CONTENTTYPE,destructionBody)
        var marketsList = call.execute().body()!!

        marketsList.items?.forEach{
            var item = Items(it.id,it.name,it.iconUrl,it.yDayAvgPrice)
            itemsDao.insertItems(item)
        }
        destructionBody = MarketsBody("GRADE",50000,null,3,null,"파괴강석",0,"ASC")
        call = LoaRetrofitObj.getRetrofitService().getItemsInfo(ACCEPT,key,CONTENTTYPE,destructionBody)
        marketsList = call.execute().body()!!

        marketsList.items?.forEach {
            var item = Items(it.id,it.name,it.iconUrl,it.yDayAvgPrice)
            itemsDao.insertItems(item)
        }
        return ArrayList(itemsDao.getSelectItemList("파괴").sortedBy { it.id })
    }

    private fun insertEvents(eventsDao: EventsDAO, key: String): ArrayList<LoaEvents> {
        val call = LoaRetrofitObj.getRetrofitService().getEvent(ACCEPT, key)
        val eventList = call.execute().body()
        eventList?.forEach {//엘비스 연산 list가 null이 아니면 forEach실행 null일시 뒤에 있는 log.d실행
            eventsDao.insertEvents(LoaEvents(it.link, it.title, it.thumbnail))
        }
        return eventsDao.getEventList() as ArrayList<LoaEvents>
    }

    private fun compareToDate(time: String, recentTime: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM-dd")
        val timeDate = sdf.parse(time.split("T").get(0))
        val recentTimeDate = sdf.parse(recentTime.split("T").get(0))
        val cmp = timeDate.compareTo(recentTimeDate)
        return when {
            cmp > 0 -> {
                true
            }
            cmp < 0 -> {
                false
            }
            else -> {
                true
            }
        }
    }

    private fun LoaTimeCheck(): String {
        val cal = Calendar.getInstance()
        val sdf = SimpleDateFormat("yyyy-MM-dd'T'HH")
        cal.time = Date()
        if (sdf.format(cal.time).toString().split('T')[1].toInt() < 10)
            cal.add(Calendar.DATE, -1)
        Log.d("LoaTimeCheck", cal.time.toString())
        return sdf.format(cal.time)
    }

}