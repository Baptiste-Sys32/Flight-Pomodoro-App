package personal.focusflight

import com.google.zxing.BinaryBitmap
import com.google.zxing.RGBLuminanceSource
import com.google.zxing.common.HybridBinarizer
import com.google.zxing.pdf417.PDF417Reader
import org.junit.Assert.assertEquals
import org.junit.Test
import personal.focusflight.domain.ticket.PassBarcode

class PassBarcodeTest {
    @Test fun passReferenceCanBeDecodedWithoutExposingItsDestination() {
        val id="7e0397a5-2fd1-49c4-b550-1366fbb27d5d"
        val m=PassBarcode.encode(id)
        val pixels=IntArray(m.width*m.height) {i->if(m[i%m.width,i/m.width]) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()}
        val result=PDF417Reader().decode(BinaryBitmap(HybridBinarizer(RGBLuminanceSource(m.width,m.height,pixels))))
        assertEquals(PassBarcode.payload(id),result.text)
    }
}
