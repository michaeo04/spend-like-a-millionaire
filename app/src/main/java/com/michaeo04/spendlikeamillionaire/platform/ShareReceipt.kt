package com.michaeo04.spendlikeamillionaire.platform

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.text.TextPaint
import android.text.TextUtils
import androidx.core.content.FileProvider
import com.michaeo04.spendlikeamillionaire.ui.cart.ReceiptModel
import java.io.File
import java.io.FileOutputStream

/** Renders the receipt to a PNG in the cache dir and opens the system share sheet. */
object ShareReceipt {
    private const val WIDTH = 1080
    private const val MARGIN = 72f
    private const val LINE_HEIGHT = 76f

    fun share(context: Context, model: ReceiptModel) {
        val bitmap = render(model)
        val dir = File(context.cacheDir, "receipts").apply { mkdirs() }
        val file = File(dir, "receipt.png")
        FileOutputStream(file).use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, model.title))
    }

    internal fun render(model: ReceiptModel): Bitmap {
        val rows = model.lines.size + (if (model.moreCount > 0) 1 else 0)
        val height = (330 + rows * LINE_HEIGHT + 420).toInt()
        val bitmap = Bitmap.createBitmap(WIDTH, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.parseColor("#F6F8F4"))

        val ink = Color.parseColor("#12372A")
        val muted = Color.parseColor("#5C6B63")
        fun paint(size: Float, bold: Boolean = false, color: Int = ink, align: Paint.Align = Paint.Align.LEFT) =
            TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = size
                this.color = color
                textAlign = align
                typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }

        var y = 150f
        canvas.drawText(model.title, WIDTH / 2f, y, paint(60f, bold = true, align = Paint.Align.CENTER))
        y += 70f
        canvas.drawText(model.personName, WIDTH / 2f, y, paint(44f, color = muted, align = Paint.Align.CENTER))
        y += 50f
        val dash = Paint().apply { color = muted; strokeWidth = 3f }
        canvas.drawLine(MARGIN, y, WIDTH - MARGIN, y, dash)
        y += 70f

        val labelPaint = paint(40f)
        val amountPaint = paint(40f, bold = true, align = Paint.Align.RIGHT)
        for (line in model.lines) {
            val amountWidth = amountPaint.measureText(line.amount)
            val maxLabel = WIDTH - 2 * MARGIN - amountWidth - 24f
            val label = TextUtils.ellipsize(line.label, labelPaint, maxLabel, TextUtils.TruncateAt.END)
            canvas.drawText(label.toString(), MARGIN, y, labelPaint)
            canvas.drawText(line.amount, WIDTH - MARGIN, y, amountPaint)
            y += LINE_HEIGHT
        }
        if (model.moreCount > 0) {
            canvas.drawText(model.moreText, MARGIN, y, paint(36f, color = muted))
            y += LINE_HEIGHT
        }
        y += 10f
        canvas.drawLine(MARGIN, y, WIDTH - MARGIN, y, dash)
        y += 80f
        fun total(label: String, value: String, size: Float, bold: Boolean) {
            canvas.drawText(label, MARGIN, y, paint(size, bold))
            canvas.drawText(value, WIDTH - MARGIN, y, paint(size, bold, align = Paint.Align.RIGHT))
            y += size * 1.6f
        }
        total(model.totalLabel, model.total, 52f, true)
        total(model.remainingLabel, model.remaining, 40f, false)
        total(model.percentLabel, model.percent, 40f, false)
        y += 30f
        canvas.drawText(model.footer, WIDTH / 2f, y, paint(34f, color = muted, align = Paint.Align.CENTER))
        return bitmap
    }
}
