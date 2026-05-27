package com.capteam.gaobackend.exception;

public class TeamNotFoundException extends RuntimeException{
    public TeamNotFoundException(String message) {
        super(message);
    }

    public TeamNotFoundException() {
        super("team not found");
    }
}
