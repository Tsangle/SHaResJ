package com.tsangle.sharesj.Model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class FileChunk {
    private byte[] chunkData;

    private FileChunk nextChunk;

    private long position;

    public FileChunk(){
        chunkData=null;
        nextChunk=null;
        position=0;
    }

    byte[] GetChunkData(){
        return this.chunkData;
    }

    public void SetChunkData(byte[] data){
        this.chunkData=data;
    }

    FileChunk GetNextChunk(){
        return this.nextChunk;
    }

    void SetNextChunk(FileChunk chunk){
        this.nextChunk=chunk;
    }

    long GetPosition(){
        return this.position;
    }

    public void SetPosition(long position){
        this.position=position;
    }
}
