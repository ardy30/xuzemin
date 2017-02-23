package com.android.jdrd.headcontrol.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;

import com.android.jdrd.headcontrol.util.Constant;
import com.android.jdrd.headcontrol.util.Contact;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerSocketUtil extends Service {

    private static ServerSocket serverSocket;
    private static Socket socket1;
    private static Socket socket2;
    private static InputStream in1 = null;
    private static InputStream in2 = null;
    private static OutputStream out1 = null;
    private static OutputStream out2 = null;
    static String ip = null;

    private static String msg = null;
    public static Intent intent;
    private MyReceiver receiver;
    IntentFilter filter;

    @Override
    public void onCreate() {
        super.onCreate();

        intent = new Intent();

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    startServerSocket(Constant.ServerPort);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

        receiver = new MyReceiver();
        filter = new IntentFilter();
        filter.addAction("com.jdrd.fragment.Map");
        registerReceiver(receiver, filter);
    }

    public class MyReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            String camera = intent.getStringExtra("camera");
            Contact.debugLog("收到摄像头数据" + camera);
            if(camera !=null){
                try {
                    sendDateToClient(camera);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public String startServerSocket(int port) throws IOException {

        serverSocket = new ServerSocket(port);
        Socket socket;
        Contact.debugLog("serverSocket is create....");

        while (true) {
            Contact.debugLog("waiting for connect....");
            socket = serverSocket.accept();
            new Thread(new Task(socket)).start();
        }
    }

    /*public static void sendDateToClient(String str, String ip) throws IOException {

        String str2 = "*" + str + "#";
        if (ip.equals("/192.168.1.100")) {
            try {
                out1.write(str2.getBytes());
                out2.write(str2.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (ip.equals("/192.168.1.102")) {
            try {
                out1.write(str2.getBytes());
                out2.write(str2.getBytes());
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Contact.debugLog("IP不对");
        }
    }*/

    public static void sendDateToClient(String str) throws IOException {
        String str2 = "*" + str + "#";

        try {
            Contact.debugLog("ip"+ip);
            if(ip !=null) {
                if (ip.equals("/192.168.88.101")) {
                    if (out1 != null) {
                        out1.write(str2.getBytes());
                    }
                    if (out2 != null) {
                        out2.write(str2.getBytes());
                    }

                } else if (ip.equals("/192.168.1.102")) {
                    if (out1 != null) {
                        out1.write(str2.getBytes());
                    }
                    if (out2 != null) {
                        out2.write(str2.getBytes());
                    }

                } else {
                    Contact.debugLog("IP不对");
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }catch (Exception e){
            e.printStackTrace();
        }

    }


    class Task implements Runnable {
        private Socket socket;

        public Task(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            ip = socket.getInetAddress().toString();
            Contact.debugLog(ip);

            if ("/192.168.88.101".equals(ip)) {
                socket1 = socket;
            } else if ("/192.168.1.102".equals(ip)) {
                socket2 = socket;
            } else {
                Contact.debugLog("IP不对");
            }

            try {

                if (ip.equals("/192.168.88.101")) {
                    in1 = socket1.getInputStream();
                    out1 = socket1.getOutputStream();
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            inputStreamParse(in1);
                        }
                    }).start();

                } else if (ip.equals("/192.168.1.102")) {
                    in2 = socket2.getInputStream();
                    out2 = socket2.getOutputStream();

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            inputStreamParse(in2);
                        }
                    }).start();

                } else {
                    Contact.debugLog("流为空");
                }

            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public void inputStreamParse(InputStream in) {
        byte[] buffer = new byte[1024];

        while (true) {
            int len = 0;
            try {
                len = in.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }

            if (len == -1) {
                break;
            }

            msg = new String(buffer, 0, len);

            if (msg != null) {
                Contact.debugLog("msg = " + msg.toString() + "  ip地址： " + ip);
            } else {
                Contact.debugLog("hehe, msg为空");
            }

            intent.putExtra("msg", msg);
            intent.setAction("com.jdrd.fragment.Map");
            sendBroadcast(intent);
        }
    }

    public static String getJSONString(String str) {
        JSONObject object = null;
        String jsonString = null;
        try {
            object = new JSONObject(msg);
            jsonString = object.getString("str");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonString;
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

}