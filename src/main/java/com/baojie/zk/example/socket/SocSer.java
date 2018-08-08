package com.baojie.zk.example.socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.concurrent.TimeUnit;

public class SocSer {
    private static Logger log = LoggerFactory.getLogger(SocSer.class);

    public static void main(String[] args) throws Exception {
        ServerSocketChannel serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(new InetSocketAddress((8080)));
        serverSocketChannel.configureBlocking(false);

        Selector selector = Selector.open();

        // It must be ACCEPT, or it will throw exception
        serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);

        while (true) {
            int sv=selector.select(TimeUnit.MINUTES.toMillis(1));

            if (sv == 0) {
                continue;
            }

            Iterator<SelectionKey> keyIter = selector.selectedKeys().iterator();

            while (keyIter.hasNext()) {
                SelectionKey key = keyIter.next();
                new Thread(new HttpHandler(key)).run();
                keyIter.remove();
            }
        }
    }

    private static final class HttpHandler implements Runnable {
        private int bufferSize = 1024;
        private String localCharset = "UTF-8";
        private SelectionKey key;

        public HttpHandler(SelectionKey key) {
            this.key = key;
        }

        public void handleAccept() throws IOException {
            SocketChannel clientChannel = ((ServerSocketChannel) key.channel()).accept();
            clientChannel.configureBlocking(false);
            clientChannel.register(key.selector(), SelectionKey.OP_READ, ByteBuffer.allocate(bufferSize));
        }

        public void handleRead() throws IOException {
            SocketChannel sc = (SocketChannel) key.channel();
            ByteBuffer buffer = (ByteBuffer) key.attachment();
            buffer.clear();

            if (sc.read(buffer) == -1) {
                sc.close();
            } else {
                buffer.flip();
                String receiveString = Charset.forName(localCharset).newDecoder().decode(buffer).toString();

                String[] requestMessage = receiveString.split("\r\n");
                for (String s : requestMessage) {
                    System.out.println(s);
                    if (s.isEmpty()) {
                        break;
                    }
                }

                String[] firstLine = requestMessage[0].split(" ");
                System.out.println();
                System.out.println("Method:\t" + firstLine[0]);
                System.out.println("url:\t" + firstLine[1]);
                System.out.println("HTTP Version:\t" + firstLine[2]);
                System.out.println();

                StringBuilder sendString = new StringBuilder();
                sendString.append("HTTP/1.1 200 OK\r\n");
                sendString.append("Content-Type:text/html;Charset=" + localCharset + "\r\n");
                sendString.append("\r\n");
                sendString.append("<html><head><title>SHOW</title></head></body>");
                sendString.append("Received:<br/>");

                for (String s : requestMessage) {
                    sendString.append(s + "<br/>");
                }
                sendString.append("</body></html>");
                buffer = ByteBuffer.wrap(sendString.toString().getBytes(localCharset));
                sc.write(buffer);
                sc.close();
            }
        }

        @Override
        public void run() {
            try {
                if (key.isAcceptable()) {
                    handleAccept();
                }
                if (key.isReadable()) {
                    handleRead();
                }
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
    }

}