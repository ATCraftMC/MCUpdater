package org.atcraftmc.updater.command;

public interface UpdateOperationListener {
    void setProgress(int prog);

    void setCommentMessage(String msg);
}
