package com.fishandhradriverapp.mapproject

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.*
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.view.isVisible
import com.google.android.gms.location.*
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.*
import com.google.maps.DirectionsApiRequest
import com.google.maps.GeoApiContext
import com.google.maps.PendingResult
import com.google.maps.model.DirectionsResult
import com.google.maps.model.TravelMode
import kotlinx.android.synthetic.main.activity_main.*

import com.fishandhradriverapp.mapproject.utils.*
import com.fishandhradriverapp.R


class MapActivity : AppCompatActivity(), OnMapReadyCallback, DirectionsAPIResponseListener {
    private lateinit var lat: String
    private lateinit var lng: String
    private lateinit var googleMap: GoogleMap
    private var fusedLocationProviderClient: FusedLocationProviderClient? = null
    private lateinit var locationCallback: LocationCallback
    private lateinit var currentLocationCallback: LocationCallback
    private var currentLatLng: LatLng? = null
    private var isDirectionsDrawn = false
    private lateinit var geoApiContext: GeoApiContext
    private var movingCabMarker: Marker? = null
    private var previousLatLng: LatLng? = null
    private var latestLatLng: LatLng? = null
    private var destinationMarker: Marker? = null
    private var originMarker: Marker? = null
    private var blackPolyLine: Polyline? = null
    private var greyPolyLine: Polyline? = null
    private val mainThread = Handler(Looper.getMainLooper())
    private var tripPath = arrayListOf<com.google.maps.model.LatLng>()
    var durationMatrix: String? = null
    var distanceMatrix: String? = null
    var destination: LatLng? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
       setContentView(R.layout.activity_main)
        val bundleExtra = intent.extras
          lat=  bundleExtra?.getString("lat").toString()
        lng=  bundleExtra?.getString("lng").toString()
        //TODO get the destination value from bundle to have a dynamic value
        destination = LatLng(lat.toDouble(), lng.toDouble())
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment?
        mapFragment?.getMapAsync(this)

        geoApiContext = GeoApiContext.Builder()
            .apiKey(getString(R.string.google_maps_key))
            .build()
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        submitBtn.setOnClickListener {
            showSubmitDialog()
        }

        startNavigationBtn.setOnClickListener {
            startNavigationBtn.isVisible = false
            infoCard.isVisible = false
            timeInfo.isVisible = false
            submitBtn.isVisible = true
            setUpLocationTrackingListener()
        }
    }

    override fun onMapReady(mMap: GoogleMap) {
        googleMap = mMap

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        googleMap.isMyLocationEnabled = true
    }

    override fun onStart() {
        super.onStart()
        if (currentLatLng == null) {
            PermissionUtils.requestAccessFineLocationPermission(
                this,
                LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }

    private fun showSubmitDialog() {
        AlertDialog.Builder(this@MapActivity)
            .setTitle("Complete Trip")
            .setMessage("Are you sure, have you reached the destination and submitted the details?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                startNavigationBtn.isVisible = true
                infoCard.isVisible = true
                timeInfo.isVisible = true
                submitBtn.isVisible = false
                fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
                finish()
            }.setNegativeButton("No") { _, _ ->

            }.show()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        when (requestCode) {
            LOCATION_PERMISSION_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    when {
                        PermissionUtils.isLocationEnabled(this) -> {
                            getCurrentLocation()
                        }
                        else -> {
                            PermissionUtils.showGPSNotEnabledDialog(this)
                        }
                    }
                } else {
                    Toast.makeText(
                        this,
                        getString(R.string.location_permission_not_granted),
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun getCurrentLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        getCurrentLocationTrackingListener()
    }

    private fun getCurrentLocationTrackingListener() {

        val locationRequest = LocationRequest.create().setInterval(3000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        currentLocationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    Log.d("TAG", "onLocationResult: $currentLatLng")
                    currentLatLng?.let { latLng ->
                        directionsAPITask(latLng, destination!!)
                    }
                }
                fusedLocationProviderClient?.removeLocationUpdates(currentLocationCallback)
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        Looper.myLooper()?.let { looper ->
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest,
                currentLocationCallback,
                looper
            )
        }
    }

    private fun setUpLocationTrackingListener() {

        // for getting the current location update after every 2 seconds
        val locationRequest = LocationRequest.create().setInterval(5000).setFastestInterval(2000)
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                for (location in locationResult.locations) {
                    currentLatLng = LatLng(location.latitude, location.longitude)
                    Log.d("TAG", "onLocationResult: $currentLatLng")
                    currentLatLng?.let {
                        updateCabLocation(it)
                    }
                }
            }
        }
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        Looper.myLooper()?.let { looper ->
            fusedLocationProviderClient?.requestLocationUpdates(
                locationRequest,
                locationCallback,
                looper
            )
        }
    }


    private fun directionsAPITask(origin: LatLng, destination: LatLng) {
        if (isDirectionsDrawn) {
            return
        }
        val directionsApiRequest = DirectionsApiRequest(geoApiContext)
        directionsApiRequest.mode(TravelMode.DRIVING)
        directionsApiRequest.origin(
            com.google.maps.model.LatLng(
                origin.latitude,
                origin.longitude
            )
        )
        directionsApiRequest.destination(
            com.google.maps.model.LatLng(
                destination.latitude,
                destination.longitude
            )
        )

        directionsApiRequest.setCallback(object : PendingResult.Callback<DirectionsResult> {
            override fun onResult(result: DirectionsResult) {
                Log.d("TAG", "directionsApiRequest onResult : $result")

                val routeList = result.routes
                // Actually it will have zero or 1 route as we haven't asked Google API
                // for multiple paths
                if (routeList.isEmpty()) {
                    Log.d("TAG", "route empty")
                } else {
                    onDirectionsAPISuccess(result)
                }
            }

            override fun onFailure(e: Throwable) {
                Log.d("TAG", "directionsApiRequest onFailure : ${e.message}")
            }
        })
    }

    private fun showPath(latLngList: List<LatLng>) {
        val builder = LatLngBounds.Builder()
        for (latLng in latLngList) {
            builder.include(latLng)
        }
        val bounds = builder.build()
        googleMap.animateCamera(
            CameraUpdateFactory.newLatLngBounds(bounds, 100),
            object : GoogleMap.CancelableCallback {
                override fun onCancel() {
                }

                override fun onFinish() {
                    startNavigationBtn.isVisible = true
                    infoCard.isVisible = true
                    timeInfo.isVisible = true
                    "$distanceMatrix , $durationMatrix".also { timeInfo.text = it }
                }

            })
        val polylineOptions = PolylineOptions()
        polylineOptions.color(Color.GRAY)
        polylineOptions.width(15f)
        polylineOptions.addAll(latLngList)
        greyPolyLine = googleMap.addPolyline(polylineOptions)

        val blackPolylineOptions = PolylineOptions()
        blackPolylineOptions.color(Color.BLACK)
        blackPolylineOptions.width(15f)
        blackPolylineOptions.addAll(latLngList)
        blackPolyLine = googleMap.addPolyline(blackPolylineOptions)
        isDirectionsDrawn = true

        originMarker = addMarkerAndGet(latLngList[0], MapUtils.getMarkerBitmap())
        originMarker?.setAnchor(0.5f, 0.5f)
        destinationMarker =
            addMarkerAndGet(latLngList[latLngList.size - 1], MapUtils.getMarkerBitmap())
        destinationMarker?.setAnchor(0.5f, 0.5f)

        val polylineAnimator = AnimationUtils.polyLineAnimator()
        polylineAnimator.addUpdateListener { valueAnimator ->
            val percentValue = (valueAnimator.animatedValue as Int)
            val index = (greyPolyLine?.points!!.size * (percentValue / 100.0f)).toInt()
            blackPolyLine?.points = greyPolyLine?.points!!.subList(0, index)
        }
        polylineAnimator.start()
    }

    fun updateCabLocation(latLng: LatLng) {
        if (movingCabMarker == null)
            movingCabMarker = addMarkerAndGet(latLng, MapUtils.getCurrentBitmap(this))
        if (previousLatLng == null) {
            latestLatLng = latLng
            previousLatLng = latestLatLng
            movingCabMarker?.position = latestLatLng!!
            movingCabMarker?.setAnchor(0.5f, 0.5f)
            animateCamera(latestLatLng)
        } else {
            previousLatLng = latestLatLng
            latestLatLng = latLng
            CarMoveAnim.startCarAnimation(
                movingCabMarker!!,
                googleMap,
                previousLatLng!!,
                latestLatLng!!,
                3000
            )
        }
    }

    private fun addMarkerAndGet(latLng: LatLng, bitmap: Bitmap): Marker? {
        return googleMap.addMarker(
            MarkerOptions().position(latLng).flat(true)
                .icon(BitmapDescriptorFactory.fromBitmap(bitmap))
        )
    }

    private fun animateCamera(latLng: LatLng?) {
        latLng?.apply {
            googleMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    this, 21f
                )
            )
        }
    }

    override fun onDestroy() {
        fusedLocationProviderClient?.removeLocationUpdates(locationCallback)
        super.onDestroy()
    }

    companion object {
        private const val LOCATION_PERMISSION_REQUEST_CODE = 999
    }

    override fun onDirectionsAPISuccess(result: DirectionsResult) {
        tripPath.clear()
        val routeList = result.routes
        for (route in routeList) {
            val path = route.overviewPolyline.decodePath()
            durationMatrix = route.legs.firstOrNull()?.duration?.humanReadable
            distanceMatrix = route.legs.firstOrNull()?.distance?.humanReadable
            tripPath.addAll(path)
        }
        mainThread.post {
            showPath(tripPath.map { latLng -> LatLng(latLng.lat, latLng.lng) })
        }
    }
}