package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

import java.util.HashMap;
import java.util.Map;

public class HandlerDispenser extends BaseRequestHandler {
    private Map<String, BaseRequestHandler> handlerMap;

    public HandlerDispenser(){
        handlerMap=new HashMap<>();
    }

    public void AddHandler(BaseRequestHandler handler){
        handlerMap.put(handler.getClass().getSimpleName().replace("Handler",""),handler);
    }

    @Override
    public void Handle(RequestSocket requestSocket) {
        String requestTaskType=requestSocket.GetUrlArray()[0];
        if(handlerMap.containsKey(requestTaskType)){
            handlerMap.get(requestTaskType).Handle(requestSocket);
        }else{
            HandleErrorMessage(requestSocket,"Cannot find the given task type: [" + requestTaskType + "]");
        }
        if(!requestSocket.isClosed()){
            if(requestSocket.GetErrorString().isEmpty())
                HandleErrorMessage(requestSocket,requestSocket.GetRequestType()+" "+requestSocket.GetUrl()+": Unknown Error!");
            else
                HandleErrorMessage(requestSocket,requestSocket.GetErrorString());
        }
    }
}
