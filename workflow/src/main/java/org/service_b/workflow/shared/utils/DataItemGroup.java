package org.service_b.workflow.shared.utils;

import java.util.ArrayList;
import java.util.List;

public class DataItemGroup extends DataItem<List<DataItem<Object>>> {

    public DataItemGroup(final String key, final List<DataItem<Object>> value) {
        super(key, value);
    }

    public DataItemGroup() {
    }

    public DataItemGroup(String key) {
        super(key, new ArrayList<>());
    }

    public DataItemGroup add(DataItem<Object> dataItem) {
        this.value.add(dataItem);
        return this;
    }
}
