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
    private lateinit var discoveryEventChanngel: EventChannel
    private lateinit var hostCardEmulationEventChanngel: EventChannel

    private var nfcAdapter: NfcAdapter? = null

    @VisibleForTesting
    lateinit var nfcFeatureChecker: NfcFeatureChecker

    @VisibleForTesting
    var tagReader: TagReader? = null
    private val hostCardEmulation = HostCardEmulation()

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

        discoveryEventChanngel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android/discovery",
            )
        hostCardEmulationEventChanngel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android/host_card_emulation",
            )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        discoveryEventChanngel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        tagReader =
            TagReader(
                nfcAdapter = nfcAdapter,
                activity = binding.activity,
            )
        discoveryEventChanngel.setStreamHandler(tagReader)
        hostCardEmulationEventChanngel.setStreamHandler(hostCardEmulation)
    }

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = onAttachedToActivity(binding)

    override fun onDetachedFromActivity() {
        discoveryEventChanngel.setStreamHandler(null)
        hostCardEmulationEventChanngel.setStreamHandler(null)
        tagReader = null
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
                tagReader?.startDiscovery()
                null
            }

        "stopDiscovery" ->
            result.trySuccess {
                tagReader?.stopDiscovery()
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
