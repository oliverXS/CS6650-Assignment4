package util;

import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xiaorui
 */
@Slf4j
public class LogUtils {
    public static void logThroughput(long startTime, long endTime, AtomicInteger successfulRequests, AtomicInteger failedRequests) {
        long totalTime = endTime - startTime;
        double wallTime  = totalTime / 1000.0;
        double throughput = (successfulRequests.get() + failedRequests.get()) / wallTime;
        log.info("Wall Time: " + wallTime + " seconds");
        log.info("Successful Requests: " + successfulRequests.get() + " times");
        log.info("Failed Requests: " + failedRequests.get() + " times");
        log.info("Throughput: " + throughput + " request/second");
    }


    public static void logStats(List<Long> latencies) {
        Collections.sort(latencies);
        int size = latencies.size();
        long sum = 0;
        for (long latency : latencies) {
            sum += latency;
        }
        double mean = sum / (double) size;
        double median = size % 2 == 0 ? (latencies.get(size / 2 - 1) + latencies.get(size / 2)) / 2.0 : latencies.get(size / 2);
        double p99 = latencies.get((int) (size * 0.99));
        long min = latencies.get(0);
        long max = latencies.get(size - 1);

        log.info(" Mean Latency: " + mean + "ms");
        log.info(" Median Latency: " + median + "ms");
        log.info(" 99th Percentile Latency: " + p99 + "ms");
        log.info(" Min Latency: " + min + "ms");
        log.info(" Max Latency: " + max + "ms");
    }
}
