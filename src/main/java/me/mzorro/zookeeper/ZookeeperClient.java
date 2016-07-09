/**
 * Created by Zorro on 7/8 008.
 */
package me.mzorro.zookeeper;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.RetryNTimes;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.ZooDefs;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class ZookeeperClient {
    private String localIFace;
    private String localIP;
    private String masterIP;
    private String zkAddress;

    public ZookeeperClient(String localIFace, String localIP, String masterIP, String zkAddress) {
        this.localIFace = localIFace;
        this.localIP = localIP;
        this.masterIP = masterIP;
        this.zkAddress = zkAddress;
    }

    private CuratorFramework client;
    private String parentPath = "/service";
    private String childPrefixPath = parentPath + "/zk-select-example_";

    private boolean isMaster;

    public void setMaster(boolean master) {
        System.out.println("setMaster:" + master);
        isMaster = master;
    }

    public void start(final CountDownLatch stopLatch) {
        client = CuratorFrameworkFactory.newClient(zkAddress, new RetryNTimes(5, 1000));
        client.getConnectionStateListenable().addListener(new ConnectionStateListener() {
            public void stateChanged(CuratorFramework client, ConnectionState newState) {
                System.out.println("Zookeeper连接状态改变: " + newState.name());
                if (newState == ConnectionState.CONNECTED || newState == ConnectionState.RECONNECTED) {
                    // 连接上zookeeper了，将当前主机注册进去
                    try {
                        // 创建一个"EPHEMERAL_SEQUENTIAL"模式的节点，节点中保存当前主机的local IP
                        client.create().withMode(CreateMode.EPHEMERAL_SEQUENTIAL)
                                .withACL(ZooDefs.Ids.OPEN_ACL_UNSAFE)
                                .forPath(childPrefixPath, (localIFace + ":" + localIP).getBytes());
                        // 获取最先注册完成的主机IP
                        String firstRegisteredIp = getFirstRegisteredIp();
                        if (!isMaster && firstRegisteredIp.equals(localIP)) {
                            // 如果当前主机还不是master，且最先注册完成的就是当前主机，则认为被选举为master了
                            // 绑定masterIp到当前主机
                            BindMasterIPHelper.upMasterIP(localIFace, localIP, masterIP);
                        }
                        // 考虑重连情况下，当前节点已经不是master了
                        setMaster(firstRegisteredIp.equals(localIP));
                    } catch (Exception e) {
                        System.out.println("注册失败！");
                        e.printStackTrace();
                    }
                } else if (newState == ConnectionState.LOST || newState == ConnectionState.SUSPENDED) {
                    // 失去了与zookeeper的连接
                    System.out.println("失去了与zookeeper的连接");
                    setMaster(false);
                }
            }
        });
        client.start();

        // 监听parentPath下的子节点变化，不在本地进行缓存
        PathChildrenCache childrenCache = new PathChildrenCache(client, parentPath, false);
        childrenCache.getListenable().addListener(new PathChildrenCacheListener() {
            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event) throws Exception {
                if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
                    // 如果有子节点被移除，说明有其他节点宕机了
                    String firstRegisteredIp = getFirstRegisteredIp();
                    if (firstRegisteredIp.equals(localIP)) {
                        // 如果剩余节点中最先完成注册的是当前节点，则当前节点被选举为master
                        // 尝试解除宕机节点对master ip的绑定
                        String data = new String(event.getData().getData());
                        String remoteIFace = data.split(":")[0];
                        String remoteIP = data.split(":")[1];
                        BindMasterIPHelper.downRemoteMasterIP(remoteIFace, remoteIP);
                        // 并绑定master ip到当前节点
                        BindMasterIPHelper.upMasterIP(localIFace, localIP, masterIP);
                        setMaster(true);
                    }
                }
            }
        });
        try {
            childrenCache.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取最先注册完成的主机IP
     */
    public String getFirstRegisteredIp() {
        try {
            List<String> children = client.getChildren().forPath(parentPath);
            // 将children进行排序，获取路径值最小的节点，也就是最近一次注册的节点
            Collections.sort(children);
            return new String(client.getData().forPath(parentPath + '/'+ children.get(0))).split(":")[1];
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
}
