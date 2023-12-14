package util;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;
import model.Album;
import model.ImageMetaData;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author xiaorui
 */
@Slf4j
public class UriUtils {
    private static final Gson gson = new Gson();
    private static final byte[] imageBytes = loadImageData();
    private static final String PROFILE_JSON =  gson.toJson(new Album("Sex Pistols", "Never Mind The Bollocks!", "1977"));

    public static HttpPost createAlbumPost(String ipAddr) {
        URI postUri = URI.create(ipAddr + "/albums");
        HttpPost postRequest = new HttpPost(postUri);

        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        builder.addBinaryBody("image", imageBytes, ContentType.IMAGE_PNG, "image.png");
        builder.addTextBody("profile", PROFILE_JSON, ContentType.APPLICATION_JSON);
        HttpEntity entity = builder.build();
        postRequest.setEntity(entity);

        return postRequest;
    }

    public static HttpGet createAlbumGet(String ipAddr, String albumId) {
        URI getUri = URI.create(ipAddr + "/albums/" + albumId);
        return new HttpGet(getUri);
    }

    public static HttpPost createReviewPost(String ipAddr, String likeOrNot, String albumId) {
        URI postUri = URI.create(ipAddr + "/review" + "/" + likeOrNot + "/" + albumId);
        HttpPost postRequest = new HttpPost(postUri);
        return postRequest;
    }

    public static HttpGet createReviewGet(String ipAddr, String albumId) {
        URI getUri = URI.create(ipAddr + "/review/" + albumId);
        return new HttpGet(getUri);
    }

    public static String executeAlbumPost(HttpUriRequest request, CloseableHttpClient httpClient) {
        int retries = Constant.MAX_RETRIES;

        while (retries > 0) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                ImageMetaData postResponse = gson.fromJson(responseBody, ImageMetaData.class);
                String albumId = postResponse.getAlbumId();
                EntityUtils.consume(response.getEntity());

                int statusCode = response.getStatusLine().getStatusCode();

                if (statusCode >= 200 && statusCode < 300) {
                    return albumId;
                } else if (statusCode >= 400 && statusCode < 600) {
                    retries--;
                    if (retries == 0) {
                        log.error("Failed to execute request after 5 retries. URL: " + request.getURI() + request.getMethod());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.info("Exception in Album Post: " + request.getMethod());
            }
        }
        return null;
    }

    public static void executeReviewPost(HttpUriRequest request, CloseableHttpClient httpClient) {
        int retries = Constant.MAX_RETRIES;

        while (retries > 0) {
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                EntityUtils.consume(response.getEntity());

                if (statusCode >= 200 && statusCode < 300) {
                    return;
                } else if (statusCode >= 400 && statusCode < 600) {
                    retries--;
                    if (retries == 0) {
                        log.error("Failed to execute request after 5 retries. URL: " + request.getURI() + request.getMethod());
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                log.info("Exception in Review Post: " + request.getMethod());
            }
        }
    }

    public static void executeReviewGet(HttpUriRequest request, CloseableHttpClient httpClient, AtomicInteger successfulRequests, AtomicInteger failedRequests, ConcurrentLinkedDeque<Long> reviewGetLatencies) {
        int retries = Constant.MAX_RETRIES;

        while (retries > 0) {
            long startTime = System.currentTimeMillis();
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                EntityUtils.consume(response.getEntity());
                long endTime = System.currentTimeMillis();
                long latency = endTime - startTime;
                reviewGetLatencies.add(latency);

                if (statusCode >= 200 && statusCode < 300) {
                    successfulRequests.incrementAndGet();
                    return;
                } else if (statusCode >= 400 && statusCode < 600) {
                    failedRequests.incrementAndGet();
                    retries--;
                    if (retries == 0) {
                        log.error("Failed to execute request after 5 retries. URL: " + request.getURI() + request.getMethod());
                    }
                }
            } catch (Exception e) {
                failedRequests.incrementAndGet();
                e.printStackTrace();
                log.info("Exception in Review Get: " + request.getMethod());
            }
        }
    }

    private static byte[] loadImageData() {
        byte[] imageBytes = null;
        try {
            Path imagePath = Paths.get("src/main/resources/nmtb.png");
            imageBytes = Files.readAllBytes(imagePath);
        } catch (IOException e) {
            log.error("Failed to read the image file into memory", e);
            System.exit(1);
        }
        return imageBytes;
    }
}
