package dev.flightlink

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import java.math.BigInteger

class LinkConverterTest {

    @Test
    fun fr24HexIdBecomesSkycardsDecimalId() {
        val conversion = LinkConverter.convertText(
            "SU6325 from St. Petersburg to Kaliningrad https://fr24.com/SDM6325/42693275"
        )

        assertNotNull(conversion)
        assertEquals(Conversion.Source.FLIGHTRADAR24, conversion!!.source)
        assertEquals("42693275", conversion.sourceId)
        assertEquals("1114190453", conversion.targetId)
        assertEquals("https://skycards.app/flight/-/1114190453", conversion.targetUrl)
    }

    @Test
    fun skycardsDecimalIdBecomesFr24HexId() {
        val conversion = LinkConverter.convertText(
            "https://skycards.app/flight/SU95/1114190453"
        )

        assertNotNull(conversion)
        assertEquals(Conversion.Source.SKYCARDS, conversion!!.source)
        assertEquals("1114190453", conversion.sourceId)
        assertEquals("42693275", conversion.targetId)
        assertEquals("https://fr24.com/42693275", conversion.targetUrl)
    }

    @Test
    fun shortFormsWork() {
        val toSkycards = LinkConverter.convertText("https://fr24.com/42693275")
        assertEquals("https://skycards.app/flight/-/1114190453", toSkycards?.targetUrl)

        val toFr24 = LinkConverter.convertText("https://skycards.app/flight/-/1114190453")
        assertEquals("https://fr24.com/42693275", toFr24?.targetUrl)
    }

    @Test
    fun hexIdWithLettersIsConverted() {
        val conversion = LinkConverter.convertText("https://fr24.com/ABC123/1a2b3c4d")

        assertNotNull(conversion)
        assertEquals("1a2b3c4d", conversion!!.sourceId)
        assertEquals(BigInteger("1a2b3c4d", 16).toString(10), conversion.targetId)
    }

    @Test
    fun registrationIsExtractedForAircraftFallback() {
        val conversion = LinkConverter.convertText(
            "https://share.skycards.app/flight/SU95/1114190453?title=Sukhoi&reg=RA-89142"
        )

        assertEquals("RA-89142", conversion?.registration)
        assertEquals("https://fr24.com/42693275", conversion?.targetUrl)
    }

    @Test
    fun linkWithoutNumericIdIsRejected() {
        assertNull(LinkConverter.convertText("https://fr24.com/SDM6325"))
    }

    @Test
    fun unrelatedTextIsRejected() {
        assertNull(LinkConverter.convertText("no links here, just a flight number SU6325"))
    }
}
