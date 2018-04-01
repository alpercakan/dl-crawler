package com.alpercakan.archivecrawler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LogKeeper {

    private static ConcurrentLinkedQueue<String> logQueue = new ConcurrentLinkedQueue<>();

    private static int LOG_DUMP_PERIOD = 100;

    public static void start(final String logFileName) {
        Timer logTimer = new Timer(true);

        logTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                try {
                    PrintStream ps = new PrintStream(new FileOutputStream(logFileName, true));

                    while (!logQueue.isEmpty()) {
                        String frontMsg = logQueue.poll();

                        if (frontMsg != null) {
                            ps.println(frontMsg);
                        }
                    }

                    ps.close();
                } catch (IOException e) { }
            }
        }, 0, LOG_DUMP_PERIOD);
    }

    public static void log(String msg) {
        logQueue.add(LocalDateTime.now().toString() + " - " + msg);
    }
}
