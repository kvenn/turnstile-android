package com.vimeo.download_sample.downloadqueue.exception;

/**
 * Generic exception thrown from the DownloadManager
 */
public class DownloadException extends Exception {

    private static final long serialVersionUID = 7839521136527513801L;

    public DownloadException(String message) {
        super(message);
    }
}
