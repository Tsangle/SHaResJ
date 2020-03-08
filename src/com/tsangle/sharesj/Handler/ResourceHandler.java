package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

import java.io.InputStream;
import java.util.logging.Logger;

public class ResourceHandler extends BaseRequestHandler {
    private static Logger logger=Logger.getLogger(ResourceHandler.class.getName());
    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.CheckUrlArrayFormat(3)){
                String contentType;
                switch (requestSocket.GetUrlArray()[1]){
                    case "Style":
                        contentType="text/css";
                        break;
                    case "Font":
                    case "Img":
                    case "Script":
                    case "Page":
                        contentType="text/html";
                        break;
                    default:
                        HandleErrorMessage(requestSocket,"Cannot find the given resource type: [" + requestSocket.GetUrlArray()[1] + "]");
                        return;
                }
                String path="/com/tsangle/sharesj/"+requestSocket.GetUrlArray()[1]+"/"+requestSocket.GetUrlArray()[2];
                InputStream inputStream=this.getClass().getResourceAsStream(path);
                if(inputStream==null){
                    HandleErrorMessage(requestSocket,"Cannot find the given resource: ["+requestSocket.GetUrlArray()[2]+"]");
                }else{
                    byte[] resourceData=inputStream.readAllBytes();
                    HandleResponseData(requestSocket,contentType,resourceData);
                }
            }else{
                HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [Resource]!");
            }
        }catch (Exception e){
            HandleException(requestSocket,e);
        }
    }
}
