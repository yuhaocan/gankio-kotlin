package com.julyyu.gankio_kotlin.ui

import android.Manifest
import android.annotation.SuppressLint
import android.app.WallpaperManager
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.support.design.widget.FloatingActionButton
import android.support.design.widget.Snackbar
import android.support.v4.view.PagerAdapter
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.*
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.GlideBitmapDrawable
import com.julyyu.gankio_kotlin.R
import com.julyyu.gankio_kotlin.model.Girl
import com.julyyu.gankio_kotlin.util.DialogUtil
import com.julyyu.gankio_kotlin.util.PermissionUtil
import kotlinx.android.synthetic.main.activity_girls.*
import rx.Observable
import rx.schedulers.Schedulers
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.URL

class GirlsActivity : AppCompatActivity(){

//    val viewPager : ViewPager by bindView(R.id.viewpager)

    var girlsAdapter : PagerAdapter ? = null
    var lookGirls : Array<ImageView?> ?= null
    var train : ArrayList<Girl> ?= null
    var flag : Boolean = true
    var alertDialog : AlertDialog ?= null
    @SuppressLint("RestrictedApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_girls)
        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar!!.setDefaultDisplayHomeAsUpEnabled(true)
        hideOrShowToolBar()
        val fab = findViewById<FloatingActionButton>(R.id.fab)
        fab.setOnClickListener { view ->
            this.finish()
        }

        train = intent.getParcelableArrayListExtra<Girl>("girls")
        var position = intent.getIntExtra("position",0)
        lookGirls = arrayOfNulls<ImageView>(train!!.size)
        girlsAdapter = object : PagerAdapter(){

            override fun instantiateItem(container: ViewGroup, position: Int): Any {
                val lookGirl = ImageView(this@GirlsActivity)
                val layout = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,ViewGroup.LayoutParams.MATCH_PARENT)
                lookGirl.adjustViewBounds = true
                lookGirl.layoutParams = layout
                Glide.with(this@GirlsActivity)
                        .load(train!![position].girlHome)
                        .fitCenter()
                        .into(lookGirl)
                container!!.addView(lookGirl)
                lookGirls!![position] = lookGirl
                return lookGirl
            }

            override fun isViewFromObject(view: View, `object`: Any): Boolean {
                return view === `object`
            }

            override fun getCount(): Int {
                return train!!.size
            }

            override fun destroyItem(container: ViewGroup, position: Int, `object`: Any) {
                container!!.removeView(lookGirls!![position])
            }

        }
        viewpager.adapter = girlsAdapter
        viewpager.currentItem = position
        viewpager.setOnTouchListener(object : View.OnTouchListener{
            override fun onTouch(v: View?, event: MotionEvent?): Boolean {
                when(event!!.action){
                    MotionEvent.ACTION_DOWN -> {
                        flag = true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        flag = false
                        supportActionBar!!.hide()
                        hideSystemUi()
                    }
                    MotionEvent.ACTION_UP -> {
                        if(flag){
                            hideOrShowToolBar()
                        }
                    }
                }
                return false
            }
        })
    }

    private fun hideOrShowToolBar(){
        if(supportActionBar!!.isShowing){
            supportActionBar!!.hide()
            hideSystemUi()
        }else{
            supportActionBar!!.show()
            showSystemUi()
        }
    }

    private fun showSystemUi(){
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    }

    private fun hideSystemUi(){
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
                View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    }
    // 保存照片到本地
    private fun loveGirl() : Boolean{
        var boolContact : Boolean = true
        var filePath : String = ""
        val file = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),"Girls")
        if(!file.exists()){
            try {
//                if(file.parentFile == null){
//                    file.parentFile.mkdir()
//                }
                file.mkdirs()
                filePath = file.absolutePath
            }catch (e : Exception){
                e.printStackTrace()
            }
        }else{
            filePath = file.absolutePath
        }
        var input : InputStream ?= null
        var output : OutputStream ?= null
        var girl : File ?= null
        var sweetGirl = train!![viewpager!!.currentItem]
        var girlName = sweetGirl.girlHome.toLowerCase().split("/".toRegex()).dropLastWhile ( {it.isEmpty()}).toTypedArray()
        try {
            var girlPhone = URL(sweetGirl.girlHome)
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
        }catch (e : Exception){
            boolContact = false
            e.printStackTrace()
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
        return boolContact
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_girl,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {

        when(item!!.itemId){
            R.id.action_save -> {
                if (!PermissionUtil().checkingPermissionRegister(applicationContext, Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                    PermissionUtil().requestPermission(this, 2, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                }else{
                    kissGirl()
                }
            }
            R.id.action_wallpaper -> {
                val wallpaper = (lookGirls!![viewpager.currentItem]!!.drawable as GlideBitmapDrawable).bitmap
                var success = false
                Observable.just(wallpaper)
                        .subscribeOn(Schedulers.io())
                        .observeOn(Schedulers.io())
                        .map {
                            try {
                                WallpaperManager.getInstance(this).setBitmap(wallpaper)
                                success = true
                            }catch (e : Exception){
                                e.printStackTrace()
                                success = false
                            }
                        }.subscribe {
                            if(success){
                                Snackbar.make(viewpager,"壁纸设置成功",Snackbar.LENGTH_SHORT).show()
                            }else{
                                Snackbar.make(viewpager,"壁纸设置失败",Snackbar.LENGTH_SHORT).show()
                            }
                        }
            }
        }

        return super.onOptionsItemSelected(item)
    }
    fun kissGirl(){
        Observable.just("")
                .subscribeOn(Schedulers.io())
                .observeOn(Schedulers.io())
                .map {
                    it -> loveGirl()
                }
                .subscribe {
                    if(it){
                        Snackbar.make(viewpager,"妹子送到相册了",Snackbar.LENGTH_SHORT).show()
                    }else{
                        Snackbar.make(viewpager,"妹子没有送到相册",Snackbar.LENGTH_SHORT).show()
                    }
                }
    }
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (PermissionUtil().hasRejectPermission(grantResults)) run {
            alertDialog = DialogUtil().getPermissionDialog(this, packageName)
            alertDialog!!.show()
        }else{
            kissGirl()
        }
    }

}
