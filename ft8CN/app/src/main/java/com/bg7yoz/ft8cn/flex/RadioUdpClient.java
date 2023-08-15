package com.bg7yoz.ft8cn.flex;
/**
 * 简单的udp封装，用于数据流的操作
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.util.Log;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RadioUdpClient {
    private static final String TAG = "RadioUdpSocket";
    private final int MAX_BUFFER_SIZE = 1024*2;
    private DatagramSocket sendSocket;
    private int port;
    private boolean activated = false;
    private OnUdpEvents onUdpEvents = null;
    private final ExecutorService sendDataThreadPool = Executors.newCachedThreadPool();
    private final ExecutorService receiveThreadPool = Executors.newCachedThreadPool();
    private final SendDataRunnable sendDataRunnable=new SendDataRunnable(this);
    private final ReceiveRunnable receiveRunnable=new ReceiveRunnable(this);

    public RadioUdpClient(int port) {
        this.port = port;
    }

    public synchronized void sendData(byte[] data, String ip,int port) throws UnknownHostException {
        if (!activated) return;
        //Log.e(TAG, "sendData: "+byteToStr(data) );
        //Log.e(TAG, String.format("sendData: ip: %s,port:%d ",ip,port) );
        InetAddress address = InetAddress.getByName(ip);
        sendDataRunnable.data=data;
        sendDataRunnable.address=address;
        sendDataRunnable.port=port;
        sendDataThreadPool.execute(sendDataRunnable);
    }

    private static class SendDataRunnable implements Runnable{
        byte[] data;
        InetAddress address;
        int port;
        RadioUdpClient client;

        public SendDataRunnable(RadioUdpClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            DatagramPacket packet = new DatagramPacket(data, data.length, address,port);
            try {
                client.sendSocket.send(packet);
            } catch (IOException e) {
                e.printStackTrace();
                Log.e(TAG, "run: " + e.getMessage());
            }
        }
    }

    public boolean isActivated() {
        return activated;
    }

    public synchronized void  setActivated(boolean activated) throws SocketException {
        this.activated = activated;
        if (activated) {//通过activated判断是否结束接收线程，并清空sendSocket指针
            sendSocket = new DatagramSocket(null);//绑定的端口号随机
            sendSocket.bind(new InetSocketAddress(port));
            // Log.e(TAG, "openUdpPort: "+sendSocket.getLocalPort());
            receiveData();
        }else {
            if (sendSocket!=null){
                sendSocket.close();
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void receiveData() {
        receiveThreadPool.execute(receiveRunnable);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                while (activated) {
//                    byte[] data = new byte[MAX_BUFFER_SIZE];
//                    DatagramPacket packet = new DatagramPacket(data, data.length);
//                    try {
//                        sendSocket.receive(packet);
//                        if (onUdpEvents != null) {
//                            byte[] temp = Arrays.copyOf(packet.getData(), packet.getLength());
//                            onUdpEvents.OnReceiveData(sendSocket, packet, temp);
//                        }
//                        //Log.d(TAG, "receiveData:host ip: " + packet.getAddress().getHostName());
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                        Log.e(TAG, "receiveData: error:" + e.getMessage());
//                    }
//
//                }
//                Log.e(TAG, "udpClient: is exit!");
//                sendSocket.close();
//                sendSocket = null;
//            }
//        }).start();

    }
    private static class ReceiveRunnable implements Runnable{
        RadioUdpClient client;

        public ReceiveRunnable(RadioUdpClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            while (client.activated) {
                byte[] data = new byte[client.MAX_BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                try {
                    client.sendSocket.receive(packet);
                    if (client.onUdpEvents != null) {
                        byte[] temp = Arrays.copyOf(packet.getData(), packet.getLength());
                        client.onUdpEvents.OnReceiveData(client.sendSocket, packet, temp);
                    }
                    //Log.d(TAG, "receiveData:host ip: " + packet.getAddress().getHostName());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "receiveData: error:" + e.getMessage());
                }

            }
            Log.e(TAG, "udpClient: is exit!");
            client.sendSocket.close();
            client.sendSocket = null;
        }
    }
    public void setOnUdpEvents(OnUdpEvents onUdpEvents) {
        this.onUdpEvents = onUdpEvents;
    }

    interface OnUdpEvents {
        void OnReceiveData(DatagramSocket socket, DatagramPacket packet, byte[] data);
    }

    public int getPort() {
        if (sendSocket!=null){
            return sendSocket.getLocalPort();
        }else {
            return 0;
        }
    }

    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x ", data[i] & 0xff));
        }
        return s.toString();
    }
}
