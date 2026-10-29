package personal.focusflight.domain.ticket

import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.pdf417.PDF417Writer
import com.google.zxing.pdf417.encoder.Dimensions

object PassBarcode {
    fun payload(id: String) = "FOCUS FLIGHT: $id"
    fun encode(id: String) = PDF417Writer().encode(payload(id),BarcodeFormat.PDF_417,960,180,mapOf(
        EncodeHintType.MARGIN to 12,
        EncodeHintType.ERROR_CORRECTION to 2,
        EncodeHintType.PDF417_DIMENSIONS to Dimensions(6,10,3,20),
    ))
}
