/**
 * Created by Zorro on 7/9 009.
 */
package me.mzorro.zookeeper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class BindMasterIPHelper {

    private static void runCommond(String cmd) {
        BufferedReader reader = null;
        try {
            Process process = Runtime.getRuntime().exec(cmd);
            if (process != null) {
                reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                System.out.println("Execute cmd: " + cmd);
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println(line);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    public static void upMasterIP(String masterIP) {
        runCommond("./up-master-ip.sh");
    }

    public static void downRemoteMasterIP(String remoteIP, String masterIP) {

    }

    public static void main(String[] args) {
        runCommond("ls -al");
    }
}
