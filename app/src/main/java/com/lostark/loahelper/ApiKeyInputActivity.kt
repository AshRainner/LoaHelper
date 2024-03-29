package com.lostark.loahelper

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.lostark.api.LoaRetrofitObj
import com.lostark.database.AppDatabase
import com.lostark.database.table.Key
import com.lostark.dto.news.NoticeItem
import retrofit2.Response


class ApiKeyInputActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.api_key_input_activity)

        val apiEditText = findViewById<EditText>(R.id.api_key_input_edit)

        val insertButton = findViewById<Button>(R.id.api_key_insert_button)
        insertButton.setOnClickListener {
            Thread{
                apiKeyCheck(apiEditText.text.toString())
            }.start()
        }


    }
    private fun apiKeyCheck(key:String) {
        val accept = "application/json"
        val call = LoaRetrofitObj.getRetrofitService().getNotice(accept,"bearer "+key)
        val response: Response<MutableList<NoticeItem>> = call.execute()

        val errorCode = response.code()
        val apiStatusText = findViewById<TextView>(R.id.api_key_status_text)
        val db = AppDatabase.getInstance(applicationContext)!!
        when (errorCode) {
            200 -> {
                Log.d("올바른 키입니다.", "errorCode: ")
                db.keyDao().insertKey(Key("bearer "+key))
                val intent = Intent(this,StartActivity::class.java)
                startActivity(intent)
                finish()
            }
            401 -> {
                apiStatusText.text = "올바르지 않은 키 입니다. 다시 입력해주세요."
                Log.d("올바르지 않은 키입니다.", "errorCode: ")
                db.keyDao().deleteAllKey()
            }
            503 -> {
                apiStatusText.text = "서버가 점검 중입니다. 나중에 다시 시도해주세요."
                Log.d("서버가 점검 중입니다.", "errorCode")
            }
        }
    }

}