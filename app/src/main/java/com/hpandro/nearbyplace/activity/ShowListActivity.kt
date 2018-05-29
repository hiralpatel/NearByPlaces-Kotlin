package com.hpandro.nearbyplace.activity

import android.Manifest
import android.annotation.SuppressLint
import android.app.ProgressDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.provider.Settings
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v7.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.LinearLayout
import android.widget.ProgressBar
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.api.GoogleApiClient
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.LatLng
import com.google.gson.Gson
import com.hpandro.nearbyplace.R
import com.hpandro.nearbyplace.adapter.DataAdapter
import com.hpandro.nearbyplace.model.ResponseData
import com.hpandro.nearbyplace.model.Result
import org.jetbrains.anko.selector
import org.jetbrains.anko.toast
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class ShowListActivity : AppCompatActivity(), GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener, com.google.android.gms.location.LocationListener {

    private val listValues: ArrayList<Result> = ArrayList()
    private val permissionsLocationRequest = 99

    private var mGoogleApiClient: GoogleApiClient? = null
    private var mLocation: Location? = null
    private var mLocationManager: LocationManager? = null

    private var mLocationRequest: LocationRequest? = null
    private val updateInterval = (2 * 1000).toLong()  /* 10 secs */
    private val fastestInterval: Long = 2000 /* 2 sec */

    private var locationManager: LocationManager? = null
    private var latLng: LatLng? = null
    lateinit var toolbar: Toolbar
    lateinit var mRecyclerView: RecyclerView
    lateinit var llNoData: LinearLayout
    lateinit var mProgress: ProgressBar

    private var linearLayoutManager: LinearLayoutManager? = null
    private val lastVisibleItemPosition: Int
        get() = linearLayoutManager!!.findLastVisibleItemPosition()

    private val isLocationEnabled: Boolean
        get() {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            return locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER) || locationManager!!.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_show_list)

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            checkLocationPermission()
        }

        llNoData = findViewById<View>(R.id.llNoData) as LinearLayout
        toolbar = findViewById<View>(R.id.toolbar) as Toolbar
        setSupportActionBar(toolbar)

        mProgress = findViewById<View>(R.id.mProgress) as ProgressBar
        mRecyclerView = findViewById<RecyclerView>(R.id.mRecyclerView) as RecyclerView

        linearLayoutManager = LinearLayoutManager(this@ShowListActivity)
        linearLayoutManager!!.orientation = LinearLayoutManager.VERTICAL
        mRecyclerView.layoutManager = linearLayoutManager

        adapter = DataAdapter(listValues, this@ShowListActivity)
        mRecyclerView.adapter = adapter

        mGoogleApiClient = GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build()

        mLocationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        checkLocation()
        if (checkLocationPermission())
            showPopup()

        mRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView?, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                if (!adapter!!.isLoading()) {
                    if (linearLayoutManager!!.findLastCompletelyVisibleItemPosition() >= linearLayoutManager!!.itemCount - 1) {
                        val handler = Handler()
                        handler.postDelayed({
                            if (token != null) {
                                mProgress.visibility = View.VISIBLE
                                val url = getUrl(latLng!!.latitude, latLng!!.longitude, type, token)
                                GetNearbyPlacesData(this@ShowListActivity, "").execute(url)
                            }
                        }, 500)
                    }
                }
            }
        })
    }

    /**
     * Create menu
     *
     */
    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return super.onCreateOptionsMenu(menu)
    }

    /**
     * Menu Item click
     */
    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            R.id.menuCategory -> {
                showPopup()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
        return true
    }

    private var type: String = ""

    private var isCategoryChange: Boolean = false

    private fun showPopup() {
        val companies = listOf("Restaurant", "Hospital", "School")
        selector("What are you looking for?", companies, { _, i ->
            if (latLng != null) {
                listValues.clear()
                isCategoryChange = true
                toolbar.title = "All " + companies[i] + "s"
                type = companies[i].toLowerCase()
                val url = getUrl(latLng!!.latitude, latLng!!.longitude, type, "")
                GetNearbyPlacesData(this@ShowListActivity, "Fetching nearby ${companies[i]}s.").execute(url)
            } else {
                finish()
                val intent = Intent(this, ShowListActivity::class.java)
                startActivity(intent)
                overridePendingTransition(0, 0)
            }
        })
    }

    private fun checkLocation(): Boolean {
        if (!isLocationEnabled)
            showAlert()
        return isLocationEnabled
    }

    /***
     * Show popup menu to select categories
     */
    private fun showAlert() {
        val dialog = AlertDialog.Builder(this)
        dialog.setTitle("Enable Location")
                .setMessage("Your Locations Settings is set to 'Off'.\nPlease Enable Location to " + "use this app")
                .setPositiveButton("Location Settings") { _, _ ->
                    val myIntent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                    startActivity(myIntent)
                }
                .setNegativeButton("Cancel") { _, _ -> }
        dialog.show()
    }

    /**
     * Check for permission of location
     */
    private fun checkLocationPermission(): Boolean {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        permissionsLocationRequest)
            } else {
                ActivityCompat.requestPermissions(this,
                        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                        permissionsLocationRequest)
            }
            return false
        } else {
            return true
        }
    }

    /**
     * On result of permission request
     */
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>, grantResults: IntArray) {
        when (requestCode) {
            permissionsLocationRequest -> {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    if (ContextCompat.checkSelfPermission(this,
                                    Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        //TODO DO STUFF HERE
                        finish()
                        val intent = Intent(this, ShowListActivity::class.java)
                        startActivity(intent)
                        overridePendingTransition(0, 0)
                    }
                } else {
//                    toast("Permission Denied!")
                }
                return
            }
        }
    }

    /**
     * Generate URL for find details
     */
    private fun getUrl(latitude: Double, longitude: Double, nearbyPlace: String, token: String): String {
        val googlePlacesUrl = StringBuilder("https://maps.googleapis.com/maps/api/place/nearbysearch/json?")
        googlePlacesUrl.append("location=$latitude,$longitude")
        googlePlacesUrl.append("&radius=10000")
        googlePlacesUrl.append("&type=$nearbyPlace")
        googlePlacesUrl.append("&sensor=true")
        googlePlacesUrl.append("&key=" + getString(R.string.google_maps_key))
        if (token != null && !token.isEmpty())
            googlePlacesUrl.append("&pagetoken=$token")
        Log.d("getUrl", googlePlacesUrl.toString())
        return googlePlacesUrl.toString()
    }

    override fun onConnected(p0: Bundle?) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        startLocationUpdates()
        mLocation = LocationServices.FusedLocationApi.getLastLocation(mGoogleApiClient)
        if (mLocation == null) {
            startLocationUpdates()
        }
        if (mLocation != null) {
            latLng = LatLng(mLocation!!.latitude, mLocation!!.longitude)
        } else {
            toast("Location not Detected")
        }
    }

    @SuppressLint("MissingPermission")
    private fun startLocationUpdates() {
        // Create the location request
        mLocationRequest = LocationRequest.create()
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                .setInterval(updateInterval)
                .setFastestInterval(fastestInterval)
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient,
                mLocationRequest, this)
    }

    override fun onConnectionSuspended(i: Int) {
        Log.i("TAG", "Connection Suspended")
        mGoogleApiClient!!.connect()
    }

    override fun onConnectionFailed(connectionResult: ConnectionResult) {
        Log.i(":TAG", "Connection failed. Error: " + connectionResult.errorCode)
    }

    override fun onLocationChanged(location: Location?) {
        latLng = LatLng(location!!.latitude, location!!.longitude)
    }

    override fun onStart() {
        super.onStart()
        if (mGoogleApiClient != null) {
            mGoogleApiClient!!.connect()
        }
    }

    override fun onStop() {
        super.onStop()
        if (mGoogleApiClient!!.isConnected) {
            mGoogleApiClient!!.disconnect()
        }
    }

    private var token: String = ""

    private var adapter: DataAdapter? = null

    /**
     * ASYNC task for getting nearby place
     */
    inner class GetNearbyPlacesData(val mContext: ShowListActivity, val message: String) : AsyncTask<String, String, String>() {
        private val CONNECTON_TIMEOUT_MILLISECONDS = 60000

        private var dialog: ProgressDialog? = null

        override fun onPreExecute() {
            llNoData.visibility = View.GONE
            dialog = ProgressDialog(mContext)
            dialog!!.setMessage(message)
            dialog!!.setCancelable(false)
            dialog!!.isIndeterminate = true
            if (message.isNotEmpty())
                dialog!!.show()
        }

        private var inString: String = ""

        override fun doInBackground(vararg urls: String?): String {
            var urlConnection: HttpURLConnection? = null
            try {
                val url = URL(urls[0])
                urlConnection = url.openConnection() as HttpURLConnection
                urlConnection.connectTimeout = CONNECTON_TIMEOUT_MILLISECONDS
                urlConnection.readTimeout = CONNECTON_TIMEOUT_MILLISECONDS

                inString = streamToString(urlConnection.inputStream)
                publishProgress(inString)
            } catch (ex: Exception) {
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect()
                }
            }
            return inString
        }

        override fun onPostExecute(result: String) {
            if (dialog!!.isShowing)
                dialog!!.dismiss()
            val gson = Gson()
            val locationData = gson?.fromJson(result, ResponseData::class.java)
            token = locationData.next_page_token

            if (locationData.results.isNotEmpty())
                listValues.addAll(locationData.results)

            if (listValues.size == 0)
                llNoData.visibility = View.VISIBLE
            else
                llNoData.visibility = View.GONE

            adapter!!.addItems(listValues)
            adapter!!.notifyDataSetChanged()

            if (lastVisibleItemPosition != -1) {
                Handler().postDelayed({ mRecyclerView.smoothScrollToPosition(lastVisibleItemPosition) }, 200)
            }
            mProgress.visibility = View.GONE
        }

        private fun streamToString(inputStream: InputStream): String {
            val bufferReader = BufferedReader(InputStreamReader(inputStream))
            var line: String
            var result = ""
            try {
                do {
                    line = bufferReader.readLine()
                    if (line != null) {
                        result += line
                    }
                } while (line != null)
                inputStream.close()
            } catch (ex: Exception) {
            }
            return result
        }
    }
}