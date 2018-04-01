package com.alpercakan.archivecrawler;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.Semaphore;

public class Runner {

    private static final int ARGUMENT_COUNT = 5;
    private static final String[] dwExts = { "pdf", "xlsx", "xls" };
    private static final String CRAWL_ROOT_DOMAIN = ""; // XXX Write here the actual root in the format: example.com

    private static ArrayList<ThreadWithId> threadList = new ArrayList<>();
    private static Semaphore threadListSemaphore = new Semaphore(1);

    public static boolean notifyComplete(int id) {
        try {
            threadListSemaphore.acquire();
        } catch (InterruptedException e) {
            return false;
        }

        int index = -1;

        for (int i = 0; i < threadList.size(); ++i) {
            if (threadList.get(i).id == id) {
                index = i;
                break;
            }
        }

        boolean success = false;

        if (index >= 0) {
            threadList.remove(index);
            success = true;
        }

        threadListSemaphore.release();
        return success;
    }

    public static void waitAllThreads() {
        while (true) {
            Thread threadToWait = null;

            try {
                threadListSemaphore.acquire();
            } catch (InterruptedException e) {
                continue;
            }

            if (threadList.size() > 0) {
                threadToWait = threadList.get(0).thread;
            }

            threadListSemaphore.release();

            if (threadToWait == null)
                break;

            try {
                threadToWait.wait();
            } catch (Exception e) { }
        }
    }

    public static void main(String []args) {

        if (args.length != ARGUMENT_COUNT) {
            System.out.println("[crawl-root-file-name] [thread-count] [max-depth] [dump-period] [log-file]");
        }

        ArrayList<String> crawlRoots = new ArrayList<>();
        final int threadCount = Integer.parseInt(args[1]);
        final int maxDepth = Integer.parseInt(args[2]);
        final int dumpPeriod = Integer.parseInt(args[3]);

        try(Scanner listScanner = new Scanner(new File(args[0]))) {
            while (listScanner.hasNextLine()) {
                crawlRoots.add(listScanner.nextLine());
            }
        } catch (IOException e) {
            System.out.println("IO exception: " + e.toString());

            return;
        }

        final int fairShare = crawlRoots.size() / threadCount;
        int threadId = 0;

        ArrayList<String> overloadedRoots = new ArrayList<>();

        while (!crawlRoots.isEmpty() && (crawlRoots.size() % threadCount != 0)) {
            overloadedRoots.add(crawlRoots.get(0));
            crawlRoots.remove(0);
        }

        threadList.add(new ThreadWithId(new Thread(() -> {
            (new Crawler(overloadedRoots.toArray(new String[0]), maxDepth, dwExts, CRAWL_ROOT_DOMAIN)).crawl(0);
        }), threadId++));

        threadList.get(0).thread.start();

        for (int i = 0; i < crawlRoots.size(); ++i) {
            ArrayList<String> roots = new ArrayList<>();

            for (int j = 0; j < threadCount; ++j) {
                roots.add(crawlRoots.get(i + j));
            }

            final int currentThreadId = threadId++;

            threadList.add(new ThreadWithId(new Thread(() -> {
                (new Crawler((String []) roots.toArray(), maxDepth, dwExts, CRAWL_ROOT_DOMAIN)).crawl(currentThreadId);
            }), currentThreadId));

            threadList.get(threadList.size() - 1).thread.run();

            i += threadCount;
        }

        Timer recordTimer = new Timer(true);
        recordTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                Crawler.saveState("visit-dump", "downloadables-dump", "bfs-queue-dump", "download-list");
            }
        }, 0, dumpPeriod);

        LogKeeper.start(args[4]);

        waitAllThreads();
    }
}
