package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.util.CourierDetector
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Scan Resi", appName)
  }

  @Test
  fun `test courier detector for indonesian logistics`() {
    val jnt = CourierDetector.detect("JP1234567890")
    assertEquals("J&T Express", jnt.courierName)

    val jne = CourierDetector.detect("0112345678901234")
    assertEquals("JNE Express", jne.courierName)

    val sicepat = CourierDetector.detect("000123456789")
    assertEquals("SiCepat", sicepat.courierName)

    val spx = CourierDetector.detect("SPXID01234567890")
    assertEquals("Shopee Xpress", spx.courierName)
  }
}
