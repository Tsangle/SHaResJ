package com.tsangle.sharesj.Handler;

import com.tsangle.sharesj.Model.RequestSocket;

import java.io.*;
import java.util.logging.Logger;

public abstract class BaseRequestHandler {
    private static Logger logger=Logger.getLogger(BaseRequestHandler.class.getName());

    void HandleResponseMessage(RequestSocket requestSocket, String contentType, String message){
        try{
            PrintWriter printWriter = new PrintWriter(
                    requestSocket.GetOutputStream(), true);
            printWriter.println("HTTP/1.1 200 OK");
            printWriter.println("Content-Type:"+contentType+";charset=utf-8");
            printWriter.println();
            printWriter.println(message);
            printWriter.close();
            requestSocket.Close();
        }catch (Exception e){
            HandleException(requestSocket,e);
        }
    }

    void HandleErrorMessage(RequestSocket requestSocket, String message){
        String errorMessage="#"+message;
        HandleResponseMessage(requestSocket,"text/html",errorMessage);
    }

    void HandleResponseData(RequestSocket requestSocket, String contentType, byte[] data){
        try{
            String responseHeader="HTTP/1.1 200 OK"+System.lineSeparator()+"Content-Type:"+contentType+";charset=utf-8"+System.lineSeparator()+System.lineSeparator();
            OutputStream outputStream=requestSocket.GetOutputStream();
            outputStream.write(responseHeader.getBytes());
            outputStream.write(data);
            outputStream.flush();
            outputStream.close();
            requestSocket.Close();
        }catch (Exception e){
            HandleException(requestSocket,e);
        }
    }

    void HandleException(RequestSocket requestSocket, Exception exception){
        StringWriter stringWriter = new StringWriter();
        exception.printStackTrace(new PrintWriter(stringWriter));
        logger.warning(stringWriter.toString());
        requestSocket.SetErrorString(exception.getMessage());
    }

    public abstract void Handle(RequestSocket requestSocket);
}
