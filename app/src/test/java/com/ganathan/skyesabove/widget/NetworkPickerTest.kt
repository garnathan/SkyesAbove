package com.ganathan.skyesabove.widget

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [NetworkPicker.rank] — the pure decision behind which network a widget refresh
 * binds to. This is the logic that was wrong in the "widget blank on 5G, fine on Wi-Fi" report:
 * the old picker required NET_CAPABILITY_VALIDATED, so during the window where 5G advertises
 * INTERNET but the OS hasn't finished its async VALIDATED probe (right after a Wi-Fi->cellular
 * handoff, Doze exit, or a cold broadcast process) it found no network, never bound, and blanked.
 */
class NetworkPickerTest {

    private fun cell(internet: Boolean = true, validated: Boolean = false, default: Boolean = false) =
        NetCandidate("cell", internet, validated, default)

    private fun wifi(internet: Boolean = true, validated: Boolean = false, default: Boolean = false) =
        NetCandidate("wifi", internet, validated, default)

    @Test
    fun `no networks yields none`() {
        assertEquals(-1 to NetworkPicker.NONE, NetworkPicker.rank(emptyList()))
    }

    @Test
    fun `healthy validated default wifi is the fast path`() {
        val (idx, label) = NetworkPicker.rank(listOf(wifi(validated = true, default = true)))
        assertEquals(0, idx)
        assertEquals("wifi:val:default", label)
    }

    @Test
    fun `validated cellular is chosen over a connected-but-unvalidated default wifi (dead-wifi fix)`() {
        // The classic captive/dead-Wi-Fi default with validated cellular alongside it.
        val candidates = listOf(
            wifi(validated = false, default = true),   // connected default, but NOT validated
            cell(validated = true)                     // validated cellular
        )
        val (idx, label) = NetworkPicker.rank(candidates)
        assertEquals(1, idx)
        assertEquals("cell:val", label)
    }

    @Test
    fun `unvalidated cellular is used when nothing is validated yet the 5G fix`() {
        // The reported failure: no network is VALIDATED at this instant, but cellular carries
        // INTERNET. The old picker returned nothing here and the widget blanked; the fallback
        // now binds cellular so the fetch can succeed over 5G.
        val candidates = listOf(cell(validated = false))
        val (idx, label) = NetworkPicker.rank(candidates)
        assertEquals(0, idx)
        assertEquals("cell:unval", label)
    }

    @Test
    fun `unvalidated cellular is preferred over unvalidated wifi in the fallback`() {
        // A dead default Wi-Fi is the usual reason nothing is validated, so when we must fall
        // back to an unvalidated network, cellular is the better bet.
        val candidates = listOf(
            wifi(validated = false, default = true),
            cell(validated = false)
        )
        val (idx, label) = NetworkPicker.rank(candidates)
        assertEquals(1, idx)
        assertEquals("cell:unval", label)
    }

    @Test
    fun `unvalidated wifi is still used if it is the only internet network`() {
        val candidates = listOf(wifi(validated = false, default = true))
        val (idx, label) = NetworkPicker.rank(candidates)
        assertEquals(0, idx)
        assertEquals("wifi:unval", label)
    }

    @Test
    fun `a validated non-default network beats an unvalidated default`() {
        val candidates = listOf(
            cell(validated = false, default = true),
            wifi(validated = true)
        )
        val (idx, label) = NetworkPicker.rank(candidates)
        assertEquals(1, idx)
        assertEquals("wifi:val", label)
    }

    @Test
    fun `networks without INTERNET capability are ignored`() {
        // e.g. a Wi-Fi that's associated but has no internet route at all.
        val candidates = listOf(wifi(internet = false, validated = false, default = true))
        assertEquals(-1 to NetworkPicker.NONE, NetworkPicker.rank(candidates))
    }
}
