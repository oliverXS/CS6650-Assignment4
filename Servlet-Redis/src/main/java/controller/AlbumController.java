package controller;

import com.google.gson.Gson;
import config.DatabaseConfig;
import dao.AlbumDAO;
import lombok.extern.slf4j.Slf4j;
import model.Album;
import model.ImageMetaData;
import util.UrlUtils;

import javax.servlet.ServletException;
import javax.servlet.annotation.MultipartConfig;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * @author xiaorui
 */

@MultipartConfig
@Slf4j
public class AlbumController extends HttpServlet {
    private static Connection connection;
    private static AlbumDAO albumDAO;
    private static Gson gson;
    public void init() throws ServletException {
        super.init();
        gson = new Gson();
        try {
            connection = DatabaseConfig.getConnection();
            log.info("Connected to database successfully!");
            albumDAO = new AlbumDAO(connection);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse res) throws ServletException, IOException {
        res.setContentType("application/json");
        String urlPath = req.getPathInfo();

        if (!UrlUtils.isValidReqForAlbumPost(req)) {
            res.getWriter().write("Post request without multipart/form-data.");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        try {
            // Handle profile part
            Album profile;
            Part profilePart = req.getPart("profile");
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(profilePart.getInputStream()))) {
                String profileJson = reader.lines().collect(Collectors.joining());
                profile = gson.fromJson(profileJson, Album.class);
            }

            // Handle image part
            byte[] imageBytes = null;
            Part imagePart = req.getPart("image");
            if (imagePart != null) {
                imageBytes = convertPartToBytes(imagePart);
            }

            Optional<Integer> albumId = albumDAO.saveAlbum(profile, imageBytes);

            int imageSize = imageBytes == null ? 0 : imageBytes.length;
            String albumIdString = albumId.isPresent() ? String.valueOf(albumId.get()) : null;

            ImageMetaData imageMetaData = new ImageMetaData(albumIdString, String.valueOf(imageSize) + " bytes");
            res.setStatus(HttpServletResponse.SC_OK);
            res.getWriter().write(gson.toJson(imageMetaData));
        } catch (Exception e) {
            res.getWriter().write("Error occur in processing the post request");
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
        if (!UrlUtils.isValidFormatForAlbumGet(urlParts)) {
            res.getWriter().write("Invalid URL format.");
            res.setStatus(HttpServletResponse.SC_BAD_REQUEST);
        }

        int albumId = Integer.parseInt(urlParts[1]);

        Album album = null;
        try {
            album = albumDAO.getAlbum(albumId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }

        if (album != null) {
            res.getWriter().write(gson.toJson(album));
            res.setStatus(HttpServletResponse.SC_OK);
        } else {
            res.getWriter().write("Album not found with albumId: " + albumId);
            res.setStatus(HttpServletResponse.SC_NOT_FOUND);
        }
    }

    private byte[] convertPartToBytes(Part part) throws IOException {
        try (InputStream inputStream = part.getInputStream()) {
            return inputStream.readAllBytes();
        }
    }
}
