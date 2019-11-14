package com.example.sukima_android

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
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
import android.util.Log
import android.widget.TextView

import com.google.maps.android.SphericalUtil.*
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.activity_time_layout.*
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.URL


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener {

    override fun onMarkerClick(p0: Marker?) = false


    private lateinit var map: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    //LatLang型の配列
    private val point_new = arrayOfNulls<LatLng>(3)

    //ジャンルを格納しているString型の配列 Ganre
    private val Ganre = arrayListOf<String>("debug","eat","mattari","play","barabura")

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


            //Http通信
            //Urlにくっつけるパラメータの設定

            val ganre = "ganre=" + Ganre[0]//これはジャンルボタンを選択したときにそれぞれ設定する
            val sukima_time = 70//これは設定したすきま時間

            val PRA_ganre = ganre//ジャンルのパラメータの設定
            val PRA_sukima_time = "sukima_time=" + sukima_time.toString()
            //HitAPITaskを呼び出して、APIをたたく
            HitAPITask().execute("http://160.16.103.99/spots" + "?" +PRA_ganre + "&" + PRA_sukima_time)

            //緯度経度を取得するまで待つ
            Thread.sleep(10000)


/*
            val point_new = arrayOfNulls<LatLng>(3)
            point_new[0] = LatLng(41.8417846, 140.7675603)
            point_new[1] = LatLng(41.8415718222, 140.767425299)
            point_new[2] = LatLng(41.8395136176, 140.76271534)
*/
            val distance:MutableList<Int> = mutableListOf()

            point_new.forEachIndexed { i, point ->
                val LatLngA = LatLng(location.latitude, location.longitude)

                Log.d("test",point_new[0].toString())

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


    @SuppressLint("StaticFieldLeak")
    inner class HitAPITask: AsyncTask<String, String, String>(){
        override fun doInBackground(vararg params: String?): String? {
            //ここでAPIを叩きます。バックグラウンドで処理する内容です。
            var connection: HttpURLConnection? = null
            var reader: BufferedReader? = null
            val buffer: StringBuffer

            try {
                //param[0]にはAPIのURI(String)を入れます(後ほど)。
                //AsynkTask<...>の一つめがStringな理由はURIをStringで指定するからです。
                val url = URL(params[0])
                connection = url.openConnection() as HttpURLConnection
                connection.connect()  //ここで指定したAPIを叩いてみてます。

                //ここから叩いたAPIから帰ってきたデータを使えるよう処理していきます。

                //とりあえず取得した文字をbufferに。
                val stream = connection.inputStream
                reader = BufferedReader(InputStreamReader(stream))
                buffer = StringBuffer()
                var line: String?
                while (true) {
                    line = reader.readLine()
                    if (line == null) {

                        break
                    }
                    buffer.append(line)
                    Log.d("CHECK_API", buffer.toString())
                }

                //ここからは、今回はJSONなので、いわゆるJsonをParseする作業（Jsonの中の一つ一つのデータを取るような感じ）をしていきます。

                //先ほどbufferに入れた、取得した文字列
                val jsonText = buffer.toString()

                //JSONObjectを使って、まず全体のJSONObjectを取ります。
                val parentJsonObj = JSONObject(jsonText)

//以下追加かつテスト
                val parentJsonArray = parentJsonObj.getJSONArray("spots")

                val detailJsonObj = parentJsonArray.getJSONObject(0)


//以下緯度経度のパース
                val position = detailJsonObj.getJSONObject("position")
                val longitude = position.getDouble("longitude")
                val latitude = position.getDouble("latitude")

                // 以下メインで使えるようにLatLng型のグローバルな配列にスポットの値を代入


                point_new[0] = LatLng(latitude, longitude)
                point_new[1] = LatLng(41.8415718222, 140.767425299)
                point_new[2] = LatLng(41.8395136176, 140.76271534)


                //以上緯度経度のパース

                //ここから下は、接続エラーとかJSONのエラーとかで失敗した時にエラーを処理する為のものです。
            } catch (e: MalformedURLException) {
                e.printStackTrace()
            } catch (e: IOException) {
                e.printStackTrace()
            } catch (e: JSONException) {
                e.printStackTrace()
            }
            //finallyで接続を切断してあげましょう。
            finally {
                connection?.disconnect()
                try {
                    reader?.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            //失敗した時はnullやエラーコードなどを返しましょう。
            return null
        }
        override fun onPostExecute(result: String?) {
            super.onPostExecute(result)
            if (result == null) return
        }
    }

}


