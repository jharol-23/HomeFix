package com.tunegocio.homefix.data.model

data class ReviewModel(
    val reviewId: String = "",
    val requestId: String = "",
    val clientId: String = "",
    val technicianId: String = "",
    val stars: Int = 0,
    val comment: String = "",
    val createdAt: Long = 0L
)