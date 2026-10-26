package io.github.datalux.sbriga

import org.json.JSONException
import org.json.JSONObject

public enum class SettingsTarget { AUTOSTART, BATTERY }

/** One vendor settings screen worth trying, in priority order. */
public data class SettingsCandidate(
    val target: SettingsTarget,
    val packageName: String,
    val className: String,
    val verified: Boolean,
) {
    /** `package/class`, the form used in reports and issues. */
    val component: String get() = "$packageName/$className"
}

/** Vendor settings screens, loaded from JSON so they can be updated without a new library release. */
public class VendorRules internal constructor(private val byVendor: Map<Vendor, List<SettingsCandidate>>) {

    public fun candidates(vendor: Vendor, target: SettingsTarget): List<SettingsCandidate> =
        byVendor[vendor].orEmpty().filter { it.target == target }

    public companion object {
        public val EMPTY: VendorRules = VendorRules(emptyMap())

        /** Parses the bundled format. Unknown vendors, targets and malformed entries are skipped. */
        public fun fromJson(json: String): VendorRules {
            val vendors = try {
                JSONObject(json).optJSONObject("vendors")
            } catch (_: JSONException) {
                null
            } ?: return EMPTY

            val parsed = mutableMapOf<Vendor, List<SettingsCandidate>>()
            for (key in vendors.keys()) {
                val vendor = Vendor.fromId(key) ?: continue
                val array = vendors.optJSONArray(key) ?: continue
                val entries = buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val target = SettingsTarget.entries.firstOrNull { it.name == item.optString("target") }
                            ?: continue
                        val pkg = item.optString("package")
                        val cls = item.optString("class")
                        if (pkg.isEmpty() || cls.isEmpty()) continue
                        add(SettingsCandidate(target, pkg, cls, item.optBoolean("verified", false)))
                    }
                }
                if (entries.isNotEmpty()) parsed[vendor] = entries
            }
            return VendorRules(parsed)
        }
    }
}
