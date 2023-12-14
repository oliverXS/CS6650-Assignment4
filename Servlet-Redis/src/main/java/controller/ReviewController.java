package controller;

import com.google.gson.Gson;
import com.rabbitmq.client.Channel;
import config.DatabaseConfig;
import config.RabbitMQConfig;
import dao.AlbumDAO;
import dao.ReviewDAO;
import lombok.extern.slf4j.Slf4j;
import model.Message;
import model.Review;
import util.Constants;
import util.UrlUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.TimeoutException;

/**
 * @author xiaorui
 */
@MultipartConfig
@Slf4j
public class ReviewController extends HttpServlet {
    private static Connection connection;
    private static ReviewDAO reviewDAO;
    private static Gson gson;
    private RabbitMQConfig rabbitMQConfig;

    public void init() throws ServletException {
        super.init();
        gson = new Gson();
        try {
            connection = DatabaseConfig.getConnection();
            log.info("Connected to database successfully!");
            reviewDAO = new ReviewDAO(connection);
            rabbitMQConfig = new RabbitMQConfig();
        } catch (SQLException | IOException | TimeoutException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        if (!UrlUtils.isValidUrl(urlPath)) {
            res.getWriter().write("Invalid URL.");
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        String[] urlParts = urlPath.split("/");
        if (!UrlUtils.isValidFormatForReviewPost(urlParts)) {
            res.getWriter().write("Invalid URL format.");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        String likeornot = urlParts[1];
        int albumId = Integer.parseInt(urlParts[2]);
        Message message = new Message(albumId, likeornot);

        try {
            String jsonMessage = gson.toJson(message);
            Channel channel = null;

            try {
                channel = rabbitMQConfig.borrowChannel();
                channel.basicPublish("", Constants.QUEUE_NAME, null, jsonMessage.getBytes());
                res.setStatus(HttpServletResponse.SC_OK);
            } catch (Exception e) {
                log.error("Error publishing message: ", e);
                res.getWriter().write("Internal server error.");
                res.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            } finally {
                if (channel != null) {
                    rabbitMQConfig.returnChannel(channel);
                }
            }
        } catch (NumberFormatException e) {
            log.error("Invalid number format: ", e);
            res.getWriter().write("Invalid album ID format.");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        if (!UrlUtils.isValidUrl(urlPath)) {
            res.getWriter().write("Invalid URL.");
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }

        String[] urlParts = urlPath.split("/");
        if (!UrlUtils.isValidFormatForReviewGet(urlParts)) {
            res.getWriter().write("Invalid URL format.");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        int albumId = Integer.parseInt(urlParts[1]);

        Review review = null;
        try {
            review = reviewDAO.getReview(albumId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (review != null) {
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write(gson.toJson(review));
        } else {
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
            res.getWriter().write("Review not found with albumId: " + albumId);
        }
    }
}
