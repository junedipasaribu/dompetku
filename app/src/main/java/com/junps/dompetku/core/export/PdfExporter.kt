package com.junps.dompetku.core.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import com.junps.dompetku.core.format.formatRupiah
import com.junps.dompetku.domain.model.TransactionType
import com.junps.dompetku.domain.model.TransactionWithCategory
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class PdfExporter(private val context: Context) {
    fun exportTransactionsToPdf(
        transactions: List<TransactionWithCategory>,
        destinationUri: Uri
    ): Boolean {
        if (transactions.isEmpty()) return false

        val document = PdfDocument()
        var pageNumber = 1
        var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        var canvas = page.canvas

        val paint = Paint().apply {
            color = Color.BLACK
            textSize = 12f
        }
        val titlePaint = Paint().apply {
            color = Color.BLACK
            textSize = 18f
            isFakeBoldText = true
        }

        var yPos = 50f
        canvas.drawText("Laporan Transaksi DompetKu", 50f, yPos, titlePaint)
        
        yPos += 20f
        val dateFormat = SimpleDateFormat("dd MMM yyyy HH:mm", Locale.getDefault())
        canvas.drawText("Tanggal Cetak: ${dateFormat.format(Date())}", 50f, yPos, paint)

        yPos += 40f
        drawTableHeader(canvas, paint, yPos)
        yPos += 25f
        var totalPemasukan = 0L
        var totalPengeluaran = 0L

        transactions.forEachIndexed { index, transaction ->
            if (yPos > 770f) {
                document.finishPage(page)
                pageNumber++
                page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
                canvas = page.canvas
                yPos = 50f
                drawTableHeader(canvas, paint, yPos)
                yPos += 25f
            }

            if (transaction.categoryType == TransactionType.INCOME) {
                totalPemasukan += transaction.amount
                paint.color = Color.rgb(0, 150, 0) // Green
            } else {
                totalPengeluaran += transaction.amount
                paint.color = Color.RED
            }

            canvas.drawText("${index + 1}", 50f, yPos, paint)
            canvas.drawText(SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(transaction.transactionDate)), 80f, yPos, paint)
            canvas.drawText(transaction.categoryName, 180f, yPos, paint)
            canvas.drawText(formatRupiah(transaction.amount), 300f, yPos, paint)
            canvas.drawText((transaction.note ?: "").take(15), 420f, yPos, paint)

            yPos += 20f
        }

        if (yPos > 730f) {
            document.finishPage(page)
            pageNumber++
            page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
            canvas = page.canvas
            yPos = 50f
            canvas.drawText("Ringkasan", 50f, yPos, titlePaint)
            yPos += 35f
        }
        paint.color = Color.BLACK
        canvas.drawLine(50f, yPos, 545f, yPos, paint)
        yPos += 22f
        canvas.drawText("Total pemasukan: ${formatRupiah(totalPemasukan)}", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Total pengeluaran: ${formatRupiah(totalPengeluaran)}", 50f, yPos, paint)
        yPos += 20f
        canvas.drawText("Selisih: ${formatRupiah(totalPemasukan - totalPengeluaran)}", 50f, yPos, paint)

        document.finishPage(page)

        return try {
            val outputStream = context.contentResolver.openOutputStream(destinationUri)
                ?: error("Lokasi penyimpanan PDF tidak dapat dibuka.")
            outputStream.use(document::writeTo)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        } finally {
            document.close()
        }
    }

    private fun drawTableHeader(canvas: Canvas, paint: Paint, y: Float) {
        paint.color = Color.BLACK
        canvas.drawText("No", 50f, y, paint)
        canvas.drawText("Tanggal", 80f, y, paint)
        canvas.drawText("Kategori", 180f, y, paint)
        canvas.drawText("Nominal", 300f, y, paint)
        canvas.drawText("Catatan", 420f, y, paint)
        canvas.drawLine(50f, y + 5f, 545f, y + 5f, paint)
    }
}
