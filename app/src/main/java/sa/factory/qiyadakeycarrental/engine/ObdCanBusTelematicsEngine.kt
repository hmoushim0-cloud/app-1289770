package sa.factory.qiyadakeycarrental.engine

import kotlin.math.roundToInt

data class VehicleFleetTelemetry(
    val speedKmh: Int,
    val engineRpm: Int,
    val fuelLevelPct: Double,
    val coolantTempC: Int,
    val runTimeSec: Int,
    val odometerKm: Long,
    val dtcCodes: List<String>,
    val ecoScore: Int,
    val isHarshEvent: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)

class ObdCanBusTelematicsEngine {

    companion object {
        const val PID_COOLANT_TEMP = 0x05
        const val PID_ENGINE_RPM = 0x0C
        const val PID_VEHICLE_SPEED = 0x0D
        const val PID_RUN_TIME = 0x1F
        const val PID_FUEL_LEVEL = 0x2F
        const val PID_DISTANCE_TRAVELED = 0x31
    }

    private var previousSpeed: Int = 0
    private var lastSpeedTimestamp: Long = 0L

    fun decodeEngineRpm(byteA: Int, byteB: Int): Int {
        return ((byteA and 0xFF) * 256 + (byteB and 0xFF)) / 4
    }

    fun decodeVehicleSpeed(byteA: Int): Int {
        return byteA and 0xFF
    }

    fun decodeFuelLevel(byteA: Int): Double {
        val pct = (100.0 * (byteA and 0xFF)) / 255.0
        return (pct * 10.0).roundToInt() / 10.0
    }

    fun decodeCoolantTemp(byteA: Int): Int {
        return (byteA and 0xFF) - 40
    }

    fun decodeRunTimeSeconds(byteA: Int, byteB: Int): Int {
        return (byteA and 0xFF) * 256 + (byteB and 0xFF)
    }

    fun decodeDtc(byteA: Int, byteB: Int): String? {
        if (byteA == 0 && byteB == 0) return null

        val systemChar = when ((byteA ushr 6) and 0x03) {
            0 -> 'P' // Powertrain
            1 -> 'C' // Chassis
            2 -> 'B' // Body
            3 -> 'U' // Network
            else -> 'P'
        }

        val digit1 = (byteA ushr 4) and 0x03
        val digit2 = byteA and 0x0F
        val digit3 = (byteB ushr 4) and 0x0F
        val digit4 = byteB and 0x0F

        return "%c%d%X%X%X".format(systemChar, digit1, digit2, digit3, digit4)
    }

    fun parseCanBusFrame(
        speed: Int,
        rpm: Int,
        fuelPct: Double,
        coolantC: Int,
        runTime: Int,
        baseOdometerKm: Long,
        rawDtcBytes: List<Pair<Int, Int>> = emptyList()
    ): VehicleFleetTelemetry {
        val dtcList = rawDtcBytes.mapNotNull { decodeDtc(it.first, it.second) }

        // Driving behavior evaluation
        val now = System.currentTimeMillis()
        var harshEvent = false
        if (lastSpeedTimestamp > 0L) {
            val dt = (now - lastSpeedTimestamp).toDouble() / 1000.0
            if (dt > 0.1) {
                val accel = (speed - previousSpeed).toDouble() / dt // km/h/s
                if (accel < -14.0 || accel > 12.0) {
                    harshEvent = true
                }
            }
        }
        previousSpeed = speed
        lastSpeedTimestamp = now

        // Calculate Fleet Eco & Safety Score (0-100)
        var score = 100
        if (rpm > 3500) score -= 15
        if (speed > 125) score -= 20
        if (dtcList.isNotEmpty()) score -= (dtcList.size * 10)
        if (harshEvent) score -= 25
        score = score.coerceIn(20, 100)

        return VehicleFleetTelemetry(
            speedKmh = speed,
            engineRpm = rpm,
            fuelLevelPct = fuelPct,
            coolantTempC = coolantC,
            runTimeSec = runTime,
            odometerKm = baseOdometerKm,
            dtcCodes = dtcList,
            ecoScore = score,
            isHarshEvent = harshEvent,
            timestamp = now
        )
    }
}
