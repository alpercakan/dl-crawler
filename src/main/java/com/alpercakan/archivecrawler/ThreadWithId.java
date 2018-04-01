package com.alpercakan.archivecrawler;

public class ThreadWithId implements Comparable<ThreadWithId> {

    public Thread thread;
    public final int id;

    public ThreadWithId(Thread th, int id) {
        this.thread = th;
        this.id = id;
    }

    @Override
    public int compareTo(ThreadWithId o) {
        return Integer.compare(id, o.id);
    }
}
