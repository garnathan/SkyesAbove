package com.ganathan.skyesabove.data.repository

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.ZoneId

/**
 * Unit tests for [isoToLocalHhMm] — the conversion of a sunrise-sunset.org ISO timestamp (UTC,
 * formatted=0) to a local-zone "HH:mm". This is the logic behind the "sunset shown an hour early"
 * report: Skyes Above string-chopped the UTC clock field and ignored the offset, so during Irish
 * Summer Time (UTC+1) it displayed 20:52 instead of 21:52. The real API values for the reported
 * day are used verbatim so the expected outputs match what SkyeJS shows.
 */
class SunTimeFormatTest {

    private val dublin = ZoneId.of("Europe/Dublin")
    private val utc = ZoneId.of("UTC")

    // The exact values the live API returned for the reported location/day.
    private val apiSunset = "2026-07-09T20:52:45+00:00"
    private val apiCivilTwilightEnd = "2026-07-09T21:40:12+00:00"
    private val apiSunrise = "2026-07-09T04:06:40+00:00"

    @Test
    fun `summer UTC sunset converts to IST local time (the bug)`() {
        // 20:52 UTC + 1h IST = 21:52, matching SkyeJS. The old code showed 20:52.
        assertEquals("21:52", isoToLocalHhMm(apiSunset, dublin))
    }

    @Test
    fun `summer UTC civil twilight end converts to IST local time`() {
        // 21:40 UTC + 1h IST = 22:40, matching SkyeJS ("usable light").
        assertEquals("22:40", isoToLocalHhMm(apiCivilTwilightEnd, dublin))
    }

    @Test
    fun `summer UTC sunrise converts to IST local time`() {
        assertEquals("05:06", isoToLocalHhMm(apiSunrise, dublin))
    }

    @Test
    fun `same instant in UTC keeps the UTC clock time`() {
        // Sanity: with no offset to apply the clock time is unchanged (what the old code did).
        assertEquals("20:52", isoToLocalHhMm(apiSunset, utc))
    }

    @Test
    fun `winter time has no DST offset in Dublin`() {
        // January: Dublin is on UTC (GMT), so no shift — guards against hard-coding +1.
        assertEquals("16:30", isoToLocalHhMm("2026-01-15T16:30:00+00:00", dublin))
    }

    @Test
    fun `a non-UTC offset is normalised to the target zone`() {
        // An ISO with a +02:00 offset (e.g. CEST) rendered in Dublin (UTC+1 in summer) drops 1h.
        assertEquals("20:52", isoToLocalHhMm("2026-07-09T21:52:45+02:00", dublin))
    }

    @Test
    fun `an unparseable string falls back to the raw input`() {
        assertEquals("not-a-time", isoToLocalHhMm("not-a-time", dublin))
    }
}
