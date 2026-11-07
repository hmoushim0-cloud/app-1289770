package sa.factory.qiyadakeycarrental

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import sa.factory.qiyadakeycarrental.data.repository.QiyadaKeyRepositoryImpl
import sa.factory.qiyadakeycarrental.engine.*
import sa.factory.qiyadakeycarrental.hardware.HardwareSensorDriver
import sa.factory.qiyadakeycarrental.model.QiyadaKeyItem

class QiyadaKeyRegressionTest {

    @Test
    fun testCheckoutCalculationAndTax() = runBlocking {
        val sensorDriver = HardwareSensorDriver()
        val repository = QiyadaKeyRepositoryImpl(sensorDriver)

        val item1 = QiyadaKeyItem("t1", "تأجير سيارة يومي", "الوصف", 250.0, "معتمد", "عام")
        val item2 = QiyadaKeyItem("t2", "تأمين شامل بدون تحمل", "الوصف", 75.0, "معتمد", "عام")

        repository.addToCart(item1)
        repository.addToCart(item2)

        val order = repository.checkout("Mada EMV")

        assertEquals(325.0, order.subtotalSar, 0.01)
        assertEquals(48.75, order.vatSar, 0.01)
        assertEquals(373.75, order.totalSar, 0.01)
        assertEquals(0, repository.activeCart.value.size)
    }

    @Test
    fun testDigitalKeyFobSignatureGenerationAndValidation() {
        val engine = BleDigitalKeyFobEngine()
        val vin = "SAUDI-AUTO-2026-X1"
        val nonce = "CHALLENGE-9988"
        val sequence = 10L

        val signature = engine.computeSignature(vin, KeyFobAction.UNLOCK_DOORS, nonce, sequence)
        val command = KeyFobSignedCommand(
            vin = vin,
            action = KeyFobAction.UNLOCK_DOORS,
            challengeNonce = nonce,
            sequenceCounter = sequence,
            signatureHex = signature,
            measuredRssiDbm = -60
        )

        val result = engine.executeKeyFobCommand(command)

        assertTrue(result.success)
        assertEquals(VehicleLockState.UNLOCKED, result.lockState)
        assertEquals(EngineState.READY_TO_START, result.engineState)
    }

    @Test
    fun testDigitalKeyFobReplayAttackPrevention() {
        val engine = BleDigitalKeyFobEngine()
        val vin = "SAUDI-AUTO-2026-X1"
        val nonce1 = "NONCE-1"
        val nonce2 = "NONCE-2"

        val sig1 = engine.computeSignature(vin, KeyFobAction.UNLOCK_DOORS, nonce1, 5L)
        val cmd1 = KeyFobSignedCommand(vin, KeyFobAction.UNLOCK_DOORS, nonce1, 5L, sig1, -55)
        val res1 = engine.executeKeyFobCommand(cmd1)
        assertTrue(res1.success)

        // Attempt to replay same sequence counter 5L
        val sig2 = engine.computeSignature(vin, KeyFobAction.LOCK_DOORS, nonce2, 5L)
        val cmd2 = KeyFobSignedCommand(vin, KeyFobAction.LOCK_DOORS, nonce2, 5L, sig2, -55)
        val res2 = engine.executeKeyFobCommand(cmd2)

        assertFalse(res2.success)
        assertTrue(res2.diagnosticMessage.contains("Replay attack detected"))
    }

    @Test
    fun testDigitalKeyFobProximityGeofence() {
        val engine = BleDigitalKeyFobEngine()
        val vin = "SAUDI-AUTO-2026-X1"
        val nonce = "NONCE-START"
        val sequence = 1L

        // Fob is outside cabin (-75 dBm < -62 dBm cabin threshold)
        val sig = engine.computeSignature(vin, KeyFobAction.START_ENGINE, nonce, sequence)
        val cmd = KeyFobSignedCommand(vin, KeyFobAction.START_ENGINE, nonce, sequence, sig, measuredRssiDbm = -75)

        val result = engine.executeKeyFobCommand(cmd)

        assertFalse(result.success)
        assertTrue(result.diagnosticMessage.contains("In-cabin proximity check failed"))
    }

    @Test
    fun testObdPidRpmAndSpeedDecoding() {
        val engine = ObdCanBusTelematicsEngine()

        // RPM: ((A * 256) + B) / 4
        // A = 0x1F (31), B = 0x40 (64) -> ((31 * 256) + 64) / 4 = 8000 / 4 = 2000 RPM
        val rpm = engine.decodeEngineRpm(0x1F, 0x40)
        assertEquals(2000, rpm)

        // Speed: A in km/h
        val speed = engine.decodeVehicleSpeed(0x64) // 100 km/h
        assertEquals(100, speed)
    }

    @Test
    fun testObdPidFuelLevelAndCoolantTempDecoding() {
        val engine = ObdCanBusTelematicsEngine()

        // Fuel: (100 * A) / 255
        // A = 0x80 (128) -> (100 * 128) / 255 = 50.2%
        val fuel = engine.decodeFuelLevel(128)
        assertEquals(50.2, fuel, 0.1)

        // Coolant: A - 40
        // A = 0x7E (126) -> 126 - 40 = 86 C
        val coolant = engine.decodeCoolantTemp(126)
        assertEquals(86, coolant)
    }

    @Test
    fun testObdDtcFaultCodeDecoding() {
        val engine = ObdCanBusTelematicsEngine()

        // P0300 (Random/Multiple Cylinder Misfire Detected):
        // P: 00 in high 2 bits. Digits: 0, 3, 0, 0
        // byteA: 0000 0011 = 0x03, byteB: 0000 0000 = 0x00
        val dtc1 = engine.decodeDtc(0x03, 0x00)
        assertEquals("P0300", dtc1)

        // P0420 (Catalyst System Efficiency Below Threshold):
        // byteA: 0000 0100 = 0x04, byteB: 0010 0000 = 0x20
        val dtc2 = engine.decodeDtc(0x04, 0x20)
        assertEquals("P0420", dtc2)
    }

    @Test
    fun testFleetEcoAndSafetyScoring() {
        val engine = ObdCanBusTelematicsEngine()

        // Normal cruise at 90 km/h, 2200 RPM -> clean score 100
        val normal = engine.parseCanBusFrame(
            speed = 90, rpm = 2200, fuelPct = 75.0, coolantC = 88, runTime = 120, baseOdometerKm = 50000
        )
        assertEquals(100, normal.ecoScore)

        // High speed (140 km/h) & high RPM (4200) -> penalizes score
        val aggressive = engine.parseCanBusFrame(
            speed = 140, rpm = 4200, fuelPct = 70.0, coolantC = 95, runTime = 200, baseOdometerKm = 50015
        )
        assertTrue(aggressive.ecoScore <= 65)
    }

    @Test
    fun testHardwareSensorDriverInitialStream() = runBlocking {
        val driver = HardwareSensorDriver()
        val frames = driver.observePeripheralStream().take(2).toList()

        assertEquals(2, frames.size)
        assertEquals("CONNECTING", frames[0].connectionState)
        assertEquals("ONLINE", frames[1].connectionState)
        assertTrue(frames[1].livePayload.contains("Key Fob:"))
        assertTrue(frames[1].livePayload.contains("Doors:"))
        assertTrue(frames[1].livePayload.contains("Engine:"))
    }
}
