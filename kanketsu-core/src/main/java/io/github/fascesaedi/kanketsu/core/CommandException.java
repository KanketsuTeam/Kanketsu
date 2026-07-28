package io.github.fascesaedi.kanketsu.core;

public class CommandException extends RuntimeException{
    private final int code;
    private final String description;

    public CommandException(int code, String message){
        super(message);
        this.code = code;
        this.description = null;
    }

    public CommandException(int code, String message, Throwable cause){
        super(message);
        this.code = code;
        this.description = null;
    }

    public CommandException(int code, String message, String description) {
        super(message);
        this.code = code;
        this.description = description;
    }

    public int getCode(){
        return code;
    }

    public String getDescription() {
        return description;
    }
}
