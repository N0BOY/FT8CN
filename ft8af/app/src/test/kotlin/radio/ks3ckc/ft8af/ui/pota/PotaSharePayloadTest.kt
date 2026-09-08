package radio.ks3ckc.ft8af.ui.pota

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import radio.ks3ckc.ft8af.pota.model.PotaActivation
import radio.ks3ckc.ft8af.pota.model.PotaQso

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class PotaSharePayloadTest {
    private val activation = PotaActivation(1, "US-1234,US-5678", "K1AF", 1000, 2000, 1, "private notes")
    private val contact = PotaQso(1, "JA1ABC", "pm95", "20m", "FT8", "-10", "-12", "20260101", "815", null, null, "fn42")

    @Test fun publicPayloadUsesHistoricalFieldsAndExcludesNotes() {
        val result = buildPotaSharePayload(activation, listOf(contact))
        assertFalse(result.toString().contains("private notes"))
        assertEquals("K1AF", result.getString("call"))
        assertEquals(2, result.getJSONArray("parks").length())
        val qso = result.getJSONArray("contacts").getJSONObject(0)
        assertEquals("FN42", qso.getString("myGrid"))
        assertEquals("PM95", qso.getString("grid"))
        assertEquals("081500", qso.getString("time"))
        assertFalse(qso.has("rstSent"))
    }

    @Test fun missingGridStaysMissing() {
        val result = buildPotaSharePayload(activation, listOf(contact.copy(myGrid = "ZZ99", grid = "")))
        assertEquals("", result.getJSONArray("contacts").getJSONObject(0).getString("myGrid"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun activeActivationsCannotBeShared() {
        buildPotaSharePayload(activation.copy(endedAtMs = null), listOf(contact))
    }
}
