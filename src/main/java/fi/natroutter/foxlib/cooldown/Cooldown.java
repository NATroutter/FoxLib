package fi.natroutter.foxlib.cooldown;


import net.jodah.expiringmap.ExpirationListener;
import net.jodah.expiringmap.ExpirationPolicy;
import net.jodah.expiringmap.ExpiringMap;

import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * A generic cooldown handler for managing cooldowns associated with any object type.
 *
 * <p>This class allows setting, checking, and removing cooldowns for arbitrary keys (e.g., users, commands).
 *
 * @param <T> the type of object the cooldown is associated with (e.g., a user ID, name, or object)
 */
public class Cooldown<T> {

    /**
     * Configuration data passed from the {@link Builder}.
     */
    private Builder<T> data;

    // Stores cooldowns mapped by their associated target
    private final ExpiringMap<T, CooldownEntry> cooldowns;

    /**
     * A builder for configuring and constructing a {@link Cooldown} instance.
     *
     * @param <T> the type of object the cooldown will be associated with
     */
    public static class Builder<T> {
        private int defaultCooldown = 5;
        private TimeUnit defaultTimeUnit = TimeUnit.SECONDS;
        private Object customData;

        private BiConsumer<T, ExpirationData<T>> listener = (key, value)->{};

        private final ExpirationListener<T, CooldownEntry> expirationListener = (key, value) -> {
            listener.accept(key, new ExpirationData<>(key, value, this.customData));
        };


        /**
         * Sets optional custom data to be associated with this cooldown builder.
         *
         * <p>This data can be used to attach metadata such as action names, user context,
         * or other identifiers that may be useful when handling cooldown expiration events.</p>
         *
         * @param data the custom data object to associate with the cooldown (can be {@code null})
         * @return this builder instance for method chaining
         */
        public Builder<T> setCustomData(Object data) {
            this.customData = data;
            return this;
        }

        /**
         * Sets the default cooldown duration.
         *
         * @param value the default duration value
         * @return this builder instance
         */
        public Builder<T> setDefaultCooldown(int value) {
            this.defaultCooldown = value;
            return this;
        }

        /**
         * Sets the default cooldown time unit.
         *
         * @param value the default time unit
         * @return this builder instance
         */
        public Builder<T> setDefaultTimeUnit(TimeUnit value) {
            this.defaultTimeUnit = value;
            onCooldownExpiry((k,v)->{});
            return this;
        }

        /**
         * Sets a listener to be called when a cooldown expires.
         *
         * @param listener the expiration listener
         * @return this builder instance
         */
        public Builder<T> onCooldownExpiry(BiConsumer<T, ExpirationData<T>> listener) {
            this.listener = listener;
            return this;
        }

        /**
         * Builds and returns a new {@link Cooldown} instance with the configured settings.
         *
         * @return a configured Cooldown instance
         */
        public Cooldown<T> build() {
            return new Cooldown<>(this);
        }
    }

    /**
     * Constructs a Cooldown with default settings using a new builder instance.
     */
    public Cooldown() {
        this.data = new Builder<>();
        cooldowns = ExpiringMap.builder()
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .expiration(this.data.defaultCooldown, this.data.defaultTimeUnit)
                .expirationListener(this.data.expirationListener)
                .build();
    }


    /**
     * Constructs a Cooldown instance using the provided builder configuration.
     *
     * @param data the builder containing cooldown configuration
     */
    private Cooldown(Builder<T> data) {
        this.data = data;
        cooldowns = ExpiringMap.builder()
                .expirationPolicy(ExpirationPolicy.ACCESSED)
                .expiration(this.data.defaultCooldown, this.data.defaultTimeUnit)
                .expirationListener(this.data.expirationListener)
                .build();
    }

    public CooldownEntry getEntry(T target) {
        return cooldowns.get(target);
    }

    /**
     * Checks whether the specified target is currently on cooldown.
     *
     * @param target the target to check
     * @return true if the target is still on cooldown; false if no cooldown exists or it has expired
     */
    public boolean hasCooldown(T target) {
        if (!cooldowns.containsKey(target)) return false;

        CooldownEntry entry = cooldowns.get(target);
        long cooldownDuration = entry.timeUnit().toMillis(entry.time());
        long elapsed = System.currentTimeMillis() - entry.start();

        if (elapsed < cooldownDuration) {
            return true;
        } else {
            cooldowns.remove(target);
            return false;
        }
    }

    /**
     * Retrieves the remaining cooldown time for the specified target, converted to the given {@link TimeUnit}.
     *
     * <p>If the target has no active cooldown, or if the cooldown has expired,
     * this method will return {@code 0} and remove the expired entry from the cache.
     *
     * @param target    the target for which to check the remaining cooldown
     * @param timeUnit  the {@link TimeUnit} to convert the remaining time into
     * @return the remaining cooldown time in the specified time unit, or {@code 0} if no cooldown is active
     */
    public long getCooldown(T target, TimeUnit timeUnit) {
        CooldownEntry entry = cooldowns.get(target);
        if (entry == null) return 0;

        long cooldownDuration = entry.timeUnit().toMillis(entry.time());
        long elapsed = System.currentTimeMillis() - entry.start();
        long remaining = cooldownDuration - elapsed;

        if (remaining > 0) {
            return timeUnit.convert(remaining, TimeUnit.MILLISECONDS);
        } else {
            // Cooldown expired — clean up and return 0
            cooldowns.remove(target);
            return 0;
        }
    }

    /**
     * Sets a cooldown for the given target using the default cooldown settings.
     *
     * @param target the target to apply the cooldown to
     */
    public void setCooldown(T target) {
        setCooldown(target,data.defaultCooldown, data.defaultTimeUnit);
    }

    /**
     * Sets a custom cooldown for the given target.
     *
     * @param target    the target to apply the cooldown to
     * @param time      the duration of the cooldown
     * @param timeUnit  the unit of time for the cooldown
     */
    public void setCooldown(T target, int time, TimeUnit timeUnit) {
        cooldowns.put(target, new CooldownEntry(System.currentTimeMillis(), time, timeUnit));
    }

    /**
     * Removes the cooldown for the specified target, if it exists.
     *
     * @param target the target whose cooldown should be removed
     */
    public void removeCooldown(T target) {
        cooldowns.remove(target);
    }

}
