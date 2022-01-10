package com.zevrant.services.zevrantandroidapp.pojo;

import com.zevrant.services.zevrantuniversalcommon.rest.backup.response.BackupFile;

public class BackupFilePair {

    private BackupFile left;
    private BackupFile right;

    public BackupFilePair(BackupFile left, BackupFile right) {
        this.left = left;
        this.right = right;
    }

    public BackupFile getLeft() {
        return left;
    }

    public void setLeft(BackupFile left) {
        this.left = left;
    }

    public BackupFile getRight() {
        return right;
    }

    public void setRight(BackupFile right) {
        this.right = right;
    }
}
