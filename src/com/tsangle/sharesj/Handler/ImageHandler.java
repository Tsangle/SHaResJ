package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.HttpServer.RequestSocket;
import com.tsangle.sharesj.HttpServer.ServiceNode;

import java.io.File;
import java.io.FileInputStream;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class ImageHandler extends BaseRequestHandler {
    private static Logger logger=Logger.getLogger(ImageHandler.class.getName());
    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.CheckUrlArrayFormat(3)){
                switch (requestSocket.GetUrlArray()[1]){
                    case "DisplayImage":
                        String logicPath = URLDecoder.decode(requestSocket.GetUrlArray()[2], StandardCharsets.UTF_8);
                        String imagePath = ServiceNode.GetInstance().GetRealPath(logicPath,true);
                        File imageFile=new File(imagePath);
                        FileInputStream inputStream=new FileInputStream(imageFile);
                        byte[] imageData=inputStream.readAllBytes();
                        inputStream.close();
                        HandleResponseData(requestSocket,"image/*",imageData);
                        break;
                    default:
                        HandleErrorMessage(requestSocket,"Cannot find the given task: [" + requestSocket.GetUrlArray()[1] + "]");
                        break;
                }
            }else{
                HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [Image]!");
            }
        }catch (Exception e){
            HandleException(requestSocket,logger,e);
        }
    }
}
