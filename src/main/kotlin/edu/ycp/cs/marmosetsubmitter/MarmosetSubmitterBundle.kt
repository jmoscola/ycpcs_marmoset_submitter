package edu.ycp.cs.marmosetsubmitter

import com.intellij.DynamicBundle
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey

/**
 * Identifies the resource bundle file located at
 * messages/MarmosetSubmitterBundle.properties. The [@NonNls] annotation
 * indicates to IntelliJ that this string is not a natural language
 * string and should not be flagged for localization.
 */
@NonNls
private const val BUNDLE = "messages.MarmosetSubmitterBundle"

/**
 * Provides access to the plugin's localized string resources defined in
 * MarmosetSubmitterBundle.properties. Extends [DynamicBundle] to integrate with
 * the IntelliJ Platform's localization infrastructure, which supports
 * runtime language pack plugins.
 *
 * All user-facing strings in the plugin should be retrieved through this
 * object rather than being hardcoded, to ensure consistent localization
 * support across the plugin.
 *
 * @see DynamicBundle
 */
object MarmosetSubmitterBundle : DynamicBundle(BUNDLE) {

    /**
     * Retrieves a localized string from MarmosetSubmitterBundle.properties for the
     * specified key, substituting any provided parameters into the message
     * using [java.text.MessageFormat] placeholder syntax (e.g. {0}, {1}).
     *
     * The [@JvmStatic] annotation makes this method callable as a static
     * method from Java code. The [@PropertyKey] annotation enables
     * IntelliJ to validate the key against the resource bundle at
     * compile time and warn if the key does not exist.
     *
     * @param key    The property key to look up in MarmosetSubmitterBundle.properties.
     * @param params Optional parameters to substitute into the message string.
     * @return The localized string with all parameters substituted.
     */
    @JvmStatic
    fun message(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getMessage(key, *params)

    /**
     * Retrieves a lazy reference to a localized string from
     * MarmosetSubmitterBundle.properties for the specified key. The message is
     * not resolved until the returned supplier is invoked, making this
     * method suitable for use in contexts where the string is needed
     * lazily, such as action descriptions or tooltip text that is only
     * evaluated when displayed.
     *
     * The [@Suppress("unused")] annotation suppresses the IntelliJ
     * warning that this method is not currently referenced elsewhere
     * in the plugin, as it is part of the standard bundle API and
     * may be used in the future.
     *
     * @param key    The property key to look up in MarmosetSubmitterBundle.properties.
     * @param params Optional parameters to substitute into the message string.
     * @return A lazy supplier that resolves to the localized string with
     *         all parameters substituted when invoked.
     */
    @Suppress("unused")
    @JvmStatic
    fun messagePointer(@PropertyKey(resourceBundle = BUNDLE) key: String, vararg params: Any) =
        getLazyMessage(key, *params)
}
