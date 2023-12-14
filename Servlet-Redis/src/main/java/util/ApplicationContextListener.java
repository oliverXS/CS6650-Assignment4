package util;

import config.DatabaseConfig;
import config.RabbitMQConfig;
import dao.ReviewDAO;

import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author xiaorui
 */
@WebListener
public class ApplicationContextListener implements ServletContextListener {
    private ExecutorService executorService;
    private Connection connection;
    private RabbitMQConfig rabbitMQConfig;
    private ReviewDAO reviewDAO;

    @Override
    public void contextInitialized(ServletContextEvent sce) {
        executorService = Executors.newFixedThreadPool(Constants.NUM_OF_CONSUMER_THREAD);
        try {
            connection = DatabaseConfig.getConnection();
            rabbitMQConfig = new RabbitMQConfig();
            reviewDAO = new ReviewDAO(connection);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (TimeoutException e) {
            throw new RuntimeException(e);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        for (int i = 0; i < Constants.NUM_OF_CONSUMER_THREAD; i++) {
            Consumer consumer = new Consumer(rabbitMQConfig, reviewDAO);
            executorService.execute(consumer);
        }

    }



    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
                // Wait a while for tasks to respond to being cancelled
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                    System.err.println("Pool did not terminate");
                }
            }
        } catch (InterruptedException e) {
            // (Re-)Cancel if current thread also interrupted
            executorService.shutdownNow();
            // Preserve interrupt status
            Thread.currentThread().interrupt();
        }
    }
}
