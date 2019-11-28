package com.example.niotest;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.logging.Logger;

/**
 * @author luffy
 * @date 2019-11-28
 * @version 1.0
 */
public class ClientTest {
    private Logger logger = Logger.getLogger(this.getClass().getName());
    private Selector selector;
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final int PORT = 9999;
    private static final String END_MSG = "bye";

    /**
     * 连接服务
     */
    private void connectServer() {
        try {
            SocketChannel socketChannel = SocketChannel.open();
            selector = Selector.open();
            InetSocketAddress address = new InetSocketAddress(IP_ADDRESS, PORT);
            //非阻塞
            socketChannel.configureBlocking(false);
            //指定IP和端口-连接服务器
            socketChannel.connect(address);
            //将channel注册到selector并设置为连接态
            socketChannel.register(selector, SelectionKey.OP_CONNECT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 连接就绪状态，切换为写就绪状态
     * @param key
     */
    private void connectReady(SelectionKey key) {
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            channel.finishConnect();
            key.interestOps(SelectionKey.OP_WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读就绪状态，读取服务端发回的数据，切换为写就绪状态
     * @param key
     */
    private void readMsg(SelectionKey key) {
        ByteBuffer buffer = null;
        try {
            SocketChannel channel = (SocketChannel) key.channel();
            buffer = ByteBuffer.allocate(1024);
            //读取服务端数据
            channel.read(buffer);
            key.interestOps(SelectionKey.OP_WRITE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("收到服务端数据：" + new String(buffer.array()));
    }

    /**
     * 写就绪状态，向服务端发送数据，切换为读就绪状态
     * @param key
     */
    private void writeMsg(SelectionKey key) {
        SocketChannel channel = null;
        try {
            channel = (SocketChannel) key.channel();
        } catch (Exception e) {
            e.printStackTrace();
        }
        String[] strArray = new String[]{"hello", "world", "bye"};
        Random random = new Random();
        //随机取数0-2 向服务端发送数据
        int randomNum = random.nextInt(3);
        String msg = strArray[randomNum];
        byte[] array = msg.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(array);
        try {
            //将数据写入channel
            channel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("已向服务器发送消息：" + msg);
        //切换为读就绪状态
        key.interestOps(SelectionKey.OP_READ);
        //如果发送"bye"，则和服务器断开连接
        if (END_MSG.equals(msg)) {
            logger.info(channel.socket().getRemoteSocketAddress() + "已和服务器断开连接！");
            try {
                key.channel().close();
                key.cancel();
                channel.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 客户端启动
     */
    private void clientStart() {
        this.connectServer();
        while (true) {
            int num = 0;
            try {
                num = selector.select();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            if (num > 0) {
                Iterator<SelectionKey> iterator = selector.selectedKeys().iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid() && key.isConnectable()) {
                        this.connectReady(key);
                    } else if (key.isReadable()) {
                        this.readMsg(key);
                    } else if (key.isValid() && key.isWritable()) {
                        this.writeMsg(key);
                    }
                }
            }
        }

    }

    public static void main(String[] args) {
        new ClientTest().clientStart();
    }
}
