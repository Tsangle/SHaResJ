package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

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
                        String imagePath = FileHandler.GenerateRealPath(URLDecoder.decode(requestSocket.GetCookie().get("resource-location"), StandardCharsets.UTF_8),true);
                        File imageFile=new File(imagePath);
                        if (imageFile.exists())
                        {
                            FileInputStream inputStream=new FileInputStream(imageFile);
                            byte[] imageData=inputStream.readAllBytes();
                            inputStream.close();
                            HandleResponseData(requestSocket,"image/*",imageData);
                        }
                        else
                        {
                            HandleErrorMessage(requestSocket, "Cannot find the given file: [" + imagePath + "]");
                        }
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
