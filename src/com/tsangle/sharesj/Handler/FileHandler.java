package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.FileChunk;
import com.tsangle.sharesj.Model.FileContainer;
import com.tsangle.sharesj.Model.RequestSocket;

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

    private final Dictionary<Integer, FileContainer> fileContainerDictionary;

    private final Object syncObject;

    private static String rootPath;

    public static void SetRootPath(String path){
        rootPath=path;
    }

    public FileHandler(){
        fileContainerDictionary=new Hashtable<>();
        syncObject=new Object();
    }

    static String GenerateRealPath(String logicPath, boolean isFile){
        if(logicPath==null||logicPath.equals("")){
            return rootPath;
        }else{
            String path=rootPath + "/" + logicPath;
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

    private void ReturnFileSystemEntries(RequestSocket requestSocket){
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

    private void SetFileInfo(RequestSocket requestSocket){
        String strDataContent=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        String[] strDataArray = strDataContent.split("\\|",4);
        String path = rootPath + strDataArray[0];
        File dir = new File(path);
        if(dir.exists())
        {
            FileContainer container=new FileContainer(path+"/"+strDataArray[1],Integer.valueOf(strDataArray[2]),Integer.valueOf(strDataArray[3]));
            Random random=new Random();
            int key=random.nextInt(10000);
            synchronized (syncObject){
                while (fileContainerDictionary.get(key)!=null)
                    key=(key+1)%10000;
                fileContainerDictionary.put(key,container);
            }
            HandleResponseMessage(requestSocket, "text/plain",String.valueOf(key));
        }
        else
        {
            HandleErrorMessage(requestSocket, "Cannot find the given path: [" + path + "]");
        }
    }

    private void StoreFileChunk(RequestSocket requestSocket){
        String[] chunkInfo=requestSocket.GetUrlArray()[2].split("&");
        int serverCacheID = Integer.valueOf(chunkInfo[0]);
        int chunkOrder = Integer.valueOf(chunkInfo[1]);
        FileContainer container;
        synchronized (syncObject){
            container=fileContainerDictionary.get(serverCacheID);
        }
        if(container==null){
            HandleErrorMessage(requestSocket, "Can't find the specified cache ID: [" + serverCacheID + "]");
        }else{
            FileChunk fileChunk = new FileChunk();
            fileChunk.SetChunkData(requestSocket.GetAdditionalData());
            if(container.AddNewChunk(fileChunk, chunkOrder))
            {
                synchronized (syncObject){
                    fileContainerDictionary.remove(serverCacheID);
                }
            }
            HandleResponseMessage(requestSocket, "text/plain","Success!");
        }
    }

    private void CancelStoreFile(RequestSocket requestSocket){
        String strServerCacheID=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
        int serverCacheID=Integer.valueOf(strServerCacheID);
        FileContainer container;
        synchronized (syncObject){
            container=fileContainerDictionary.get(serverCacheID);
        }
        container.CancelOutput();
        synchronized (syncObject){
            fileContainerDictionary.remove(serverCacheID);
        }
        HandleResponseMessage(requestSocket, "text/plain","Success!");
    }

    private void SendRequestedFile(RequestSocket requestSocket){
        try{
            String strDataContent=new String(requestSocket.GetAdditionalData(), StandardCharsets.UTF_8);
            String logicPath= URLDecoder.decode(strDataContent,StandardCharsets.UTF_8).split("=",2)[1];
            String path = GenerateRealPath(logicPath,true);
            File targetFile=new File(path);
            if (targetFile.exists())
            {
                FileInputStream inputStream=new FileInputStream(path);
                int chunkLength=50000000;
                String responseHeader="HTTP/1.1 200 OK"+System.lineSeparator()+
                        "Content-Length:"+targetFile.length()+System.lineSeparator()+
                        "Content-Disposition:attachment; filename="+targetFile.getName()+System.lineSeparator()+
                        System.lineSeparator();
                OutputStream outputStream=requestSocket.GetOutputStream();
                outputStream.write(responseHeader.getBytes());
                for(byte[] chunkData=inputStream.readNBytes(chunkLength);chunkData.length>0;chunkData=inputStream.readNBytes(chunkLength)){
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
        }catch (Exception exception){
            HandleException(requestSocket,exception);
        }
    }

    private void DeleteFile(RequestSocket requestSocket){
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
            HandleException(requestSocket,e);
        }
    }
}
