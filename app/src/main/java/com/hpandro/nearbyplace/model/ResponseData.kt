package com.hpandro.nearbyplace.model

data class ResponseData(
        val html_attributions: List<Any>,
        val next_page_token: String = "",
        val results: List<Result>,
        val status: String = ""
)

data class Result(
        val geometry: Geometry,
        val icon: String,
        val id: String,
        val name: String,
        val opening_hours: OpeningHours,
        val photos: List<Photo>,
        val place_id: String,
        val rating: Double,
        val reference: String,
        val scope: String,
        val types: List<String>,
        val vicinity: String,
        val price_level: Int
)

data class Geometry(
        val location: Location,
        val viewport: Viewport
)

data class Location(
        val lat: Double,
        val lng: Double
)

data class Viewport(
        val northeast: Northeast,
        val southwest: Southwest
)

data class Southwest(
        val lat: Double,
        val lng: Double
)

data class Northeast(
        val lat: Double,
        val lng: Double
)

data class Photo(
        val height: Int,
        val html_attributions: List<String>,
        val photo_reference: String,
        val width: Int
)

data class OpeningHours(
        val open_now: Boolean,
        val weekday_text: List<Any>
)