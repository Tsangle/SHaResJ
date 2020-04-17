package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class VideoHandler extends BaseRequestHandler{
    private static Logger logger=Logger.getLogger(VideoHandler.class.getName());

    private static final int chunkSize=40000000;
    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.CheckUrlArrayFormat(3)){
                switch (requestSocket.GetUrlArray()[1]){
                    case "PlayVideo":
                        String videoPath = FileHandler.GenerateRealPath(URLDecoder.decode(requestSocket.GetUrlArray()[2],"UTF-8"),true);
                        File videoFile=new File(videoPath);
                        if (videoFile.exists())
                        {
                            String rangeString=requestSocket.GetRange();
                            FileInputStream inputStream=new FileInputStream(videoFile);
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
                                if(endIndex>=videoFile.length()){
                                    endIndex=videoFile.length()-1;
                                }
                                requestSocket.AddAdditionalResponseHeader("Content-Range: bytes "+startIndex+"-"+endIndex+"/"+videoFile.length());
                                long skippedLength=inputStream.skip(startIndex);
                                if(skippedLength==startIndex){
                                    long currentChunkLength=endIndex-startIndex+1;
                                    byte[] videoChunkData=new byte[(int)currentChunkLength];
                                    if(inputStream.read(videoChunkData,0,(int)currentChunkLength)==-1){
                                        HandleErrorMessage(requestSocket,"Fail to read chunk from video file.");
                                    }
                                    requestSocket.SetStatusCode("206");
                                    HandleResponseData(requestSocket,"video/mp4",videoChunkData);
                                }else{
                                    HandleErrorMessage(requestSocket,"Fail to handle the start index: [" + startIndex + "]");
                                }
                            }else{
                                String responseHeader="HTTP/1.1 200 OK"+System.lineSeparator()+
                                        "Content-Type:video/mp4;charset=utf-8"+System.lineSeparator()+
                                        "Content-Length:"+videoFile.length()+System.lineSeparator()+
                                        System.lineSeparator();
                                OutputStream outputStream=requestSocket.GetOutputStream();
                                outputStream.write(responseHeader.getBytes());
                                for(byte[] chunkData=new byte[chunkSize];;){
                                    if(inputStream.read(chunkData,0,chunkSize)==-1){
                                        logger.info("Video data reading finished.");
                                        break;
                                    }
                                    outputStream.write(chunkData);
                                    outputStream.flush();
                                }
                                outputStream.close();
                                requestSocket.Close();
                            }
                            inputStream.close();
                        }
                        else
                        {
                            HandleErrorMessage(requestSocket, "Cannot find the given file: [" + videoPath + "]");
                        }
                        break;
                    default:
                        HandleErrorMessage(requestSocket,"Cannot find the given task: [" + requestSocket.GetUrlArray()[1] + "]");
                        break;
                }
            }else{
                HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [Video]!");
            }
        }catch (Exception e){
            HandleException(requestSocket,logger,e);
        }
    }
}
