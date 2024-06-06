package com.docgen.paper.model;

import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * @author Денис on 04.06.2024
 */
@Component
public class TagMap {
    private Map<String, String> tagMap = new HashMap<>();

    public Map<String, String> getTagMap() {
        return tagMap;
    }

    public void setTagMap(Map<String, String> tagMap) {
        this.tagMap = tagMap;
    }

    public void addTag(String tag, String value) {
        tagMap.put(tag, value);
    }

}