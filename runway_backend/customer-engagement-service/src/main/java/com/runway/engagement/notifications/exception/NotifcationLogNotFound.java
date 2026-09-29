package com.runway.engagement.notifications.exception;

public class NotifcationLogNotFound extends RuntimeException {
    public NotifcationLogNotFound(String noSuchNotifcationLogFound) {
        super(noSuchNotifcationLogFound);
    }
}
