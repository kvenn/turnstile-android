package com.vimeo.download_sample.downloadqueue.exception;

/**
 * Exception thrown by the DownloadManager when a download is initiated with a null video
 */
public class NullRemoteFileUrlException extends DownloadException {

    private static final long serialVersionUID = 5590875620172472120L;

    public NullRemoteFileUrlException(String message) {
        super(message);
    }
}