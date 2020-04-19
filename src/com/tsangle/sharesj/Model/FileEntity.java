package com.tsangle.sharesj.Model;

import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.util.Date;
import java.util.logging.Logger;

public class FileEntity {
    private static Logger logger=Logger.getLogger(FileEntity.class.getName());

    private final String filePath;

    private final long fileSize;

    private final Object syncChunk;

    private long writtenSize;

    private boolean isCanceled;

    private String errorMessage;

    private Thread outputThread;

    private FileChunk headChunk;

    private FileChunk currentChunk;

    private long createdTime;

    private long readSize;

    private long newlyReadSize;

    public FileEntity(String filePath, long fileSize){
        this.filePath=filePath;
        this.fileSize=fileSize;
        writtenSize=0;
        isCanceled=false;
        errorMessage=null;
        syncChunk=new Object();
        headChunk=new FileChunk();
        currentChunk=headChunk;
        outputThread=new Thread(new Runnable() {
            @Override
            public void run() {
                OutputChunk();
            }
        });
        outputThread.start();
        createdTime=new Date().getTime();
        newlyReadSize =0;
        readSize=0;
    }

    public void AddNewChunk(FileChunk newChunk){
        synchronized (syncChunk)
        {
            headChunk.SetNextChunk(newChunk);
            headChunk=newChunk;
            readSize +=newChunk.GetChunkData().length;
            newlyReadSize +=newChunk.GetChunkData().length;
        }
    }

    public void CancelOutput(){
        isCanceled=true;
    }

    public void WaitForOutputCompletion() throws Exception{
        outputThread.join();
    }

    public String GetErrorMessage(){
        return errorMessage;
    }

    public long GetFileSize(){
        return fileSize;
    }

    public long GetReadSize(){
        return readSize;
    }

    public long GetNewlyReadSize(){
        return newlyReadSize;
    }

    public void ResetNewlyReadSize(){
        synchronized (syncChunk){
            newlyReadSize =0;
        }
    }

    public long GetCreatedTime(){
        return createdTime;
    }

    public boolean IsCanceled(){
        return isCanceled;
    }

    public void SetErrorMessage(String message){
        errorMessage=message;
    }

    private void OutputChunk(){
        try{
            RandomAccessFile randomAccessFile=new RandomAccessFile(filePath,"rws");
            for(;writtenSize<fileSize;){
                if(currentChunk.GetNextChunk()==null){
                    Thread.sleep(100);
                }else{
                    randomAccessFile.seek(currentChunk.GetNextChunk().GetPosition());
                    randomAccessFile.write(currentChunk.GetNextChunk().GetChunkData());
                    currentChunk=currentChunk.GetNextChunk();
                    writtenSize+=currentChunk.GetChunkData().length;
                    System.gc();
                }
                if(isCanceled)
                    break;
                System.gc();
            }
            randomAccessFile.close();
        }catch (Exception e){
            StringWriter writer=new StringWriter();
            e.printStackTrace(new PrintWriter(writer));
            logger.warning(writer.toString());
            errorMessage=e.getMessage();
        }
    }
}
