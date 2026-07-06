package com.ganathan.skyesabove.data.api

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Streaming
import retrofit2.http.Url

/**
 * Fetches the garden Pi's published history from the public GitHub repo
 * (garnathan/wunderground-killi). History is monthly CSV files under `data/YYYY-MM.csv`;
 * `latest.json` holds the most recent single reading.
 *
 * We use a `@Url` (absolute) so one API can pull any month's file, and return the raw
 * [ResponseBody] (no converter needed) — the repository parses the CSV itself.
 */
interface GardenHistoryApi {

    /** Fetch a raw file (CSV or JSON) by absolute URL. 404 => that month has no data yet. */
    @Streaming
    @GET
    suspend fun getRaw(@Url absoluteUrl: String): Response<ResponseBody>

    companion object {
        // Retrofit needs a baseUrl even when every call uses an absolute @Url.
        const val BASE_URL = "https://raw.githubusercontent.com/"
        const val REPO_BASE = "https://raw.githubusercontent.com/garnathan/wunderground-killi/main/"

        /** Absolute URL of a month's CSV, e.g. monthCsvUrl("2026-07"). */
        fun monthCsvUrl(yearMonth: String): String = "${REPO_BASE}data/$yearMonth.csv"
    }
}
