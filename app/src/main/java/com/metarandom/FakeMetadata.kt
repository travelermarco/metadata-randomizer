package com.metarandom

import androidx.exifinterface.media.ExifInterface
import java.util.Calendar
import kotlin.math.abs
import kotlin.random.Random

data class DeviceProfile(val make: String, val model: String, val software: String)
data class CityCoord(val name: String, val lat: Double, val lon: Double)

object FakeMetadata {

    private val devices = listOf(
        DeviceProfile("samsung",  "SM-A546B",        "A546BXXS5CXD1"),
        DeviceProfile("samsung",  "SM-S911B",        "S911BXXS4CXD2"),
        DeviceProfile("samsung",  "SM-G991B",        "G991BXXS7GWJ1"),
        DeviceProfile("samsung",  "SM-A325F",        "A325FXXS6CWJ1"),
        DeviceProfile("samsung",  "SM-A135F",        "A135FXXS4CWJ2"),
        DeviceProfile("Xiaomi",   "Redmi Note 12",   "V14.0.2.0.TMGEUXM"),
        DeviceProfile("Xiaomi",   "2210129SG",       "OS1.0.7.0.TMAITUS"),
        DeviceProfile("Xiaomi",   "22071212AG",      "V13.0.11.0.TLSMIXM"),
        DeviceProfile("OPPO",     "CPH2483",         "CPH2483_11_F.30"),
        DeviceProfile("OPPO",     "CPH2307",         "CPH2307_11_F.50"),
        DeviceProfile("OnePlus",  "CPH2491",         "PGP110_14.0.0.500(EX01)"),
        DeviceProfile("OnePlus",  "PHB110",          "PHB110_13.1.0.516(EX01)"),
        DeviceProfile("realme",   "RMX3310",         "RMX3310_11_A.50"),
        DeviceProfile("realme",   "RMX3710",         "RMX3710_11_C.10"),
        DeviceProfile("motorola", "moto g84 5G",     "T2RP34.60-Q3-6-0"),
        DeviceProfile("motorola", "XT2203-1",        "S3RP33.20-Q4-4-4"),
        DeviceProfile("Nokia",    "TA-1581",         "01.110"),
        DeviceProfile("Nokia",    "TA-1563",         "00.140"),
        DeviceProfile("Sony",     "XQ-CQ54",         "62.2.A.0.459"),
        DeviceProfile("Sony",     "XQ-DQ54",         "67.1.A.1.116"),
        DeviceProfile("Google",   "Pixel 7a",        "TD2A.221216.004"),
        DeviceProfile("Google",   "Pixel 6",         "TP1A.221005.002"),
        DeviceProfile("vivo",     "V2248",           "PD2248F_EX_A_7.24.0"),
        DeviceProfile("HONOR",    "REA-NX9",         "REA-NX9 7.0.0.190(C10E2R1P2)"),
        DeviceProfile("Nothing",  "A065",            "Pong_U2.6-241218-1736"),
        DeviceProfile("asus",     "ASUS_AI2302",     "WW_AI2302-34.0804.2060.26"),
        DeviceProfile("TECNO",    "CK7n",            "CK7n-H6711E-Q-210917"),
        DeviceProfile("Infinix",  "X6833B",          "X6833B-H6251E-Q-231110"),
        DeviceProfile("ZTE",      "V2350",           "V2350V1.0.0B04"),
        DeviceProfile("TCL",      "T610K",           "T610K-TEUR-2PD.P12"),
    )

    private val cities = listOf(
        CityCoord("London",      51.5074,  -0.1278),
        CityCoord("Paris",       48.8566,   2.3522),
        CityCoord("Berlin",      52.5200,  13.4050),
        CityCoord("Rome",        41.9028,  12.4964),
        CityCoord("Madrid",      40.4168,  -3.7038),
        CityCoord("Amsterdam",   52.3676,   4.9041),
        CityCoord("Vienna",      48.2082,  16.3738),
        CityCoord("Brussels",    50.8503,   4.3517),
        CityCoord("Prague",      50.0755,  14.4378),
        CityCoord("Warsaw",      52.2297,  21.0122),
        CityCoord("Budapest",    47.4979,  19.0402),
        CityCoord("Barcelona",   41.3851,   2.1734),
        CityCoord("Lisbon",      38.7169,  -9.1399),
        CityCoord("Copenhagen",  55.6761,  12.5683),
        CityCoord("Stockholm",   59.3293,  18.0686),
        CityCoord("Helsinki",    60.1699,  24.9384),
        CityCoord("Oslo",        59.9139,  10.7522),
        CityCoord("Zurich",      47.3769,   8.5417),
        CityCoord("Munich",      48.1351,  11.5820),
        CityCoord("Hamburg",     53.5753,  10.0153),
        CityCoord("New York",    40.7128, -74.0060),
        CityCoord("Tokyo",       35.6762, 139.6503),
        CityCoord("Sydney",     -33.8688, 151.2093),
        CityCoord("Dubai",       25.2048,  55.2708),
        CityCoord("São Paulo",  -23.5505, -46.6333),
        CityCoord("Toronto",     43.6532, -79.3832),
        CityCoord("Melbourne",  -37.8136, 144.9631),
        CityCoord("Singapore",    1.3521, 103.8198),
        CityCoord("Seoul",       37.5665, 126.9780),
        CityCoord("Mexico City", 19.4326, -99.1332),
    )

    fun randomImageFilename(): String = "IMG_%08d.jpg".format(Random.nextInt(10_000_000, 99_999_999))
    fun randomVideoFilename(): String = "VID_%08d.mp4".format(Random.nextInt(10_000_000, 99_999_999))

    private fun randomDateTime(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -Random.nextInt(180, 1100))
        cal.set(Calendar.HOUR_OF_DAY, Random.nextInt(7, 21))
        cal.set(Calendar.MINUTE,      Random.nextInt(0, 60))
        cal.set(Calendar.SECOND,      Random.nextInt(0, 60))
        return "%04d:%02d:%02d %02d:%02d:%02d".format(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.SECOND)
        )
    }

    /**
     * Applies randomized fake EXIF to [exif] and returns a human-readable
     * summary of what was written (for display in the confirmation screen).
     */
    fun applyToExif(exif: ExifInterface): String {
        val device   = devices.random()
        val city     = cities.random()
        val lat      = city.lat + Random.nextDouble(-0.04, 0.04)
        val lon      = city.lon + Random.nextDouble(-0.04, 0.04)
        val dateTime = randomDateTime()

        // Device fingerprint → fake
        exif.setAttribute(ExifInterface.TAG_MAKE,     device.make)
        exif.setAttribute(ExifInterface.TAG_MODEL,    device.model)
        exif.setAttribute(ExifInterface.TAG_SOFTWARE, device.software)

        // Timestamps → fake
        exif.setAttribute(ExifInterface.TAG_DATETIME,           dateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,  dateTime)
        exif.setAttribute(ExifInterface.TAG_DATETIME_DIGITIZED, dateTime)

        // GPS → fake city with small jitter
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE,      toDms(abs(lat)))
        exif.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF,  if (lat >= 0) "N" else "S")
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE,     toDms(abs(lon)))
        exif.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, if (lon >= 0) "E" else "W")
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE,      "${Random.nextInt(5, 280)}/1")
        exif.setAttribute(ExifInterface.TAG_GPS_ALTITUDE_REF,  "0")
        // EXIF spec: TAG_GPS_DATESTAMP format is "YYYY:MM:DD" (colons, not slashes)
        exif.setAttribute(ExifInterface.TAG_GPS_DATESTAMP, dateTime.substring(0, 10))

        // Private fields → strip
        exif.setAttribute(ExifInterface.TAG_USER_COMMENT,       null)
        exif.setAttribute(ExifInterface.TAG_IMAGE_DESCRIPTION,  null)
        exif.setAttribute(ExifInterface.TAG_ARTIST,             null)
        exif.setAttribute(ExifInterface.TAG_COPYRIGHT,          null)
        exif.setAttribute(ExifInterface.TAG_CAMERA_OWNER_NAME,  null)
        exif.setAttribute(ExifInterface.TAG_BODY_SERIAL_NUMBER, null)
        exif.setAttribute(ExifInterface.TAG_ORIENTATION,        ExifInterface.ORIENTATION_NORMAL.toString())

        return "${device.make} ${device.model} · ${city.name} · ${dateTime.substring(0, 10)}"
    }

    // Decimal degrees → "DD/1,MM/1,SSSS/100" rational DMS string required by ExifInterface
    private fun toDms(decimal: Double): String {
        val deg  = decimal.toInt()
        val minD = (decimal - deg) * 60.0
        val min  = minD.toInt()
        val sec  = (minD - min) * 60.0
        return "$deg/1,$min/1,${(sec * 100).toInt()}/100"
    }
}
