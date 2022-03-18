package com.example.penaltykick

import android.graphics.Bitmap
import java.io.ByteArrayOutputStream
import java.util.*

class BitmapConverter {
    fun bitmapToString(bitmap: Bitmap):String{
        val stream=ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.PNG,100,stream)

        val bytes=stream.toByteArray()

        return Base64.getEncoder().encodeToString(bytes)
    }
}