package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

import java.io.InputStream;
import java.util.logging.Logger;

public class ResourceHandler extends BaseRequestHandler {
    private static Logger logger=Logger.getLogger(ResourceHandler.class.getName());
    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(!requestSocket.CheckUrlListFormat(3)){
                HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [Resource]!");
            }else{
                String contentType;
                switch (requestSocket.GetUrlList().get(1)){
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
                        HandleErrorMessage(requestSocket,"Cannot find the given resource type: [" + requestSocket.GetUrlList().get(1) + "]");
                        return;
                }
                String path="/com/tsangle/sharesj/"+requestSocket.GetUrlList().get(1)+"/"+requestSocket.GetUrlList().get(2);
                InputStream inputStream=this.getClass().getResourceAsStream(path);
                byte[] resourceData=inputStream.readAllBytes();
                HandleResponseData(requestSocket,contentType,resourceData);
            }
        }catch (Exception e){
            HandleException(requestSocket,e);
        }
    }
}
