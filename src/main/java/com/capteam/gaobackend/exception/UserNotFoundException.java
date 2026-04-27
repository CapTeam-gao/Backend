package com.capteam.gaobackend.exception;

public class UserNotFoundException extends RuntimeException{
    public UserNotFoundException(String message) {super(message);}
    public UserNotFoundException() {super("user not found");}
}
