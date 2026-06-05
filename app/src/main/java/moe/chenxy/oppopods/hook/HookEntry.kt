package moe.chenxy.oppopods.hook

import android.os.Build
import androidx.annotation.RequiresApi
import io.github.libxposed.api.XposedModule
import io.github.libxposed.api.XposedModuleInterface.PackageLoadedParam

class HookEntry : XposedModule() {
    private val loadedHooks = mutableSetOf<String>()

    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onPackageLoaded(param: PackageLoadedParam) {
        val hook = when (param.packageName) {
            "com.android.bluetooth" -> HeadsetStateDispatcher
            "com.milink.service" -> MiLinkServiceHook
            "com.xiaomi.bluetooth" -> MiBluetoothToastHook
            else -> return
        }
        val hookKey = "${param.packageName}@${System.identityHashCode(param.defaultClassLoader)}"
        if (!loadedHooks.add(hookKey)) return

        loadHook(hook, param.defaultClassLoader)
    }

    private fun loadHook(hook: HookContext, classLoader: ClassLoader) {
        hook.module = this
        hook.appClassLoader = classLoader
        hook.prefs = getRemotePreferences("oppopods_settings")
        hook.onHook()
    }
}
