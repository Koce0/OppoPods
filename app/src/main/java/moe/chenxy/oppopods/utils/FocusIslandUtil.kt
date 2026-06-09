package moe.chenxy.oppopods.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.drawable.Icon
import android.os.Handler
import android.os.Looper
import com.xzakota.hyper.notification.focus.FocusNotification
import moe.chenxy.oppopods.R
import moe.chenxy.oppopods.hook.Log
import moe.chenxy.oppopods.utils.miuiStrongToast.data.BatteryParams
import moe.chenxy.oppopods.utils.miuiStrongToast.data.OppoPodsAction

@SuppressLint("WrongConstant")
object FocusIslandUtil {
    private const val TAG = "OppoPods-FocusIsland"
    private const val CHANNEL_ID = "oppopods_focus_island"
    private const val CHANNEL_NAME = "OppoPods Battery"
    private const val PERSISTENT_CHANNEL_ID = "oppopods_persistent_focus_island"
    private const val PERSISTENT_CHANNEL_NAME = "OppoPods Island"
    private const val NOTIFICATION_ID = 10086
    private const val NOTIFICATION_ID_PERSISTENT = 10088
    private const val ISLAND_TIMEOUT_SECONDS = 3
    private const val DISMISS_DELAY_MS = 4000L

    fun showBatteryIsland(
        context: Context,
        prefs: SharedPreferences,
        batteryParams: BatteryParams,
        address: String,
    ): Boolean {
        try {
            val leftConnected = batteryParams.left?.isConnected == true
            val rightConnected = batteryParams.right?.isConnected == true

            // Need at least one ear connected
            if (!leftConnected && !rightConnected) return false

            val leftText = if (leftConnected) "${batteryParams.left!!.battery}" else "-"
            val rightText = if (rightConnected) "${batteryParams.right!!.battery}" else "-"

            val leftBitmap = PodImageLoader.loadIslandLeftBitmap(context, prefs, address)
            val rightBitmap = PodImageLoader.loadIslandRightBitmap(context, prefs, address)

            if (leftBitmap == null || rightBitmap == null) {
                Log.e(TAG, "Failed to decode earphone icon bitmaps")
                return false
            }

            // 使用 createWithBitmap 直接嵌入图片数据，SystemUI 无需再访问模块资源
            val leftIcon = Icon.createWithBitmap(leftBitmap)
            val rightIcon = Icon.createWithBitmap(rightBitmap)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setAllowBubbles(true)
                }
            )

            val contentParts = mutableListOf<String>()
            if (leftConnected) contentParts.add("L: ${batteryParams.left!!.battery}%")
            if (rightConnected) contentParts.add("R: ${batteryParams.right!!.battery}%")
            val contentText = contentParts.joinToString("  ")

            val extras = FocusNotification.buildV3 {
                val picLeft = createPicture("key_pic_left", leftIcon)
                val picRight = createPicture("key_pic_right", rightIcon)

                enableFloat = true
                ticker = "OppoPods"
                tickerPic = picLeft

                isShowNotification = false
                island {
                    islandProperty = 1
                    bigIslandArea {
                        imageTextInfoLeft {
                            type = 1
                            picInfo {
                                type = 1
                                pic = picLeft
                            }
                            textInfo {
                                title = leftText
                                content = "%"
                            }
                        }
                        imageTextInfoRight {
                            type = 2
                            picInfo {
                                type = 1
                                pic = picRight
                            }
                            textInfo {
                                title = rightText
                                content = "%"
                            }
                        }
                    }
                    shareData {
                        title = "OppoPods"
                        content = contentText
                        shareContent = contentText
                    }
                }
            } ?: return false

            val notification = Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle("OppoPods")
                .setContentText(contentText)
                .setTicker("OppoPods")
                .addExtras(extras)
                .build()

            nm.notify(NOTIFICATION_ID, notification)

            Handler(Looper.getMainLooper()).postDelayed({
                try { nm.cancel(NOTIFICATION_ID) } catch (_: Exception) {}
            }, DISMISS_DELAY_MS)

            Log.d(TAG, "Focus Island shown: L=$leftText% R=$rightText%")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show Focus Island", e)
            return false
        }
    }

    fun showPersistentIsland(
        context: Context,
        prefs: SharedPreferences,
        batteryParams: BatteryParams,
        device: BluetoothDevice?,
    ): Boolean {
        try {
            val leftConnected = batteryParams.left?.isConnected == true
            val rightConnected = batteryParams.right?.isConnected == true
            if (!leftConnected && !rightConnected) return false

            val address = device?.address.orEmpty()
            val leftText = if (leftConnected) "${batteryParams.left!!.battery}" else "-"
            val rightText = if (rightConnected) "${batteryParams.right!!.battery}" else "-"
            var deviceName: String? = device?.alias
            if (deviceName?.isEmpty() == true) {
                deviceName = device?.name
            }
            val displayTitle = deviceName ?: "OppoPods"
            val notificationTag = "BTHeadset${address.ifEmpty { "unknown" }}"

            val moduleContext = context.createPackageContext(
                "moe.chenxy.oppopods", Context.CONTEXT_IGNORE_SECURITY
            )
            val leftBitmap = PodImageLoader.loadIslandLeftBitmap(context, prefs, address)
            val rightBitmap = PodImageLoader.loadIslandRightBitmap(context, prefs, address)
            val boxBitmap = PodImageLoader.loadBoxBitmap(context, prefs, address)

            if (leftBitmap == null || rightBitmap == null || boxBitmap == null) {
                Log.e(TAG, "Failed to decode persistent island icon bitmaps")
                return false
            }

            val leftIcon = Icon.createWithBitmap(leftBitmap)
            val rightIcon = Icon.createWithBitmap(rightBitmap)
            val boxIcon = Icon.createWithBitmap(boxBitmap)

            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(
                NotificationChannel(PERSISTENT_CHANNEL_ID, PERSISTENT_CHANNEL_NAME, NotificationManager.IMPORTANCE_MIN).apply {
                    setSound(null, null)
                    enableVibration(false)
                    setAllowBubbles(true)
                }
            )

            val miheadsetNotificationBox = context.resources.getIdentifier("miheadset_notification_Box", "string", "com.xiaomi.bluetooth")
            val miheadsetNotificationLeftEar = context.resources.getIdentifier("miheadset_notification_LeftEar", "string", "com.xiaomi.bluetooth")
            val miheadsetNotificationRightEar = context.resources.getIdentifier("miheadset_notification_RightEar", "string", "com.xiaomi.bluetooth")
            val caseBattStr = if (batteryParams.case != null && batteryParams.case!!.isConnected)
                "${context.resources.getString(miheadsetNotificationBox)}${batteryParams.case!!.battery}%" +
                    "${if (batteryParams.case!!.isCharging) "\u26A1" else " "}\n"
            else ""
            val leftEar = if (batteryParams.left != null && batteryParams.left!!.isConnected)
                "${context.resources.getString(miheadsetNotificationLeftEar)}${batteryParams.left!!.battery}%" +
                    (if (batteryParams.left!!.isCharging) "\u26A1" else "")
            else ""
            val leftToRight = if (leftConnected && rightConnected) " " else ""
            val rightEar = if (batteryParams.right != null && batteryParams.right!!.isConnected)
                "$leftToRight${context.resources.getString(miheadsetNotificationRightEar)}${batteryParams.right!!.battery}%" +
                    (if (batteryParams.right!!.isCharging) "\u26A1" else " ")
            else ""
            val contentText = caseBattStr + leftEar + rightEar
            val islandContentParts = mutableListOf<String>()
            if (leftConnected) islandContentParts.add("L: ${batteryParams.left!!.battery}%")
            if (rightConnected) islandContentParts.add("R: ${batteryParams.right!!.battery}%")
            val islandContentText = islandContentParts.joinToString("  ")

            val pendingIntent = PendingIntent.getActivity(
                context,
                0,
                Intent("chen.action.oppopods.show_pods_ui").apply {
                    setClassName("moe.chenxy.oppopods", "moe.chenxy.oppopods.PopupActivity")
                    putExtra("android.bluetooth.device.extra.DEVICE", device)
                    putExtra("bluetoothaddress", address)
                    putExtra("device_name", deviceName)
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val ancCycleIntent = Intent(OppoPodsAction.ACTION_CYCLE_ANC).apply {
                setPackage("com.android.bluetooth")
                setIdentifier(notificationTag)
                putExtra("device_name", displayTitle)
            }
            val ancAction = Notification.Action.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_lock_silent_mode),
                moduleContext.getString(R.string.cycle_anc),
                PendingIntent.getBroadcast(
                    context,
                    1,
                    ancCycleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            ).build()

            val disconnectIntent = Intent("com.android.bluetooth.headset.notification").apply {
                putExtra("btData", android.os.Bundle().apply {
                    putParcelable("Device", device)
                })
                putExtra("disconnect", "1")
                setIdentifier(notificationTag)
            }
            val disconnectAction = Notification.Action.Builder(
                Icon.createWithResource(context, android.R.drawable.ic_delete),
                moduleContext.getString(R.string.notification_btn_disconnect),
                PendingIntent.getBroadcast(
                    context,
                    2,
                    disconnectIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            ).build()

            val extras = FocusNotification.buildV3 {
                val picLeft = createPicture("key_pic_left", leftIcon)
                val picRight = createPicture("key_pic_right", rightIcon)
                val logo = createPicture("key_headset", boxIcon)

                enableFloat = false
                islandFirstFloat = false
                updatable = true
                ticker = "OppoPods"
                tickerPic = picLeft
                isShowNotification = true

                iconTextInfo {
                    animIconInfo {
                        type = 0
                        src = logo
                    }
                    title = displayTitle
                    content = contentText
                }

                island {
                    islandProperty = 1
                    bigIslandArea {
                        imageTextInfoLeft {
                            type = 1
                            picInfo {
                                type = 1
                                pic = picLeft
                            }
                            textInfo {
                                title = leftText
                                content = "%"
                            }
                        }
                        imageTextInfoRight {
                            type = 2
                            picInfo {
                                type = 1
                                pic = picRight
                            }
                            textInfo {
                                title = rightText
                                content = "%"
                            }
                        }
                    }
                    smallIslandArea {
                        picInfo {
                            type = 1
                            pic = picLeft
                        }
                    }
                    shareData {
                        title = "OppoPods"
                        content = islandContentText
                        shareContent = islandContentText
                    }
                }

                textButton {
                    addActionInfo {
                        action = createAction("key_anc_cycle", ancAction)
                        actionTitle = moduleContext.getString(R.string.cycle_anc)
                    }
                    addActionInfo {
                        action = createAction("key_disconnect", disconnectAction)
                        actionTitle = moduleContext.getString(R.string.notification_btn_disconnect)
                    }
                }
            } ?: return false

            val aodParts = mutableListOf<String>()
            if (batteryParams.left?.isConnected == true)
                aodParts.add("L ${batteryParams.left!!.battery}%")
            if (batteryParams.right?.isConnected == true)
                aodParts.add("R ${batteryParams.right!!.battery}%")
            val aodTitle = aodParts.joinToString(" | ")
            try {
                val json = org.json.JSONObject(extras.getString("miui.focus.param") ?: "{}")
                val pv2 = json.optJSONObject("param_v2") ?: org.json.JSONObject()
                pv2.put("aodTitle", aodTitle)
                pv2.put("aodPic", "key_headset")
                json.put("param_v2", pv2)
                extras.putString("miui.focus.param", json.toString())
            } catch (_: Exception) {}

            val notification = Notification.Builder(context, PERSISTENT_CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_data_bluetooth)
                .setContentTitle(displayTitle)
                .setContentText(contentText)
                .setContentIntent(pendingIntent)
                .setTicker("OppoPods")
                .setOngoing(true)
                .addExtras(extras)
                .build()

            nm.notify(NOTIFICATION_ID_PERSISTENT, notification)

            Log.d(TAG, "Persistent Focus Island shown: $contentText")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show Persistent Focus Island", e)
            return false
        }
    }

    fun cancelIsland(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
            nm.cancel(NOTIFICATION_ID_PERSISTENT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel Focus Island", e)
        }
    }

    fun cancelTemporaryIsland(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel temporary Focus Island", e)
        }
    }

    fun cancelPersistentIsland(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.cancel(NOTIFICATION_ID_PERSISTENT)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel Persistent Focus Island", e)
        }
    }
}
