package org.atcraftmc.updater.command.operation;

import com.google.gson.JsonElement;
import org.atcraftmc.updater.FilePath;
import org.atcraftmc.updater.command.UpdateOperationListener;

import java.util.function.Consumer;

public abstract class Operation implements Consumer<UpdateOperationListener> {
    private final String base;

    public Operation() {
        this.base = FilePath.runtime();
    }

    public Operation(String base) {
        this.base = base;
    }

    static Operation create(String type, JsonElement dom) {
        return switch (type) {
            case "extract" -> new PatchOperation(dom);
            case "delete" -> new DeleteOperation(dom);
            default -> throw new IllegalStateException("Unexpected value: " + type);
        };
    }

    public String getBase() {
        return base;
    }

    public abstract JsonElement json();
}
