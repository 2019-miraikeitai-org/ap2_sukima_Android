package com.example.sukima_android


import android.app.Activity
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.provider.AlarmClock.EXTRA_MESSAGE
import android.util.Log
import android.view.View
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.facebook.stetho.okhttp3.StethoInterceptor
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
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.coroutines.CoroutineContext


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener,
    CoroutineScope {

    override fun onMarkerClick(p0: Marker?) = false


    private lateinit var map: GoogleMap
    private lateinit var lastLocation: Location
    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private var kani=20

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
    private val point_new = arrayOfNulls<LatLng>(4)

    //ジャンルを格納しているString型の配列 Ganre
    private val Genre = arrayListOf<String>("debug", "eat", "relax", "play", "stroll")

    //サーバーからとってきた値のジャンル
    private val SER_Genre = arrayOfNulls<String>(4)
    private val SER_Comment = arrayOfNulls<String>(4)

    private val okHttp = OkHttpClient()
        .newBuilder()
        .addNetworkInterceptor(StethoInterceptor())
        .build()

    private val moshi = Moshi.Builder()
        .add(KotlinJsonAdapterFactory())
        .build()

    private val retrofit = Retrofit.Builder()
        .client(okHttp)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .baseUrl("http://160.16.103.99")
        .build()

    private val client by lazy { retrofit.create(SkimattiClient::class.java) }

    private val job = Job()
    override val coroutineContext: CoroutineContext = Dispatchers.Main + job

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

        if(kani<5) {


            AlertDialog.Builder(this)
                .setView(layoutInflater.inflate(R.layout.dialog, null))
                .show()
        }




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
        //2
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            null /* Looper */
        )
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
                    e.startResolutionForResult(
                        this@MapsActivity,
                        REQUEST_CHECK_SETTINGS
                    )
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

            val seachButton: ImageButton = findViewById(R.id.imageButton)
            val Button01: ImageButton = findViewById(R.id.imageButton01)
            val Button02: ImageButton = findViewById(R.id.imageButton02)
            val Button03: ImageButton = findViewById(R.id.imageButton03)
            val Button04: ImageButton = findViewById(R.id.imageButton04)
            val closeButton: ImageButton = findViewById(R.id.closeButton)
            seachButton.setOnClickListener {
                seachButton.visibility = View.INVISIBLE
                Button01.visibility = View.VISIBLE
                Button02.visibility = View.VISIBLE
                Button03.visibility = View.VISIBLE
                Button04.visibility = View.VISIBLE
                closeButton.visibility = View.VISIBLE
            }
            closeButton.setOnClickListener {
                seachButton.visibility = View.VISIBLE
                Button01.visibility = View.INVISIBLE
                Button02.visibility = View.INVISIBLE
                Button03.visibility = View.INVISIBLE
                Button04.visibility = View.INVISIBLE
                closeButton.visibility = View.INVISIBLE
            }

            //Http通信
            //Urlにくっつけるパラメータの設定
            val intent: Intent = getIntent()//インスタンスの引継ぎ
            val message: String = intent.getStringExtra(EXTRA_MESSAGE)//すきま時間の呼び出し
            val skima_time = message//これは設定したすきま時間
            val PRA_Curlatitude = location.latitude//現在地
            val PRA_Curlongitude = location.longitude//現在地
            val PRA_skima_time = "skima_time=" + skima_time.toString()


            ////HitAPITaskを呼び出して、APIをたたく
            //HitAPITask().execute(
            //    "GET",
            //    "http://160.16.103.99/spots" + "?" + PRA_skima_time + "&latitude=" + PRA_Curlatitude + "&longitude=" + PRA_Curlongitude
            //)
            //Thread.sleep(10000)

            launch {
                try{
                    val resp =
                        client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude)

                    resp.spots.forEachIndexed { i, v ->
                        if (i > 3) return@forEachIndexed

                        Log.d("as",v.toString())

                        val lat = v.position.latitude
                        val lng = v.position.longitude

                        SER_Comment[i] = v.comment
                        SER_Genre[i] = v.genre
                        point_new[i] = LatLng(lat, lng)
                    }
                    val distance: MutableList<Int> = mutableListOf()
                    //for文で配列を入れていく
                    point_new.forEachIndexed { i, point ->
                        val LatLngA = LatLng(location.latitude, location.longitude)
                        Log.d("test", point_new[i].toString())
                        //現在位置とスポットまでの距離計算
                        distance.add(computeDistanceBetween(LatLngA, point).toInt())
                        if (SER_Genre[i] == Genre[1]) {
                            val marker: Marker =
                                map.addMarker(point?.let {
                                    MarkerOptions()
                                        .position(it)
                                        .title("${distance[i] / 80}分"+":${distance[i]} m")
                                        .snippet("${SER_Comment[i]}")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot1))
                                }
                                )

                        } else if (SER_Genre[i] == Genre[2]) {
                            val marker: Marker =
                                map.addMarker(point?.let {
                                    MarkerOptions()
                                        .position(it)
                                        .title("${distance[i] / 80}分" + ":${distance[i]} m")
                                        .snippet("${SER_Comment[i]}")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot2))
                                }
                                )

                        } else if (SER_Genre[i] == Genre[3]) {
                            val marker: Marker =
                                map.addMarker(point?.let {
                                    MarkerOptions()
                                        .position(it)
                                        .title("${distance[i] / 80}分"+":${distance[i]} m")
                                        .snippet("${SER_Comment[i]}")

                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot3))
                                }
                                )

                        } else if (SER_Genre[i] == Genre[4]) {
                            val marker: Marker =
                                map.addMarker(point?.let {
                                    MarkerOptions()
                                        .position(it)
                                        .title("${distance[i] / 80}分"+ ":${distance[i]} m")
                                        .snippet("${SER_Comment[i]}")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot4))
                                }
                                )

                        }


                    }

                }catch (e: Throwable) {
                    Log.e("e", e.toString())
                }
            }



            //以下ジャンルにボタンを押したときの処理
            Button01.setOnClickListener {
                val PRA_genre = Genre[1]//eat

                map.clear()

                launch {
                    try {
                        val resp =
                            client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude, PRA_genre)

                        resp.spots.forEachIndexed { i, v ->
                            if (i > 3) return@forEachIndexed

                            val lat = v.position.latitude
                            val lng = v.position.longitude
                            SER_Genre[i] = v.genre
                            point_new[i] = LatLng(lat, lng)
                        }

                val distance: MutableList<Int> = mutableListOf()
                //for文で配列を入れていく
                point_new.forEachIndexed { i, point ->
                    val LatLngA = LatLng(location.latitude, location.longitude)
                    Log.d("test", point_new[i].toString())
                    //現在位置とスポットまでの距離計算
                    distance.add(computeDistanceBetween(LatLngA, point).toInt())
                    val marker: Marker =
                        map.addMarker(point?.let {
                            MarkerOptions()
                                .position(it)
                                .title("${distance[i]} m"+":${distance[i] / 80}分")
                                .snippet("${SER_Comment[i]}")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot3))
                        }
                        )

                }
                    } catch (e: Throwable) {
                        Log.e("e", e.toString())
                    }
                }
            }

            Button02.setOnClickListener {
                val PRA_genre = Genre[2]//relax
                map.clear()

                //HitAPITask().execute(
                //    "GET",
                //    "http://160.16.103.99/spots" + "?" + PRA_genre + "&" + PRA_skima_time + "&latitude=" + PRA_Curlatitude + "&longitude=" + PRA_Curlongitude
                //)

                launch {
                    try {
                        val resp =
                            client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude, PRA_genre)

                        resp.spots.forEachIndexed { i, v ->
                            if (i > 3) return@forEachIndexed

                            val lat = v.position.latitude
                            val lng = v.position.longitude
                            SER_Genre[i] = v.genre
                            point_new[i] = LatLng(lat, lng)
                        }
                        val distance: MutableList<Int> = mutableListOf()
                        //for文で配列を入れていく
                        point_new.forEachIndexed { i, point ->
                            val LatLngA = LatLng(location.latitude, location.longitude)
                            Log.d("test", point_new[i].toString())
                            //現在位置とスポットまでの距離計算
                            distance.add(computeDistanceBetween(LatLngA, point).toInt())
                            map.addMarker(point?.let {
                                MarkerOptions()
                                    .position(it)
                                    .title("${distance[i]} m"+":${distance[i] / 80}分")
                                    .snippet("${SER_Comment[i]}")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot1))
                            }
                            )

                        }
                    } catch (e: Throwable) {
                        Log.e("e", e.toString())
                    }
                }

                //緯度経度を取得するまで待つ
            }
            Button03.setOnClickListener {
                val PRA_genre = Genre[3]//play

                map.clear()

                launch {
                    try {
                        val resp =
                            client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude, PRA_genre)

                        resp.spots.forEachIndexed { i, v ->
                            if (i > 3) return@forEachIndexed

                            val lat = v.position.latitude
                            val lng = v.position.longitude
                            SER_Genre[i] = v.genre
                            point_new[i] = LatLng(lat, lng)
                        }

                val distance: MutableList<Int> = mutableListOf()
                //for文で配列を入れていく
                point_new.forEachIndexed { i, point ->
                    val LatLngA = LatLng(location.latitude, location.longitude)
                    Log.d("test", point_new[i].toString())
                    //現在位置とスポットまでの距離計算
                    distance.add(computeDistanceBetween(LatLngA, point).toInt())
                    val marker: Marker =
                        map.addMarker(point?.let {
                            MarkerOptions()
                                .position(it)
                                .title("${distance[i]} m"+":${distance[i] / 80}分")
                                .snippet("${SER_Comment[i]}")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot4))
                        }
                        )

                }
            } catch (e: Throwable) {
            Log.e("e", e.toString())
        }
        }
            }
            Button04.setOnClickListener {
                val PRA_genre = Genre[4]//stroll
                map.clear()

                launch {
                    try {
                        val resp =
                            client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude, PRA_genre)

                        resp.spots.forEachIndexed { i, v ->
                            if (i > 3) return@forEachIndexed

                            val lat = v.position.latitude
                            val lng = v.position.longitude
                            SER_Genre[i] = v.genre
                            point_new[i] = LatLng(lat, lng)
                        }
                val distance: MutableList<Int> = mutableListOf()
                //for文で配列を入れていく
                point_new.forEachIndexed { i, point ->
                    val LatLngA = LatLng(location.latitude, location.longitude)
                    Log.d("test", point_new[i].toString())
                    //現在位置とスポットまでの距離計算
                    distance.add(computeDistanceBetween(LatLngA, point).toInt())
                    val marker: Marker =
                        map.addMarker(point?.let {
                            MarkerOptions()
                                .position(it)
                                .title("${distance[i]} m"+":${distance[i] / 80}分")
                                .snippet("${SER_Comment[i]}")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot2))
                        }
                        )
                }
                    } catch (e: Throwable) {
                        Log.e("e", e.toString())
                    }
                }
            }
//以上ジャンルボタンを押したときの処理


        }


    }


    //現在位置にカメラが戻るボタン
    fun currentLatLng(currentLatLng: LatLng) {
        current_btn.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
        }
    }

    fun ikuyo(view: View){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    fun mata(view: View)
    {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }


}


