package com.example.niotest;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;
import java.util.logging.Logger;

/**
 * @author luffy
 * @date 2019-11-28
 * @version 1.0
 */
public class ServerTest {
    private static final String CHARSET = "utf-8";
    private static final String END_MSG = "bye";
    private Logger logger = Logger.getLogger(getClass().getName());
    private Selector selector;

    /**
     * 连接准备
     */
    private void connectReady() {
        try {
            selector = Selector.open();
            ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
            InetSocketAddress address = new InetSocketAddress(9999);
            serverSocketChannel.socket().bind(address);
            //非阻塞
            serverSocketChannel.configureBlocking(false);
            //将ServerSocketChannel注册到Selector，将状态置为接收就绪
            serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 接收就绪
     * @param key
     */
    private void acceptReady(SelectionKey key) {
        ServerSocketChannel channel = (ServerSocketChannel) key.channel();
        SocketChannel tmpChannel = null;
        try {
            //获取客户端连接
            tmpChannel = channel.accept();
        } catch (IOException e) {
            e.printStackTrace();
        }
        logger.info("客户端IP：" + tmpChannel.socket().getRemoteSocketAddress() + "连接进来了！");
        try {
            //非阻塞
            tmpChannel.configureBlocking(false);
            //将channel注册到selector，切换为读就绪状态
            tmpChannel.register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * 读取客户端消息，切换为写就绪状态
     * @param key
     */
    private void readMsg(SelectionKey key) {
        SocketChannel tmpSocketChannel = (SocketChannel) key.channel();
        ByteBuffer buffer = ByteBuffer.allocate(1024);
        int sum = 0;
        try {
            sum = tmpSocketChannel.read(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (sum != -1) {
            //切换到读模式
            buffer.flip();
            String msg = null;
            try {
                msg = new String(buffer.array(), buffer.position(), buffer.limit(), CHARSET);
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            //清空buffer
            buffer.clear();

            logger.info("接收来自客户端的数据：" + new String(buffer.array()));
            //切换为写就绪态
            key.interestOps(SelectionKey.OP_WRITE);
            //如果接受到"bye"，客户端断开连接
            if (END_MSG.equals(msg)) {
                logger.info(tmpSocketChannel.socket().getRemoteSocketAddress() + "断开连接！");
                try {
                    key.channel().close();
                    key.cancel();
                    tmpSocketChannel.close();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 向客户端发送数据，切换为读就绪状态
     * @param key
     */
    private void writeMsg(SelectionKey key) {
        SocketChannel tmpSocketChannel = (SocketChannel) key.channel();
        String[] strArray = new String[]{"I do", "I love", "I think"};
        Random random = new Random();
        //随机数 0-2
        int randomNum = random.nextInt(3);

        String msg = strArray[randomNum];
        byte[] resByte = msg.getBytes();
        ByteBuffer buffer = ByteBuffer.wrap(resByte);
        try {
            tmpSocketChannel.write(buffer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //切换为读就绪状态
        key.interestOps(SelectionKey.OP_READ);
        logger.info("发送给客户端完毕！");
    }

    /**
     * 开启服务端
     */
    private void startServer() {
        this.connectReady();
        while (true) {
            int num = 0;
            try {
                num = selector.select();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (num > 0) {
                Set<SelectionKey> keys = selector.selectedKeys();
                Iterator<SelectionKey> iterator = keys.iterator();
                while (iterator.hasNext()) {
                    SelectionKey key = iterator.next();
                    iterator.remove();
                    if (key.isValid() && key.isAcceptable()) {
                        this.acceptReady(key);
                    } else if (key.isValid() && key.isReadable()) {
                        this.readMsg(key);
                    } else if (key.isValid() && key.isWritable()) {
                        this.writeMsg(key);
                    }
                }
            } else {
                logger.info("没有连接！");
            }
        }
    }

    public static void main(String[] args) {
        new ServerTest().startServer();
    }
}


