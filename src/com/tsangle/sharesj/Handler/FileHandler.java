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
import java.nio.file.Files;
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

    protected void GetFileSystemEntries(RequestSocket requestSocket) throws Exception{
        String logicPath=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        if (logicPath.isEmpty()){
            StringBuilder sharedDiskInfoStringBuilder=new StringBuilder();
            for (Map.Entry<String, String> entry : ServiceNode.GetInstance().GetSharedDisk().entrySet()){
                File file = new File(entry.getValue());
                long totalSpace = file.getTotalSpace();
                long freeSpace = file.getFreeSpace();
                String sharedDiskInfoString=entry.getKey()+"*"+(totalSpace-freeSpace)+"*"+totalSpace+"|";
                sharedDiskInfoStringBuilder.append(sharedDiskInfoString);
            }
            HandleResponseData(requestSocket,"text/plain", sharedDiskInfoStringBuilder.toString().getBytes(StandardCharsets.UTF_8));
        }else {
            String realPath=ServiceNode.GetInstance().GetRealPath(logicPath,false);
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
                        String fileSize=String.valueOf(item.length());
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
        String filePath = ServiceNode.GetInstance().GetRealPath(strDataArray[0], false);
        String fileName = strDataArray[1];
        long fileSize = Long.parseLong(strDataArray[2]);
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

    private String generateSpeedString(double speed){
        String speedUnit="B/s";
        String[] speedUnitArray={"KB/s","MB/s","GB/s"};
        for(int counter=0;counter<3;counter++){
            if(speed/1024>1){
                speed=speed/1024;
                speedUnit=speedUnitArray[counter];
            }else{
                break;
            }
        }
        return (int)speed+" "+speedUnit;
    }

    private void CheckUploadingProgress(RequestSocket requestSocket) throws Exception{
        String strDataContent=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String[] strDataArray = strDataContent.split("\\|",4);
        int serverCacheID=Integer.parseInt(strDataArray[0]);
        double currentUploadingProgress=Double.parseDouble(strDataArray[1]);
        double currentWritingProgress=Double.parseDouble(strDataArray[2]);
        long lastTimeStamp=Long.parseLong(strDataArray[3]);
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
                long writtenSize=fileEntity.GetWrittenSize();
                double fReadSize=(double)readSize;
                double fWrittenSize=(double)writtenSize;
                double fFileSize=(double)fileSize;
                double uploadingProgress=fReadSize*100/fFileSize;
                double writingProgress=fWrittenSize*100/fFileSize;
                if(uploadingProgress<=currentUploadingProgress&&writingProgress<=currentWritingProgress){
                    Thread.sleep(100);
                }else{
                    long currentTime=new Date().getTime();
                    long currentTimeStamp=currentTime-fileEntity.GetCreatedTime();
                    double timeGap=(double) (currentTimeStamp-lastTimeStamp);
                    double uploadingSpeed;
                    String uploadingSpeedString="0 B/s";
                    double writingSpeed;
                    String writingSpeedString="0 B/s";
                    if(timeGap>1000){
                        double newlyReadSize=(double)fileEntity.GetNewlyReadSize();
                        double newlyWrittenSize=(double)fileEntity.GetNewlyWrittenSize();
                        fileEntity.ResetNewlyReadSize();
                        fileEntity.ResetNewlyWrittenSize();
                        uploadingSpeed=newlyReadSize/timeGap*1000;
                        writingSpeed=newlyWrittenSize/timeGap*1000;
                        uploadingSpeedString=generateSpeedString(uploadingSpeed);
                        writingSpeedString=generateSpeedString(writingSpeed);
                    }else{
                        currentTimeStamp=lastTimeStamp;
                    }
                    String isFinished="0";
                    if(writtenSize==fileSize){
                        isFinished="1";
                        fileEntity.WaitForOutputCompletion();
                        synchronized (syncObject){
                            fileEntityDictionary.remove(serverCacheID);
                        }
                        if(fileEntity.GetErrorMessage()!=null){
                            HandleErrorMessage(requestSocket,fileEntity.GetErrorMessage());
                            break;
                        }
                    }
                    HandleResponseMessage(requestSocket,"text/plain",uploadingProgress+"|"+writingProgress+"|"+currentTimeStamp+"|"+uploadingSpeedString+"|"+writingSpeedString+"|"+isFinished);
                    break;
                }
            }
        }else{
            HandleErrorMessage(requestSocket, "Can't find the specified cache ID: [" + serverCacheID + "]");
        }
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
        String path = ServiceNode.GetInstance().GetRealPath(logicPath,true);
        File targetFile=new File(path);
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

    private void DeleteFile(RequestSocket requestSocket) throws Exception{
        String logicPath = new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String path = ServiceNode.GetInstance().GetRealPath(logicPath,true);
        File targetFile=new File(path);
        boolean result=targetFile.delete();
        if(result)
            HandleResponseMessage(requestSocket, "text/plain","Success!");
        else
            HandleErrorMessage(requestSocket,"Fail to delete the file: [" + logicPath + "]");
    }

    private boolean deleteDir(File file) throws Exception{
        File[] contents = file.listFiles();
        if (contents != null) {
            for (File f : contents) {
                if (! Files.isSymbolicLink(f.toPath())) {
                    deleteDir(f);
                }else{
                    Files.delete(f.toPath());
                }
            }
        }
        return file.delete();
    }

    private void DeleteFolder(RequestSocket requestSocket) throws Exception{
        String logicPath = new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String path = ServiceNode.GetInstance().GetRealPath(logicPath,false);
        File targetFile=new File(path);
        boolean result=deleteDir(targetFile);
        if(result)
            HandleResponseMessage(requestSocket, "text/plain","Success!");
        else
            HandleErrorMessage(requestSocket,"Fail to delete the file: [" + logicPath + "]");
    }

    private void CreateFolder(RequestSocket requestSocket) throws Exception{
        String strDataContent=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String[] strDataArray = strDataContent.split("\\|",2);
        String path = ServiceNode.GetInstance().GetRealPath(strDataArray[0],false);
        String folderName = strDataArray[1];
        String fullPath = path + "/" + folderName;
        File targetFolder = new File(fullPath);
        if (!targetFolder.exists()){
            if(!targetFolder.mkdirs()){
                throw new Exception("Fail to create the folder: [" + strDataArray[0] + "/" + folderName + "]");
            }
        }
        HandleResponseMessage(requestSocket, "text/plain","Success!");
    }

    private void RenameEntry(RequestSocket requestSocket) throws Exception{
        String strDataContent=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String[] strDataArray = strDataContent.split("\\|",3);
        String path = ServiceNode.GetInstance().GetRealPath(strDataArray[0],false);
        String original_name = strDataArray[1];
        String new_name = strDataArray[2];
        File original_entry = new File(path+"/"+original_name);
        File new_entry = new File(path+"/"+new_name);
        if (new_entry.exists()){
            throw new Exception(new_name + " already exists!");
        }
        if(!original_entry.renameTo(new_entry)){
            throw new Exception("Fail in renaming " + original_entry);
        }
        HandleResponseMessage(requestSocket, "text/plain","Success!");
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
                            GetFileSystemEntries(requestSocket);
                            break;
                        case "SetFileInfo":
                            SetFileInfo(requestSocket);
                            break;
                        case "CheckUploadingProgress":
                            CheckUploadingProgress(requestSocket);
                            break;
                        case "CancelUploading":
                            CancelStoreFile(requestSocket);
                            break;
                        case "DownloadFile":
                            SendRequestedFile(requestSocket);
                            break;
                        case "DeleteFile":
                            DeleteFile(requestSocket);
                            break;
                        case "DeleteFolder":
                            DeleteFolder(requestSocket);
                            break;
                        case "CreateFolder":
                            CreateFolder(requestSocket);
                            break;
                        case "RenameEntry":
                            RenameEntry(requestSocket);
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
