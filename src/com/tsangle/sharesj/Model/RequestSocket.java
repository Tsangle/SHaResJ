package com.tsangle.sharesj.Model;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class RequestSocket{
    private static Logger logger=Logger.getLogger(RequestSocket.class.getName());
    private String[] urlArray;
    private String requestType;
    private String host;
    private Socket acceptSocket;
    private String errorString;
    private String url;
    private byte[] additionalData;

    public RequestSocket(Socket acceptSocket){
        this.acceptSocket=acceptSocket;
        this.errorString="";
        this.additionalData=new byte[0];
        try{
            InputStream inputStream=acceptSocket.getInputStream();
            StringBuilder requestInfoBuilder=new StringBuilder();
            StringBuilder lineBuilder=new StringBuilder();
            boolean emptyLineEncountered=false;
            for(ByteBuffer byteBuffer=ByteBuffer.wrap(inputStream.readNBytes(1));byteBuffer.capacity()>0;byteBuffer=ByteBuffer.wrap(inputStream.readNBytes(1))){
                char inputChar=StandardCharsets.UTF_8.decode(byteBuffer).get(0);
                requestInfoBuilder.append(inputChar);
                if(inputChar=='\r'){
                    String line=lineBuilder.toString();
                    lineBuilder.delete(0,lineBuilder.length());
                    if(line.equals("")){
                        emptyLineEncountered=true;
                    }else{
                        String[] requestArray=line.split(" ");
                        switch (requestArray[0]){
                            case "GET":
                            case "POST":
                                requestType=requestArray[0];
                                if(requestArray[1].equals("/favicon.ico")){
                                    urlArray=new String[]{"Resource","Img","shares.ico"};
                                    url="/Resource/Img/shares.ico";
                                }else{
                                    urlArray=requestArray[1].substring(1).split("/",3);
                                    url=requestArray[1];
                                }
                                break;
                            case "Host:":
                                host=requestArray[1];
                                break;
                            case "Content-Length:":
                                int contentLength=Integer.valueOf(requestArray[1]);
                                additionalData=new byte[contentLength];
                        }
                    }
                }else if(inputChar=='\n'){
                    if(emptyLineEncountered)
                        break;
                }else{
                    lineBuilder.append(inputChar);
                }
            }
            String requestInfo=requestInfoBuilder.toString();
            logger.info(requestInfo);
            if(this.additionalData.length>0){
                this.additionalData=inputStream.readNBytes(this.additionalData.length);
            }
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }

    public String[] GetUrlArray() {
        return urlArray;
    }

    public String GetRequestType() {
        return requestType;
    }

    public String GetHost() {
        return host;
    }

    public String GetUrl(){
        return url;
    }

    public byte[] GetAdditionalData(){
        return additionalData;
    }

    public OutputStream GetOutputStream(){
        try{
            return acceptSocket.getOutputStream();
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
            return null;
        }
    }

    public String GetErrorString(){
        return errorString;
    }

    public void SetErrorString(String errorString){
        this.errorString=errorString;
    }

    public InputStream GetInputStream(){
        try{
            return acceptSocket.getInputStream();
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
            return null;
        }
    }

    public void Close(){
        try{
            acceptSocket.close();
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }

    public boolean isClosed(){
        return acceptSocket.isClosed();
    }

    public boolean CheckUrlArrayFormat(int requiredCount){
        return urlArray.length==requiredCount;
    }
}
