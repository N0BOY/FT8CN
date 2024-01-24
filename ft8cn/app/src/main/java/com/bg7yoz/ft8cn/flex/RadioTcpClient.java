package com.bg7yoz.ft8cn.flex;
/**
 * 简单封装的Tcp类，用于Flex的命令操作
 * @author BGY70Z
 * @date 2023-03-20
 */

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RadioTcpClient {
    private static final String TAG = "RadioTcpClient";
    private static RadioTcpClient radioTcpClient = null;
    private String ip;
    private int port;
    public static final int MAX_BUFFER_SIZE=1024 * 32;

    private final ExecutorService sendByteThreadPool = Executors.newCachedThreadPool();
    private final SendByteRunnable sendByteRunnable=new SendByteRunnable(this);

    public static RadioTcpClient getInstance() {
        if (radioTcpClient == null) {
            synchronized (RadioTcpClient.class) {
                radioTcpClient = new RadioTcpClient();
            }
        }
        return radioTcpClient;
    }

    private Socket mSocket;
    private OutputStream mOutputStream;
    private InputStream mInputStream;

    private SocketThread mSocketThread;
    private boolean isStop = false;//thread flag

    private OnDataReceiveListener onDataReceiveListener = null;

    public boolean isConnect() {
        boolean flag = false;
        if (mSocket != null) {
            flag = mSocket.isConnected();
        }
        return flag;
    }

    public void connect(String ip, int port) {
        this.ip = ip;
        this.port = port;
        //mSocketThread = new SocketThread(ip, port);
        mSocketThread = new SocketThread();
        mSocketThread.start();
    }

    public void disconnect() {
        isStop = true;
        try {
            if (mOutputStream != null) {
                mOutputStream.close();
            }

            if (mInputStream != null) {
                mInputStream.close();
            }

            if (mSocket != null) {
                mSocket.close();
                mSocket = null;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mSocketThread != null) {
            mSocketThread.interrupt();//not intime destory thread,so need a flag
        }
    }


    private class SocketThread extends Thread {
        @Override
        public void run() {
            Log.d(TAG, "TcpSocketThread start...");
            super.run();
            try {
                if (mSocket != null) {
                    mSocket.close();
                    mSocket = null;
                }
                InetAddress ipAddress = InetAddress.getByName(ip);
                mSocket = new Socket(ipAddress, port);
                //设置不延时发送
                //mSocket.setTcpNoDelay(true);
                //设置输入输出缓冲流大小
                //mSocket.setSendBufferSize(8*1024);
                //mSocket.setReceiveBufferSize(8*1024);
                if (isConnect()) {
                    mOutputStream = mSocket.getOutputStream();
                    mInputStream = mSocket.getInputStream();


                    isStop = false;
                    connectSuccess();
                }
                /* 此处这样做没什么意义不大，真正的socket未连接还是靠心跳发送，等待服务端回应比较好，一段时间内未回应，则socket未连接成功 */
                else {
                    connectFail();
                    return;
                }

            }catch (SocketException e){
                Log.e(TAG,"TCP Connection exception:"+e.getMessage());
            }
            catch (IOException e) {
                connectFail();
                Log.e(TAG, "SocketThread connect io exception = " + e.getMessage());
                e.printStackTrace();
                return;
            }
            int errorCount=0;
            //read ...
            while (isConnect() && !isStop && !isInterrupted()) {
                int size;
                try {
                    byte[] buffer = new byte[MAX_BUFFER_SIZE];
                    if (mInputStream == null) return;
                    size = mInputStream.read(buffer);//null data -1 ,
                    if (size > 0) {
                        if (onDataReceiveListener != null) {
                            byte[] temp = Arrays.copyOf(buffer, size);
                            onDataReceiveListener.onDataReceive(temp);
                        }
                        errorCount =0;
                    }else {
                        errorCount ++;
                        if (errorCount > 10){
                            if (onDataReceiveListener!=null){
                                onDataReceiveListener.onConnectionClosed();
                            }
                        }
                    }

                } catch (SocketException e){
                    Log.e(TAG,"Tcp Connection exception:"+e.getMessage());
                } catch (IOException e) {
                    //uiHandler.sendEmptyMessage(-1);
                    Log.e(TAG, "SocketThread read io exception = " + e.getMessage());
                    e.printStackTrace();
                    return;
                }
            }
        }
    }

    private void connectFail() {
        if (onDataReceiveListener != null) {
            onDataReceiveListener.onConnectFail();
        }
    }

    private void connectSuccess() {
        if (onDataReceiveListener != null) {
            onDataReceiveListener.onConnectSuccess();
        }
    }

    /**
     * send byte[] cmd
     * Exception : android.os.NetworkOnMainThreadException
     */
    public synchronized void sendByte(final byte[] mBuffer) {
        sendByteRunnable.mBuffer=mBuffer;
        sendByteThreadPool.execute(sendByteRunnable);
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    if (mOutputStream != null) {
//                        mOutputStream.write(mBuffer);
//                        mOutputStream.flush();
//                    }
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }

    private static class SendByteRunnable implements Runnable{
        RadioTcpClient client;
        byte[] mBuffer;
        public SendByteRunnable(RadioTcpClient client) {
            this.client = client;
        }

        @Override
        public void run() {
            try {
                if (mBuffer==null) return;
                if (client.mOutputStream != null) {
                    client.mOutputStream.write(mBuffer);
                    client.mOutputStream.flush();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public interface OnDataReceiveListener {
        void onConnectSuccess();

        void onConnectFail();

        void onDataReceive(byte[] buffer);
        void onConnectionClosed();
    }

    public void setOnDataReceiveListener(
            OnDataReceiveListener dataReceiveListener) {
        onDataReceiveListener = dataReceiveListener;
    }

    public String getIp() {
        return ip;
    }

    public int getPort() {
        return port;
    }
}
