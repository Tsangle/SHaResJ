package com.tsangle.sharesj;

import com.tsangle.sharesj.HttpServer.HttpListener;

import java.io.*;
import java.util.Scanner;
import java.util.logging.Logger;

public class SharesDriver {
    private static Logger logger = Logger.getLogger(SharesDriver.class.getName());

    public static void main(String[] args){
        try {
            HttpListener listener=new HttpListener(50010);
            listener.Start();
            Scanner scanner=new Scanner(System.in);
            while (true){
                String inputString=scanner.nextLine();
                if(inputString.equals("0")){
                    listener.Stop();
                    break;
                }
            }
        } catch (Exception e) {
            StringWriter stringWriter = new StringWriter();
            e.printStackTrace(new PrintWriter(stringWriter));
            logger.warning(stringWriter.toString());
        }
    }
}
