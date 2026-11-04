package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private final Gson gson = new Gson();

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String method = ex.getRequestMethod().toUpperCase();
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getQuery();

        String[] pathParts = path.split("/");

        switch (method) {
            case "GET" -> {
                if (pathParts.length == 2 && pathParts[1].equals("movies")) {
                    if (query != null && !query.isEmpty()) {
                        handleGetWithQuery(ex, query);
                    } else {
                        sendJson(ex, 200, gson.toJson(store.getAll()));
                    }
                } else if (pathParts.length == 3 && pathParts[1].equals("movies")) {
                    handleGetById(ex, pathParts[2]);
                } else {
                    sendJson(ex, 404, gson.toJson(new ErrorResponse("Эндпоинт не найден")));
                }
            }
            case "POST" -> {
                if (pathParts.length == 2 && pathParts[1].equals("movies")) {
                    handlePost(ex);
                } else {
                    sendJson(ex, 404, gson.toJson(new ErrorResponse("Эндпоинт не найден")));
                }
            }
            case "DELETE" -> {
                if (pathParts.length == 3 && pathParts[1].equals("movies")) {
                    handleDelete(ex, pathParts[2]);
                } else {
                    sendJson(ex, 404, gson.toJson(new ErrorResponse("Эндпоинт не найден")));
                }
            }
            default -> sendJson(ex, 405, gson.toJson(new ErrorResponse("Метод не поддерживается")));
        }
    }

    private void handleGetWithQuery(HttpExchange ex, String query) throws IOException {
        String[] params = query.split("&");
        String yearParam = null;
        for (String param : params) {
            String[] pair = param.split("=");
            if (pair.length == 2 && pair[0].equals("year")) {
                yearParam = pair[1];
            }
        }

        if (yearParam == null) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'")));
            return;
        }

        try {
            int year = Integer.parseInt(yearParam);
            List<Movie> movies = store.getByYear(year);
            sendJson(ex, 200, gson.toJson(movies));
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный параметр запроса — 'year'")));
        }
    }

    private void handleGetById(HttpExchange ex, String idStr) throws IOException {
        try {
            int id = Integer.parseInt(idStr);
            Optional<Movie> movieOpt = store.getById(id);
            if (movieOpt.isPresent()) {
                sendJson(ex, 200, gson.toJson(movieOpt.get()));
            } else {
                sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
            }
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный ID")));
        }
    }

    private void handlePost(HttpExchange ex) throws IOException {
        String contentType = ex.getRequestHeaders().getFirst("Content-Type");
        if (contentType == null || !contentType.toLowerCase().contains("application/json")) {
            sendJson(ex, 415, gson.toJson(new ErrorResponse("Неподдерживаемый Content-Type")));
            return;
        }

        String body;
        try (InputStream is = ex.getRequestBody()) {
            body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }

        Movie movie;
        try {
            movie = gson.fromJson(body, Movie.class);
            if (movie == null) {
                sendJson(ex, 400, gson.toJson(new ErrorResponse("Ошибка валидации", List.of("Тело запроса не может быть пустым"))));
                return;
            }
        } catch (JsonSyntaxException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный JSON")));
            return;
        }

        List<String> details = new ArrayList<>();
        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            details.add("название не должно быть пустым");
        } else if (movie.getTitle().length() > 100) {
            details.add("длина названия не должна превышать 100 символов");
        }

        int maxYear = Year.now().getValue() + 1;
        if (movie.getYear() == null || movie.getYear() < 1888 || movie.getYear() > maxYear) {
            details.add("год должен быть между 1888 и " + maxYear);
        }

        if (!details.isEmpty()) {
            sendJson(ex, 422, gson.toJson(new ErrorResponse("Ошибка валидации", details)));
            return;
        }

        Movie created = store.add(movie);
        sendJson(ex, 201, gson.toJson(created));
    }

    private void handleDelete(HttpExchange ex, String idStr) throws IOException {
        try {
            int id = Integer.parseInt(idStr);
            if (store.delete(id)) {
                sendNoContent(ex);
            } else {
                sendJson(ex, 404, gson.toJson(new ErrorResponse("Фильм не найден")));
            }
        } catch (NumberFormatException e) {
            sendJson(ex, 400, gson.toJson(new ErrorResponse("Некорректный ID")));
        }
    }
}
