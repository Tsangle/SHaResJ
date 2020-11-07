package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.HttpServer.RequestSocket;
import com.tsangle.sharesj.HttpServer.ServiceNode;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class ResourceHandler extends BaseRequestHandler {
    private static Logger logger=Logger.getLogger(ResourceHandler.class.getName());
    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.CheckUrlArrayFormat(3)){
                String path="/com/tsangle/sharesj/"+requestSocket.GetUrlArray()[1]+"/"+requestSocket.GetUrlArray()[2];
                InputStream inputStream=this.getClass().getResourceAsStream(path);
                if(inputStream==null){
                    HandleErrorMessage(requestSocket,"Cannot find the given resource: ["+requestSocket.GetUrlArray()[2]+"]");
                }else{
                    ByteArrayOutputStream outputStream=new ByteArrayOutputStream();
                    byte[] buffer=new byte[1000];
                    for(int result=inputStream.read(buffer,0,buffer.length);result!=-1;result=inputStream.read(buffer,0,buffer.length)){
                        outputStream.write(buffer,0,result);
                    }
                    byte[] responseData;
                    String contentType;
                    switch (requestSocket.GetUrlArray()[1]){
                        case "Style":
                            contentType="text/css";
                            responseData = outputStream.toByteArray();
                            break;
                        case "Font":
                            contentType="application/*";
                            responseData = outputStream.toByteArray();
                            break;
                        case "Icon":
                            contentType="image/x-icon";
                            responseData = outputStream.toByteArray();
                            break;
                        case "Script":
                            contentType="application/javascript";
                            responseData = outputStream.toByteArray();
                            break;
                        case "Page":
                            contentType="text/html";
                            String contentString = outputStream.toString().replace("{NodeName}", ServiceNode.GetInstance().GetNodeName());
                            responseData = contentString.getBytes();
                            break;
                        default:
                            HandleErrorMessage(requestSocket,"Cannot find the given resource type: [" + requestSocket.GetUrlArray()[1] + "]");
                            return;
                    }
                    HandleResponseData(requestSocket,contentType,responseData);
                }
                System.gc();
            }else{
                HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [Resource]!");
            }
        }catch (Exception e){
            HandleException(requestSocket,logger,e);
        }
    }
}
