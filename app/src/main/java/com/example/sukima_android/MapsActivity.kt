package com.example.sukima_android

import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.model.LatLng
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.widget.TextView

import com.google.maps.android.SphericalUtil.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.activity_time_layout.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener {

    override fun onMarkerClick(p0: Marker?) = false


    private lateinit var map: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient


    //拡大縮小機能の値
    enum class Zoom(val value: Float) {
        Min(14.0f),
        Max(19.0f)
    }


    //fused locationのデフォルトであるやつ
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        val intent: Intent = getIntent()
        val message: String = intent.getStringExtra(EXTRA_MESSAGE)
        val textView: TextView = findViewById(R.id.sukimaTime)
        textView.text = message


    }

    //位置情報の権限
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    //位置情報の権限
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
            return
        }
    }


    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */


    override fun onMapReady(googleMap: GoogleMap) {


        map = googleMap

        setUpMap()

        map.isMyLocationEnabled = true //位置情報取得可能

        map.getUiSettings().setZoomControlsEnabled(true)

        map.mapType = GoogleMap.MAP_TYPE_NONE  //mapを消せる(最後にコメントアウト消す)

        map.uiSettings.isMapToolbarEnabled = false

        //拡大縮小機能
        map = googleMap.apply {
            setMaxZoomPreference(Zoom.Max.value)
            setMinZoomPreference(Zoom.Min.value)
        }

        //スクロール、チルト、回転を無効
        map.uiSettings.run {
            isScrollGesturesEnabled = false
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = false
            isZoomGesturesEnabled = false
        }

        //fused locationの機能(位置情報取得)
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                currentLatLng(currentLatLng)
            }

            val point_new = arrayOfNulls<LatLng>(3)
            point_new[0] = LatLng(41.8417846, 140.7675603)
            point_new[1] = LatLng(41.8415718222, 140.767425299)
            point_new[2] = LatLng(41.8395136176, 140.76271534)

            val distance:MutableList<Int> = mutableListOf()

            point_new.forEachIndexed { i, point ->
                val LatLngA = LatLng(location.latitude, location.longitude)


                distance.add(computeDistanceBetween(LatLngA, point).toInt())

                val marker: Marker =
                    map.addMarker(point?.let {
                        MarkerOptions()
                            .position(it)
                            .title("${distance[i]} m")
                            .snippet("${distance[i]/ 80}分")
                            .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot4))
                    }


                    )
                setOnMarkerClick(marker)
                Check(distance[i])
            }
        }

    }

    private var selectedMarker : Marker?=null

    private fun setOnMarkerClick(marker:Marker?):Boolean {
        if (marker == selectedMarker) {
            selectedMarker = null
            return true
        }
        selectedMarker = marker
        return false
    }


    fun currentLatLng(currentLatLng : LatLng) {
        current_btn.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
        }
    }

    private fun Check(distance: Int) {

        if (distance < 5) {
            val intent = Intent(this, CheckIn::class.java)
            startActivity(intent)
        }
    }
}


