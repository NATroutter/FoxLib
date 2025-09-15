package fi.natroutter.foxlib.cooldown;

import java.util.concurrent.TimeUnit;

/**
 * Represents a single cooldown entry with a start time, duration, and time unit.
 *
 * @param start     the start timestamp (in ms since epoch)
 * @param time      the duration of the cooldown
 * @param timeUnit  the time unit of the cooldown duration
 */
public record CooldownEntry(long start, int time, TimeUnit timeUnit){};
