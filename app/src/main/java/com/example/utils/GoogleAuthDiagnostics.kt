package com.example.utils

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import com.google.android.gms.common.api.ApiException
import java.security.MessageDigest

object GoogleAuthDiagnostics {
    const val TAG_RUNTIME = "GOOGLE_AUTH_RUNTIME"
    const val TAG_FLOW = "GOOGLE_AUTH_FLOW"

    const val EXPECTED_PACKAGE_NAME = "com.aistudio.brainquizai.app"
    const val EXPECTED_SERVER_CLIENT_ID = "106236832575-nv10u3crcpl0dh353k88c8hkfidh448e.apps.googleusercontent.com"
    const val FIREBASE_PROJECT_ID = "brainquiz-ai-app"
    const val FIREBASE_APP_ID = "1:106236832575:android:8bb30cbfcabc48ffdfc18a"

    fun getRuntimeSigningCertificates(context: Context): Pair<String, String> {
        return try {
            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNING_CERTIFICATES
                )
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(
                    context.packageName,
                    PackageManager.GET_SIGNATURES
                )
            }

            val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                packageInfo.signingInfo?.apkContentsSigners
            } else {
                @Suppress("DEPRECATION")
                packageInfo.signatures
            }

            if (!signatures.isNullOrEmpty()) {
                val cert = signatures[0].toByteArray()
                val mdSha1 = MessageDigest.getInstance("SHA-1")
                val sha1Bytes = mdSha1.digest(cert)
                val sha1Formatted = sha1Bytes.joinToString(":") { "%02X".format(it) }

                val mdSha256 = MessageDigest.getInstance("SHA-256")
                val sha256Bytes = mdSha256.digest(cert)
                val sha256Formatted = sha256Bytes.joinToString(":") { "%02X".format(it) }

                Pair(sha1Formatted, sha256Formatted)
            } else {
                Pair("NO_SIGNATURES_FOUND", "NO_SIGNATURES_FOUND")
            }
        } catch (e: Exception) {
            Pair("ERROR_${e.javaClass.simpleName}_${e.message}", "ERROR_${e.javaClass.simpleName}_${e.message}")
        }
    }

    fun getDefaultWebClientId(context: Context): String {
        return try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) context.getString(resId) else EXPECTED_SERVER_CLIENT_ID
        } catch (e: Exception) {
            EXPECTED_SERVER_CLIENT_ID
        }
    }

    fun logEvent(
        context: Context,
        stage: String,
        flowStep: String? = null,
        exception: Exception? = null,
        statusCode: Int? = null,
        serverClientId: String? = null,
        additionalInfo: String? = null
    ) {
        val packageName = context.packageName
        val (sha1, sha256) = getRuntimeSigningCertificates(context)
        val defaultWebClientId = getDefaultWebClientId(context)
        val actualServerClientId = serverClientId ?: defaultWebClientId

        val extractedStatusCode = statusCode
            ?: (exception as? ApiException)?.statusCode
            ?: -1

        val statusExplanation = when (extractedStatusCode) {
            10 -> "10 (DEVELOPER_ERROR / SHA-1 or OAuth Client ID mismatch)"
            12500 -> "12500 (SIGN_IN_FAILED)"
            12501 -> "12501 (USER_CANCELLED)"
            7 -> "7 (NETWORK_ERROR)"
            -1 -> "N/A"
            else -> "$extractedStatusCode"
        }

        val exceptionClass = exception?.javaClass?.name ?: "None"
        val exceptionMessage = exception?.message ?: "None"

        val gsoState = "GoogleSignInOptions[DEFAULT_SIGN_IN, requestIdToken=$actualServerClientId, requestEmail=true, requestProfile=true]"

        val runtimeLog = StringBuilder().apply {
            append("stage=$stage")
            append(" | package=$packageName")
            append(" | isPackageCorrect=${packageName == EXPECTED_PACKAGE_NAME}")
            append(" | firebaseProjectId=$FIREBASE_PROJECT_ID")
            append(" | firebaseAppId=$FIREBASE_APP_ID")
            append(" | runtime SHA-1=$sha1")
            append(" | runtime SHA-256=$sha256")
            append(" | serverClientId=$actualServerClientId")
            append(" | default_web_client_id=$defaultWebClientId")
            append(" | isServerClientIdCorrect=${actualServerClientId == EXPECTED_SERVER_CLIENT_ID}")
            append(" | GoogleSignInOptionsState=$gsoState")
            if (exception != null || (extractedStatusCode != -1 && extractedStatusCode != 12501)) {
                append(" | failure stage=$stage")
                append(" | exceptionClass=$exceptionClass")
                append(" | statusCode=$statusExplanation")
                append(" | exceptionMessage=$exceptionMessage")
            }
            if (!additionalInfo.isNullOrBlank()) {
                append(" | details=$additionalInfo")
            }
        }.toString()

        if (exception != null || (extractedStatusCode != -1 && extractedStatusCode != 12501)) {
            Log.e(TAG_RUNTIME, runtimeLog)
        } else {
            Log.i(TAG_RUNTIME, runtimeLog)
        }

        if (flowStep != null) {
            val flowLog = "[$flowStep] Stage: $stage | Package: $packageName | ServerClientId: ${actualServerClientId.take(25)}... | Status: $statusExplanation${if (!additionalInfo.isNullOrBlank()) " | $additionalInfo" else ""}"
            if (exception != null || (extractedStatusCode != -1 && extractedStatusCode != 12501)) {
                Log.e(TAG_FLOW, flowLog, exception)
            } else {
                Log.d(TAG_FLOW, flowLog)
            }
        }
    }
}
