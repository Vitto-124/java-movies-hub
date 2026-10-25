package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    private final String error;
    private List<String> details;

    public ErrorResponse(String error) {
        this.error = error;
    }

    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    @SuppressWarnings("unused")
    public String getError() {
        return error;
    }

    @SuppressWarnings("unused")
    public List<String> getDetails() {
        return details;
    }
}