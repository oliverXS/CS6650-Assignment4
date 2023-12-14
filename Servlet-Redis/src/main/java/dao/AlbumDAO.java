package dao;

import lombok.extern.slf4j.Slf4j;
import model.Album;

import java.sql.*;
import java.util.Optional;

/**
 * @author xiaorui
 */
@Slf4j
public class AlbumDAO {
    private Connection connection;

    public AlbumDAO(Connection connection) {
        this.connection = connection;
    }

    public Optional<Integer> saveAlbum(Album album, byte[] imageBytes) {
        String insertAlbumSQL = "INSERT INTO albums.album(artist, title, year, image_data) VALUES(?,?,?,?)";
        try (PreparedStatement st = connection.prepareStatement(insertAlbumSQL, Statement.RETURN_GENERATED_KEYS)) {

            st.setString(1, album.getArtist());
            st.setString(2, album.getTitle());
            st.setString(3, album.getYear());
            st.setBytes(4, imageBytes);
            st.executeUpdate();

            try (ResultSet generatedKeys = st.getGeneratedKeys()) {
                if (generatedKeys.next()) {
                    return Optional.of(generatedKeys.getInt(1));
                } else {
                    log.error("Failed to retrieve ID for the inserted album.");
                    return Optional.empty();
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to save the album.", ex);
            return Optional.empty();
        }
    }

    public Album getAlbum(int albumId) throws SQLException {
        String getAlbumSQL = "SELECT artist, title, year FROM albums.album WHERE album_id = ?";
        try (PreparedStatement st = connection.prepareStatement(getAlbumSQL)) {
            st.setInt(1, albumId);

            try (ResultSet rs = st.executeQuery()) {
                if (rs.next()) {
                    return new Album(rs.getString("artist"), rs.getString("title"), rs.getString("year"));
                }
            }
        } catch (SQLException ex) {
            log.error("Failed to retrieve the album with ID: " + albumId, ex);
        }
        return null;
    }

}
