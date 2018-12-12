package com.popalay.kvytok

import android.graphics.Bitmap
import android.util.Log

data class Ticket(
    val ticketNumber: String,
    val passenger: String,
    val trainNumber: String,
    val carNumber: String,
    val seatNumber: String,
    val departure: String,
    val arrival: String,
    val departureDate: String,
    val arrivalDate: String,
    val qrCode: Bitmap
) {

    override fun toString(): String = """
Ticket number: $ticketNumber
Passenger: $passenger
Train number: $trainNumber
Car number: $carNumber
Seat number: $seatNumber
Departure: $departure
Arrival: $arrival
Departure date: $departureDate
Arrival date: $arrivalDate
    """.trimIndent()
}

fun parseTicket(rawText: String?, qrCode: Bitmap): Ticket? {
    Log.d("sss", rawText)
    return rawText?.trimIndent()?.lines()?.takeIf { it.isNotEmpty() && it.size > 14 }?.let {
        Ticket(
            trainNumber = it.getOrNull(0)?.trim() ?: "",
            departure = it.getOrNull(1)?.substringAfterLast(")")?.trim()?.toLowerCase()?.capitalize()
                ?: "",
            arrival = it.getOrNull(2)?.substringAfterLast(")")?.trim()?.toLowerCase()?.capitalize()
                ?: "",
            departureDate = it.getOrNull(3)?.trim() ?: "",
            arrivalDate = it.getOrNull(4)?.trim() ?: "",
            carNumber = it.getOrNull(5)?.trim() ?: "",
            seatNumber = it.getOrNull(6)?.trim() ?: "",
            passenger = it.getOrNull(8)?.toLowerCase()?.split(' ')?.joinToString(" ") { s -> s.capitalize() }?.trim()
                ?: "",
            ticketNumber = it.getOrNull(14)?.trim() ?: "",
            qrCode = qrCode
        )
    }
}