package cetus.openai;

import java.net.Authenticator;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;

/**
 * GPTActor
 */
public class GPTActor {

    private HttpClient client;

    public GPTActor() {
        client = HttpClient.newHttpClient();
    }

    public void test() {
        // create a request
        HttpRequest request = HttpRequest.newBuilder(
                URI.create("https://api.openai.com/v1/models"))
                .header("accept", "application/json")
                .header("Authorization", "Bearer "+System.getenv("OPENAI_API_KEY"))
                .build();

        // client.send(request, new JsonBodyHandler<>(APOD.class));
    }
}