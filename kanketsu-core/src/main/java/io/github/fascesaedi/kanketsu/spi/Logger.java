package io.github.fascesaedi.kanketsu.spi;

public interface Logger {
    void log(String message);

    default void info(String message) { log("[INFO] " + message); }
    default void warn(String message) { log("[WARN] " + message); }
    default void error(String message) { log("[ERROR] " + message); }
    default void success(String message) { log("[SUCCESS] " + message); }

    static Logger system() {
        return System.out::println;
    }

    static Logger noop() {
        return message -> {};
    }
}
