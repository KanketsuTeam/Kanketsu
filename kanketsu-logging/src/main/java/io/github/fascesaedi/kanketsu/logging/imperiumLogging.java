package io.github.fascesaedi.kanketsu.logging;

import io.github.fascesaedi.kanketsu.spi.Logger;
import xyz.imperium.log.Log;

public class imperiumLogging implements Logger {
    @Override
    public void log(String message) {
        Log.norm(message);
    }

    @Override
    public void warn(String message) {
        Log.warn(message);
    }

    @Override
    public void info(String message) {
        Log.info(message);
    }

    @Override
    public void error(String message) {
        Log.error(message);
    }

    @Override
    public void success(String message) {
        Log.success(message);
    }
}
