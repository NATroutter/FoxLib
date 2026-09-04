package fi.natroutter.foxlib.logger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What the logger has to survive.
 *
 * <p>Each of these is a way for a log to be quietly wrong or quietly absent, which is the only
 * way a log can fail that matters. A consumer may write to this from many threads at once, may
 * be relying on the file as its record of a period when nothing else was reachable, and may run
 * unattended for months between anyone reading it.
 */
class FoxLoggerTest {

    @TempDir
    Path directory;

    /** A logger writing into the test's own directory, saving on demand rather than on a timer. */
    private FoxLogger logger(int saveIntervalSeconds) {
        return new FoxLogger.Builder()
                .setLoggerName("test")
                .setDataFolder(directory.toFile())
                .setSaveIntervalSeconds(saveIntervalSeconds)
                .setUseColors(false)
                .setPrintter(line -> {
                })
                .build();
    }

    /** The file `save()` writes for today, named the way `save()` names it. */
    private Path todaysLog() {
        ZonedDateTime now = ZonedDateTime.now();
        return directory.resolve("logs").resolve(
                "Log_" + now.getMonthValue() + "-" + now.getDayOfMonth() + "-" + now.getYear() + ".log");
    }

    @Test
    void keepsEveryLineWrittenFromManyThreadsAtOnce() throws Exception {
        // A consumer logs from worker threads, reconnect threads, the console and a shutdown
        // hook. `entries` was a bare ArrayList appended from all of them and iterated by the save
        // timer, so a save during a burst threw ConcurrentModificationException, and an unchecked
        // exception out of a TimerTask kills that thread for good: from that moment nothing is
        // ever written again.
        FoxLogger logger = logger(3600);
        int threads = 8;
        int each = 500;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int thread = 0; thread < threads; thread++) {
            int id = thread;
            pool.submit(() -> {
                try {
                    start.await();
                    for (int line = 0; line < each; line++) {
                        logger.info("thread " + id + " line " + line);
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertNull(failure.get(), "logging threw: " + failure.get());

        logger.close();

        List<String> written = Files.readAllLines(todaysLog());
        assertEquals(threads * each, written.size(), "lines were lost");
    }

    @Test
    void keepsSavingAfterOneSaveFails() throws Exception {
        // A consumer logs from worker threads, reconnect threads, the console and a shutdown
        // hook, all appending to the same bare ArrayList while a background timer periodically
        // iterates it to save and clear it. No lock protects `entries`, so a save that iterates
        // it while another thread is still appending throws ConcurrentModificationException, and
        // an unchecked exception out of a TimerTask kills that timer's thread for good -
        // permanently, not just for the tick that failed, and not only while whatever caused the
        // failure is still there. This drives saves through the real scheduler rather than through
        // close(), because close() saves synchronously and would hide a dead scheduler completely.
        int intervalSeconds = 1;
        FoxLogger logger = logger(intervalSeconds);

        int threads = 8;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicBoolean keepGoing = new AtomicBoolean(true);
        AtomicReference<Throwable> failure = new AtomicReference<>();

        for (int thread = 0; thread < threads; thread++) {
            int id = thread;
            pool.submit(() -> {
                try {
                    start.await();
                    int line = 0;
                    while (keepGoing.get()) {
                        logger.info("thread " + id + " line " + (line++));
                    }
                } catch (Throwable t) {
                    failure.compareAndSet(null, t);
                }
            });
        }
        start.countDown();
        // Long enough for several scheduled saves to land while the burst is still in flight.
        Thread.sleep(intervalSeconds * 2500L);
        keepGoing.set(false);
        pool.shutdown();
        assertTrue(pool.awaitTermination(30, TimeUnit.SECONDS));
        assertNull(failure.get(), "logging threw: " + failure.get());

        // A save failing for an unrelated reason must not matter either, once that cause is gone:
        // replace the log directory with a plain file so a save can't write, let a tick or two
        // pass, then delete that file so a future save can recreate the directory on its own.
        Path logs = directory.resolve("logs");
        Files.walk(logs).sorted(Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
        Files.writeString(logs, "not a directory");
        Thread.sleep(intervalSeconds * 2000L);
        Files.delete(logs);

        String sentinel = "sentinel-" + UUID.randomUUID();
        logger.info(sentinel);
        // Past two more scheduler ticks, without ever calling close().
        Thread.sleep(intervalSeconds * 2500L);

        assertTrue(Files.exists(todaysLog()) && Files.readString(todaysLog()).contains(sentinel),
                "the logger stopped saving after a save failed, so the sentinel line was never written");
    }

    @Test
    void appendsToTodaysFileRatherThanRewritingIt() throws Exception {
        // `save()` read the whole file, concatenated, and wrote it all back. That is quadratic
        // in file size, holds two copies in heap, and truncates before it writes, so a crash
        // or a full disk mid-save loses the entire day rather than the last few lines.
        Files.createDirectories(todaysLog().getParent());
        String seed = "carried over from an earlier run";
        Files.writeString(todaysLog(), seed);

        FoxLogger logger = logger(3600);
        logger.info("first batch");
        logger.close();

        long afterFirst = Files.size(todaysLog());

        logger.info("second batch");
        logger.close();

        String contents = Files.readString(todaysLog());
        assertTrue(contents.startsWith(seed), contents);
        assertEquals('[', contents.charAt(seed.length()),
                "a rewrite put a line separator between the carried-over content and the first "
                        + "new entry, rather than appending directly after it: " + contents);
        assertTrue(contents.contains("first batch"), contents);
        assertTrue(contents.contains("second batch"), contents);
        assertEquals(1, contents.split("first batch", -1).length - 1,
                "the first batch was written twice, so the file was rewritten rather than appended");
        assertTrue(Files.size(todaysLog()) > afterFirst);
    }

    @Test
    void rollsToANewFileRatherThanGrowingWithoutABound() throws Exception {
        // Age was the only bound. A link flapping once a second fills a volume long before
        // anything in it is old enough to prune.
        FoxLogger logger = logger(3600);
        Files.createDirectories(directory.resolve("logs"));
        Files.write(todaysLog(), new byte[33 * 1024 * 1024]);

        logger.info("after the cap");
        logger.close();

        ZonedDateTime now = ZonedDateTime.now();
        Path second = directory.resolve("logs").resolve(
                "Log_" + now.getMonthValue() + "-" + now.getDayOfMonth() + "-" + now.getYear() + ".2.log");
        assertTrue(Files.exists(second), "no second part was started");
        assertTrue(Files.readString(second).contains("after the cap"));
    }

    @Test
    void timestampsInTheConfiguredZone() {
        // `timeStamp()` computed `formatter.withZone(zone)` and threw the result away, because
        // DateTimeFormatter is immutable, then formatted LocalDateTime.now() in the system zone.
        // So setTimeZone did nothing at all.
        AtomicReference<String> captured = new AtomicReference<>();
        FoxLogger capturing = new FoxLogger.Builder()
                .setLoggerName("test")
                .setDataFolder(directory.toFile())
                .setTimeZone("Asia/Tokyo")
                .setTimeFormat("XXX")
                .setUseColors(false)
                .setSaveLogs(false)
                .setPrintter(captured::set)
                .build();

        capturing.info("anything");

        assertNotNull(captured.get());
        assertTrue(captured.get().contains("+09:00"),
                "the configured zone was ignored: " + captured.get());
    }
}
