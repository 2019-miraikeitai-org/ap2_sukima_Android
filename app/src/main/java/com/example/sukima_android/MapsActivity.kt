package com.example.sukima_android

import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.location.Location
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import kotlinx.android.synthetic.main.activity_maps.*
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.model.LatLng
import androidx.core.app.ComponentActivity.ExtraData
import androidx.core.content.ContextCompat.getSystemService
import android.icu.lang.UCharacter.GraphemeClusterBreak.T
import android.widget.Toast


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


    }

    //位置情報の権限
    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1
    }

    //位置情報の権限
    private fun setUpMap() {
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), LOCATION_PERMISSION_REQUEST_CODE)
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

        /*map.setOnMarkerClickListener (object : OnMarkerClickListener{
             override fun onMarkerClick(marker: Marker): Boolean {
                 return true
             }
         })*/


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
            }

            fun LatLng.distanceBetween(toLatLang: LatLng): Float {
                val results = FloatArray(1)
                try {
                    Location.distanceBetween(
                        this.latitude, this.longitude,
                        toLatLang.latitude, toLatLang.longitude,
                        results
                    )
                } catch (e: IllegalArgumentException) {
                    return -1.0f
                }
                return results[0]
            }

            val LatLngA = LatLng(location.latitude, location.longitude)
            val LatLngB = LatLng(41.8417846, 140.7675603)
            val LatLngC = LatLng(41.8162013296, 140.735571384)
            val distance  = LatLngA.distanceBetween(LatLngC)

            val d : Int = distance.toInt()

            googleMap.run{
                map.addMarker(
                    MarkerOptions()
                        .position(LatLngC)
                        .title("$d m")
                        .snippet("${d / 80}分")
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot4))
                )

            }





        }



    }





}

