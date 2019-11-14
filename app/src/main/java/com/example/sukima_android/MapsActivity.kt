package com.example.sukima_android

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.AsyncTask
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.maps.android.SphericalUtil.computeDistanceBetween
import kotlinx.android.synthetic.main.activity_maps.*
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

    //現在地を更新していくやつ
    private lateinit var locationCallback: LocationCallback
    //プロパティと位置更新状態プロパティを宣言
    private lateinit var locationRequest: LocationRequest
    private var locationUpdateState = false

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1

        private const val REQUEST_CHECK_SETTINGS = 2
    }



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

        //fused location
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)


        //画面遷移の時に、time_layoutから受け取る引数
        val intent: Intent = getIntent()
        val message: String = intent.getStringExtra(EXTRA_MESSAGE)
        val textView: TextView = findViewById(R.id.sukimaTime)
        textView.text = message

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(p0: LocationResult) {
                super.onLocationResult(p0)

                lastLocation = p0.lastLocation
                currentLatLng(LatLng(lastLocation.latitude, lastLocation.longitude))
            }
        }

        createLocationRequest()

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

    //場所の更新をリクエスト
    private fun startLocationUpdates() {
        //1
        if (ActivityCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE)
            return
        }
        //2
        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */)
    }

    private fun createLocationRequest() {
        //ユーザの場所の変更の処理を行う
        locationRequest = LocationRequest()
        // インターバル。アプリが更新を受信する頻度
        locationRequest.interval = 10000
        // アプリが更新を最速にできるレート
        locationRequest.fastestInterval = 5000
        locationRequest.priority = LocationRequest.PRIORITY_HIGH_ACCURACY

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)

        // 設定クライアントと場所の設定を確認するタスク
        val client = LocationServices.getSettingsClient(this)
        val task = client.checkLocationSettings(builder.build())

        // タスク成功。位置情報要求を開始
        task.addOnSuccessListener {
            locationUpdateState = true
            startLocationUpdates()
        }
        task.addOnFailureListener { e ->
            // タスク失敗。ユーザの位置情報権限がオフになった場合である。ダイアログを表示させる。
            if (e is ResolvableApiException) {
                // タスク失敗
                try {
                    // ダイアログを表示
                    e.startResolutionForResult(this@MapsActivity,
                        REQUEST_CHECK_SETTINGS)
                } catch (sendEx: IntentSender.SendIntentException) {
                    // エラーを無視する
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            if (resultCode == Activity.RESULT_OK) {
                locationUpdateState = true
                startLocationUpdates()
            }
        }
    }

    // onPause（）をオーバーライドして、ロケーション更新リクエストを停止します
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    // onResume（）をオーバーライドして、ロケーション更新リクエストを再開します。
    public override fun onResume() {
        super.onResume()
        if (!locationUpdateState) {
            startLocationUpdates()
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

        //map.mapType = GoogleMap.MAP_TYPE_NONE  //mapを消せる(最後にコメントアウト消す)

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


            val distance:MutableList<Int> = mutableListOf()

            //for文で配列を入れていく
            point_new.forEachIndexed { i, point ->
                val LatLngA = LatLng(location.latitude, location.longitude)

                Log.d("test",point_new[0].toString())

                //現在位置とスポットまでの距離計算
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

                Check(distance[i])
            }
        }

    }




    //現在位置にカメラが戻るボタン
    fun currentLatLng(currentLatLng : LatLng) {
        current_btn.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
        }
    }

    //スポットとの距離が5m以下になったら画面遷移
    private fun Check(distance: Int) {

        if (distance < 5) {
            val intent = Intent(this, CheckIn::class.java)
            startActivity(intent)
        }
    }

    //以下サーバとの通信
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


