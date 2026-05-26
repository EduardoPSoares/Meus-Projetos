package me.ray.midgardLoremakers.service;

public final class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
