package sa.factory.qiyadakeycarrental.engine

import java.security.MessageDigest

enum class VehicleLockState {
    LOCKED,
    UNLOCKED
}

enum class EngineState {
    IMMOBILIZED,
    READY_TO_START,
    RUNNING
}

enum class KeyFobAction {
    UNLOCK_DOORS,
    LOCK_DOORS,
    TRUNK_RELEASE,
    START_ENGINE,
    STOP_ENGINE,
    IMMOBILIZE_VEHICLE
}

data class KeyFobChallenge(
    val vin: String,
    val challengeNonce: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class KeyFobSignedCommand(
    val vin: String,
    val action: KeyFobAction,
    val challengeNonce: String,
    val sequenceCounter: Long,
    val signatureHex: String,
    val measuredRssiDbm: Int
)

data class KeyFobExecutionResult(
    val success: Boolean,
    val action: KeyFobAction,
    val lockState: VehicleLockState,
    val engineState: EngineState,
    val diagnosticMessage: String,
    val timestamp: Long = System.currentTimeMillis()
)

class BleDigitalKeyFobEngine(
    private val sharedSecretKey: String = "QIYADA_FLEET_SEC_2026",
    private val doorUnlockRssiThreshold: Int = -72,
    private val engineStartRssiThreshold: Int = -62
) {
    private var lastSequenceCounter: Long = 0L
    private var currentLockState: VehicleLockState = VehicleLockState.LOCKED
    private var currentEngineState: EngineState = EngineState.IMMOBILIZED

    fun computeSignature(
        vin: String,
        action: KeyFobAction,
        nonce: String,
        seq: Long,
        secret: String = sharedSecretKey
    ): String {
        val payload = "$vin:${action.name}:$nonce:$seq:$secret"
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        return digest.take(12).joinToString("") { "%02x".format(it) }
    }

    fun executeKeyFobCommand(command: KeyFobSignedCommand): KeyFobExecutionResult {
        // 1. Anti-Replay Sequence Validation
        if (command.sequenceCounter <= lastSequenceCounter) {
            return KeyFobExecutionResult(
                success = false,
                action = command.action,
                lockState = currentLockState,
                engineState = currentEngineState,
                diagnosticMessage = "Replay attack detected: sequence counter ${command.sequenceCounter} <= $lastSequenceCounter"
            )
        }

        // 2. Cryptographic Signature Verification
        val expectedSignature = computeSignature(
            command.vin,
            command.action,
            command.challengeNonce,
            command.sequenceCounter
        )
        if (command.signatureHex.lowercase() != expectedSignature.lowercase()) {
            return KeyFobExecutionResult(
                success = false,
                action = command.action,
                lockState = currentLockState,
                engineState = currentEngineState,
                diagnosticMessage = "Digital key signature verification failed. Unauthorized key fob."
            )
        }

        // 3. Proximity RSSI Geofence Verification
        if (command.action == KeyFobAction.START_ENGINE && command.measuredRssiDbm < engineStartRssiThreshold) {
            return KeyFobExecutionResult(
                success = false,
                action = command.action,
                lockState = currentLockState,
                engineState = currentEngineState,
                diagnosticMessage = "In-cabin proximity check failed (${command.measuredRssiDbm} dBm < $engineStartRssiThreshold dBm)"
            )
        }

        if ((command.action == KeyFobAction.UNLOCK_DOORS || command.action == KeyFobAction.TRUNK_RELEASE) &&
            command.measuredRssiDbm < doorUnlockRssiThreshold) {
            return KeyFobExecutionResult(
                success = false,
                action = command.action,
                lockState = currentLockState,
                engineState = currentEngineState,
                diagnosticMessage = "Key fob outside passive range (${command.measuredRssiDbm} dBm < $doorUnlockRssiThreshold dBm)"
            )
        }

        // 4. State Machine Transition
        lastSequenceCounter = command.sequenceCounter
        when (command.action) {
            KeyFobAction.UNLOCK_DOORS -> {
                currentLockState = VehicleLockState.UNLOCKED
                if (currentEngineState == EngineState.IMMOBILIZED) {
                    currentEngineState = EngineState.READY_TO_START
                }
            }
            KeyFobAction.LOCK_DOORS -> {
                currentLockState = VehicleLockState.LOCKED
                if (currentEngineState == EngineState.READY_TO_START) {
                    currentEngineState = EngineState.IMMOBILIZED
                }
            }
            KeyFobAction.TRUNK_RELEASE -> {
                // Trunk unlocked
            }
            KeyFobAction.START_ENGINE -> {
                if (currentLockState == VehicleLockState.UNLOCKED || command.measuredRssiDbm >= engineStartRssiThreshold) {
                    currentEngineState = EngineState.RUNNING
                }
            }
            KeyFobAction.STOP_ENGINE -> {
                currentEngineState = EngineState.READY_TO_START
            }
            KeyFobAction.IMMOBILIZE_VEHICLE -> {
                currentEngineState = EngineState.IMMOBILIZED
                currentLockState = VehicleLockState.LOCKED
            }
        }

        return KeyFobExecutionResult(
            success = true,
            action = command.action,
            lockState = currentLockState,
            engineState = currentEngineState,
            diagnosticMessage = "Command ${command.action.name} executed successfully. Lock: $currentLockState, Engine: $currentEngineState"
        )
    }

    fun getVehicleState(): Pair<VehicleLockState, EngineState> = Pair(currentLockState, currentEngineState)

    fun resetState() {
        lastSequenceCounter = 0L
        currentLockState = VehicleLockState.LOCKED
        currentEngineState = EngineState.IMMOBILIZED
    }
}
