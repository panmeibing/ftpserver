package com.example.myftpserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.preference.PreferenceManager;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.listener.Listener;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.WritePermission;

import java.io.File;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private FtpServer ftpServer;
    private Button btnStart, btnStop, btnSetting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnStart = findViewById(R.id.btnStart);
        btnStop = findViewById(R.id.btnStop);
        btnSetting = findViewById(R.id.btnSetting);
        btnStart.setOnClickListener(this);
        btnStop.setOnClickListener(this);
        btnSetting.setOnClickListener(this);
        requestPermissionSingle();
    }

    private void requestPermissionSingle() {
        if (ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        } else {
            Log.d("requestPermissionSingle", "already granted WRITE_EXTERNAL_STORAGE permission");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(getApplicationContext(), "授权读写权限成功", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("requestPermission", "request permission WRITE_EXTERNAL_STORAGE failed");
            Toast.makeText(getApplicationContext(), "请先授权读写权限", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
            System.exit(0);
        }
    }

    private boolean mkdir(String path) {
        String storagePath = Environment.getExternalStorageDirectory().getPath() + "/" + path;
        File dirFile = new File(storagePath);
        Log.d("mkdir", "mkdir path:" + dirFile);
        if (dirFile.exists()) {
            Log.e("mkdir", "文件夹已存在");
            return true;
        } else {
            boolean mkdirs = dirFile.mkdirs();
            if (mkdirs) {
                Log.e("mkdir", "文件夹创建成功");
                return true;
            } else {
                Log.e("mkdir", "文件夹创建失败");
                return false;
            }
        }
    }

    private void startFtpServer() {
        Log.d("startFtpServer", "尝试开启ftpServer");
        String ipAddress = getIPAddress(getApplicationContext());
        Log.e("startFtpServer", "ipAddress: " + ipAddress);
        String userName = "anonymous";
        String password = "";
        String path = "/";
        int port = 2221;
//        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean spIsAuth = sharedPreferences.getBoolean("is_auth", false);
        String spUsername = sharedPreferences.getString("username", "");
        String spPassword = sharedPreferences.getString("password", "");
        Log.e("startFtpServer", "isAuth: " + spIsAuth + ", spUsername:" + spUsername + ", spPassword:" + spPassword);
        Toast.makeText(getApplicationContext(), "isAuth: " + spIsAuth + ", spUsername:" + spUsername + ", spPassword:" + spPassword, Toast.LENGTH_SHORT).show();
        if (spIsAuth && !spUsername.equals("")) {
            userName = spUsername;
            password = spPassword;
            Log.d("startFtpServer", "使用认证");
        }
        if (!mkdir("pan")) {
            Toast.makeText(getApplicationContext(), "创建文件夹失败", Toast.LENGTH_SHORT).show();
        }
        FtpServerFactory serverFactory = new FtpServerFactory();
        BaseUser baseUser = new BaseUser();
        baseUser.setName(userName);
        baseUser.setPassword(password);
        baseUser.setHomeDirectory(path);

        List<Authority> authorities = new ArrayList<Authority>();
        authorities.add(new WritePermission());
        baseUser.setAuthorities(authorities);
        try {
            serverFactory.getUserManager().save(baseUser);
        } catch (Exception e) {
            Log.e("startFtpServer", "serverFactory userManager save baseUser error");
        }
        ListenerFactory listenerFactory = new ListenerFactory();
//        listenerFactory.setServerAddress();
        listenerFactory.setPort(port);
        Listener listener = listenerFactory.createListener();
        String address = listener.getServerAddress();
        Log.d("startFtpServer", "address:" + address + ", port:" + port);
        Toast.makeText(getApplicationContext(), "address:" + address + ", port:" + port, Toast.LENGTH_SHORT).show();
        serverFactory.addListener("default", listener);
        try {
            ftpServer = serverFactory.createServer();
            ftpServer.start();
            Log.d("startFtpServer", "开启FTPServer成功");
            Toast.makeText(getApplicationContext(), "开启FTPServer成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "start ftp server failed", Toast.LENGTH_SHORT).show();
        }

    }


    private void stopFtpServer() {
        try {
            if (!ftpServer.isStopped()) {
                ftpServer.stop();
                Toast.makeText(getApplicationContext(), "已关闭FTP服务", Toast.LENGTH_SHORT).show();
                Log.d("stopFtpServer", "ftpServer stop successful");
            } else {
                Log.d("stopFtpServer", "ftpServer already stopped");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.d("stopFtpServer", "ftpServer stop error");
        }
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnStart) {
            Log.d("onClick", "btnStart");
            startFtpServer();
        } else if (id == R.id.btnStop) {
            Log.d("onClick", "btnStop");
            stopFtpServer();
        } else if (id == R.id.btnSetting) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
        }
    }


    public static String getIPAddress(Context context) {
        NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
        if (info != null && info.isConnected()) {
            if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//当前使用2G/3G/4G网络
                try {
                    //Enumeration<NetworkInterface> en=NetworkInterface.getNetworkInterfaces();
                    for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements(); ) {
                        NetworkInterface intf = en.nextElement();
                        for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements(); ) {
                            InetAddress inetAddress = enumIpAddr.nextElement();
                            if (!inetAddress.isLoopbackAddress() && inetAddress instanceof Inet4Address) {
                                return inetAddress.getHostAddress();
                            }
                        }
                    }
                } catch (SocketException e) {
                    e.printStackTrace();
                }

            } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//当前使用无线网络
                WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                return intIP2StringIP(wifiInfo.getIpAddress());
            }
        } else {
            //当前无网络连接,请在设置中打开网络
            Log.e("getIPAddress", "当前无网络连接,请在设置中打开网络");
        }
        return null;
    }

    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." +
                ((ip >> 8) & 0xFF) + "." +
                ((ip >> 16) & 0xFF) + "." +
                (ip >> 24 & 0xFF);
    }
}