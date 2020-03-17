package com.tsangle.sharesj.Model;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class FileChunk {
    private static Logger logger=Logger.getLogger(FileChunk.class.getName());

    private byte[] chunkData;

    private FileChunk nextChunk;

    private CountDownLatch latch=new CountDownLatch(1);

    public FileChunk(){
        chunkData=null;
        nextChunk=null;
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

    void WaitForOutput(){
        try{
            latch.await();
        }catch (Exception exception){
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }

    void EnableOutput(){
        latch.countDown();
    }
}
