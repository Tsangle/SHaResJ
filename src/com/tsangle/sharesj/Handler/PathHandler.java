package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;

public class PathHandler extends BaseRequestHandler {
    private static Logger logger=Logger.getLogger(PathHandler.class.getName());

    private static String rootPath;

    public static void SetRootPath(String path){
        rootPath=path;
    }

    public static String GetRootPath(){
        return rootPath;
    }

    public static String GenerateRealPath(String logicPath, boolean isFile){
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

    private void GetFileSystemEntries(RequestSocket requestSocket){
        String logicPath=new String(requestSocket.GetAdditionalData(),StandardCharsets.UTF_8);
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

    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.GetRequestType().equals("GET")){
                HandleErrorMessage(requestSocket,"The request type of [Path] must be POST!");
            }else{
                if(requestSocket.CheckUrlArrayFormat(2)){
                    switch (requestSocket.GetUrlArray()[1]){
                        case "GetFileSystemEntries":
                            GetFileSystemEntries(requestSocket);
                            break;
                        default:
                            HandleErrorMessage(requestSocket,"Cannot find the given task: [" + requestSocket.GetUrlArray()[1] + "]");
                            break;
                    }
                }else{
                    HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [Path]!");
                }
            }
        }catch (Exception e){
            HandleException(requestSocket,e);
        }
    }
}
