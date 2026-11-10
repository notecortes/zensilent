package com.example.sync

import android.content.Context
import com.example.data.AudioZenDatabase
import com.example.model.AudioMode
import com.example.model.GeofenceZone
import com.example.model.ScheduleRule
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import android.util.Base64

class SyncManager(private val context: Context) {

    private val db = AudioZenDatabase.getInstance(context)

    suspend fun exportBackup(password: String = ""): Result<String> = withContext(Dispatchers.IO) {
        try {
            val rules = db.scheduleRuleDao().getAllRules().first()
            val zones = db.geofenceZoneDao().getAllZones().first()

            val rootJson = JSONObject().apply {
                put("app", "AudioZen")
                put("version", 1)
                put("timestamp", System.currentTimeMillis())

                val rulesArray = JSONArray()
                rules.forEach { rule ->
                    rulesArray.put(JSONObject().apply {
                        put("title", rule.title)
                        put("daysOfWeek", rule.daysOfWeek)
                        put("startHour", rule.startHour)
                        put("startMinute", rule.startMinute)
                        put("endHour", rule.endHour)
                        put("endMinute", rule.endMinute)
                        put("targetMode", rule.targetMode.name)
                        put("restoreMode", rule.restoreMode.name)
                        put("isEnabled", rule.isEnabled)
                    })
                }
                put("rules", rulesArray)

                val zonesArray = JSONArray()
                zones.forEach { zone ->
                    zonesArray.put(JSONObject().apply {
                        put("name", zone.name)
                        put("latitude", zone.latitude)
                        put("longitude", zone.longitude)
                        put("radiusMeters", zone.radiusMeters.toDouble())
                        put("enterMode", zone.enterMode.name)
                        put("exitMode", zone.exitMode.name)
                        put("isEnabled", zone.isEnabled)
                    })
                }
                put("zones", zonesArray)
            }

            val plainPayload = rootJson.toString()
            val checksum = calculateSha256(plainPayload)

            val finalEnvelope = JSONObject()
            finalEnvelope.put("audiozen_sync_version", 1)
            finalEnvelope.put("checksum", checksum)

            if (password.isNotBlank()) {
                val encrypted = encryptPayload(plainPayload, password)
                finalEnvelope.put("is_encrypted", true)
                finalEnvelope.put("payload", encrypted)
            } else {
                finalEnvelope.put("is_encrypted", false)
                finalEnvelope.put("payload", Base64.encodeToString(plainPayload.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            }

            Result.success(finalEnvelope.toString(2))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun importBackup(content: String, password: String = ""): Result<Pair<Int, Int>> = withContext(Dispatchers.IO) {
        try {
            val envelope = JSONObject(content.trim())
            val checksum = envelope.getString("checksum")
            val isEncrypted = envelope.getBoolean("is_encrypted")
            val rawPayload = envelope.getString("payload")

            val plainJsonString = if (isEncrypted) {
                if (password.isBlank()) {
                    return@withContext Result.failure(IllegalArgumentException("Esta copia de seguridad requiere una contraseña para descifrarse."))
                }
                decryptPayload(rawPayload, password)
            } else {
                String(Base64.decode(rawPayload, Base64.NO_WRAP), Charsets.UTF_8)
            }

            // Verify Integrity
            val calculated = calculateSha256(plainJsonString)
            if (calculated != checksum) {
                return@withContext Result.failure(SecurityException("La verificación de integridad SHA-256 falló. El archivo está corrupto o ha sido alterado."))
            }

            val root = JSONObject(plainJsonString)
            val rulesArray = root.optJSONArray("rules") ?: JSONArray()
            val zonesArray = root.optJSONArray("zones") ?: JSONArray()

            var importedRulesCount = 0
            for (i in 0 until rulesArray.length()) {
                val obj = rulesArray.getJSONObject(i)
                val rule = ScheduleRule(
                    title = obj.getString("title"),
                    daysOfWeek = obj.getString("daysOfWeek"),
                    startHour = obj.getInt("startHour"),
                    startMinute = obj.getInt("startMinute"),
                    endHour = obj.getInt("endHour"),
                    endMinute = obj.getInt("endMinute"),
                    targetMode = AudioMode.valueOf(obj.getString("targetMode")),
                    restoreMode = AudioMode.valueOf(obj.optString("restoreMode", AudioMode.NORMAL.name)),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
                db.scheduleRuleDao().insertRule(rule)
                importedRulesCount++
            }

            var importedZonesCount = 0
            for (i in 0 until zonesArray.length()) {
                val obj = zonesArray.getJSONObject(i)
                val zone = GeofenceZone(
                    name = obj.getString("name"),
                    latitude = obj.getDouble("latitude"),
                    longitude = obj.getDouble("longitude"),
                    radiusMeters = obj.optDouble("radiusMeters", 150.0).toFloat(),
                    enterMode = AudioMode.valueOf(obj.getString("enterMode")),
                    exitMode = AudioMode.valueOf(obj.optString("exitMode", AudioMode.NORMAL.name)),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
                db.geofenceZoneDao().insertZone(zone)
                importedZonesCount++
            }

            Result.success(Pair(importedRulesCount, importedZonesCount))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun calculateSha256(text: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hash = digest.digest(text.toByteArray(Charsets.UTF_8))
        return hash.joinToString("") { "%02x".format(it) }
    }

    private fun encryptPayload(plainText: String, pass: String): String {
        val salt = "AudioZenSalt2026".toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(pass.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val combined = iv + encrypted
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decryptPayload(cipherText: String, pass: String): String {
        val combined = Base64.decode(cipherText, Base64.NO_WRAP)
        val iv = combined.copyOfRange(0, 12)
        val encrypted = combined.copyOfRange(12, combined.size)

        val salt = "AudioZenSalt2026".toByteArray(Charsets.UTF_8)
        val spec = PBEKeySpec(pass.toCharArray(), salt, 10000, 256)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val keyBytes = factory.generateSecret(spec).encoded
        val secretKey = SecretKeySpec(keyBytes, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
        val decryptedBytes = cipher.doFinal(encrypted)

        return String(decryptedBytes, Charsets.UTF_8)
    }
}
