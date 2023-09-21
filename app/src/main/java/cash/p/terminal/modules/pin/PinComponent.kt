package cash.p.terminal.modules.pin

import android.app.Activity
import cash.p.terminal.core.managers.UserManager
import cash.p.terminal.modules.pin.core.LockManager
import cash.p.terminal.modules.pin.core.PinManager
import io.horizontalsystems.core.IEncryptionManager
import io.horizontalsystems.core.IPinComponent
import io.horizontalsystems.core.IPinStorage
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable

class PinComponent(
    private val pinStorage: IPinStorage,
    private val encryptionManager: IEncryptionManager,
    private val excludedActivityNames: List<String>,
    private val userManager: UserManager
) : IPinComponent {

    private val pinManager: PinManager by lazy {
        PinManager(encryptionManager, pinStorage)
    }

    private val appLockManager: LockManager by lazy {
        LockManager(pinManager)
    }

    override val pinSetFlowable: Flowable<Unit>
        get() = pinManager.pinSetSubject.toFlowable(BackpressureStrategy.BUFFER)

    override val isLocked: Boolean
        get() = appLockManager.isLocked && isPinSet

    override var isBiometricAuthEnabled: Boolean
        get() = pinStorage.biometricAuthEnabled
        set(value) {
            pinStorage.biometricAuthEnabled = value
        }

    override val isPinSet: Boolean
        get() = pinManager.isPinSet

    override fun store(pin: String, level: Int) {
        if (appLockManager.isLocked) {
            appLockManager.onUnlock()
        }

        pinManager.store(pin, level)
    }

    override fun getPinLevel(pin: String): Int? {
        return pinManager.getPinLevel(pin)
    }

    override fun clear(level: Int) {
        pinManager.clear(level)
    }

    override fun onUnlock(pinLevel: Int) {
        appLockManager.onUnlock()
        userManager.setUserLevel(pinLevel)
    }

    override fun initDefaultPinLevel() {
        userManager.setUserLevel(pinManager.getPinLevelLast())
    }

    override fun onBiometricUnlock() {
        appLockManager.onUnlock()
        userManager.setUserLevel(pinManager.getPinLevelLast())
    }

    override fun lock() {
        appLockManager.lock()
    }

    override fun updateLastExitDateBeforeRestart() {
        appLockManager.updateLastExitDate()
    }

    override fun willEnterForeground(activity: Activity) {
        appLockManager.willEnterForeground()
    }

    override fun didEnterBackground() {
        appLockManager.didEnterBackground()
    }

    override fun shouldShowPin(activity: Activity): Boolean {
        return isLocked && !excludedActivityNames.contains(activity::class.java.name)
    }
}
