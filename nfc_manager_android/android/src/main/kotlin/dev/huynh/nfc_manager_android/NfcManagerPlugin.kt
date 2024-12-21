package dev.huynh.nfc_manager_android

import android.nfc.NfcAdapter
import androidx.annotation.VisibleForTesting
import dev.huynh.nfc_manager_android.feature_checker.FeatureChecker
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

class NfcManagerPlugin :
    ActivityAware,
    FlutterPlugin,
    MethodCallHandler {
    private lateinit var methodChannel: MethodChannel
    private lateinit var discoveryEventChannel: EventChannel
    private lateinit var hostCardEmulationEventChannel: EventChannel
    private var nfcAdapter: NfcAdapter? = null

    @VisibleForTesting
    lateinit var featureChecker: FeatureChecker

    @VisibleForTesting
    var tagReader: TagReader? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val context = flutterPluginBinding.applicationContext
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        featureChecker =
            FeatureChecker(
                nfcAdapter = nfcAdapter,
                packageManager = context.packageManager,
            )

        methodChannel =
            MethodChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android",
            ).apply { setMethodCallHandler(this@NfcManagerPlugin) }
        discoveryEventChannel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android/discovery",
            )
        hostCardEmulationEventChannel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android/host_card_emulation",
            ).apply { setStreamHandler(HostCardEmulation()) }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel.setMethodCallHandler(null)
        discoveryEventChannel.setStreamHandler(null)
        hostCardEmulationEventChannel.setStreamHandler(null)
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        tagReader =
            TagReader(
                nfcAdapter = nfcAdapter,
                activity = binding.activity,
            )
        discoveryEventChannel.setStreamHandler(tagReader)
    }

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = onAttachedToActivity(binding)

    override fun onDetachedFromActivity() {
        discoveryEventChannel.setStreamHandler(null)
        tagReader = null
    }

    override fun onMethodCall(
        call: MethodCall,
        result: Result,
    ) = when (call.method) {
        "isHceSupported" -> result.success(featureChecker.isHceSupported())
        "isNfcSupported" -> result.success(featureChecker.isNfcSupported())
        "isNfcEnabled" -> result.success(featureChecker.isNfcEnabled())
        "sendCommand" ->
            result.trySuccess {
                tagReader?.sendCommand(call.argument<ByteArray>("command")!!)
            }

        else -> result.notImplemented()
    }
}
