package com.zevrant.services.zevrantandroidapp.volley.requests;

import com.zevrant.services.zevrantandroidapp.pojo.FileInfo;

public class BackupFileRequest {

    private FileInfo fileInfo;
    private String serializedFileData;

    public BackupFileRequest() {
    }

    public BackupFileRequest(FileInfo fileInfo, String serializedFileData) {
        this.fileInfo = fileInfo;
        this.serializedFileData = serializedFileData;
    }

    public FileInfo getFileInfo() {
        return fileInfo;
    }

    public void setFileInfo(FileInfo fileInfo) {
        this.fileInfo = fileInfo;
    }

    public String getSerializedFileData() {
        return serializedFileData;
    }

    public void setSerializedFileData(String serializedFileData) {
        this.serializedFileData = serializedFileData;
    }
}
