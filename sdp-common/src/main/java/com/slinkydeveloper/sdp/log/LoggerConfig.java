package com.slinkydeveloper.sdp.log;

import java.util.Optional;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public class LoggerConfig {

    private static volatile ConsoleHandler instance = null;

    private LoggerConfig() {}

    public static ConsoleHandler getConsoleHandlerInstance() {
        if (instance == null) {
            synchronized(LoggerConfig.class) {
                if (instance == null) {
                    instance = new ConsoleHandler();
                    instance.setLevel(
                            Level.parse(
                                    Optional.ofNullable(System.getenv("SDP_LOG")).orElse("INFO")
                            )
                    );
                }
            }
        }
        return instance;
    }

    public static Logger getLogger(Class<?> clazz) {
        Logger logger = Logger.getLogger(clazz.getName());
        logger.addHandler(getConsoleHandlerInstance());
        logger.setLevel(
                Level.parse(
                        Optional.ofNullable(System.getenv("SDP_LOG")).orElse("INFO")
                )
        );
        return logger;
    }
}
