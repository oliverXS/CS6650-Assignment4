package util;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.DeliverCallback;
import config.RabbitMQConfig;
import config.RedisConfig;
import dao.ReviewDAO;
import lombok.extern.slf4j.Slf4j;
import model.Message;
import model.Review;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.sql.SQLException;

/**
 * @author xiaorui
 */
@Slf4j
public class Consumer implements Runnable{
    private final RabbitMQConfig rabbitMQConfig;
    private final ReviewDAO reviewDAO;
    private JedisPool jedisPool;

    private final Gson gson = new Gson();

    public Consumer(RabbitMQConfig rabbitMQConfig, ReviewDAO reviewDAO) {
        this.rabbitMQConfig = rabbitMQConfig;
        this.reviewDAO = reviewDAO;
        this.jedisPool = RedisConfig.getPool();
    }

    @Override
    public void run() {
        try {
            Channel channel = rabbitMQConfig.borrowChannel();
            channel.queueDeclare(Constants.QUEUE_NAME, true, false, false, null);
            channel.basicQos(50);
            DeliverCallback deliverCallback = (consumerTag, delivery) -> {
                String messageJson = new String(delivery.getBody(), "UTF-8");
                // channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
                Message message = gson.fromJson(messageJson, Message.class);
                processMessage(message);
            };
            channel.basicConsume(Constants.QUEUE_NAME, true, deliverCallback, consumerTag -> {});
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void processMessage(Message message) {
        try{
            boolean like = message.getMessage().equalsIgnoreCase("like");
            int albumId = message.getAlbumId();
            // 1. Update the database with the new review
            reviewDAO.updateReview(albumId, like);
            // 2. Update in Redis
            updateCache(albumId);

            log.info("Processed review for album ID: " + message.getAlbumId());
        } catch (SQLException e) {
            log.error("Database error while processing review message: ", e);
        }
    }

    private void updateCache(int albumId) {
        try {
            Review review = reviewDAO.getReview(albumId);
            try (Jedis jedis = jedisPool.getResource()) {
                jedis.setex("albumReview:" + albumId, 3600, gson.toJson(review));
            }
        } catch (SQLException e) {
            log.error("Database error while updating cache: ", e);
        } catch (Exception e) {
            log.error("Redis error while updating review cache: ", e);
        }
    }
}
