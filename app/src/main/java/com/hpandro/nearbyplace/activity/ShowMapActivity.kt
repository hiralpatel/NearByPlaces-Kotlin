package com.hpandro.nearbyplace.activity

import android.graphics.BitmapFactory
import android.os.AsyncTask
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.View
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.hpandro.nearbyplace.R
import com.hpandro.nearbyplace.util.MarkerInfoWindowAdapter
import org.jetbrains.anko.toast
import java.net.URL


class ShowMapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_maps)

        val mapFragment = supportFragmentManager
                .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        toolbar.setNavigationOnClickListener { finish() }
    }

    override fun onMapReady(googleMap: GoogleMap?) {
        mMap = googleMap!!

        if (!intent.hasExtra("lat") || !intent.hasExtra("lng")) {
            toast("no values")
            return
        }

        val locationOnMap = LatLng(intent.getDoubleExtra("lat", 0.0), intent.getDoubleExtra("lng", 0.0))
        mMap.addMarker(MarkerOptions().position(locationOnMap)
                .title(intent.getStringExtra("title"))
                .snippet(intent.getStringExtra("add") + "|" + intent.getDoubleExtra("ratings", 0.0)))
        toolbar.title = intent.getStringExtra("title")
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(locationOnMap, 15.0f))

        val markerInfoWindowAdapter = MarkerInfoWindowAdapter(applicationContext)
        googleMap.setInfoWindowAdapter(markerInfoWindowAdapter)
    }

    private inner class Connection : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void?): Void? {
            val url = URL(intent.getStringExtra("icon"))
            runOnUiThread({
                run {
                    val bmp = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    mMap.addMarker(MarkerOptions()
                            .icon(BitmapDescriptorFactory.fromBitmap(bmp)))
                }
            })
            return null
        }
    }
}