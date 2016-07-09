# zk-master-select-example
这是一个简单的分布式服务，用来测试zookeeper的master选举功能。如果被选举为master，则应该绑定输入参数中的masterIP(作为子IP绑定到eth0:0上)。这样通过masterIP总是可以在集群中访问到服务。
