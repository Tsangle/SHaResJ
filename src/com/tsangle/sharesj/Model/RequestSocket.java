package com.tsangle.sharesj.Model;

import java.io.*;
import java.net.Socket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;

public class RequestSocket{
    private static Logger logger=Logger.getLogger(RequestSocket.class.getName());
    private String[] urlArray;
    private String requestType;
    private String host;
    private Socket acceptSocket;
    private String errorString;
    private String url;
    private String range;
    private byte[] additionalData;
    private StringBuilder additionResponseHeaderBuilder;
    private String statusCode;
    private String acceptEncoding;

    public RequestSocket(Socket acceptSocket){
        this.acceptSocket=acceptSocket;
        this.errorString="";
        this.additionalData=new byte[0];
        this.additionResponseHeaderBuilder=new StringBuilder();
        this.statusCode="200";
        try{
            DataInputStream inputStream=new DataInputStream(acceptSocket.getInputStream());
            StringBuilder requestInfoBuilder=new StringBuilder();
            StringBuilder lineBuilder=new StringBuilder();
            boolean emptyLineEncountered=false;
            for(int inputData=inputStream.read();inputData!=-1;inputData=inputStream.read()){
                byte[] byteArray={(byte)inputData};
                ByteBuffer byteBuffer=ByteBuffer.wrap(byteArray);
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
                            case "Accept-Encoding:":
                                acceptEncoding=requestArray[1];
                                break;
                            case "Content-Length:":
                                int contentLength=Integer.valueOf(requestArray[1]);
                                additionalData=new byte[contentLength];
                            case "Range:":
                                range=requestArray[1];
                                break;
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
                inputStream.readFully(this.additionalData);
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

    public String GetRange(){
        return range;
    }

    public void AddAdditionalResponseHeader(String header){
        this.additionResponseHeaderBuilder.append(header);
        this.additionResponseHeaderBuilder.append(System.lineSeparator());
    }

    public String GetAdditionalResponseHeaders(){
        return this.additionResponseHeaderBuilder.toString();
    }

    public void SetStatusCode(String code){
        this.statusCode=code;
    }

    public String GetStatusCode(){
        return this.statusCode;
    }

    public String GetAcceptEncoding(){
        return this.acceptEncoding;
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
