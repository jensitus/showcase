package org.service_b.workflow.shared.utils;

import lombok.Data;

@Data
public class DataItem<T> {
    String key;
    T value;

    public DataItem() {
    }

    public DataItem(final String key, final T value) {
        this.key = key;
        this.value = value;
    }
}
