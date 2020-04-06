package com.tsangle.sharesj;

import com.tsangle.sharesj.Handler.FileHandler;
import com.tsangle.sharesj.HttpServer.HttpListener;
import com.tsangle.sharesj.Model.MachineInfo;

import java.io.*;
import java.net.Inet4Address;
import java.util.Scanner;
import java.util.logging.Logger;

public class SharesDriver {
    private static Logger logger = Logger.getLogger(SharesDriver.class.getName());

    public static void main(String[] args){
        try {
            HttpListener listener=new HttpListener();
            Scanner scanner=new Scanner(System.in);
            System.out.println("Please input the path of shared folder:");
            String path=scanner.nextLine();
            MachineInfo.GetInstance().SetRootPath(path);
            System.out.println("Please input the name of this machine:");
            String machineName=scanner.nextLine();
            MachineInfo.GetInstance().SetMachineName(machineName);
            listener.Start();
            boolean continueScannerLoop=true;
            System.out.println("You can enter [E] to exit.");
            while (continueScannerLoop){
                switch (scanner.nextLine()){
                    case "e":
                    case "E":
                        System.out.println("exiting...");
                        listener.Stop();
                        continueScannerLoop=false;
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
