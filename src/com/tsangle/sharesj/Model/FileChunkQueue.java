package com.tsangle.sharesj.Model;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.Logger;

class FileChunkQueue {
    private static Logger logger=Logger.getLogger(FileChunkQueue.class.getName());

    private FileChunk headChunk;

    private FileChunk currentChunk;

    private boolean isCompleted;

    FileChunkQueue(){
        headChunk=new FileChunk();
        currentChunk=headChunk;
        isCompleted=false;
    }

    void AddChunk(FileChunk chunk){
        headChunk.SetNextChunk(chunk);
        headChunk.EnableOutput();
        headChunk=chunk;
    }

    void CompleteAddingChunk(){
        if(!isCompleted){
            headChunk.EnableOutput();
            isCompleted=true;
        }
    }

    void OutputCurrentChunk(FileOutputStream outputStream){
        try{
            currentChunk.WaitForOutput();
            if(currentChunk.GetChunkData()==null){
                currentChunk=currentChunk.GetNextChunk();
                currentChunk.WaitForOutput();
            }
            outputStream.write(currentChunk.GetChunkData());
            outputStream.flush();
            currentChunk=currentChunk.GetNextChunk();
            System.gc();
        }catch (Exception e){
            StringWriter writer=new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            logger.warning(writer.toString());
        }
    }
}
