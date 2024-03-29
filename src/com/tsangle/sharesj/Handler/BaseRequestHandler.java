package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.HttpServer.RequestSocket;

import java.io.*;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public abstract class BaseRequestHandler {
    private static Logger logger=Logger.getLogger(BaseRequestHandler.class.getName());

    void HandleResponseMessage(RequestSocket requestSocket, String contentType, String message){
        try{
            byte[] messageData=message.getBytes(StandardCharsets.UTF_8);
            HandleResponseData(requestSocket,contentType,messageData);
        }catch (Exception e){
            HandleException(requestSocket,logger,e);
        }
    }

    void HandleErrorMessage(RequestSocket requestSocket, String message){
        String errorMessage="#"+message;
        HandleResponseMessage(requestSocket,"text/html",errorMessage);
    }

    void HandleResponseData(RequestSocket requestSocket, String contentType, byte[] data){
        try{
            String responseHeader="HTTP/1.1 "+requestSocket.GetStatusCode()+" OK"+System.lineSeparator()+
                    "Content-Type:"+contentType+";charset=utf-8"+System.lineSeparator()+
                    requestSocket.GetAdditionalResponseHeaders()+
                    System.lineSeparator();
            OutputStream outputStream=requestSocket.GetOutputStream();
            outputStream.write(responseHeader.getBytes());
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
            requestSocket.Close();
        }catch (SocketException e){
            logger.info("Abort writing data: "+e.getMessage()+System.lineSeparator());
        }catch (Exception e){
            HandleException(requestSocket,logger,e);
        }
    }

    void HandleException(RequestSocket requestSocket,Logger exceptionLogger, Exception exception){
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        exceptionLogger.warning(stringWriter.toString());
        requestSocket.SetErrorString(exception.getMessage());
    }

    public abstract void Handle(RequestSocket requestSocket);
}
