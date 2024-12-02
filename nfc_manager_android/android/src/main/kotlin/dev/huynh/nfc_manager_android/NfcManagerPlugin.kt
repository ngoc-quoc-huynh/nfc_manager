package dev.huynh.nfc_manager_android

import android.nfc.NfcAdapter
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result

class NfcManagerPlugin :
    FlutterPlugin,
    MethodCallHandler {
    private lateinit var methodChannel: MethodChannel
    lateinit var nfcService: NfcService

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        methodChannel =
            MethodChannel(
                flutterPluginBinding.binaryMessenger,
                "nfc_manager_android",
            )
        methodChannel.setMethodCallHandler(this)
        val context = flutterPluginBinding.applicationContext
        nfcService =
            NfcService(
                nfcAdapter = NfcAdapter.getDefaultAdapter(context),
                packageManager = context.packageManager,
            )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) = methodChannel.setMethodCallHandler(null)

    override fun onMethodCall(
        call: MethodCall,
        result: Result,
    ) = when (call.method) {
        "isHceSupported" -> result.success(nfcService.isHceSupported())
        "isNfcSupported" -> result.success(nfcService.isNfcSupported())
        else -> result.notImplemented()
    }
}
