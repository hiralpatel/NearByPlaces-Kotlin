package com.hpandro.nearbyplace.adapter

import android.content.Context
import android.content.Intent
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide
import com.hpandro.nearbyplace.R
import com.hpandro.nearbyplace.activity.ShowMapActivity
import com.hpandro.nearbyplace.model.Result
import kotlinx.android.synthetic.main.row_data_list.view.*

class DataAdapter(private var listValues: ArrayList<Result>, private val context: Context) : RecyclerView.Adapter<ViewHolder>() {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(context).inflate(R.layout.row_data_list, parent, false))
    }

    override fun getItemCount(): Int {
        return listValues.size
    }

    private var isFooter: Boolean = false

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder?.tvAnimalType?.text = listValues[position].name
        holder?.ratings?.rating = listValues[position].rating.toFloat()

        if (!listValues[position].icon.isEmpty())
            Glide.with(context).load(listValues[position].icon).into(holder?.ivIcon)
        else
            Glide.with(context).load("https://www.freeiconspng.com/minicovers/error-icon-32.png").into(holder?.ivIcon)

        holder.relMain.setOnClickListener({
            val mainIntent = Intent(context, ShowMapActivity::class.java)
            mainIntent.putExtra("title", listValues[position].name)
            mainIntent.putExtra("add", listValues[position].vicinity)
            mainIntent.putExtra("icon", listValues[position].icon)
            mainIntent.putExtra("ratings", listValues[position].rating)
            mainIntent.putExtra("lat", listValues[position].geometry.location.lat)
            mainIntent.putExtra("lng", listValues[position].geometry.location.lng)
            context.startActivity(mainIntent)
        })

        isFooter = listValues.size / 2 == position
    }

    fun isLoading(): Boolean {
        return isFooter
    }

    fun addItems(mListValues: ArrayList<Result>) {
        listValues = mListValues
    }
}

class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val tvAnimalType = view.tvTitle!!
    val relMain = view.relMain
    val ivIcon = view.ivIcon
    val ratings = view.ratings
    val tvStatus = view.tvStatus
}