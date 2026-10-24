package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import ru.practicum.moviehub.model.Movie;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private final Gson gson = new Gson();

    @BeforeAll
    static void beforeAll() {
        server = new MoviesServer();
        server.start();
        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @BeforeEach
    void setUp() {
        server.getStore().clear();
    }

    @Test
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse(""));
        assertEquals("[]", resp.body().trim());
    }

    @Test
    void createMovie_valid_returnsCreated() throws Exception {
        Movie movie = new Movie("Матрица", 1999);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(201, resp.statusCode());
        Movie created = gson.fromJson(resp.body(), Movie.class);
        assertNotNull(created.getId());
        assertEquals("Матрица", created.getTitle());
        assertEquals(1999, created.getYear());
    }

    @Test
    void createMovie_invalidTitleAndYear_returns422() throws Exception {
        Movie movie = new Movie("", 1700);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(422, resp.statusCode());
    }

    @Test
    void getMovieById_exists_returns200() throws Exception {
        Movie added = server.getStore().add(new Movie("Начало", 2010));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + added.getId()))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());
        Movie found = gson.fromJson(resp.body(), Movie.class);
        assertEquals("Начало", found.getTitle());
    }

    @Test
    void deleteMovie_exists_returns204() throws Exception {
        Movie added = server.getStore().add(new Movie("Интерстеллар", 2014));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/" + added.getId()))
                .DELETE()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode());
        assertTrue(server.getStore().getAll().isEmpty());
    }

    @Test
    void getMoviesByYear_returnsFilteredList() throws Exception {
        server.getStore().add(new Movie("Фильм A", 2020));
        server.getStore().add(new Movie("Фильм B", 2020));
        server.getStore().add(new Movie("Фильм C", 2021));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2020"))
                .GET()
                .build();

        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode());

        // Используем выделенный класс TypeToken
        Type listType = new ListOfMoviesTypeToken().getType();
        List<Movie> movies = gson.fromJson(resp.body(), listType);

        assertEquals(2, movies.size());
    }
}