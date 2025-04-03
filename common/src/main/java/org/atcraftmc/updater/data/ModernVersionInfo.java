package org.atcraftmc.updater.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class ModernVersionInfo {
    private final String version;
    private final long timestamp;
    private final Map<String, FileData> add;
    private final Set<String> remove;

    public ModernVersionInfo(String version, long timestamp, Map<String, FileData> add, Set<String> remove) {
        this.version = version;
        this.timestamp = timestamp;
        this.add = add;
        this.remove = remove;
    }

    public ModernVersionInfo(JsonObject dom) {
        this.version = dom.get("version").getAsString();
        this.timestamp = dom.get("timestamp").getAsLong();

        this.add = new HashMap<>();
        this.remove = new HashSet<>();

        var addEntry = dom.get("add").getAsJsonObject();
        for (var entry : addEntry.entrySet()) {
            this.add.put(entry.getKey(), FileData.decode(entry.getValue().getAsString()));
        }
    }

    public ModernVersionInfo copyOfInformation(ModernVersionInfo informationSource) {
        return new ModernVersionInfo(informationSource.version, informationSource.timestamp, add, remove);
    }

    public String getVersion() {
        return version;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public Map<String, FileData> getAddList() {
        return add;
    }

    public Set<String> getRemoveList() {
        return remove;
    }

    @Override
    public String toString() {
        var dom = new JsonObject();

        dom.addProperty("version", version);
        dom.addProperty("timestamp", timestamp);

        var add = new JsonObject();
        for (var entry : this.add.entrySet()) {
            add.addProperty(entry.getKey(), entry.getValue().toString());
        }
        dom.add("add", add);

        var remove = new JsonArray();
        for (var entry : this.remove) {
            remove.add(entry);
        }
        dom.add("remove", remove);
        return dom.toString();
    }
}
