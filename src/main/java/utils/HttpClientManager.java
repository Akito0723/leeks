package utils;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * @author by laugh on 2016/3/29.
 */
public class HttpClientManager {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration DEFAULT_REQUEST_TIMEOUT = Duration.ofSeconds(2);

    private static volatile HttpClientManager instance;

    private final HttpClient directClient;
    private volatile HttpClient proxyAwareClient;

    public static HttpClientManager getInstance() {
        HttpClientManager tmp = instance;
        if (tmp == null) {
            synchronized (HttpClientManager.class) {
                tmp = instance;
                if (tmp == null) {
                    tmp = new HttpClientManager();
                    instance = tmp;
                }
            }
        }
        return tmp;
    }

    private HttpClientManager() {
        directClient = newClientBuilder().build();
        proxyAwareClient = directClient;
    }

    public synchronized void configureProxy(String proxyStr) {
        if (StringUtils.isBlank(proxyStr)) {
            proxyAwareClient = directClient;
            return;
        }

        HttpClient.Builder httpClientBuilder = newClientBuilder();
        String[] proxyParts = StringUtils.split(proxyStr, ':');
        if (proxyParts.length == 2) {
            String host = proxyParts[0];
            int port = Integer.parseInt(proxyParts[1]);
            httpClientBuilder.proxy(ProxySelector.of(new InetSocketAddress(host, port)));
        }
        LogUtil.notifyInfo("setup proxy success->" + proxyStr);
        proxyAwareClient = httpClientBuilder.build();
    }

    public String get(String url) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_REQUEST_TIMEOUT)
                .GET()
                .build();
        return getResponseContent(url, request, proxyAwareClient, HttpResponse.BodyHandlers.ofString());
    }

    public String post(String url) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_REQUEST_TIMEOUT)
                .POST(HttpRequest.BodyPublishers.noBody())
                .build();
        return getResponseContent(url, request, proxyAwareClient, HttpResponse.BodyHandlers.ofString());
    }

    public byte[] getBytesDirect(String url, Duration requestTimeout) throws HttpRequestException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(requestTimeout)
                .GET()
                .build();
        return getResponseContent(url, request, directClient, HttpResponse.BodyHandlers.ofByteArray());
    }

    private static HttpClient.Builder newClientBuilder() {
        return HttpClient.newBuilder()
                .connectTimeout(CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL);
    }

    private <T> T getResponseContent(String url, HttpRequest request, HttpClient client,
            HttpResponse.BodyHandler<T> bodyHandler) throws HttpRequestException {
        try {
            HttpResponse<T> response = client.send(request, bodyHandler);
            int statusCode = response.statusCode();
            if (statusCode < 200 || statusCode >= 300) {
                String responseBody = response.body() instanceof String body ? body : "";
                throw new HttpStatusException(url, statusCode, responseBody);
            }
            return response.body();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpRequestException("HTTP request interrupted for url : " + URLDecoder.decode(url, StandardCharsets.UTF_8), e);
        } catch (IOException | IllegalArgumentException e) {
            throw new HttpRequestException("got an error from HTTP for url : " + URLDecoder.decode(url, StandardCharsets.UTF_8), e);
        }
    }

    public static class HttpRequestException extends Exception {
        public HttpRequestException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    public static class HttpStatusException extends HttpRequestException {
        private final int statusCode;

        public HttpStatusException(String url, int statusCode, String responseBody) {
            super(buildMessage(url, statusCode, responseBody), null);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }

        private static String buildMessage(String url, int statusCode, String responseBody) {
            String body = responseBody == null ? "" : responseBody;
            String detail = body.length() > 200 ? body.substring(0, 200) : body;
            return "HTTP status error for url : " + URLDecoder.decode(url, StandardCharsets.UTF_8)
                    + ", statusCode=" + statusCode + ", responseBody=" + detail;
        }
    }
}
