## DL Crawler

Crawls the files of the selected extensions, starting from the given roots (which can be more than one) using **multiple threads**.

Supports **pause/resume**: you can close the program without any explicit save action that you need to perform, and on the next run it will be just continuing from where it was left.

### How to use

1) Put the list of the root URLs that you want as the crawling roots to the file **crawl_roots**

2) Change the root domain variable in the code Runner.java

3) Run the program. The list of URLs will be in the file **download-list**, logs will be in the file **logs**.

The files **bfs-queue-dump**, **downloadables-dump** and **visit-dump** are needed for pause/resume: do not delete them unless you want to start the crawling from scratch.
