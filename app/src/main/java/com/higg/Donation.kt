package com.higg

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Environment
import android.provider.MediaStore
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.QRCodeWriter
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel

/** Thông tin ủng hộ tác giả và mã VietQR (chuẩn EMVCo của NAPAS) để quét bằng app ngân hàng. */
object Donation {
    const val BANK_NAME = "Timo by Ban Viet Bank"
    private const val BANK_BIN = "963388" // Timo trong danh sách ngân hàng VietQR
    const val ACCOUNT_NUMBER = "9037041009728"
    const val ACCOUNT_NAME = "NGUYEN HUU NAM"
    const val TRANSFER_NOTE = "Ung Ho HiGG"
    const val PAYPAL = "starnam23@gmail.com"

    /** Chuỗi VietQR tĩnh: chuyển tới tài khoản, người chuyển tự nhập số tiền. */
    val vietQrPayload: String by lazy {
        val payload = tlv("00", "01") +
            tlv("01", "11") +
            tlv(
                "38",
                tlv("00", "A000000727") +
                    tlv("01", tlv("00", BANK_BIN) + tlv("01", ACCOUNT_NUMBER)) +
                    tlv("02", "QRIBFTTA")
            ) +
            tlv("53", "704") +
            tlv("58", "VN") +
            tlv("62", tlv("08", TRANSFER_NOTE)) +
            "6304"
        payload + "%04X".format(crc16(payload))
    }

    fun qrMatrix(): BitMatrix = QRCodeWriter().encode(
        vietQrPayload, BarcodeFormat.QR_CODE, 0, 0,
        mapOf(EncodeHintType.ERROR_CORRECTION to ErrorCorrectionLevel.M, EncodeHintType.MARGIN to 0),
    )

    private const val QR_FILE = "HiGG-ung-ho.png"
    private val QR_DIR = Environment.DIRECTORY_PICTURES + "/HiGG/"

    /**
     * Lưu mã QR (nền trắng, chừa lề 4 ô theo chuẩn) vào thư mục Ảnh để chọn từ app ngân hàng.
     * Đã lưu rồi thì dùng lại ảnh cũ, không tạo bản trùng.
     */
    fun saveQrToGallery(context: Context): Boolean = runCatching {
        if (qrAlreadySaved(context)) return true
        val matrix = qrMatrix()
        val scale = 16
        val quiet = 4
        val size = (matrix.width + quiet * 2) * scale
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.WHITE)
        for (y in 0 until matrix.height) for (x in 0 until matrix.width) {
            if (!matrix[x, y]) continue
            for (dy in 0 until scale) for (dx in 0 until scale) {
                bitmap.setPixel((x + quiet) * scale + dx, (y + quiet) * scale + dy, Color.BLACK)
            }
        }
        val values = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, QR_FILE)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, QR_DIR)
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)!!
        resolver.openOutputStream(uri)!!.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        true
    }.getOrDefault(false)

    /**
     * Ảnh do chính app lưu thì app đọc lại được mà không cần quyền truy cập ảnh. Tìm theo tiền tố
     * vì hệ thống tự đổi tên thành "HiGG-ung-ho (1).png" khi trùng với ảnh không còn thuộc app
     * (vd. ảnh lưu từ lần cài trước).
     */
    private fun qrAlreadySaved(context: Context): Boolean = context.contentResolver.query(
        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        arrayOf(MediaStore.Images.Media._ID),
        "${MediaStore.Images.Media.DISPLAY_NAME} LIKE ? AND ${MediaStore.Images.Media.RELATIVE_PATH} = ?",
        arrayOf(QR_FILE.removeSuffix(".png") + "%", QR_DIR),
        null,
    )?.use { it.count > 0 } ?: false

    private fun tlv(id: String, value: String) = id + "%02d".format(value.length) + value

    /** CRC-16/CCITT-FALSE theo chuẩn EMVCo. */
    private fun crc16(data: String): Int {
        var crc = 0xFFFF
        for (byte in data.toByteArray(Charsets.UTF_8)) {
            crc = crc xor ((byte.toInt() and 0xFF) shl 8)
            repeat(8) {
                crc = if (crc and 0x8000 != 0) (crc shl 1) xor 0x1021 else crc shl 1
                crc = crc and 0xFFFF
            }
        }
        return crc
    }
}
