package util;


import javax.servlet.http.HttpServletRequest;

/**
 * @author xiaorui
 */
public class UrlUtils {
    public static boolean isValidUrl(String urlPath) {
        if (urlPath == null || urlPath.isEmpty()) {
            return false;
        }
        return true;
    }

    public static boolean isValidFormatForAlbumGet(String[] urlParts) {
        if (urlParts.length != 2) {
            return false;
        }
        try {
            Integer.parseInt(urlParts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isValidReqForAlbumPost(HttpServletRequest request) {
        if (!request.getContentType().regionMatches(true, 0, "multipart/", 0, 10)) {
            return false;
        }
        return true;
    }

    public static boolean isValidFormatForReviewPost(String[] urlParts) {
        // POST: /review/{likeornot}/{albumId}
        if (urlParts.length != 3) {
            return false;
        }
        if (!urlParts[1].equals("like") && !urlParts[1].equals("dislike")) {
            return false;
        }
        try {
            Integer.parseInt(urlParts[2]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }

    public static boolean isValidFormatForReviewGet(String[] urlParts) {
        // GET: /review/{albumId}
        if (urlParts.length != 2) {
            return false;
        }
        try {
            Integer.parseInt(urlParts[1]);
        } catch (NumberFormatException e) {
            return false;
        }
        return true;
    }
}
