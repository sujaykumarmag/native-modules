package com.fishandhradriverapp.mapproject.utils

import com.google.maps.model.DirectionsResult

interface DirectionsAPIResponseListener {
    fun onDirectionsAPISuccess(result: DirectionsResult)
}