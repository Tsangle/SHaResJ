package com.tsangle.sharesj.Model;

import java.net.Inet4Address;
import java.util.Map;

public class MachineInfo {
    private String rootPath;

    private String machineName;

    private Map<String,String> machineMap;

    private final int port;

    static private MachineInfo instance;

    private MachineInfo(){
        port=10320;
    }

    static public MachineInfo GetInstance(){
        if(instance==null){
            instance=new MachineInfo();
        }
        return instance;
    }

    public void SetRootPath(String path){
        rootPath=path;
    }

    public String GetRootPath(){
        return rootPath;
    }

    public void SetMachineName(String name){
        machineName=name;
    }

    public String GetMachineName(){
        return machineName;
    }

    public void AddMachine(String ipAddress,String machineName){
        machineMap.put(ipAddress,machineName);
    }

    public void RemoveMachine(String ipAddress){
        machineMap.remove(ipAddress);
    }

    public Map<String,String> GetMachineMap(){
        return machineMap;
    }

    public int GetPort(){
        return port;
    }

    public String GetIPAddress(){
        try{
            return Inet4Address.getLocalHost().getHostAddress();
        }catch (Exception e){
            return null;
        }
    }
}
