package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

public class VideoHandler extends BaseRequestHandler{
    private static final int chunkSize=40000000;
    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.CheckUrlArrayFormat(3)){
                switch (requestSocket.GetUrlArray()[1]){
                    case "PlayVideo":
                        String videoPath = FileHandler.GenerateRealPath(URLDecoder.decode(requestSocket.GetUrlArray()[2], StandardCharsets.UTF_8),true);
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
                                    byte[] videoChunkData=inputStream.readNBytes((int)currentChunkLength);
                                    requestSocket.SetStatusCode("206");
                                    HandleResponseData(requestSocket,"video/mp4",videoChunkData);
                                }else{
                                    HandleErrorMessage(requestSocket,"Fail to handle the start index: [" + startIndex + "]");
                                }
                            }else{
                                byte[] videoData=inputStream.readAllBytes();
                                HandleResponseData(requestSocket,"video/mp4",videoData);
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
            HandleException(requestSocket,e);
        }
    }
}
