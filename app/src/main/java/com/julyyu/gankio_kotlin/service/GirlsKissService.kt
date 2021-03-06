package com.julyyu.gankio_kotlin.service

import android.Manifest
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.IBinder
import android.text.TextUtils
import com.julyyu.gankio_kotlin.http.ApiFactory
import com.julyyu.gankio_kotlin.http.GankResponse
import com.julyyu.gankio_kotlin.util.PermissionUtil
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import rx.Observable
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

/**
 * Created by julyyu on 2017/9/5.
 */
class GirlsKissService : Service() {


    companion object {
        val CALL_GIRL : String ?= "android.appwidget.action.CALL_GIRL"
        val TAKE_GIRL : String ?= "android.appwidget.action.TAKE_GIRL"
    }
    override fun onBind(intent: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent!!.action){
            TAKE_GIRL ->{
                takeGirlHand(intent)
            }
            CALL_GIRL ->{
                callGirlPhone()
            }
        }
        return super.onStartCommand(intent, Service.START_REDELIVER_INTENT, startId)
    }
    fun callGirlPhone(){
        ApiFactory.getGankApi()
                .getRandomGirl()
                .enqueue(object : Callback<GankResponse> {
                    override fun onResponse(call: Call<GankResponse>?, response: Response<GankResponse>?) {
                        if(response!!.isSuccessful){
                            var singleLady = response.body()!!.results!![0].url
                            val girl = Intent("takeGirl")
                            girl.putExtra("girl", singleLady)
                            sendBroadcast(girl)
                        }
                    }

                    override fun onFailure(call: Call<GankResponse>?, t: Throwable?) {
                    }

                })
    }
    fun takeGirlHand(intent: Intent){
        val girlPhone = intent?.getStringExtra("girl")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            if (!PermissionUtil().checkingPermissionRegister(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                val girl = Intent("refusePermission")
                sendBroadcast(girl)
            }else{
                kissGirl(girlPhone)
            }
        }else{
            kissGirl(girlPhone)
        }


    }

    fun kissGirl(girlPhone : String){
        Observable.just<String>(girlPhone)
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map {
                    it -> !TextUtils.isEmpty(girlPhone)
                }
                .subscribe {
                    if(it){
                        var filePath : String = ""
                        val file = File(Environment.getExternalStorageDirectory(),"Girls")
                        if(!file.exists()){
                            try {
                                file.mkdirs()
                                filePath = file.absolutePath
                            }catch (e : Exception){
                                e.printStackTrace()
                            }
                        }else{
                            filePath = file.absolutePath
                        }
                        var input : InputStream?= null
                        var output : OutputStream?= null
                        var girl : File?= null
                        var girlName = girlPhone!!.toLowerCase().split("/".toRegex()).dropLastWhile ( {it.isEmpty()}).toTypedArray()
                        try {
                            var girlPhone = URL(girlPhone)
                            var con = girlPhone.openConnection()
                            con.connectTimeout = 5 * 1000
                            input = con.getInputStream()
                            var bytes = ByteArray(1024)
                            girl = File(filePath,girlName[girlName.size - 1])
                            output = FileOutputStream(girl)
                            var length : Int = 0
                            do {
                                length = input!!.read(bytes)
                                if(length == -1){
                                    continue
                                }
                                output.write(bytes,0,length)
                            }while (length != -1)
                            val girl = Intent("kissGirl")
                            girl.putExtra("msg","妹子送到相册了")
                            sendBroadcast(girl)
                        }catch (e : Exception){
                            e.printStackTrace()
                            val girl = Intent("kissGirl")
                            girl.putExtra("msg","妹子没送到相册了")
                            sendBroadcast(girl)
                        }finally {
                            try {
                                if(input != null) input.close()
                                if(output != null) output.close()
                            }catch (e : Exception){
                                e.printStackTrace()
                            }
                            val uri = Uri.fromFile(girl)
                            val scannerIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,uri)
                            sendBroadcast(scannerIntent)
                        }
                        stopSelf()
                    } else{
                        stopSelf()
                    }
                }
    }
}