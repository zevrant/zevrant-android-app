package com.zevrant.services.zevrantandroidapp.pojo;

public class FileInfo {

    private String fileName;
    private String hash;
    //internal android id do not use unless communicating with android api and only good while file exists on device
    private long id;
    private long size;

    public FileInfo() {
    }

    public FileInfo(String fileName, String hash, long id, long size) {
        this.fileName = fileName;
        this.hash = hash;
        this.id = id;
        this.size = size;
    }

    public String getFileName() {
        return fileName;
    }

    public String getHash() {
        return hash;
    }

    public long getId() {
        return id;
    }

    public long getSize() {
        return size;
    }

}
