package com.tsangle.sharesj;

import com.tsangle.sharesj.HttpServer.HttpListener;
import com.tsangle.sharesj.HttpServer.ServiceNode;

import java.io.*;
import java.util.Map;
import java.util.Scanner;
import java.util.TreeMap;
import java.util.logging.Logger;

public class SharesDriver {
    private static Logger logger = Logger.getLogger(SharesDriver.class.getName());

    public static void main(String[] args){
        try {
            HttpListener listener=new HttpListener();
            Scanner scanner=new Scanner(System.in);
            System.out.println("Please input a comma-separated string to set the disks for sharing, each item should follow the format of \"DisplayName-PATH\"(e.g. D Drive-D:/My Document,Temp Folder-G:/temp)");
            String sharedDiskString=scanner.nextLine();
            String[] sharedDisks=sharedDiskString.split(",");
            Map<String, String> sharedDiskMap=new TreeMap<>();
            for (String sharedFolder: sharedDisks) {
                String[] sharedFolderInfo=sharedFolder.split("-",2);
                sharedDiskMap.put(sharedFolderInfo[0], sharedFolderInfo[1]);
            }
            ServiceNode.GetInstance().SetSharedDisk(sharedDiskMap);
            System.out.println("Please input the name of this machine:");
            String machineName=scanner.nextLine();
            ServiceNode.GetInstance().SetNodeName(machineName);
            listener.Start();
            boolean continueScannerLoop=true;
            System.out.println("You can enter [E] to exit.");
            while (continueScannerLoop&&scanner.hasNextLine()){
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
