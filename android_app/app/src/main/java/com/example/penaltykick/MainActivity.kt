package com.example.penaltykick

import android.annotation.SuppressLint
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.BitmapFactory
import android.location.*
import android.location.LocationListener

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Looper
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup

import android.widget.Button
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.widget.Toolbar
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import com.google.android.gms.location.*
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions

import org.json.JSONObject
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity(), OnMapReadyCallback,OnMyLocationButtonClickListener,
OnMyLocationClickListener,ActivityCompat.OnRequestPermissionsResultCallback{

    lateinit var btnRent:Button
    lateinit var btnLogout:Button
    lateinit var qrScanLauncher:ActivityResultLauncher<ScanOptions>
    lateinit var mMap:GoogleMap
    lateinit var locationRequest:com.google.android.gms.location.LocationRequest
    lateinit var locationCallback:LocationCallback
    lateinit var mFusedLocationClient:FusedLocationProviderClient
    lateinit var mCurrentLocation:Location
    lateinit var mLayout: View
    lateinit var progressDialog: ProgressDialog
    lateinit var mPreferences:SharedPreferences

    var lockerList=mutableListOf<LockerLocation>()
    var markerList=mutableListOf<Marker>()


    val UPDATE_INTERVAL_MS=10000
    val FASTEST_UPDATE_INTERVAL_MS=10000
    val PERMISSIONS_REQUEST_CODE=100
    val REQUIRED_PERMISSIONS=Array<String>(1){android.Manifest.permission.ACCESS_FINE_LOCATION}



    private fun connectMainToUnlock(lockerId:Int){

        val server=retrofitClient.mainServer

        server.unlockRequest(lockerId).enqueue(object:Callback<String>{
            override fun onFailure(call: Call<String>, t: Throwable) {
                Log.d("main server",t.localizedMessage)
                Toast.makeText(applicationContext,"서버 연결에 실패했습니다.",Toast.LENGTH_LONG).show()
                progressDialog.dismiss()
            }

            override fun onResponse(call: Call<String>, response: Response<String>) {
                if(response?.isSuccessful){
                    Log.d("main server",response?.body().toString())
                    val resultCode=JSONObject(response.body().toString()).getString("result")

                    if(resultCode=="success"){
                        val preferencesEditor=mPreferences.edit()
                        preferencesEditor.putInt("lockerid",lockerId)
                        preferencesEditor.apply()

                        Toast.makeText(applicationContext,"잠금 해제가 완료되었습니다.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                        val intent=Intent(this@MainActivity,onRentActivity::class.java)
                        startActivity(intent)
                        finish()
                    }
                    else if(resultCode=="fail"){
                        Toast.makeText(applicationContext,"잠금 해제에 실패했습니다.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                    else{ // resultCode=="already unlock"
                        Toast.makeText(applicationContext,"사용 중인 기기입니다.",Toast.LENGTH_LONG).show()
                        progressDialog.dismiss()
                    }
                }
                else{
                    Log.d("main server","Some error occured")
                    Toast.makeText(applicationContext,"서버 응답이 잘못되었습니다.",Toast.LENGTH_LONG).show()
                    progressDialog.dismiss()
                }
            }
        })

    }

    private fun getLockerLocation(presentLat:Double,presentLong:Double){
        val server=retrofitClient.mainServer

        server.lockerLocationRequest(presentLat,presentLong).enqueue(object:Callback<MutableList<LockerLocation>>{
            override fun onFailure(call: Call<MutableList<LockerLocation>>, t: Throwable) {
                Log.d("main server","get locker location fail"+t.message.toString())
            }

            override fun onResponse(
                call: Call<MutableList<LockerLocation>>,
                response: Response<MutableList<LockerLocation>>
            ) {
                if(response.isSuccessful){
                    lockerList.clear() // 기존에 lockerlist 원소들이 존재하는 경우 초기화 후 다시 사용
                    lockerList= response.body()!!
                    Log.d("main server","get locker location success")
                    printLockerLocation(lockerList)
                }
                else{
                    Log.d("main server","get locker location fail")
                }
            }
        })

    }

    private fun printLockerLocation(lockers:List<LockerLocation>){

        //기존 마커가 있는 경우 삭제
        if (markerList.isNotEmpty()){
            for(m: Marker in markerList){
                m.remove()
            }
            markerList.clear()
        }

        //받아온 locker 값들이 있는 경우 print
        if(lockers.isNotEmpty()){
            for(i: LockerLocation in lockers){
                val id=i.id
                val location=LatLng(i.latitude,i.longitude)



                val marker: Marker? =mMap.addMarker(
                    MarkerOptions()
                        .position(location)
                        .title("Locker ID: $id")
                        .icon(BitmapDescriptorFactory
                            .fromBitmap(BitmapFactory.decodeResource(resources,R.drawable.placeholder128)))
                )

                if (marker != null) {
                    markerList.add(marker)
                }

            }
        }

    }

    /*
    fun getAppKeyHash() {
        try {
            val info =
                packageManager.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            for (signature in info.signatures) {
                var md: MessageDigest
                md = MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val something = String(Base64.encode(md.digest(), 0))
                Log.e("Hash key", something)
            }
        } catch (e: Exception) {

            Log.e("name not found", e.toString())
        }
    }

    */

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.appbar_action,menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.action_logout->{
                val preferencesEditor:SharedPreferences.Editor=mPreferences.edit()
                preferencesEditor.putString("userid",null)
                preferencesEditor.apply()
                val intent=Intent(this,loginActivity::class.java)
                startActivity(intent)
                finish()
            }

            R.id.action_log->return false


        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val tb: Toolbar =findViewById(R.id.app_actionbar)
        setSupportActionBar(tb)

        progressDialog=ProgressDialog(this)
        mPreferences=getSharedPreferences("user", MODE_PRIVATE)

        val mapFragment=supportFragmentManager.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        mFusedLocationClient=LocationServices.getFusedLocationProviderClient(this)

        locationRequest=com.google.android.gms.location.LocationRequest.create().apply{
            interval=UPDATE_INTERVAL_MS.toLong()
            fastestInterval=FASTEST_UPDATE_INTERVAL_MS.toLong()
            priority=com.google.android.gms.location.LocationRequest.PRIORITY_HIGH_ACCURACY
        }

        val builder:LocationSettingsRequest.Builder=LocationSettingsRequest.Builder()
        builder.addLocationRequest(locationRequest)


        locationCallback=object:LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.let{
                    for(location in it.locations){
                        //mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude),17F))
                        mCurrentLocation=location
                        Log.d("Main","currentLocation: ${location.latitude}, ${location.longitude}")
                    }

                    getLockerLocation(mCurrentLocation.latitude,mCurrentLocation.longitude)

                }
            }
        }


        mLayout=findViewById(R.id.layout_main)
        btnRent=findViewById<Button>(R.id.rent)

        qrScanLauncher=registerForActivityResult(ScanContract()){result->
            if(result.contents==null){
                Toast.makeText(this,"취소되었습니다.",Toast.LENGTH_LONG).show()
            }
            else{
                Log.d("QR scanned",result.contents)
                val lockerId=result.contents.toInt()
                connectMainToUnlock(lockerId)
                progressDialog.show()

            }
        }


        btnRent.setOnClickListener {
            val scanOptions=ScanOptions()
            scanOptions.setBeepEnabled(false)
            scanOptions.setOrientationLocked(true)
            scanOptions.setPrompt("QR코드를 스캔해주세요.")

            qrScanLauncher.launch(scanOptions)
        }

    }


    override fun onMapReady(googleMap: GoogleMap) {
        mMap=googleMap

        googleMap.setOnMyLocationButtonClickListener(this)
        googleMap.setOnMyLocationClickListener(this)

        //권한이 있는 경우 위치 업데이트 시작
        if(ActivityCompat.checkSelfPermission(this,android.Manifest.permission.ACCESS_FINE_LOCATION)==
            PERMISSION_GRANTED){
            startLocationUpdates()

        }
        else{ //권한 없는 경우 요청
            //사용자가 퍼미션 거부한 경우 있는 경우
            if(ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSIONS[0])){
                Snackbar.make(mLayout,"이 앱을 실행하려면 위치 접근 권한이 필요합니다",
                Snackbar.LENGTH_INDEFINITE).setAction("확인",View.OnClickListener {
                    ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,PERMISSIONS_REQUEST_CODE)
                }).show()
            }else{ //요청 거부한 적이 없는 경우 바로 요청
                ActivityCompat.requestPermissions(this,REQUIRED_PERMISSIONS,PERMISSIONS_REQUEST_CODE)
            }
            startLocationUpdates()
        }

    }



    @SuppressLint("MissingPermission")
    private fun startLocationUpdates(){

        val hasFineLocationPermission=ContextCompat.checkSelfPermission(this,
            android.Manifest.permission.ACCESS_FINE_LOCATION)
        if(hasFineLocationPermission!=PackageManager.PERMISSION_GRANTED){
            Log.d("Main","startLocationUpdates: 퍼미션 없음")
            return
        }

        mMap.uiSettings.isMyLocationButtonEnabled=true
        mMap.isMyLocationEnabled=true

        //location request 등록
        mFusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())

        mFusedLocationClient.lastLocation.addOnSuccessListener { location->
            if(location==null){ //마지막 location이 없는 경우 default location (seoul) 설정
                Log.d("Main","location get fail(null)")
                val SEOUL=LatLng(37.56,126.97)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SEOUL,17F))
            }
            else{ //마지막 location이 있는 경우 설정
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude),17F))
                Log.d("Main ","Last Loction: ${location.latitude},${location.longitude}")
            }

            //현재 위치 주변 잠금 장치 좌표 구하기
            getLockerLocation(location.latitude,location.longitude)

        }
            .addOnFailureListener {
                Log.e("Main","location error is ${it.message}")
                it.printStackTrace()
            }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode==PERMISSIONS_REQUEST_CODE&&grantResults.size==REQUIRED_PERMISSIONS.size){
            var check_result:Boolean=true

            for(result in grantResults){
                if(result!= PERMISSION_GRANTED){
                    check_result=false
                    break
                }
            }

            if(check_result){
                startLocationUpdates()
            }
            else{
                if(ActivityCompat.shouldShowRequestPermissionRationale(this,REQUIRED_PERMISSIONS[0])){
                    Snackbar.make(mLayout,"퍼미션이 거부되었습니다. 앱을 다시 실행하여 퍼미션을 허용하세요",
                    Snackbar.LENGTH_INDEFINITE).setAction("확인",View.OnClickListener {
                        finish()
                    }).show()
                }
                else{
                    Snackbar.make(mLayout,"퍼미션이 거부되었습니다. 설정(앱 정보)에서 퍼미션을 허용하세요",
                    Snackbar.LENGTH_INDEFINITE).setAction("확인",View.OnClickListener {
                        finish()
                    }).show()
                }
            }
        }
    }


    override fun onMyLocationButtonClick(): Boolean {
        Toast.makeText(this,"현재 위치로 이동합니다",Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this,"Current location: \n$location",Toast.LENGTH_LONG).show()
    }

    override fun onStop() {
        super.onStop()
        Log.d("Main onStop","call stopLocationUpdates")
        mFusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        Log.d("Main onPause","call stopLocationUpdates")
        mFusedLocationClient.removeLocationUpdates(locationCallback)
    }

    @SuppressLint("MissingPermission")
    override fun onStart() {
        super.onStart()
        Log.d("Main onStart","call startLocationUpdates")
        mFusedLocationClient.requestLocationUpdates(locationRequest,locationCallback, Looper.getMainLooper())
    }



}

