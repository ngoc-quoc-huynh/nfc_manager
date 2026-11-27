package dev.huynh.nfc_manager_android

import android.nfc.NfcAdapter
import androidx.annotation.VisibleForTesting
import dev.huynh.nfc_manager_android.hce.HostCardEmulationStreamHandler
import dev.huynh.nfc_manager_android.reader.DiscoveryStreamHandler
import dev.huynh.nfc_manager_android.reader.TagReader
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.EventChannel

/** Connects the Pigeon host API and event channels to NFC discovery, commands, and HCE. */
class NfcManagerPlugin :
    ActivityAware,
    FlutterPlugin,
    NfcManagerHostApi {
    private lateinit var discoveryEventChannel: EventChannel

    private lateinit var hostCardEmulationEventChannel: EventChannel

    private var nfcAdapter: NfcAdapter? = null

    @VisibleForTesting
    internal lateinit var featureChecker: FeatureChecker

    /** The reader for the attached activity, or null without one. */
    @VisibleForTesting
    internal var tagReader: TagReader? = null

    /** The HCE stream handler; it owns the HCE session it starts. */
    @VisibleForTesting
    internal var hostCardEmulationStreamHandler: HostCardEmulationStreamHandler? = null

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        val context = flutterPluginBinding.applicationContext
        nfcAdapter = NfcAdapter.getDefaultAdapter(context)
        featureChecker =
            FeatureChecker(
                nfcAdapter = nfcAdapter,
                packageManager = context.packageManager,
            )
        NfcManagerHostApi.setUp(flutterPluginBinding.binaryMessenger, this)
        discoveryEventChannel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android/discovery",
            ).apply {
                // Keep the channel registered so Flutter can cancel after a reader ends its stream.
                setStreamHandler(DiscoveryStreamHandler { tagReader })
            }
        val streamHandler = HostCardEmulationStreamHandler()
        hostCardEmulationStreamHandler = streamHandler
        hostCardEmulationEventChannel =
            EventChannel(
                flutterPluginBinding.binaryMessenger,
                "dev.huynh/nfc_manager_android/host_card_emulation",
            ).apply { setStreamHandler(streamHandler) }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        NfcManagerHostApi.setUp(binding.binaryMessenger, null)
        discoveryEventChannel.setStreamHandler(null)
        hostCardEmulationStreamHandler?.close()
        hostCardEmulationEventChannel.setStreamHandler(null)
        hostCardEmulationStreamHandler = null
        tagReader?.dispose()
        tagReader = null
    }

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        tagReader =
            TagReader(
                nfcAdapter = nfcAdapter,
                activity = binding.activity,
            )
    }

    override fun onDetachedFromActivityForConfigChanges() = onDetachedFromActivity()

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) = onAttachedToActivity(binding)

    override fun onDetachedFromActivity() {
        tagReader?.dispose()
        tagReader = null
    }

    override fun isHceSupported() = featureChecker.isHceSupported()

    override fun isNfcSupported() = featureChecker.isNfcSupported()

    override fun isNfcEnabled() = featureChecker.isNfcEnabled()

    override fun sendCommand(
        command: ByteArray,
        callback: (Result<ByteArray>) -> Unit,
    ) {
        val reader = tagReader ?: return callback(Result.failure(TagConnectionException().toFlutterError()))
        reader.sendCommand(
            command,
            onSuccess = { callback(Result.success(it)) },
            onError = { callback(Result.failure(it.toFlutterError())) },
        )
    }
}
