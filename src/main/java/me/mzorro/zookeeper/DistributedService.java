/**
 * Created by Zorro on 7/8 008.
 */
package me.mzorro.zookeeper;

import java.util.concurrent.CountDownLatch;

/**
 * 这是一个简单的分布式服务，用来测试zookeeper的master选举功能
 * 如果被选举为master，则应该抢占绑定输入参数中的masterIp(作为子IP绑定到eth0:0上)
 * 这样通过masterIp总是可以在集群中访问到服务
 */
public class DistributedService {
    public static void main(String[] args) {
        if (args.length == 4) {
            String localIFace = args[0];
            // 当前主机的本地IP
            String localIP = args[1];
            // 对外开放的masterIp
            String masterIP = args[2];
            // zookeeper的地址
            String zkAddress = args[3];

            // 用Latch来控制程序结束
            CountDownLatch stopLatch = new CountDownLatch(1);

            // 启动ZookeeperClient
            ZookeeperClient client = new ZookeeperClient(localIFace, localIP, masterIP, zkAddress);
            client.start(stopLatch);

            // 启动NettyTimeServer
            new NettyTimeServer(8013).run();

            // 停止服务，让出master权
            stopLatch.countDown();
        } else {
            System.out.println("usage: java .. [local-interface] [local-ip] [master-ip] [zookeeper-address]");
        }
    }
}
