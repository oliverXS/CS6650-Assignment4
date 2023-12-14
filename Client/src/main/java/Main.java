import client.ApiClient;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import util.Constant;
import util.LogUtils;
import util.ThreadUtils;
import util.UriUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xiaorui
 */
@Slf4j
public class Main {
    private static List<String> albumIdList = Collections.synchronizedList(new ArrayList<>());
    private static final ConcurrentLinkedDeque<Long> reviewGetLatencies = new ConcurrentLinkedDeque<>();
    private static final AtomicBoolean allPostCompleted = new AtomicBoolean(false);
    private static final AtomicInteger GET_SUCCESSFUL = new AtomicInteger(0);
    private static final AtomicInteger GET_FAILED = new AtomicInteger(0);


    public static void main(String[] args) throws InterruptedException {
        if (args.length < Constant.ARGS_NUM) {
            log.error("Usage: java ApiClient <threadGroupSize> <numThreadGroups> <delay> <IPAddr>");
            System.exit(1);
        }

        int threadGroupSize = Integer.parseInt(args[0]);
        int numThreadGroups = Integer.parseInt(args[1]);
        int delay = Integer.parseInt(args[2]);
        String ipAddr = args[3];
        CloseableHttpClient httpClient = ApiClient.generateHttpClient();

        ThreadPoolExecutor postExecutor = ThreadUtils.generateThreadPool(threadGroupSize * numThreadGroups);
        ExecutorService getExecutor = ThreadUtils.generateThreadPool(3);
        CountDownLatch firstGroupLatch = new CountDownLatch(threadGroupSize);
        long startTime = 0;

        for (int i = 0; i < numThreadGroups; i++) {
            final int groupIndex = i;
            for (int j = 0; j < threadGroupSize; j++) {
                postExecutor.submit(() -> {
                    try {
                        HttpPost albumPost = UriUtils.createAlbumPost(ipAddr);
                        for (int k = 0; k < Constant.POST_API_CALLS; k++) {
                            String albumId = UriUtils.executeAlbumPost(albumPost, httpClient);
                            // log.info(albumId);
                            albumIdList.add(albumId);
                            HttpPost reviewLikePost = UriUtils.createReviewPost(ipAddr, "like", albumId);
                            HttpPost reviewDislikePost = UriUtils.createReviewPost(ipAddr, "dislike", albumId);
                            UriUtils.executeReviewPost(reviewLikePost, httpClient);
                            UriUtils.executeReviewPost(reviewLikePost, httpClient);
                            UriUtils.executeReviewPost(reviewDislikePost, httpClient);
                        }
                    } finally {
                        if (groupIndex == 0) {
                            log.info("First group working.");
                            firstGroupLatch.countDown();
                        }
                    }
                });
            }

            if (i == 0) {
                firstGroupLatch.await();
                log.info("First group completed.");
                startTime = System.currentTimeMillis();
                startReviewGetThreads(getExecutor, ipAddr, httpClient, GET_SUCCESSFUL, GET_FAILED, reviewGetLatencies);
            }

            if (i < numThreadGroups - 1) {
                Thread.sleep(delay * 1000L);
            }
        }
        postExecutor.shutdown();
        try {
            postExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Main thread interrupted while waiting for task completion", e);
        }
        allPostCompleted.set(true);
        getExecutor.shutdown();
        log.info("All threads down.");
        try {
            getExecutor.awaitTermination(Long.MAX_VALUE, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Main thread interrupted while waiting for task completion", e);
        }
        long endTime = System.currentTimeMillis();

        LogUtils.logThroughput(startTime, endTime, GET_SUCCESSFUL, GET_FAILED);

        List<Long> latenciesList = new ArrayList<>(reviewGetLatencies);
        LogUtils.logStats(latenciesList);
    }

    private static void startReviewGetThreads(ExecutorService getExecutor, String ipAddr, CloseableHttpClient httpClient, AtomicInteger successful, AtomicInteger failed, ConcurrentLinkedDeque<Long> reviewGetLatencies) {
        for (int i = 0; i < 3; i++) {
            getExecutor.submit(() -> {
                while (!allPostCompleted.get()) {
                    String albumId = generateRandomAlbumId();
                    HttpGet reviewGet = UriUtils.createReviewGet(ipAddr, albumId);
                    UriUtils.executeReviewGet(reviewGet, httpClient, successful, failed, reviewGetLatencies);
                }
            });
        }
    }

    private static String generateRandomAlbumId() {
        int size = albumIdList.size();
        if (size > 0) {
            int randomIndex = ThreadLocalRandom.current().nextInt(size);
            return albumIdList.get(randomIndex);
        }
        return null;
    }
}
