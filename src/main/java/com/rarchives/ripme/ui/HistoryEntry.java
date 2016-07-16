package com.rarchives.ripme.ui;

import org.json.JSONObject;

import java.util.Date;

public class HistoryEntry {

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

        if (json.has("title"))
            this.title = json.getString("title");

        if (json.has("count"))
            this.count = json.getInt("count");

        if (json.has("dir"))
            this.dir = json.getString("dir");

        if (json.has("selected"))
            this.selected = json.getBoolean("selected");

        return this;
    }

    public JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put("url", this.url);
        json.put("startDate", this.startDate.getTime());
        json.put("modifiedDate", this.modifiedDate.getTime());
        json.put("title", this.title);
        json.put("count", this.count);
        json.put("selected", this.selected);
        return json;
    }

    @Override
    public String toString() {
        return this.url;
    }
}