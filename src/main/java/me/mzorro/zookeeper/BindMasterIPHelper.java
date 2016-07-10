/**
 * Created by Zorro on 7/9 009.
 */
package me.mzorro.zookeeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BindMasterIPHelper {

    private static void runCommond(String cmd) {
        BufferedReader inputReader = null, errorReader = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            if (process != null) {
                System.out.println("Execute cmd: " + cmd);
                inputReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                errorReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
                String line;
                while ((line = inputReader.readLine()) != null) {
                    System.out.println(line);
                }
                while ((line = errorReader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (inputReader != null) {
                try {
                    inputReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            if (errorReader != null) {
                try {
                    errorReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void upMasterIP(String localIFace, String localIP, String masterIP) {
        runCommond(String.format("ssh -l root %s /sbin/ifconfig %s:0 %s broadcast %s netmask 255.255.255.255 up",
                localIP, localIFace, masterIP, masterIP));
    }

    public static void downRemoteMasterIP(String remoteIFace, String remoteIP) {
        runCommond(String.format("ssh -l root %s /sbin/ifconfig %s:0 down",
                remoteIP, remoteIFace));
    }

    public static void main(String[] args) {
        runCommond("ssh -l root 10.82.60.69 /sbin/ifconfig enp0s31f6:0 10.82.60.50 broadcast 10.82.60.50 netmask 255.255.255.255 up");
    }
}
