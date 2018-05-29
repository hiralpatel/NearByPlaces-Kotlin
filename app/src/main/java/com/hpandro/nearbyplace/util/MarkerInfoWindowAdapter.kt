package com.hpandro.nearbyplace.util

import android.content.Context
import android.graphics.Point
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.RatingBar
import android.widget.RelativeLayout
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import com.hpandro.nearbyplace.R


class MarkerInfoWindowAdapter(context: Context) : GoogleMap.InfoWindowAdapter {
    private val context: Context = context.applicationContext

    override fun getInfoWindow(arg0: Marker): View? {
        return null
    }

    override fun getInfoContents(mMarker: Marker): View {
        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        val v = inflater.inflate(R.layout.map_marker_info_window, null)

        // Set info window width
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val display = wm.defaultDisplay
        val size = Point()
        display.getSize(size)

        v.layoutParams = RelativeLayout.LayoutParams(size.x, RelativeLayout.LayoutParams.WRAP_CONTENT)

        val tvTitle = v.findViewById(R.id.tvTitle) as TextView
        val tvDetails = v.findViewById(R.id.tvDetails) as TextView
        val ratings = v.findViewById(R.id.ratings) as RatingBar

        tvTitle.text = mMarker.title
        tvDetails.text = mMarker.snippet.substringBeforeLast("|")
        ratings.rating = mMarker.snippet.substringAfterLast("|").toFloat()
        return v
    }
}