package com.fishandhradriverapp.mapproject.utils

import com.google.android.gms.maps.GoogleMap
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker

object CarMoveAnim {
    fun startCarAnimation(
        carMarker: Marker?, googleMap: GoogleMap?, startPosition: LatLng?,
        endPosition: LatLng?, duration: Int
    ) {
        var valueAnimatorDuration = duration
        val valueAnimator = ValueAnimator.ofFloat(0f, 1f)
        if (duration == 0 || duration < 3000) {
            valueAnimatorDuration = 3000
        }
        valueAnimator.duration = valueAnimatorDuration.toLong()
        val latLngInterpolator: LatLngInterpolatorNew = LatLngInterpolatorNew.LinearFixed()
        valueAnimator.interpolator = LinearInterpolator()
        valueAnimator.addUpdateListener {
            if (startPosition != null && endPosition != null) {
                val v = it.animatedFraction
                val newPos = latLngInterpolator.interpolate(v, startPosition, endPosition)
                carMarker?.position = newPos
                carMarker?.setAnchor(0.5f, 0.5f)
                carMarker?.rotation = bearingBetweenLocations(startPosition, endPosition)
                    .toFloat()
                googleMap?.moveCamera(
                    CameraUpdateFactory
                        .newCameraPosition(
                            CameraPosition.Builder()
                                .target(newPos)
                                .tilt(30f)
                                .bearing(
                                    bearingBetweenLocations(startPosition, endPosition)
                                        .toFloat()
                                )
                                .zoom(20f)
                                .build()
                        )
                )
            }
        }
        valueAnimator.start()
    }

    private fun bearingBetweenLocations(latLng1: LatLng, latLng2: LatLng): Double {
        val PI = 3.14159
        val lat1 = latLng1.latitude * PI / 180
        val long1 = latLng1.longitude * PI / 180
        val lat2 = latLng2.latitude * PI / 180
        val long2 = latLng2.longitude * PI / 180
        val dLon = long2 - long1
        val y = Math.sin(dLon) * Math.cos(lat2)
        val x = Math.cos(lat1) * Math.sin(lat2) - (Math.sin(lat1)
                * Math.cos(lat2) * Math.cos(dLon))
        var brng = Math.atan2(y, x)
        brng = Math.toDegrees(brng)
        brng = (brng + 360) % 360
        return brng
    }

    interface LatLngInterpolatorNew {
        fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng
        class LinearFixed : LatLngInterpolatorNew {
            override fun interpolate(fraction: Float, a: LatLng, b: LatLng): LatLng {
                val lat = (b.latitude - a.latitude) * fraction + a.latitude
                var lngDelta = b.longitude - a.longitude
                // Take the shortest path across the 180th meridian.
                if (Math.abs(lngDelta) > 180) {
                    lngDelta -= Math.signum(lngDelta) * 360
                }
                val lng = lngDelta * fraction + a.longitude
                return LatLng(lat, lng)
            }
        }
    }
}