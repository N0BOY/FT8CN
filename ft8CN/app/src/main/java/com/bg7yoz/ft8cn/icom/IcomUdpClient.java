package com.bg7yoz.ft8cn.icom;

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

public class IcomUdpClient {
    private static final String TAG = "RadioUdpSocket";


    private final int MAX_BUFFER_SIZE = 1024 *2;
    private DatagramSocket sendSocket;
    //private int remotePort;
    private int localPort=-1;
    private boolean activated = false;
    private OnUdpEvents onUdpEvents = null;
    private final ExecutorService doReceiveThreadPool = Executors.newCachedThreadPool();
    private DoReceiveRunnable doReceiveRunnable=new DoReceiveRunnable(this);
    private final ExecutorService sendDataThreadPool = Executors.newCachedThreadPool();
    private SendDataRunnable sendDataRunnable=new SendDataRunnable(this);

    public IcomUdpClient() {//本地端口随机
        localPort=-1;
    }
    public IcomUdpClient(int localPort) {//如果localPort==-1，本地端口随机
        this.localPort=localPort;
    }

    public void sendData(byte[] data, String ip,int port) throws UnknownHostException {
        if (!activated) return;

        InetAddress address = InetAddress.getByName(ip);
        sendDataRunnable.address=address;
        sendDataRunnable.data=data;
        sendDataRunnable.port=port;
        sendDataThreadPool.execute(sendDataRunnable);

    }
    private static class SendDataRunnable implements Runnable{
        byte[] data;
        int port;
        InetAddress address;
        IcomUdpClient client;

        public SendDataRunnable(IcomUdpClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            DatagramPacket packet = new DatagramPacket(data, data.length, address, port);
            synchronized (this) {
                try {
                    client.sendSocket.send(packet);
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "IComUdpClient: " + e.getMessage());
                    if (client.onUdpEvents!=null){
                        client.onUdpEvents.OnUdpSendIOException(e);
                    }
                }
            }
        }
    }

    public boolean isActivated() {
        return activated;
    }

    public synchronized void setActivated(boolean activated) throws SocketException {
        this.activated = activated;
        if (activated) {//通过activated判断是否结束接收线程，并清空sendSocket指针
            sendSocket = new DatagramSocket();
            //new DatagramSocket(null);//绑定的端口号随机
            sendSocket.setReuseAddress(true);
            if (localPort!=-1) {//绑定指定的本机端口
                sendSocket.bind(new InetSocketAddress(localPort));
            }

            //更新一下本地端口值
            localPort=sendSocket.getLocalPort();
            Log.e(TAG, "openUdpPort: " + sendSocket.getLocalPort());
            //Log.e(TAG, "openUdpIp: " + sendSocket.getLocalAddress());


            receiveData();
        } else {
            if (sendSocket != null) {
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
        doReceiveThreadPool.execute(doReceiveRunnable);
    }

    public void setOnUdpEvents(OnUdpEvents onUdpEvents) {
        this.onUdpEvents = onUdpEvents;
    }

    public interface OnUdpEvents {
        void OnReceiveData(DatagramSocket socket, DatagramPacket packet, byte[] data);
        void OnUdpSendIOException(IOException e);
    }

    public int getLocalPort() {
        if (sendSocket != null) {
            return sendSocket.getLocalPort();
        } else {
            return 0;
        }
    }

    public String getLocalIp() {
        if (sendSocket != null) {
            return sendSocket.getLocalAddress().toString();
        } else {
            return "127.0.0.1";
        }
    }

    public DatagramSocket getSendSocket() {
        return sendSocket;
    }


    public static String byteToStr(byte[] data) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            s.append(String.format("%02x ", data[i] & 0xff));
        }
        return s.toString();
    }
    private static class DoReceiveRunnable implements Runnable{
        IcomUdpClient icomUdpClient;

        public DoReceiveRunnable(IcomUdpClient icomUdpClient) {
            this.icomUdpClient = icomUdpClient;
        }

        @Override
        public void run() {
            while (icomUdpClient.activated) {
                byte[] data = new byte[icomUdpClient.MAX_BUFFER_SIZE];
                DatagramPacket packet = new DatagramPacket(data, data.length);
                try {
                    icomUdpClient.sendSocket.receive(packet);
                    if (icomUdpClient.onUdpEvents != null) {
                        byte[] temp = Arrays.copyOf(packet.getData(), packet.getLength());
                        icomUdpClient.onUdpEvents.OnReceiveData(icomUdpClient.sendSocket, packet, temp);
                    }
                    //Log.d(TAG, "receiveData:host ip: " + packet.getAddress().getHostName());
                } catch (IOException e) {
                    e.printStackTrace();
                    Log.e(TAG, "receiveData: error:" + e.getMessage());
                }

            }
            Log.e(TAG, "udpClient: is exit!");
            icomUdpClient.sendSocket.close();
            icomUdpClient.sendSocket = null;
        }
    }

}
