package com.tsangle.sharesj.HttpServer;

import java.io.File;
import java.net.Inet4Address;
import java.util.HashMap;
import java.util.Map;

public class ServiceNode {
    private Map<String, String> sharedFolders;

    private String nodeName;

    private final int port;

    private final long creationTime;

    static private ServiceNode instance;

    private ServiceNode(){
        port=10320;
        creationTime=System.currentTimeMillis();
        sharedFolders=new HashMap<>();
    }

    static public ServiceNode GetInstance(){
        if(instance==null){
            instance=new ServiceNode();
        }
        return instance;
    }

    public void SetSharedFolder(Map<String, String> sharedFolders){
        for (Map.Entry<String, String> entry : sharedFolders.entrySet()) {
            String path=entry.getValue().replace('\\', '/');
            if (path.endsWith("/")) path=path.substring(0,path.length()-1);
            this.sharedFolders.put(entry.getKey(), path);
        }
    }

    public Map<String, String> GetSharedFolders(){
        return sharedFolders;
    }

    public String GetRealPath(String logicPath, boolean isFile) throws Exception{
        if (logicPath.isEmpty()){
            throw new Exception("The given path is empty!");
        }
        String path=logicPath.replace('\\', '/');
        if (path.startsWith("/")){
            path=path.substring(1);
        }
        String[] folders=path.split("/", 2);
        String realRootFolder=sharedFolders.get(folders[0]);
        if (realRootFolder==null){
            throw new Exception("The root folder in the give path cannot be found in shared folders: [" + logicPath + "]");
        }
        path=realRootFolder;
        if (folders.length==2){
            path+="/"+folders[1];
        }
        File file = new File(path);
        if (file.exists()&&file.isFile()==isFile){
            return path;
        } else {
            throw new Exception("The given path does not exist: [" + logicPath + "]");
        }
    }

    public void SetNodeName(String name){
        nodeName =name;
    }

    public String GetNodeName(){
        return nodeName;
    }

    public int GetPort(){
        return port;
    }

    public long GetCreationTime(){
        return creationTime;
    }

    public String GetIPAddress(){
        try{
            return Inet4Address.getLocalHost().getHostAddress();
        }catch (Exception e){
            return null;
        }
    }
}
