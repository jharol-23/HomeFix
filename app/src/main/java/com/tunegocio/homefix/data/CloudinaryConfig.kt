package com.tunegocio.homefix.data

object CloudinaryConfig {
    const val CLOUD_NAME = "dnaul8kll"      // ← reemplaza con el tuyo
    const val UPLOAD_PRESET = "homefix_upload"         // ← el que creaste

    fun getUploadUrl(): String {
        return "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"
    }
}