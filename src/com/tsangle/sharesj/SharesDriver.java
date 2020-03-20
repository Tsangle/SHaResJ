package com.tsangle.sharesj;

import com.tsangle.sharesj.Handler.FileHandler;
import com.tsangle.sharesj.HttpServer.HttpListener;

import java.io.*;
import java.util.Scanner;
import java.util.logging.Logger;

public class SharesDriver {
    private static Logger logger = Logger.getLogger(SharesDriver.class.getName());

    public static void main(String[] args){
        try {
            HttpListener listener=new HttpListener(50020);
            listener.Start();
            Scanner scanner=new Scanner(System.in);
            boolean continueScannerLoop=true;
            System.out.println("Enter [P] to set the root path or enter [E] to exit:");
            while (continueScannerLoop){
                switch (scanner.nextLine()){
                    case "e":
                    case "E":
                        System.out.println("exiting...");
                        listener.Stop();
                        continueScannerLoop=false;
                        break;
                    case "p":
                    case "P":
                        System.out.println("Set Path:");
                        String path=scanner.nextLine();
                        FileHandler.SetRootPath(path);
                        System.out.println("Root Path is set to: ["+path+"]");
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
