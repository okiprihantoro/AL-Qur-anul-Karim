package com.example.prayer

data class CityOption(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val timezoneHours: Double
)

object IndonesianCities {
    val list = listOf(
        CityOption("Jakarta, Indonesia", -6.2088, 106.8456, 7.0),
        CityOption("Bandung, Indonesia", -6.9175, 107.6191, 7.0),
        CityOption("Surabaya, Indonesia", -7.2575, 112.7521, 7.0),
        CityOption("Yogyakarta, Indonesia", -7.7956, 110.3695, 7.0),
        CityOption("Semarang, Indonesia", -6.9667, 110.4167, 7.0),
        CityOption("Medan, Indonesia", 3.5952, 98.6722, 7.0),
        CityOption("Palembang, Indonesia", -2.9761, 104.7754, 7.0),
        CityOption("Banda Aceh, Indonesia", 5.5483, 95.3238, 7.0),
        CityOption("Padang, Indonesia", -0.9471, 100.4172, 7.0),
        CityOption("Pekanbaru, Indonesia", 0.5071, 101.4478, 7.0),
        CityOption("Bandar Lampung, Indonesia", -5.3971, 105.2668, 7.0),
        CityOption("Bogor, Indonesia", -6.5971, 106.8060, 7.0),
        CityOption("Bekasi, Indonesia", -6.2383, 106.9756, 7.0),
        CityOption("Tangerang, Indonesia", -6.1783, 106.6319, 7.0),
        CityOption("Depok, Indonesia", -6.4025, 106.7942, 7.0),
        CityOption("Malang, Indonesia", -7.9666, 112.6326, 7.0),
        CityOption("Solo (Surakarta), Indonesia", -7.5755, 110.8243, 7.0),
        CityOption("Pontianak, Indonesia", -0.0263, 109.3425, 7.0),
        CityOption("Banjarmasin, Indonesia", -3.3186, 114.5944, 8.0),
        CityOption("Balikpapan, Indonesia", -1.2379, 116.8529, 8.0),
        CityOption("Samarinda, Indonesia", -0.5022, 117.1536, 8.0),
        CityOption("Makassar, Indonesia", -5.1477, 119.4327, 8.0),
        CityOption("Manado, Indonesia", 1.4748, 124.8421, 8.0),
        CityOption("Denpasar, Indonesia", -8.6705, 115.2126, 8.0),
        CityOption("Mataram, Indonesia", -8.5833, 116.1167, 8.0),
        CityOption("Ambon, Indonesia", -3.6954, 128.1814, 9.0),
        CityOption("Jayapura, Indonesia", -2.5916, 140.6690, 9.0),
        CityOption("Kuala Lumpur, Malaysia", 3.1390, 101.6869, 8.0),
        CityOption("Singapura", 1.3521, 103.8198, 8.0),
        CityOption("Makkah, Arab Saudi", 21.4225, 39.8262, 3.0),
        CityOption("Madinah, Arab Saudi", 24.4672, 39.6111, 3.0)
    )
}
