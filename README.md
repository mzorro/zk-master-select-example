# zk-master-select-example
这是一个简单的分布式服务，用来测试zookeeper的master选举功能。
输入参数为: [localIFace] [localIP] [masterIP] [zookeeper-address]
其中localIP和localIFace为本地IP地址和网卡名称。

如果被选举为master，则应该绑定输入参数中的masterIP(作为子IP绑定到[localIFace]:0上)。这样通过masterIP总是可以在集群中访问到服务。

## 前提
- 两个或以上节点，例如10.82.60.69与10.82.60.70
- 各个节点之间可以通过ssh无密码通信，且known_hosts中有相关信息。
- 一个空闲的IP地址作为masterIP，例如10.82.60.50

## 执行示例

在10.82.60.69节点
```
mvn clean compile exec:java -Dexec.mainClass="me.mzorro.zookeeper.DistributedService" -Dexec.args="eth0 10.82.60.69 10.82.60.50 10.82.81.28:2181"
```

在10.82.60.70节点
```
mvn clean compile exec:java -Dexec.mainClass="me.mzorro.zookeeper.DistributedService" -Dexec.args="eth0 10.82.60.70 10.82.60.50 10.82.81.28:2181"
```
