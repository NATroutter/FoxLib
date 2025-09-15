package fi.natroutter.foxlib.cooldown;

/**
 * Represents metadata related to a cooldown that has expired.
 *
 * <p>This record is typically used in cooldown expiration listeners to provide
 * context about the expired cooldown entry.</p>
 *
 * @param <T> the type of the key associated with the cooldown (e.g., user ID or command name)
 *
 * @param key         the key that the cooldown was associated with
 * @param cooldown    the {@link CooldownEntry} instance containing cooldown start time, duration, and unit
 * @param customData  optional custom data attached to the cooldown, or {@code null} if none
 */
public record ExpirationData<T>(T key, CooldownEntry cooldown, Object customData){};
