package util;

import java.util.concurrent.Executors;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author xiaorui
 */
public class ThreadUtils {
    public static ThreadPoolExecutor generateThreadPool(int poolSize) {
        return new ThreadPoolExecutor(
                poolSize,
                poolSize * 2,
                1L,
                TimeUnit.SECONDS,
                new LinkedBlockingQueue<>(),
                Executors.defaultThreadFactory(),
                new ThreadPoolExecutor.AbortPolicy()
        );
    }
}
