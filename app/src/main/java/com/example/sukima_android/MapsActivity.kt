package com.example.sukima_android


import android.app.Activity
import android.content.Context
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
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.example.sukima_android.model.visited_data
import com.facebook.stetho.okhttp3.StethoInterceptor
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMarkerClickListener
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.*
import com.google.maps.android.SphericalUtil.computeDistanceBetween
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.android.synthetic.main.activity_maps.*
import kotlinx.android.synthetic.main.dialog.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import org.w3c.dom.Text
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import kotlin.coroutines.CoroutineContext


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, OnMarkerClickListener,
    CoroutineScope {

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

    //visitedのポストを1回のみ行うためのフラグ
    private var VFlag = false

    private var PointNum = 0


    //LatLang型の配列
    private val POINT_new = arrayOfNulls<LatLng>(4)
    //visited型の配列
    private val VisitData = arrayOfNulls<visited_data>(4)

    private var SpotName = arrayOfNulls<String>(4)

    //ジャンルを格納しているString型の配列 Ganre
    private val Genre = arrayListOf<String>("debug", "eat", "relax", "play", "stroll")

    //動的な配列でスポットとの距離を格納している
    private var Distance = IntArray(4)

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
        Max(22.0f)
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

        time.setOnClickListener {
            val intent = Intent(this, time_layout::class.java)
            startActivity(intent)
        }

        menue.setOnClickListener {
            val intent = Intent(this, MenueList::class.java)
            startActivity(intent)
        }


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
                //Log.d("checkin",lastLocation.latitude.toString())

       Distance.forEachIndexed {i,Dist ->
            if (POINT_new[i] != null) {
             Distance[i] = computeDistanceBetween(
             POINT_new[i],
             LatLng(lastLocation.latitude, lastLocation.longitude)
             ).toInt()//自分の現在位置とスポットとの距離*/
             if(Distance[i] != 0 && Distance[i] < 200) {
                 if(VFlag == false) {

                    // val textView1: TextView = findViewById(R.id.spotName)
                    // textView1.text = SpotName[i]

                     Log.d("spotaaaaa",SpotName[i] )


                     //spotName.text= "aaa"


                     AlertDialog.Builder(this@MapsActivity)
                         .setView(layoutInflater.inflate(R.layout.dialog, null))
                         //.setMessage("llllllllll")
                         .show()


                     PointNum = i
                     VFlag = true

                 }
               }
                Log.d("checkiDP",POINT_new[i].toString())
                Log.d("checkiDD",Distance[i].toString())
               }
             }
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

        map.setInfoWindowAdapter(ToiletInfoWindowViewer(this@MapsActivity))

        //拡大縮小機能
        map = googleMap.apply {
            setMaxZoomPreference(Zoom.Max.value)
            setMinZoomPreference(Zoom.Min.value)
        }

        //スクロール、チルト、回転を無効
        map.uiSettings.run {
            isScrollGesturesEnabled = true
            isTiltGesturesEnabled = false
            isRotateGesturesEnabled = false
            isZoomGesturesEnabled = true
        }

        //fused locationの機能(位置情報取得)
        fusedLocationClient.lastLocation.addOnSuccessListener(this) { location ->

            if (location != null) {
                lastLocation = location
                val currentLatLng = LatLng(location.latitude, location.longitude)
                map.moveCamera(CameraUpdateFactory.newLatLng(currentLatLng))
                currentLatLng(currentLatLng)

            }

            val Button01: ImageButton = findViewById(R.id.imageButton01)
            val Button02: ImageButton = findViewById(R.id.imageButton02)
            val Button03: ImageButton = findViewById(R.id.imageButton03)
            val Button04: ImageButton = findViewById(R.id.imageButton04)


            //Http通信
            //Urlにくっつけるパラメータの設定
            val intent: Intent = getIntent()//インスタンスの引継ぎ
            val message: String = intent.getStringExtra(EXTRA_MESSAGE)//すきま時間の呼び出し
            val skima_time = message//これは設定したすきま時間
            val PRA_Curlatitude = location.latitude//現在地
            val PRA_Curlongitude = location.longitude//現在地
            val PRA_skima_time = "skima_time=" + skima_time.toString()

            val point_new = arrayOfNulls<LatLng>(4)

             val data = getSharedPreferences("Data", Context.MODE_PRIVATE)
             var dataInt = data.getInt("DataInt",0)
             var user_id = dataInt


            launch {
                try{

                    val resp =
                        client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude,user_id)
                    Log.d("userId",resp.toString())

                    resp.spots.forEachIndexed { i, v ->
                        if (i > (resp.spots.size)-1) return@forEachIndexed

                        Log.d("size",((resp.spots.size) -1 ).toString())
                        Log.d("as",v.toString())

                        val lat = v.position.latitude
                        val lng = v.position.longitude

                        val new_comment = buildString{
                            append(v.comment)

                            val a = v.comment.length / 16
                            for(k in 1..a) {
                                insert(16*k, "\n")
                            }
                        }

                        SER_Comment[i] = new_comment
                        SER_Genre[i] = v.genre
                        point_new[i] = LatLng(lat, lng)
                        POINT_new[i] = point_new[i]
                        SpotName[i]= v.name

                        //スポットIDとセッションIDの格納
                        val spot_id = v.spot_id
                        val session_id = resp.session_id
                        VisitData[i] = visited_data(spot_id,session_id)

                    }


                    val distance: MutableList<Int> = mutableListOf()
                    //for文で配列を入れていく
                    point_new.forEachIndexed { i, point ->
                        val LatLngA = LatLng(location.latitude, location.longitude)
                        Log.d("test", point.toString())
                        //現在位置とスポットまでの距離計算
                        distance.add(computeDistanceBetween(LatLngA, point).toInt())
                        if (SER_Genre[i] == Genre[1]) {
                            val marker: Marker =
                                map.addMarker(point?.let {
                                    MarkerOptions()
                                        .position(it)
                                        .title("${SER_Comment[i]}")
                                        .snippet("${distance[i] / 60 + 5}分  ${distance[i]} m")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot3))
                                }
                                )

                        } else if (SER_Genre[i] == Genre[2]) {
                            val marker: Marker =
                                map.addMarker(point?.let {
                                    MarkerOptions()
                                        .position(it)
                                        .title("${SER_Comment[i]}")
                                        .snippet("${distance[i] / 60 + 5}分  ${distance[i]} m")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot1))
                                }
                                )

                        } else if (SER_Genre[i] == Genre[3]) {
                            val marker: Marker =
                                map.addMarker(point?.let {
                                    MarkerOptions()
                                        .position(it)
                                        .title("${SER_Comment[i]}")
                                        .snippet("${distance[i] / 60 + 5}分  ${distance[i]} m")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot4))
                                }
                                )

                        } else if (SER_Genre[i] == Genre[4]) {
                            val marker: Marker =
                                map.addMarker(point?.let {
                                    MarkerOptions()
                                        .position(it)
                                        .title("${SER_Comment[i]}")
                                        .snippet("${distance[i] / 60 + 5}分  ${distance[i]} m")
                                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot2))
                                }
                                )

                        }


                    }

                }catch (e: Throwable) {
                    Log.e("e", e.toString())
                }
            }

            closeButton.setOnClickListener {
                imageButton.visibility = View.VISIBLE
                closeButton.visibility = View.INVISIBLE
                motion_layout.transitionToStart()
            }

            //以下ジャンルにボタンを押したときの処理
            Button01.setOnClickListener {
                val PRA_genre = Genre[1]//eat
                val point_new = arrayOfNulls<LatLng>(4)

                map.clear()

                launch {
                    try {
                        val resp =
                            client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude, user_id, PRA_genre)

                        resp.spots.forEachIndexed { i, v ->
                            if (i > (resp.spots.size)-1) return@forEachIndexed
                            Log.d("size1", ((resp.spots.size)-1).toString())

                            //スポットのジャンルと座標の格納
                            val lat = v.position.latitude
                            val lng = v.position.longitude
                            SER_Genre[i] = v.genre
                            point_new[i] = LatLng(lat, lng)
                            POINT_new[i] = point_new[i]
                            SpotName[i]= v.name

                            val new_comment = buildString{
                                append(v.comment)

                                val a = v.comment.length / 16
                                for(k in 1..a) {
                                    insert(16*k, "\n")
                                }
                            }


                            SER_Comment[i] = new_comment

                            //スポットIDとセッションIDの格納
                            val spot_id = v.spot_id
                            val session_id = resp.session_id
                            VisitData[i] = visited_data(spot_id,session_id)
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
                                .title("${SER_Comment[i]}")
                                .snippet("${distance[i] / 60 + 5}分  ${distance[i]} m")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot3))

                        }
                        )


                }
                    } catch (e: Throwable) {
                        Log.e("e", e.toString())
                    }
                }
                Toast.makeText(this, "食べたい", Toast.LENGTH_SHORT).show()
                motion_layout.transitionToStart()
            }

            Button02.setOnClickListener {
                val PRA_genre = Genre[2]//relax
                val point_new = arrayOfNulls<LatLng>(4)

                map.clear()


                launch {
                    try {
                        val resp =
                            client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude, user_id, PRA_genre)

                        resp.spots.forEachIndexed { i, v ->
                            if (i > (resp.spots.size)-1) return@forEachIndexed
                            Log.d("size2", ((resp.spots.size)-1).toString())

                            val lat = v.position.latitude
                            val lng = v.position.longitude
                            SER_Genre[i] = v.genre
                            point_new[i] = LatLng(lat, lng)
                            POINT_new[i] = point_new[i]
                            SpotName[i]= v.name
                            val new_comment = buildString{
                                append(v.comment)

                                val a = v.comment.length / 16
                                for(k in 1..a) {
                                    insert(16*k, "\n")
                                }
                            }

                            SER_Comment[i] = new_comment

                            //スポットIDとセッションIDの格納
                            val spot_id = v.spot_id
                            val session_id = resp.session_id
                            VisitData[i] = visited_data(spot_id,session_id)


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
                                    .title("${SER_Comment[i]}")
                                    .snippet("${distance[i] / 60 + 5}分  ${distance[i]} m")
                                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot1))
                            }
                            )

                        }
                    } catch (e: Throwable) {
                        Log.e("e", e.toString())
                    }
                }
                Toast.makeText(this, "まったりしたい", Toast.LENGTH_SHORT).show()
                motion_layout.transitionToStart()
            }

            Button03.setOnClickListener {
                val PRA_genre = Genre[3]//play
                val point_new = arrayOfNulls<LatLng>(4)

                map.clear()

                launch {
                    try {
                        val resp =
                            client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude, user_id, PRA_genre)

                        resp.spots.forEachIndexed { i, v ->
                            if (i > (resp.spots.size)-1) return@forEachIndexed
                            Log.d("size3", ((resp.spots.size)-1).toString())
                            val lat = v.position.latitude
                            val lng = v.position.longitude
                            SER_Genre[i] = v.genre
                            point_new[i] = LatLng(lat, lng)
                            POINT_new[i] = point_new[i]
                            SpotName[i]= v.name
                            val new_comment = buildString{
                                append(v.comment)

                                val a = v.comment.length / 16
                                for(k in 1..a) {
                                    insert(16*k, "\n")
                                }
                            }

                            SER_Comment[i] = new_comment


                            //スポットIDとセッションIDの格納
                            val spot_id = v.spot_id
                            val session_id = resp.session_id
                            VisitData[i] = visited_data(spot_id,session_id)


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
                                .title("${SER_Comment[i]}")
                                .snippet("${distance[i] / 60 + 5}分  ${distance[i]} m")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot4))
                        }
                        )
                }
                    } catch (e: Throwable) {
                        Log.e("e", e.toString())
                    }
                }
                Toast.makeText(this, "あそびたい", Toast.LENGTH_SHORT).show()
                motion_layout.transitionToStart()
            }

            Button04.setOnClickListener {
                val PRA_genre = Genre[4]//stroll
                val point_new = arrayOfNulls<LatLng>(4)

                map.clear()

                launch {
                    try {
                        val resp =
                            client.getSpot(skima_time, PRA_Curlatitude, PRA_Curlongitude, user_id, PRA_genre)

                        resp.spots.forEachIndexed { i, v ->
                            if (i > (resp.spots.size)-1) return@forEachIndexed
                            Log.d("size4", ((resp.spots.size)-1).toString())
                            val lat = v.position.latitude
                            val lng = v.position.longitude
                            SER_Genre[i] = v.genre
                            point_new[i] = LatLng(lat, lng)
                            POINT_new[i] = point_new[i]
                            SpotName[i]= v.name
                            val new_comment = buildString{
                                append(v.comment)

                                val a = v.comment.length / 16
                                for(k in 1..a) {
                                    insert(16*k, "\n")
                                }
                            }

                            SER_Comment[i] = new_comment


                            //スポットIDとセッションIDの格納
                            val spot_id = v.spot_id
                            val session_id = resp.session_id
                            VisitData[i] = visited_data(spot_id,session_id)


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
                                .title("${SER_Comment[i]}")
                                .snippet("${distance[i] / 60 + 5}分  ${distance[i]} m")
                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.spot2))
                        }
                        )
                }
                    } catch (e: Throwable) {
                        Log.e("e", e.toString())
                    }
                }
                Toast.makeText(this, "ぶらぶらしたい", Toast.LENGTH_SHORT).show()
                motion_layout.transitionToStart()
            }
        }


    }


    //現在位置にカメラが戻るボタン
    fun currentLatLng(currentLatLng: LatLng) {
        current_btn.setOnClickListener {
            map.animateCamera(CameraUpdateFactory.newLatLng(currentLatLng))
        }
    }

    fun ikuyo(view: View) {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

         val data = getSharedPreferences("Data", Context.MODE_PRIVATE)
         var dataInt = data.getInt("DataInt",0)
         var user_id = dataInt

       launch {
            try {
                Log.d("tryX",VisitData[PointNum].toString())
                val resp =
                    client.postVisited(user_id, VisitData[PointNum])

                Log.d("tryX",resp.toString())

            } catch (e: Throwable) {
                Log.e("e", e.toString())
            }
        }

    }



    fun mata(view: View)
    {

        val points = ArrayList<LatLng>()
        val lineOptions = PolylineOptions()
        val now = LatLng(lastLocation.latitude, lastLocation.longitude)
        points.add(now)
        val position = POINT_new[PointNum]
        if (position != null) {
            points.add(position)
        }

        lineOptions.addAll(points)
        lineOptions.width(10f)
        lineOptions.color(0x550000ff)

        map.addPolyline(lineOptions)

        VFlag = false

        //sukimaTime.text = SpotName[i]


    }

    fun fin(view: View){
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)

        val data = getSharedPreferences("Data", Context.MODE_PRIVATE)
        var dataInt = data.getInt("DataInt",0)
        var user_id = dataInt

        launch {
            try {
                Log.d("tryX",VisitData[PointNum].toString())
                val resp =
                    client.postVisited(user_id, VisitData[PointNum])

                Log.d("tryX",resp.toString())

            } catch (e: Throwable) {
                Log.e("e", e.toString())
            }
        }

        VFlag = false


    }

    override fun onDestroy() {
        super.onDestroy()
        job.cancel()
    }




}


