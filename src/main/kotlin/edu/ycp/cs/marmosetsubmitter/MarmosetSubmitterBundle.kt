package edu.ycp.cs.marmosetsubmitter

import com.intellij.DynamicBundle
import org.jetbrains.annotations.Nls
import org.jetbrains.annotations.NonNls
import org.jetbrains.annotations.PropertyKey
import java.util.function.Supplier

/**
 * Identifies the resource bundle file located at
 * messages/MarmosetSubmitterBundle.properties. The [@NonNls] annotation
 * indicates that this string is not a natural language
 * string and should not be flagged for localization.
 */
@NonNls
private const val BUNDLE = "messages.MarmosetSubmitterBundle"

/**
 * Provides access to the plugin's localized string resources defined in
 * MarmosetSubmitterBundle.properties. Delegates to a [DynamicBundle] instance
 * to integrate with the IntelliJ Platform's localization infrastructure,
 * which supports runtime language pack plugins.
 *
 * All user-facing strings in the plugin should be retrieved through this
 * object rather than being hardcoded, to ensure consistent localization
 * support across the plugin.
 *
 * Note: this class does not extend [DynamicBundle] directly, as recommended
 * by the JetBrains IntelliJ Platform Plugin SDK documentation. Instead, it
 * holds a private [DynamicBundle] instance and delegates to it.
 *
 * @see DynamicBundle
 */
internal object MarmosetSubmitterBundle {
    private val INSTANCE = DynamicBundle(MarmosetSubmitterBundle::class.java, BUNDLE)

    /**
     * Retrieves a localized string from MarmosetSubmitterBundle.properties for
     * the specified key, substituting any provided parameters into the message
     * using [java.text.MessageFormat] placeholder syntax (e.g. {0}, {1}).
     *
     * The [@PropertyKey] annotation enables validation of the key against the
     * resource bundle at compile time and warns if the key does not exist.
     *
     * @param key    The property key to look up in MarmosetSubmitterBundle.properties.
     * @param params Optional parameters to substitute into the message string.
     * @return The localized string with all parameters substituted.
     */
    fun message(
        key: @PropertyKey(resourceBundle = BUNDLE) String,
        vararg params: Any
    ): @Nls String {
        return INSTANCE.getMessage(key, *params)
    }

    /**
     * Retrieves a lazy reference to a localized string from
     * MarmosetSubmitterBundle.properties for the specified key. The message is
     * not resolved until the returned supplier is invoked, making this method
     * suitable for use in contexts where the string is needed lazily, such as
     * action descriptions or tooltip text that is only evaluated when displayed.
     *
     * @param key    The property key to look up in MarmosetSubmitterBundle.properties.
     * @param params Optional parameters to substitute into the message string.
     * @return A [Supplier] that resolves to the localized string with all
     *         parameters substituted when invoked.
     */
    @Suppress("unused")
    fun lazyMessage(
        @PropertyKey(resourceBundle = BUNDLE) key: String,
        vararg params: Any
    ): Supplier<@Nls String> {
        return INSTANCE.getLazyMessage(key, *params)
    }
}
