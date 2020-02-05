package com.tsangle.sharesj.Model;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

public class RequestSocket{
    private static Logger logger=Logger.getLogger(RequestSocket.class.getName());
    private List<String> urlList;
    private String requestType;
    private String host;
    private Socket acceptSocket;
    private String errorString;
    private String url;

    public RequestSocket(Socket acceptSocket){
        this.acceptSocket=acceptSocket;
        this.errorString="";
        try{
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(acceptSocket.getInputStream()));
            StringBuilder stringBuilder=new StringBuilder();
            for(int inputChar=bufferedReader.read();inputChar!=-1&&bufferedReader.ready();inputChar=bufferedReader.read()){
                stringBuilder.append((char)inputChar);
            }
            String requestString=stringBuilder.toString();
            logger.info(requestString);
            String[] requestLineArray=requestString.split(System.lineSeparator());
            for(String requestLine:requestLineArray){
                String[] requestArray=requestLine.split(" ");
                switch (requestArray[0]){
                    case "GET":
                        requestType="GET";
                        if(requestArray[1].equals("/favicon.ico")){
                            String[] urlArray={"Resource","Img","shares.ico"};
                            urlList=Arrays.asList(urlArray);
                            url="/Resource/Img/shares.ico";
                        }else{
                            urlList=Arrays.asList(requestArray[1].substring(1).split("/"));
                            url=requestArray[1];
                        }
                        break;
                    case "Host:":
                        host=requestArray[1];
                        break;
                    case "POST":
                        requestType="POST";
                        url=requestArray[1];
                        urlList= Arrays.asList(requestArray[1].substring(1).split("/"));
                        break;
                }
            }
        }catch (Exception e){
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }

    public List<String> GetUrlList() {
        return urlList;
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

    public boolean CheckUrlListFormat(int requiredCount){
        return urlList.size()==requiredCount;
    }
}
