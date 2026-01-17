package com.tarun.repository;

import com.tarun.model.Item;

import java.util.concurrent.ConcurrentHashMap;
import java.util.Map;

public class ItemRepository {
    private final Map<String, Item> items = new ConcurrentHashMap<>();

    public void save(Item item) {
        items.put(item.getId(), item);
    }

    public Item findById(String id) {
        return items.get(id);
    }

    public boolean exists(String id) {
        return items.containsKey(id);
    }
}
