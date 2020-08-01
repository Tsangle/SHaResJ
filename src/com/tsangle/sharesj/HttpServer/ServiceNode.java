package com.tsangle.sharesj.HttpServer;

import java.net.Inet4Address;
import java.util.Map;

public class ServiceNode {
    private String rootPath;

    private String nodeName;

    private Map<String,String> nodeMap;

    private final int port;

    private final long creationTime;

    static private ServiceNode instance;

    private ServiceNode(){
        port=10320;
        creationTime=System.currentTimeMillis();
    }

    static public ServiceNode GetInstance(){
        if(instance==null){
            instance=new ServiceNode();
        }
        return instance;
    }

    public void SetRootPath(String path){
        rootPath=path;
    }

    public String GetRootPath(){
        return rootPath;
    }

    public void SetNodeName(String name){
        nodeName =name;
    }

    public String GetNodeName(){
        return nodeName;
    }

    public void AddExistingServiceNode(String ipAddress, String nodeName){
        nodeMap.put(ipAddress,nodeName);
    }

    public void RemoveCachedServiceNode(String ipAddress){
        nodeMap.remove(ipAddress);
    }

    public Map<String,String> GetServiceNodeMap(){
        return nodeMap;
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
