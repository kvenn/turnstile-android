package com.vimeo.download_sample.downloadqueue.exception;

/**
 * Exception thrown by the DownloadManager when the file to be downloaded exceeds the space on the device
 */
public class OutOfSpaceException extends DownloadException {

    private static final long serialVersionUID = 1982157402859085674L;

    public OutOfSpaceException(String message) {
        super(message);
    }
}