package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Map;
import java.util.logging.Logger;

public class PathHandler extends BaseRequestHandler {
    private static Logger logger=Logger.getLogger(PathHandler.class.getName());

    private Map<String,String> rootPathMap;

    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.GetRequestType().equals("GET")){
                HandleErrorMessage(requestSocket,"The request type of [Path] must be POST!");
            }else{
                if(!requestSocket.CheckUrlListFormat(2)){
                    HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [Path]!");
                }else{
                    switch (requestSocket.GetUrlList().get(1)){
                        case "GetFileSystemEntries":
                            break;
                        case "SetRootPathMap":
                            break;
                        case "GetRootPathMap":
                            break;
                        case "GetRealPath":
                            break;
                    }
                }
            }
        }catch (Exception e){
            HandleException(requestSocket,e);
        }
    }
}
