package com.tunegocio.homefix.navigation

object Routes {
    // Auth
    const val SPLASH = "splash"
    const val LOGIN = "login"
    const val REGISTER = "register"

    // Cliente
    const val HOME_CLIENT = "home_client"
    const val NEW_REQUEST = "new_request"
    const val TECHNICIAN_LIST = "technician_list"
    const val REQUEST_TRACKING = "request_tracking/{requestId}"

    // Técnico
    const val HOME_TECHNICIAN = "home_technician"
    const val REQUEST_DETAIL = "request_detail/{requestId}"
    const val EARNINGS = "earnings"

    // Compartidas
    const val PROFILE = "profile"
    const val HISTORY = "history"
    const val RATING = "rating/{requestId}"
    const val SETTINGS = "settings"

    const val VERIFICAR_EMAIL = "verificar_email"
    const val OLVIDE_CONTRASENA = "olvide_contrasena"

    // Funciones helper para rutas con parámetros
    fun requestTracking(requestId: String) = "request_tracking/$requestId"
    fun requestDetail(requestId: String) = "request_detail/$requestId"
    fun rating(requestId: String) = "rating/$requestId"
}