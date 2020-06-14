package com.slinkydeveloper.sdp.timer;

import com.slinkydeveloper.sdp.log.LoggerConfig;

import java.util.logging.Logger;

class TimerTask extends java.util.TimerTask {

    private final static Logger LOG = LoggerConfig.getLogger(TimerTask.class);

    private final String name;
    private final Runnable runnable;

    public TimerTask(String name, Runnable runnable) {
        this.name = name;
        this.runnable = runnable;
    }

    @Override
    public void run() {
        LOG.info("Triggered timer '" + name + "'");
        this.runnable.run();
    }
}
