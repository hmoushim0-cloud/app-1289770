package sa.factory.qiyadakeycarrental.hardware

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import sa.factory.qiyadakeycarrental.engine.*
import kotlin.random.Random

data class HardwareTelemetryFrame(
    val peripheralName: String,
    val connectionState: String,
    val livePayload: String,
    val timestamp: Long = System.currentTimeMillis()
)

class HardwareSensorDriver {
    val peripheral = "OBD-II CAN-Bus Telematics + BLE Digital Key Fob"
    val keyFobEngine = BleDigitalKeyFobEngine()
    val obdEngine = ObdCanBusTelematicsEngine()

    fun observePeripheralStream(): Flow<HardwareTelemetryFrame> = flow {
        emit(HardwareTelemetryFrame(peripheral, "CONNECTING", "CAN-Bus ISO 15765-4 & BLE Key Fob GATT 0xFFE0 initializing..."))
        delay(200)

        // Seed initial rental pickup: authenticate customer key fob and unlock vehicle
        val vin = "WBA33AY08MFS2026"
        val unlockNonce = "NONCE-KEY-001"
        val unlockSignature = keyFobEngine.computeSignature(vin, KeyFobAction.UNLOCK_DOORS, unlockNonce, 1L)
        val unlockCommand = KeyFobSignedCommand(
            vin = vin,
            action = KeyFobAction.UNLOCK_DOORS,
            challengeNonce = unlockNonce,
            sequenceCounter = 1L,
            signatureHex = unlockSignature,
            measuredRssiDbm = -65
        )
        val unlockRes = keyFobEngine.executeKeyFobCommand(unlockCommand)

        val initPayload = "[ONLINE] VIN: " + vin + " | Key Fob: " + (if (unlockRes.success) "AUTHENTICATED" else "DENIED") +
                " | Doors: " + unlockRes.lockState.name + " | Engine: " + unlockRes.engineState.name
        emit(HardwareTelemetryFrame(peripheral, "ONLINE", initPayload))

        var frameCounter = 1
        var sequence = 1L
        var odometer = 42180L
        var fuel = 88.5

        while (true) {
            delay(1500)
            frameCounter++
            sequence++
            val jitter = Random.nextInt(1, 8)

            // Simulate driving speed & engine telemetry
            val speed = when {
                frameCounter < 6 -> frameCounter * 16 // Accelerating (0 -> 80 km/h)
                frameCounter < 15 -> Random.nextInt(85, 115) // Highway cruise
                else -> Random.nextInt(40, 65) // Urban city drive
            }
            val rpm = (speed * 32).coerceIn(850, 4200)
            val coolant = (88 + (frameCounter % 5))
            odometer += (speed / 720).coerceAtLeast(1)
            fuel -= 0.05

            // Periodically check in-cabin BLE presence for engine immobilizer
            val fobRssi = Random.nextInt(-64, -52)
            val startNonce = "NONCE-KEY-" + sequence
            val sig = keyFobEngine.computeSignature(vin, KeyFobAction.START_ENGINE, startNonce, sequence)
            val cmd = KeyFobSignedCommand(vin, KeyFobAction.START_ENGINE, startNonce, sequence, sig, fobRssi)
            val fobRes = keyFobEngine.executeKeyFobCommand(cmd)

            val telemetry = obdEngine.parseCanBusFrame(
                speed = speed,
                rpm = rpm,
                fuelPct = fuel,
                coolantC = coolant,
                runTime = frameCounter * 2,
                baseOdometerKm = odometer
            )

            val fuelFmt = String.format("%.1f", telemetry.fuelLevelPct)
            val liveStatus = "[ACTIVE_STREAMING] Frame #" + frameCounter + " (+" + jitter + "ms) | " +
                    "Speed: " + telemetry.speedKmh + " km/h | " +
                    "RPM: " + telemetry.engineRpm + " | " +
                    "Fuel: " + fuelFmt + "% | " +
                    "Odo: " + telemetry.odometerKm + " km | " +
                    "Eco: " + telemetry.ecoScore + "/100 | " +
                    "Fob: " + fobRes.engineState.name

            emit(HardwareTelemetryFrame(peripheral, "ACTIVE_STREAMING", liveStatus))
        }
    }
}
