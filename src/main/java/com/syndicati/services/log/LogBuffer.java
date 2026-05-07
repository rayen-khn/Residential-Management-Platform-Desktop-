package com.syndicati.services.log;

import com.syndicati.models.log.AppEventLog;
import java.util.ArrayList;
import java.util.List;

public class LogBuffer {

    private final List<AppEventLog> items = new ArrayList<>();

    public synchronized void push(AppEventLog entity) {
        if (entity != null) {
            items.add(entity);
        }
    }

    public synchronized List<AppEventLog> snapshot() {
        return new ArrayList<>(items);
    }

    public synchronized List<AppEventLog> drain() {
        List<AppEventLog> copy = new ArrayList<>(items);
        items.clear();
        return copy;
    }

    public synchronized void clear() {
        items.clear();
    }
}