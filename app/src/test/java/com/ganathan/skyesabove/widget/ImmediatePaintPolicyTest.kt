package com.ganathan.skyesabove.widget

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [ImmediatePaintPolicy.suppressNoDataPaint] — the pure decision behind whether the
 * immediate (broadcast) paint should suppress a NO-DATA repaint. This is the logic that fixes the
 * "both halves vanish ~twice an hour" report: the on-device diagnostics log proved every both-blank
 * cycle was the ~30-min `system_update` framework tick being denied network access in Doze/standby
 * (both halves unreachable, yet a validated network sat unused in the inventory), while the
 * network-capable WorkManager paths never failed. That is not a real outage, so we must not flash
 * a dash — we leave the last render for WorkManager. A genuine no-network outage must still blank.
 */
class ImmediatePaintPolicyTest {

    private fun suppress(fc: SourceStatus, hm: SourceStatus, usableNet: Boolean) =
        ImmediatePaintPolicy.suppressNoDataPaint(fc, hm, usableNet)

    @Test
    fun `both halves unreachable with a usable network is the background-denial false blank - suppress`() {
        // The exact 30-min system_update signature from the field log: net present, both fail.
        assertTrue(suppress(SourceStatus.FETCH_ERROR, SourceStatus.FETCH_ERROR, usableNet = true))
    }

    @Test
    fun `both halves unreachable with NO usable network is a genuine outage - do not suppress`() {
        // Airplane mode / real dead zone: real-time-only means the widget SHOULD blank here.
        assertFalse(suppress(SourceStatus.FETCH_ERROR, SourceStatus.FETCH_ERROR, usableNet = false))
    }

    @Test
    fun `a fully healthy cycle is never suppressed`() {
        assertFalse(suppress(SourceStatus.OK, SourceStatus.OK, usableNet = true))
    }

    @Test
    fun `forecast ok but home unreachable is not suppressed - show the fresh half plus a home flag`() {
        assertFalse(suppress(SourceStatus.OK, SourceStatus.FETCH_ERROR, usableNet = true))
    }

    @Test
    fun `home ok but forecast unreachable is not suppressed - show the fresh half plus a forecast flag`() {
        assertFalse(suppress(SourceStatus.FETCH_ERROR, SourceStatus.OK, usableNet = true))
    }

    @Test
    fun `a stale home half is a real dash and must paint - the sensor is down, not the network`() {
        // STALE means WU was reached (network worked) but the obs is old => Pi down. Never suppress:
        // that dash is real and honours real-time-only.
        assertFalse(suppress(SourceStatus.OK, SourceStatus.STALE, usableNet = true))
        assertFalse(suppress(SourceStatus.STALE, SourceStatus.STALE, usableNet = true))
    }

    @Test
    fun `forecast unreachable and home stale is not the denial signature - do not suppress`() {
        // Home was reached (stale) so the network worked for at least one stack; not a full denial.
        assertFalse(suppress(SourceStatus.FETCH_ERROR, SourceStatus.STALE, usableNet = true))
    }
}
