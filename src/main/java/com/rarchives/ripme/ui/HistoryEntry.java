package com.rarchives.ripme.ui;

import org.json.JSONObject;

import java.util.Date;

public class HistoryEntry {

    private static final String KEY_TITLE = "title";
    private static final String KEY_COUNT = "count";
    private static final String KEY_SELECTED = "selected";
    public String url = "";
    public String title = "";
    String dir = "";
    public int count = 0;
    Date startDate = new Date();
    Date modifiedDate = new Date();
    public boolean selected = false;

    public HistoryEntry() {
    }

    public HistoryEntry fromJSON(JSONObject json) {
        this.url = json.getString("url");
        this.startDate = new Date(json.getLong("startDate"));
        this.modifiedDate = new Date(json.getLong("modifiedDate"));

        if (json.has(KEY_TITLE))
            this.title = json.getString(KEY_TITLE);

        if (json.has(KEY_COUNT))
            this.count = json.getInt(KEY_COUNT);

        if (json.has("dir"))
            this.dir = json.getString("dir");

        if (json.has(KEY_SELECTED))
            this.selected = json.getBoolean(KEY_SELECTED);

        return this;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("url", this.url);
        json.put("startDate", this.startDate.getTime());
        json.put("modifiedDate", this.modifiedDate.getTime());
        json.put(KEY_TITLE, this.title);
        json.put(KEY_COUNT, this.count);
        json.put(KEY_SELECTED, this.selected);
        return json;
    }

    @Override
    public String toString() {
        return this.url;
    }

}