package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.stream.Collectors;

public class MoviesStore {
    private final Map<Integer, Movie> movies = new HashMap<>();
    private int nextId = 1;

    public synchronized List<Movie> getAll() {
        return new ArrayList<>(movies.values());
    }

    public synchronized Optional<Movie> getById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public synchronized Movie add(Movie movie) {
        movie.setId(nextId++);
        movies.put(movie.getId(), movie);
        return movie;
    }

    public synchronized boolean delete(int id) {
        return movies.remove(id) != null;
    }

    public synchronized List<Movie> getByYear(int year) {
        return movies.values().stream()
                .filter(m -> m.getYear() != null && m.getYear() == year)
                .collect(Collectors.toList());
    }

    public synchronized void clear() {
        movies.clear();
        nextId = 1;
    }
}