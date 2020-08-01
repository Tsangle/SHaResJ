package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.FileChunk;
import com.tsangle.sharesj.Model.FileEntity;
import com.tsangle.sharesj.HttpServer.ServiceNode;
import com.tsangle.sharesj.HttpServer.RequestSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

public class FileHandler extends BaseRequestHandler {
    private static Logger logger=Logger.getLogger(FileHandler.class.getName());

    private final Dictionary<Integer, FileEntity> fileEntityDictionary;

    private final Object syncObject;

    private final long chunkLength;

    public FileHandler(){
        fileEntityDictionary =new Hashtable<>();
        syncObject=new Object();
        chunkLength=5000000;
    }

    static String GenerateRealPath(String logicPath, boolean isFile){
        if(logicPath==null||logicPath.equals("")){
            return ServiceNode.GetInstance().GetRootPath();
        }else{
            String path= ServiceNode.GetInstance().GetRootPath() + "/" + logicPath;
            File file = new File(path);
            if (file.exists()&&file.isFile()==isFile)
            {
                return path;
            }
            else
            {
                return "";
            }
        }
    }

    protected void ReturnFileSystemEntries(RequestSocket requestSocket) throws Exception{
        String logicPath=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String realPath=GenerateRealPath(logicPath,false);
        if(realPath.equals("")){
            HandleErrorMessage(requestSocket,"The path does not exist: [" + logicPath + "]");
        }else{
            StringBuilder directoryInfoStringBuilder=new StringBuilder();
            StringBuilder fileInfoStringBuilder=new StringBuilder();
            File dir=new File(realPath);
            File[] fileSystemEntityArray=dir.listFiles();
            if(fileSystemEntityArray!=null){
                for(File item : fileSystemEntityArray){
                    String entityName=item.getName();
                    Date date=new Date(item.lastModified());
                    SimpleDateFormat format=new SimpleDateFormat("yyyy/MM/dd HH:mm");
                    String lastModifiedTime=format.format(date);
                    if(item.isDirectory()){
                        String directoryInfoString=entityName+"*"+lastModifiedTime+"*|";
                        directoryInfoStringBuilder.append(directoryInfoString);
                    }else{
                        String fileSize=String.valueOf(item.length()/1024);
                        String fileInfoString=entityName+"*"+lastModifiedTime+"*"+fileSize+"|";
                        fileInfoStringBuilder.append(fileInfoString);
                    }
                }
            }
            String fileSystemEntityInfoString=directoryInfoStringBuilder.toString()+fileInfoStringBuilder.toString();
            HandleResponseData(requestSocket,"text/plain", fileSystemEntityInfoString.getBytes(StandardCharsets.UTF_8));
        }
    }

    private void SetFileInfo(RequestSocket requestSocket) throws Exception{
        String strDataContent=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String[] strDataArray = strDataContent.split("\\|",4);
        String filePath = ServiceNode.GetInstance().GetRootPath() + strDataArray[0];
        String fileName = strDataArray[1];
        long fileSize = Long.parseLong(strDataArray[2]);
        File dir = new File(filePath);
        if(dir.exists())
        {
            FileEntity container=new FileEntity(filePath+"/"+fileName,fileSize);
            Random random=new Random();
            int key=random.nextInt(10000);
            synchronized (syncObject){
                while (fileEntityDictionary.get(key)!=null)
                    key=(key+1)%10000;
                fileEntityDictionary.put(key,container);
            }
            HandleResponseMessage(requestSocket, "text/plain",String.valueOf(key));
        }
        else
        {
            HandleErrorMessage(requestSocket, "Cannot find the given path: [" + filePath + "]");
        }
    }

    private void StoreFileChunk(RequestSocket requestSocket){
        String[] chunkInfo=requestSocket.GetUrlArray()[2].split("&");
        int serverCacheID = Integer.parseInt(chunkInfo[0]);
        long startIndex = Long.parseLong(chunkInfo[1]);
        FileEntity fileEntity;
        fileEntity= fileEntityDictionary.get(serverCacheID);
        if(fileEntity==null){
            HandleErrorMessage(requestSocket, "Can't find the specified cache ID: [" + serverCacheID + "]");
        }else{
            try{
                for(long remainByteCount=requestSocket.GetAdditionDataLength();remainByteCount>0;){
                    if(fileEntity.IsCanceled()){
                        break;
                    }
                    FileChunk fileChunk = new FileChunk();
                    byte[] buffer;
                    if(remainByteCount>chunkLength){
                        buffer=requestSocket.ReadAdditionalDataChunk((int)chunkLength);
                    }else{
                        buffer=requestSocket.ReadAdditionalDataChunk((int)remainByteCount);
                    }
                    fileChunk.SetChunkData(buffer);
                    fileChunk.SetPosition(startIndex+requestSocket.GetAdditionDataLength()-remainByteCount);
                    fileEntity.AddNewChunk(fileChunk);
                    remainByteCount-=buffer.length;
                }
            }catch (Exception e){
                fileEntity.CancelOutput();
                fileEntity.SetErrorMessage(e.getMessage());
            }
            HandleResponseMessage(requestSocket, "text/plain","Success!");
        }
    }

    private void CheckUploadProgress(RequestSocket requestSocket) throws Exception{
        String strDataContent=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String[] strDataArray = strDataContent.split("\\|",3);
        int serverCacheID=Integer.parseInt(strDataArray[0]);
        double currentUploadProgress=Double.parseDouble(strDataArray[1]);
        long lastTimeStamp=Long.parseLong(strDataArray[2]);
        FileEntity fileEntity=fileEntityDictionary.get(serverCacheID);
        if(fileEntity!=null){
            for(long fileSize=fileEntity.GetFileSize();;){
                if(fileEntity.GetErrorMessage()!=null){
                    synchronized (syncObject){
                        fileEntityDictionary.remove(serverCacheID);
                    }
                    fileEntity.WaitForOutputCompletion();
                    HandleErrorMessage(requestSocket,fileEntity.GetErrorMessage());
                    break;
                }
                long readSize=fileEntity.GetReadSize();
                double fReadSize=(double)readSize;
                double fFileSize=(double)fileSize;
                double progress=fReadSize*100/fFileSize;
                if(progress<=currentUploadProgress){
                    Thread.sleep(100);
                }else{
                    long currentTime=new Date().getTime();
                    long currentTimeStamp=currentTime-fileEntity.GetCreatedTime();
                    double timeGap=(double) (currentTimeStamp-lastTimeStamp);
                    double speed=0;
                    String speedUnit="B/s";
                    if(timeGap>1000){
                        double newlyAddedSize=(double)fileEntity.GetNewlyReadSize();
                        fileEntity.ResetNewlyReadSize();
                        speed=newlyAddedSize/timeGap*1000;
                        String[] speedUnitArray={"KB/s","MB/s","GB/s"};
                        for(int counter=0;counter<3;counter++){
                            if(speed/1024>1){
                                speed=speed/1024;
                                speedUnit=speedUnitArray[counter];
                            }else{
                                break;
                            }
                        }
                    }else{
                        currentTimeStamp=lastTimeStamp;
                    }
                    String isFinished="0";
                    if(readSize==fileSize){
                        isFinished="1";
                    }
                    HandleResponseMessage(requestSocket,"text/plain",progress+"|"+currentTimeStamp+"|"+(int)speed+" "+speedUnit+"|"+isFinished);
                    break;
                }
            }
        }else{
            HandleErrorMessage(requestSocket, "Can't find the specified cache ID: [" + serverCacheID + "]");
        }
    }

    private void WaitForUploadCompletion(RequestSocket requestSocket) throws Exception{
        String strServerCacheID=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        int serverCacheID=Integer.parseInt(strServerCacheID);
        FileEntity fileEntity=fileEntityDictionary.get(serverCacheID);
        if(fileEntity!=null){
            fileEntity.WaitForOutputCompletion();
            synchronized (syncObject){
                fileEntityDictionary.remove(serverCacheID);
            }
        }
        HandleResponseMessage(requestSocket, "text/plain","Success!");
    }

    private void CancelStoreFile(RequestSocket requestSocket) throws Exception{
        String strServerCacheID=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        int serverCacheID=Integer.parseInt(strServerCacheID);
        FileEntity fileEntity;
        fileEntity= fileEntityDictionary.get(serverCacheID);
        if(fileEntity!=null){
            fileEntity.CancelOutput();
            fileEntity.WaitForOutputCompletion();
            synchronized (syncObject){
                fileEntityDictionary.remove(serverCacheID);
            }
        }
        HandleResponseMessage(requestSocket, "text/plain","Success!");
    }

    private void SendRequestedFile(RequestSocket requestSocket) throws Exception{
        String strDataContent=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String logicPath= URLDecoder.decode(strDataContent,StandardCharsets.UTF_8).split("=",2)[1];
        String path = GenerateRealPath(logicPath,true);
        File targetFile=new File(path);
        if (targetFile.exists())
        {
            FileInputStream inputStream=new FileInputStream(path);
            String responseHeader="HTTP/1.1 200 OK"+System.lineSeparator()+
                    "Content-Length:"+targetFile.length()+System.lineSeparator()+
                    "Content-Disposition:attachment; filename="+targetFile.getName()+System.lineSeparator()+
                    System.lineSeparator();
            OutputStream outputStream=requestSocket.GetOutputStream();
            outputStream.write(responseHeader.getBytes());
            for(byte[] chunkData=inputStream.readNBytes((int)chunkLength);chunkData.length>0;chunkData=inputStream.readNBytes((int)chunkLength)){
                outputStream.write(chunkData);
                outputStream.flush();
            }
            outputStream.close();
            requestSocket.Close();
        }
        else
        {
            HandleErrorMessage(requestSocket, "Cannot find the given path: [" + path + "]");
        }
    }

    private void DeleteFile(RequestSocket requestSocket) throws Exception{
        String path = GenerateRealPath(new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8),true);
        File targetFile=new File(path);
        if (targetFile.exists())
        {
            boolean result=targetFile.delete();
            if(result)
                HandleResponseMessage(requestSocket, "text/plain","Success!");
            else
                HandleErrorMessage(requestSocket,"Fail to delete the file: [" + path + "]");
        }
        else
        {
            HandleErrorMessage(requestSocket, "Cannot find the given path: [" + path + "]");
        }
    }

    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.GetRequestType().equals("GET")){
                HandleErrorMessage(requestSocket,"The request type of [File] must be POST!");
            }else{
                if(requestSocket.CheckUrlArrayFormat(2)){
                    switch (requestSocket.GetUrlArray()[1]){
                        case "GetFileSystemEntries":
                            ReturnFileSystemEntries(requestSocket);
                            break;
                        case "SetFileInfo":
                            SetFileInfo(requestSocket);
                            break;
                        case "CheckUploadProgress":
                            CheckUploadProgress(requestSocket);
                            break;
                        case "WaitForUploadCompletion":
                            WaitForUploadCompletion(requestSocket);
                            break;
                        case "CancelUpload":
                            CancelStoreFile(requestSocket);
                            break;
                        case "DownloadFile":
                            SendRequestedFile(requestSocket);
                            break;
                        case "DeleteFile":
                            DeleteFile(requestSocket);
                            break;
                        default:
                            HandleErrorMessage(requestSocket,"Cannot find the given task: [" + requestSocket.GetUrlArray()[1] + "]");
                            break;
                    }
                }else if(requestSocket.CheckUrlArrayFormat(3)){
                    if(requestSocket.GetUrlArray()[1].equals("UploadFileChunk")){
                        StoreFileChunk(requestSocket);
                    }
                }else{
                    HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [File]!");
                }
            }
        }catch (Exception e){
            HandleException(requestSocket,logger,e);
        }
    }
}
