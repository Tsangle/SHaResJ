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
    private String[] host;
    private Socket acceptSocket;
    private String errorString;
    private String url;
    private String range;
    private long additionalDataLength;
    private StringBuilder additionResponseHeaderBuilder;
    private String statusCode;
    private DataInputStream requestInputStream;

    public RequestSocket(Socket acceptSocket){
        this.acceptSocket=acceptSocket;
        this.errorString="";
        this.additionResponseHeaderBuilder=new StringBuilder();
        this.additionalDataLength=0;
        this.requestInputStream=null;
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
                                }else if(requestArray[1].equals("/")){
                                    urlArray=new String[]{"Resource","Page","Home.html"};
                                    url="/Resource/Page/Home.html";
                                }else{
                                    urlArray=requestArray[1].substring(1).split("/",3);
                                    url=requestArray[1];
                                }
                                break;
                            case "Host:":
                                host=requestArray[1].split(":",2);
                                break;
                            case "Content-Length:":
                                additionalDataLength=Long.parseLong(requestArray[1]);
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
            if(this.additionalDataLength>0){
                requestInputStream=inputStream;
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

    public String[] GetHost() {
        return host;
    }

    public String GetUrl(){
        return url;
    }

    public byte[] GetAdditionalData() throws Exception{
        byte[] buffer=new byte[0];
        if(additionalDataLength>0){
            buffer=new byte[(int)additionalDataLength];
            requestInputStream.readFully(buffer);
        }
        return buffer;
    }

    public byte[] ReadAdditionalDataChunk(int length) throws Exception{
        byte[] buffer;
        if(additionalDataLength>0){
            buffer=new byte[length];
            requestInputStream.readFully(buffer);
        }else{
            buffer=new byte[0];
        }
        return buffer;
    }

    public long GetAdditionDataLength(){
        return additionalDataLength;
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
