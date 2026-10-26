package io.github.datalux.sbriga

/** Device families whose background-execution policy differs from stock Android. */
public enum class Vendor(internal val id: String) {
    XIAOMI("xiaomi"),
    HUAWEI("huawei"),
    OPPO("oppo"),
    VIVO("vivo"),
    SAMSUNG("samsung"),
    ONEPLUS("oneplus"),
    ASUS("asus"),
    OTHER("other"),
    ;

    /** True for families known to add their own killers on top of AOSP Doze. */
    internal val isAggressive: Boolean
        get() = this != OTHER && this != SAMSUNG

    public companion object {
        /** Maps `Build.MANUFACTURER` / `Build.BRAND` to a [Vendor]; comparison is case-insensitive. */
        public fun detect(manufacturer: String?, brand: String? = null): Vendor {
            val keys = listOfNotNull(manufacturer, brand).map { it.trim().lowercase() }
            for (key in keys) {
                val vendor = when {
                    key.contains("xiaomi") || key.contains("redmi") || key.contains("poco") -> XIAOMI
                    key.contains("huawei") || key.contains("honor") -> HUAWEI
                    key.contains("oppo") || key.contains("realme") -> OPPO
                    key.contains("vivo") || key.contains("iqoo") -> VIVO
                    key.contains("samsung") -> SAMSUNG
                    key.contains("oneplus") -> ONEPLUS
                    key.contains("asus") -> ASUS
                    else -> null
                }
                if (vendor != null) return vendor
            }
            return OTHER
        }

        internal fun fromId(id: String): Vendor? = entries.firstOrNull { it.id == id.lowercase() }
    }
}
