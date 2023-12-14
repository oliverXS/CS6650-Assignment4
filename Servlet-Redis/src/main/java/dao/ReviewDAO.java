package dao;

import com.google.gson.Gson;
import config.RabbitMQConfig;
import config.RedisConfig;
import lombok.extern.slf4j.Slf4j;
import model.Review;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import util.Constants;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

/**
 * @author xiaorui
 */
@Slf4j
public class ReviewDAO {
    private Connection connection;
    private JedisPool jedisPool;
    private Gson gson;

    public ReviewDAO(Connection connection) {
        this.connection = connection;
        this.jedisPool = RedisConfig.getPool();
        this.gson = new Gson();
    }

    public void updateReview(int albumId, boolean like) throws SQLException {
        String updateReviewSQL = like ? "UPDATE albums.album SET likes = likes + 1 WHERE album_id = ?"
                : "UPDATE albums.album SET dislikes = dislikes + 1 WHERE album_id = ?";

        try (PreparedStatement st = connection.prepareStatement(updateReviewSQL)) {
            st.setInt(1, albumId);
            st.executeUpdate();
        } catch (SQLException ex) {
            log.error("Failed to update album review.", ex);
        }
    }

    public Review getReview(int albumId) throws SQLException {
        // Attempt to fetch from Redis cache
        try (Jedis jedis = jedisPool.getResource()) {
            String reviewJson = jedis.get("albumReview:" + albumId);
            if (reviewJson != null) {
                log.info("Fetch from Cache.");
                return deserializeReview(reviewJson);
            }
        } catch (Exception e) {
            log.error("Redis operation failed", e);
        }

        // Fetch from database as fallback
        String getReviewSQL = "SELECT likes, dislikes FROM albums.album WHERE album_id = ?";
        try (PreparedStatement st = connection.prepareStatement(getReviewSQL)) {
            st.setInt(1, albumId);
            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    Review review = new Review(rs.getInt("likes"), rs.getInt("dislikes"));

                    // Store in Redis cache
                    try (Jedis jedis = jedisPool.getResource()) {
                        // Cache for one hour
                        jedis.setex("albumReview:" + albumId, 3600, serializeReview(review));
                    }

                    log.info("Ferch from DB.");
                    return review;
                } else {
                    log.error("No album found with ID: " + albumId);
                    return null;
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to get album review.", ex);
            return null;
        }
    }

    private String serializeReview(Review review) {
        return gson.toJson(review);
    }

    private Review deserializeReview(String reviewJson) {
        return gson.fromJson(reviewJson, Review.class);
    }


}
