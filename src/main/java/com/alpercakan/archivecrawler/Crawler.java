package com.alpercakan.archivecrawler;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import sun.rmi.runtime.Log;

import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Crawler {
    private String[] baseUrls, dwExtensions;
    private String allowedHost;
    private int maxDepth;
    private OkHttpClient client = new OkHttpClient();


    private static ConcurrentSkipListMap<URI, Integer> distMap = new ConcurrentSkipListMap<>();
    private static ConcurrentLinkedQueue<URI> bfsQueue = new ConcurrentLinkedQueue<>();
    private static ConcurrentLinkedQueue<String> downloadables = new ConcurrentLinkedQueue<>();

    public Crawler(final String[] baseUrls, final int maxDepth, final String[] dwExtensions, String allowedHost) {
        this.baseUrls = baseUrls;
        this.maxDepth = maxDepth;
        this.dwExtensions = dwExtensions;
        this.allowedHost = allowedHost;
    }

    private String getPage(String url) {
        try {
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            Response response = client.newCall(request).execute();
            return response.body().string();
        } catch (IOException e) {
            return null;
        }
    }

    private ArrayList<URI> getNeighbors(URI uri){
        String content = getPage(uri.toString());

        if (content == null)
            return null;

        ArrayList<URI> result = new ArrayList<>();

        Pattern hrefPattern = Pattern.compile("(href|src)=\".+?\"");
        Matcher matcher = hrefPattern.matcher(content);

        while (matcher.find()) {
            boolean failed = false;
            URI currUri = null;
            String group = matcher.group();
            try {
                currUri = new URI(group.substring(group.indexOf('=') + 2, group.length() - 1));
            } catch (URISyntaxException e) {
                failed = true;
            }

            if (currUri != null && (failed || currUri.getHost() == null)) {
                try {
                    currUri = new URI(uri.getHost() + '/' + group.substring(group.indexOf('=') + 2, group.length() - 1));
                } catch (URISyntaxException e) { }
            }

            if (currUri != null)
                result.add(currUri);
        }

        return result;
    }

    public void crawl(int threadId) {
        LogKeeper.log("Crawler with thread ID " + threadId + " has started with root " + Arrays.toString(baseUrls));

        for (String baseUrl : baseUrls) {
            try {
                URI uri = new URI(baseUrl);

                distMap.put(uri, 0);
                bfsQueue.add(uri);
            } catch (URISyntaxException e) {
                LogKeeper.log("Illegal URI syntax: " + e.toString());
            }
        }

        while (!bfsQueue.isEmpty()) {
            URI front = bfsQueue.poll();

            if (front == null)
                continue;

            ArrayList<URI> adjs = getNeighbors(front);

            if (adjs == null) {
                LogKeeper.log("Failed to fetch " + front.toString());
            } else {
                for (URI adj : adjs) {
                    if (adj.getHost() == null || (!adj.getHost().endsWith('.' + allowedHost) && !allowedHost.equals(adj.getHost()))) {
                        continue;
                    }

                    Integer dist = distMap.putIfAbsent(adj, distMap.get(front) + 1);

                    if (dist == null) {
                        boolean isDownloadable = false;

                        for (String extension : dwExtensions) {
                            if (adj.toString().endsWith('.' + extension)) {
                                isDownloadable = true;
                                break;
                            }
                        }

                        if (isDownloadable) {
                            downloadables.add(adj.toString());

                            LogKeeper
                                    .log("Download " + adj.toString());
                        } else if (distMap.get(front) < maxDepth) {
                            bfsQueue.add(adj);
                        }
                    }
                }
            }
        }

        Runner.notifyComplete(threadId);
    }

    public static boolean saveState(String visitFile,
                                    String downloadFile,
                                    String queueFile,
                                    String humanReadable) {
        try {
            ObjectOutputStream oosVisit = new ObjectOutputStream(new FileOutputStream(visitFile)),
                               oosDownload = new ObjectOutputStream(new FileOutputStream(downloadFile)),
                               oosQueue = new ObjectOutputStream(new FileOutputStream(queueFile));

            PrintStream humane = new PrintStream(new FileOutputStream(humanReadable));

            oosVisit.writeObject(distMap);
            oosDownload.writeObject(downloadables);
            oosQueue.writeObject(bfsQueue);

            humane.print(downloadables.toString().replaceAll("(, )", "\n"));

            humane.close();
            oosDownload.close();
            oosVisit.close();
            oosQueue.close();
        } catch (IOException e) {
            return false;
        }

        return true;
    }
}
