package com.fishandhradriverapp.mapproject

import android.content.Intent
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod


class MapManager(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "MapNavigations"
    }

    @ReactMethod
    fun StartNavigation(lat: String, lng: String) {
        val intent = Intent(reactApplicationContext, MapActivity::class.java)
        intent.putExtra("lat", lat)
        intent.putExtra("lng", lng)
        reactApplicationContext.startActivity(intent)
    }
}
