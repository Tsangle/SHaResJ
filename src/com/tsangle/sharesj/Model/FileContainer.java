package com.tsangle.sharesj.Model;

import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

public class FileContainer {
    private static Logger logger=Logger.getLogger(FileContainer.class.getName());

    private final List<FileChunkQueue> fileChunkQueueList;

    private final String filePath;

    private final int totalChunkCount;

    private final Object syncChunkCount;

    private final Object syncCanceledFlag;

    private final CountDownLatch latch;

    private int addedChunkCount;

    private boolean isCanceled;

    private Thread outputThread;

    public FileContainer(String filePath, int threadCount, int totalChunkCount){
        this.filePath=filePath;
        fileChunkQueueList=new ArrayList<>();
        for(int index=0;index<threadCount;index++){
            fileChunkQueueList.add(new FileChunkQueue());
        }
        this.totalChunkCount=totalChunkCount;
        addedChunkCount=0;
        isCanceled=false;
        syncChunkCount=new Object();
        syncCanceledFlag=new Object();
        latch=new CountDownLatch(1);
        outputThread=new Thread(this::OutputChunk);
        outputThread.start();
    }

    public boolean AddNewChunk(FileChunk newChunk, int order){
        try{
            int index = order % fileChunkQueueList.size();
            fileChunkQueueList.get(index).AddChunk(newChunk);
            synchronized (syncChunkCount)
            {
                addedChunkCount++;
                if (addedChunkCount == totalChunkCount)
                {
                    for (var fileChunkQueue : fileChunkQueueList){
                        fileChunkQueue.CompleteAddingChunk();
                    }
                    latch.await();
                    return true;
                }
            }
            return false;
        }catch (Exception exception){
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
            return false;
        }
    }

    public void CancelOutput(){
        try{
            synchronized (syncCanceledFlag){
                isCanceled=true;
            }
            outputThread.join();
        }catch (Exception exception){
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }

    private void OutputChunk(){
        try{
            FileOutputStream outputStream=new FileOutputStream(filePath);
            for(int counter=0;counter<totalChunkCount;counter++){
                int index=counter%fileChunkQueueList.size();
                fileChunkQueueList.get(index).OutputCurrentChunk(outputStream);
                synchronized (syncCanceledFlag){
                    if(isCanceled)
                        break;
                }
            }
            outputStream.close();
            latch.countDown();
        }catch (Exception exception){
            StringWriter stringWriter = new StringWriter();
            exception.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }

    }
}
