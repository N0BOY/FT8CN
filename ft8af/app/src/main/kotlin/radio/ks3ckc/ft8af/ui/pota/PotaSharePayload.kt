package radio.ks3ckc.ft8af.ui.pota

import org.json.JSONArray
import org.json.JSONObject
import radio.ks3ckc.ft8af.pota.model.PotaActivation
import radio.ks3ckc.ft8af.pota.model.PotaQso
import java.util.Locale

/** Explicit public-field allowlist. Never include private activation notes or auth data. */
internal fun buildPotaSharePayload(activation: PotaActivation, contacts: List<PotaQso>): JSONObject {
    require(!activation.isActive) { "End the activation before sharing" }
    require(contacts.size <= 5000) { "Too many contacts to share" }
    fun grid(value: String): String {
        val normalized = value.trim().uppercase(Locale.ROOT)
        return normalized.takeIf { Regex("[A-R]{2}[0-9]{2}([A-X]{2}([0-9]{2})?)?").matches(it) }.orEmpty()
    }
    return JSONObject().apply {
        put("v", 1)
        put("call", activation.operator.orEmpty().trim().uppercase(Locale.ROOT))
        put("parks", JSONArray(activation.parkRefs.map { it.uppercase(Locale.ROOT) }))
        put("start", activation.startedAtMs)
        put("end", activation.endedAtMs)
        put("contacts", JSONArray().apply {
            contacts.forEach { qso ->
                put(JSONObject().apply {
                    put("call", qso.callsign.trim().uppercase(Locale.ROOT))
                    put("grid", grid(qso.grid))
                    put("myGrid", grid(qso.myGrid))
                    put("band", qso.band.trim())
                    put("mode", qso.mode.trim())
                    put("date", qso.qsoDate.takeIf { Regex("[0-9]{8}").matches(it) }.orEmpty())
                    put("time", normalizeAdifTimeOn(qso.timeOn).orEmpty())
                })
            }
        })
    }
}
