package com.julyyu.gankio_kotlin.ui

import android.content.Intent
import android.os.Bundle
import android.support.design.widget.Snackbar
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import butterknife.bindView
import com.julyyu.gankio_kotlin.R
import com.julyyu.gankio_kotlin.adapter.GankAdapter
import com.julyyu.gankio_kotlin.http.ApiFactory
import com.julyyu.gankio_kotlin.model.Gank
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers

/**
 * Created by JulyYu on 2017/4/26.
 */
class GankFragment: Fragment(){

    internal var view: View? = null
    internal val swipeFreshLayout: SwipeRefreshLayout by bindView(R.id.swipelayout)
    internal val recyclerView: RecyclerView by bindView(R.id.recycler)
    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        view = LayoutInflater.from(activity).inflate(R.layout.view_recycler,null)
        return view
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recyclerView.layoutManager = LinearLayoutManager(activity, LinearLayoutManager.VERTICAL,false)
        swipeFreshLayout.isRefreshing = true
        loadMore()
        swipeFreshLayout.setOnRefreshListener(object : SwipeRefreshLayout.OnRefreshListener{
            override fun onRefresh() {
                loadMore()
            }

        })
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
    fun loadMore(){
        ApiFactory.getGankApi()
                .getGankHistory()
                .observeOn(Schedulers.io())
                .subscribeOn(Schedulers.io())
                .subscribe(
                        {
                            if(!it.error) {
                                val date: String? = it.results?.get(0)
                                ApiFactory.getGankApi()
                                        .getGankDayDate(date?.replace("-","/")!!)
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe {
                                            swipeFreshLayout.isRefreshing = false
                                            if(!it.error){
                                                val ganks = ArrayList<Gank>()
                                                if(it.results!!.Android != null){
                                                    ganks.addAll(it.results!!.Android!!)
                                                }
                                                if(it.results!!.iOS != null){
                                                    ganks.addAll(it.results!!.iOS!!)
                                                }
                                                if(it.results!!.拓展资源 != null){
                                                    ganks.addAll(it.results!!.拓展资源!!)
                                                }
                                                if(it.results!!.福利 != null){
                                                    ganks.addAll(it.results!!.福利!!)
                                                }
                                                recyclerView.adapter = GankAdapter(ganks)
                                            }
                                        }
                            }
                        },
                        {
                            error ->
                            swipeFreshLayout.isRefreshing = false
                            Snackbar.make(swipeFreshLayout,"日常加载失败",Snackbar.LENGTH_SHORT).show()
                            Log.i("ERROR",error.message)
                        }
                )
    }

}