package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.HttpServer.RequestSocket;
import com.tsangle.sharesj.HttpServer.ServiceNode;

import java.util.logging.Logger;

public class InfoHandler extends BaseRequestHandler {
    private static Logger logger=Logger.getLogger(InfoHandler.class.getName());

    @Override
    public void Handle(RequestSocket requestSocket) {
        try{
            if(requestSocket.CheckUrlArrayFormat(2)){
                switch (requestSocket.GetUrlArray()[1]){
                    case "Name":
                        HandleResponseMessage(requestSocket,"text/plain", ServiceNode.GetInstance().GetNodeName());
                        break;
                    default:
                        HandleErrorMessage(requestSocket,"Cannot find the given task: [" + requestSocket.GetUrlArray()[1] + "]");
                        break;
                }
            }else{
                HandleErrorMessage(requestSocket,"The url format doesn't meet the requirement of [Info]!");
            }
        }catch (Exception e){
            HandleException(requestSocket,logger,e);
        }
    }
}
