package dev.huynh.nfc_manager_android

import android.nfc.NfcAdapter
import dev.huynh.nfc_manager_android.feature_checker.NfcFeatureChecker
import dev.huynh.nfc_manager_android.host_card_emulation.HostCardEmulation
import dev.huynh.nfc_manager_android.utils.trySuccess
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import org.jetbrains.annotations.VisibleForTesting

class NfcManagerPlugin :
    ActivityAware,
    FlutterPlugin,
    MethodCallHandler {
    private lateinit var methodChannel: MethodChannel
    private lateinit var eventChannel: EventChannel
    private var nfcAdapter: NfcAdapter? = null

    @VisibleForTesting
    lateinit var nfcFeatureChecker: NfcFeatureChecker
    private val hostCardEmulation = HostCardEmulation()

    @VisibleForTesting
    var nfcReader: NfcReader? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val context = flutterPluginBinding.applicationContext
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        nfcFeatureChecker =
            NfcFeatureChecker(
                nfcAdapter = nfcAdapter,
                packageManager = context.packageManager,
            )

        methodChannel =
            MethodChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android",
            ).apply { setMethodCallHandler(this@NfcManagerPlugin) }

        eventChannel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android/discovery",
            )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        eventChannel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        nfcReader =
            NfcReader(
                nfcAdapter = nfcAdapter,
                activity = binding.activity,
            )
        eventChannel.setStreamHandler(nfcReader)
    }

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) =
        onAttachedToActivity(
            binding,
        )

    override fun onDetachedFromActivity() {
        eventChannel.setStreamHandler(null)
        nfcReader = null
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result,
    ) = when (call.method) {
        "isHceSupported" -> result.success(nfcFeatureChecker.isHceSupported())
        "isNfcSupported" -> result.success(nfcFeatureChecker.isNfcSupported())
        "isNfcEnabled" -> result.success(nfcFeatureChecker.isNfcEnabled())
        "startDiscovery" ->
            result.trySuccess {
                nfcReader?.startDiscovery()
                null
            }

        "stopDiscovery" ->
            result.trySuccess {
                nfcReader?.stopDiscovery()
                null
            }

        "startEmulation" -> {
            hostCardEmulation.startEmulation(
                aid = call.argument<ByteArray>("aid")!!,
                pin = call.argument<ByteArray>("pin")!!,
            )
            result.success(null)
        }

        "stopEmulation" -> {
            hostCardEmulation.stopEmulation()
            result.success(null)
        }

        else -> result.notImplemented()
    }
}
