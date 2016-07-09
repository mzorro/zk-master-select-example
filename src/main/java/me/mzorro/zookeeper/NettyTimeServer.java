/**
 * Created by Zorro on 7/3 003.
 */
package me.mzorro.zookeeper;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LineBasedFrameDecoder;
import io.netty.handler.codec.string.StringDecoder;
import io.netty.handler.codec.string.StringEncoder;
import io.netty.util.CharsetUtil;

import java.util.Date;

public class NettyTimeServer implements Runnable {
    private int port;

    public NettyTimeServer(int port) {
        this.port = port;
    }

    public void run() {
        // 创建服务端的NIO线程组
        // parentGroup用于服务端接受客户端连接(acceptor)
        NioEventLoopGroup parentGroup = new NioEventLoopGroup();
        // childGroup用于进行SocketChannel的网络读写(client)
        NioEventLoopGroup childGroup = new NioEventLoopGroup();
        try {
            // 创建一个Server
            ServerBootstrap b = new ServerBootstrap();
            b.group(parentGroup, childGroup)
                    // 设置channel
                    .channel(NioServerSocketChannel.class)
                    // 设置服务socket参数(backlog可理解为允许同时连接的client数量)
                    .option(ChannelOption.SO_BACKLOG, 1024)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(new StringEncoder(CharsetUtil.UTF_8))
                                    .addLast(new LineBasedFrameDecoder(1024))
                                    .addLast(new StringDecoder(CharsetUtil.UTF_8))
                                    .addLast(new TimeServerHandler());
                        }
                    });
            // 绑定端口，同步等待绑定成功
            ChannelFuture f = b.bind(port).sync();

            // 等待服务器监听端口关闭
            f.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            // 优雅退出，释放线程池资源
            parentGroup.shutdownGracefully();
            childGroup.shutdownGracefully();
        }
    }

    private static class TimeServerHandler extends ChannelHandlerAdapter {
        @Override
        public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
            // 读数据
            String order = (String) msg;
            System.out.println("Receive order: \"" + order + "\" from: " + ctx.channel().remoteAddress());
            String response = "query time".equalsIgnoreCase(order) ? new Date().toString() : "BAD ORDER";
            System.out.println("Send response: \"" + response + "\" to: " + ctx.channel().remoteAddress());
            // 写响应
            //ByteBuf resBuf = Unpooled.copiedBuffer((response+'\n').getBytes());
            ctx.write(response+"\n");
        }

        @Override
        public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
            // 读取完毕，调用flush
            // 为防止频繁唤醒Selector进行消息发送，Netty的write方法并不立即将消息写入SocketChannel
            // 而是会把待发送的消息存放到缓冲数组
            ctx.flush();
        }

        @Override
        public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
            // 发生异常，直接关闭
            ctx.close();
        }
    }

    public static void main(String[] args) {
        new NettyTimeServer(8013).run();
    }
}
