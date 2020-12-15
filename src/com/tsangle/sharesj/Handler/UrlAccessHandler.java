package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.HttpServer.RequestSocket;
import com.tsangle.sharesj.HttpServer.ServiceNode;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.util.logging.Logger;

public class UrlAccessHandler extends BaseRequestHandler {
    private static Logger logger = Logger.getLogger(UrlAccessHandler.class.getName());

    private static final int chunkSize=40000000;
    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.CheckUrlArrayFormat(2)){
                String logicPath = URLDecoder.decode(requestSocket.GetUrlArray()[1],"UTF-8");
                String realPath = ServiceNode.GetInstance().GetRealPath(logicPath,true);
                File requestedFile=new File(realPath);
                String rangeString=requestSocket.GetRange();
                FileInputStream inputStream=new FileInputStream(requestedFile);
                if(rangeString!=null&&!rangeString.isEmpty()){
                    String[] rangeIndexArray=rangeString.split("=")[1].split("-");
                    long startIndex=Long.valueOf(rangeIndexArray[0]);
                    long endIndex=startIndex+chunkSize-1;
                    if(rangeIndexArray.length>1&&rangeIndexArray[1]!=null&&!rangeIndexArray[1].isEmpty()){
                        long endIndexInRangeArray=Long.valueOf(rangeIndexArray[1]);
                        if(endIndexInRangeArray>startIndex){
                            endIndex=endIndexInRangeArray;
                        }
                    }
                    if(endIndex>=requestedFile.length()){
                        endIndex=requestedFile.length()-1;
                    }
                    requestSocket.AddAdditionalResponseHeader("Content-Range: bytes "+startIndex+"-"+endIndex+"/"+requestedFile.length());
                    long skippedLength=inputStream.skip(startIndex);
                    if(skippedLength==startIndex){
                        long currentChunkLength=endIndex-startIndex+1;
                        byte[] videoChunkData=new byte[(int)currentChunkLength];
                        if(inputStream.read(videoChunkData,0,(int)currentChunkLength)==-1){
                            HandleErrorMessage(requestSocket,"Fail in reading chunks from ["+logicPath+"].");
                        }
                        requestSocket.SetStatusCode("206");
                        HandleResponseData(requestSocket,"application/octet-stream",videoChunkData);
                    }else{
                        HandleErrorMessage(requestSocket,"Fail in skipping the range: 0 ~ " + startIndex + "");
                    }
                }else{
                    String responseHeader="HTTP/1.1 200 OK"+System.lineSeparator()+
                            "Content-Type:application/octet-stream;charset=utf-8"+System.lineSeparator()+
                            "Content-Length:"+requestedFile.length()+System.lineSeparator()+
                            "Content-Disposition:attachment; filename="+requestedFile.getName()+System.lineSeparator()+
                            System.lineSeparator();
                    OutputStream outputStream=requestSocket.GetOutputStream();
                    outputStream.write(responseHeader.getBytes());
                    for(byte[] chunkData=new byte[chunkSize];;){
                        if(inputStream.read(chunkData,0,chunkSize)==-1){
                            logger.info("File data reading finished.");
                            break;
                        }
                        outputStream.write(chunkData);
                        outputStream.flush();
                    }
                    outputStream.close();
                    requestSocket.Close();
                }
                inputStream.close();
            }else{
                HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [UrlAccess]!");
            }
        }catch (Exception e){
            HandleException(requestSocket,logger,e);
        }
    }
}
