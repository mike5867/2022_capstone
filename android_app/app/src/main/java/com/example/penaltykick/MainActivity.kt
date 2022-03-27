package com.example.penaltykick

import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.location.*
import android.location.LocationRequest

import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Environment
import android.os.Looper
import android.util.Log
import android.view.View

import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.ContextCompat.getSystemService
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.GoogleMap.OnMyLocationButtonClickListener
import com.google.android.gms.maps.GoogleMap.OnMyLocationClickListener
import androidx.core.content.FileProvider.getUriForFile
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.zxing.client.android.Intents
import com.journeyapps.barcodescanner.ScanContract
import com.journeyapps.barcodescanner.ScanOptions
import okhttp3.MediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.jar.Manifest
import javax.security.auth.callback.PasswordCallback
import kotlin.jvm.Throws

class MainActivity : AppCompatActivity(), OnMapReadyCallback,OnMyLocationButtonClickListener,
OnMyLocationClickListener,ActivityCompat.OnRequestPermissionsResultCallback{

    lateinit var btnRent:Button
    lateinit var qrScanLauncher:ActivityResultLauncher<ScanOptions>
    lateinit var mMap:GoogleMap
    lateinit var locationRequest:com.google.android.gms.location.LocationRequest
    lateinit var locationCallback:LocationCallback
    lateinit var mFusedLocationClient:FusedLocationProviderClient
    lateinit var mCurrentLocation:Location
    lateinit var mLayout: View

    val UPDATE_INTERVAL_MS=10000
    val FASTEST_UPDATE_INTERVAL_MS=5000
    val PERMISSIONS_REQUEST_CODE=100
    val REQUIRED_PERMISSIONS=Array<String>(1){android.Manifest.permission.ACCESS_FINE_LOCATION}

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

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
                // TODO Something
                Log.d("scanned",result.contents.toString())

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
                Log.e("Main","location get fail")
                val SEOUL=LatLng(37.56,126.97)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(SEOUL,17F))
            }
            else{ //마지막 location이 있는 경우 설정
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(LatLng(location.latitude,location.longitude),17F))
                Log.d("Main ","Last Loction: ${location.latitude},${location.longitude}")
            }
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
        Toast.makeText(this,"MyLocation button clicked",Toast.LENGTH_SHORT).show()
        return false
    }

    override fun onMyLocationClick(location: Location) {
        Toast.makeText(this,"Current location: \n$location",Toast.LENGTH_LONG).show()
    }

    override fun onStop() {
        super.onStop()
        Log.d("onStop","call stopLocationUpdates")
        mFusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onPause() {
        super.onPause()
        mFusedLocationClient.removeLocationUpdates(locationCallback)
    }



}

