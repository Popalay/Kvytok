package com.popalay.kvytok

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.pdf.PdfRenderer
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.firebase.ml.vision.FirebaseVision
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetectorOptions
import com.google.firebase.ml.vision.common.FirebaseVisionImage
import java.io.File

class MainActivity : AppCompatActivity() {

    companion object {

        private const val REQUEST_CODE_FILE = 112
        private const val REQUEST_CODE_READ_FILE = 113
    }

    private val buttonChooseFile: MaterialButton by bindView(R.id.button_choose_file)
    private val layoutTicket: TicketLayout by bindView(R.id.layout_ticket)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        layoutTicket.ticket = null
        buttonChooseFile.setOnClickListener {
            requestReadFilePermission()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_FILE && resultCode == Activity.RESULT_OK && data != null) {
            val fileUri = data.data
            val file = File(fileUri?.getRealPath(this))
            file.pdfFileToBitmaps().firstOrNull()?.removeTransparentBackground()?.let {
                getQRCodeDetails(it)
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>, grantResults: IntArray
    ) {
        if (requestCode == REQUEST_CODE_READ_FILE) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                openFileChooser()
            }
        }
    }

    private fun File.pdfFileToBitmaps(): List<Bitmap> =
        mutableListOf<Bitmap>().apply {
            try {
                val renderer = PdfRenderer(
                    ParcelFileDescriptor.open(
                        this@pdfFileToBitmaps,
                        ParcelFileDescriptor.MODE_READ_ONLY
                    )
                )

                var bitmap: Bitmap
                val pageCount = renderer.pageCount
                for (i in 0 until pageCount) {
                    val page = renderer.openPage(i)

                    val ratio = resources.displayMetrics.densityDpi / 72
                    val width = page.width * ratio
                    val height = page.height * ratio
                    bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    add(bitmap)
                    page.close()

                }
                renderer.close()
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
        }

    private fun requestReadFilePermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                REQUEST_CODE_READ_FILE
            )
        } else {
            openFileChooser()
        }
    }

    private fun openFileChooser() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "application/pdf"
            addCategory(Intent.CATEGORY_OPENABLE)
        }
        startActivityForResult(Intent.createChooser(intent, "select file"), REQUEST_CODE_FILE)
    }

    private fun getQRCodeDetails(bitmap: Bitmap) {
        val options = FirebaseVisionBarcodeDetectorOptions.Builder()
            .setBarcodeFormats(FirebaseVisionBarcode.FORMAT_QR_CODE)
            .build()
        val detector = FirebaseVision.getInstance().getVisionBarcodeDetector(options)
        val image = FirebaseVisionImage.fromBitmap(bitmap)
        detector.detectInImage(image)
            .addOnSuccessListener {
                for (firebaseBarcode in it) {
                    val qrCode = bitmap.crop(firebaseBarcode.boundingBox)
                    val ticket = parseTicket(firebaseBarcode.displayValue, qrCode)
                    if (ticket == null) {
                        Toast.makeText(
                            baseContext,
                            "Unsupported type of ticket, please try another",
                            Toast.LENGTH_SHORT
                        )
                            .show()
                    } else {
                        layoutTicket.ticket = ticket
                    }
                }
            }
            .addOnFailureListener {
                it.printStackTrace()
                Toast.makeText(baseContext, "Sorry, something went wrong!", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun Bitmap.removeTransparentBackground(): Bitmap {
        val converted = Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

        Canvas(converted).apply {
            drawColor(Color.WHITE)
            drawBitmap(this@removeTransparentBackground, 0F, 0F, null)
        }
        return converted
    }

    private fun Bitmap.crop(rect: Rect?): Bitmap = rect?.let {
        Bitmap.createBitmap(
            this,
            it.left,
            it.top,
            it.width(),
            it.height()
        )
    } ?: this
}