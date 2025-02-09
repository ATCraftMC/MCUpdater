package org.atcraftmc.updater.command;

import com.google.gson.JsonObject;
import org.atcraftmc.updater.command.operation.DeleteOperation;
import org.atcraftmc.updater.command.operation.Operation;
import org.atcraftmc.updater.command.operation.PatchOperation;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class VersionInfo {
    private final String version;
    private final Map<Class<? extends Operation>, Operation> operations;
    private final long timeStamp;

    public VersionInfo(String comments, long timeStamp) {
        this.version = comments;
        this.timeStamp = timeStamp;
        this.operations = new HashMap<>();
    }

    public VersionInfo(JsonObject dom) {
        this.version = dom.get("version").getAsString();
        this.timeStamp = dom.get("time").getAsLong();

        this.operations = new HashMap<>();
        if(dom.has("patch")){
            this.operations.put(PatchOperation.class, new PatchOperation(dom.get("patch")));
        }
        if(dom.has("delete")){
            this.operations.put(DeleteOperation.class, new DeleteOperation(dom.get("delete")));
        }
    }

    public Collection<Operation> getOperations() {
        return operations.values();
    }

    public String getVersion() {
        return version;
    }

    public void addOperation(Operation operation) {
        operations.put(operation.getClass(), operation);
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    @Override
    public String toString() {
        return json().toString();
    }

    public JsonObject json() {
        var dom = new JsonObject();

        dom.addProperty("version", this.version);
        dom.addProperty("time", this.timeStamp);

        this.operations.forEach((t, o) -> {
            if (t == PatchOperation.class) {
                dom.add("patch", o.json());
            }
            if(t == DeleteOperation.class) {
                dom.add("delete", o.json());
            }
        });

        return dom;
    }
}
