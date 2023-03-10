package com.ice.icyftpserver;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
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
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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
    private boolean isStartFtp = false;
    private Button btnSwitch;
    private ImageView imageView;
    private String defaultPath;
    private TextView tvTipText;
    private AlertDialog.Builder builder;
    private long exitTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSwitch = findViewById(R.id.btnSwitch);
        btnSwitch.setOnClickListener(this);
        imageView = findViewById(R.id.ivLogo);
        tvTipText = findViewById(R.id.tvTipText);
        requestPermissionSingle();
        createDefaultSPValue(false);
        initDialog();
    }

    private void createDefaultSPValue(boolean isForce) {
        defaultPath = Environment.getExternalStorageDirectory().getPath() + "/icyFtpFiles";
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        String workPath = sharedPreferences.getString("work_path", "");
        if (isForce) {
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("work_path", defaultPath);
            editor.apply();
        } else {
            if (workPath.equals("")) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("work_path", defaultPath);
                editor.apply();
            }
        }


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
            Toast.makeText(getApplicationContext(), "????????????????????????", Toast.LENGTH_SHORT).show();
        } else {
            Log.e("requestPermission", "request permission WRITE_EXTERNAL_STORAGE failed");
            Toast.makeText(getApplicationContext(), "????????????????????????", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            this.startActivity(intent);
            System.exit(0);
        }
    }

    private boolean mkdir(String path) {
        File dirFile = new File(path);
        Log.d("mkdir", "mkdir path:" + dirFile);
        if (dirFile.exists()) {
            Log.e("mkdir", "??????????????????");
            return true;
        } else {
            boolean mkdirs = dirFile.mkdirs();
            if (mkdirs) {
                Log.e("mkdir", "?????????????????????");
                return true;
            } else {
                Log.e("mkdir", "?????????????????????");
                return false;
            }
        }
    }

    private boolean startFtpServer(String ipAddress) {
        Log.d("startFtpServer", "????????????ftpServer");
        String userName = "anonymous";
        String password = "";
        int port = 2221;
//        SharedPreferences sharedPreferences = getSharedPreferences(getPackageName() + "_preferences", Context.MODE_PRIVATE);
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        boolean spIsAuth = sharedPreferences.getBoolean("is_auth", false);
        String spUsername = sharedPreferences.getString("username", "");
        String spPassword = sharedPreferences.getString("password", "");
        String workPath = sharedPreferences.getString("work_path", "");
        Log.e("startFtpServer", "isAuth: " + spIsAuth + ", spUsername:" + spUsername + ", spPassword:" + spPassword);
        if (spIsAuth && !spUsername.equals("")) {
            userName = spUsername;
            password = spPassword;
            Log.d("startFtpServer", "????????????");
        }
        if (!workPath.equals("")) {
            if (!mkdir(workPath)) {
                Toast.makeText(getApplicationContext(), "?????????????????????", Toast.LENGTH_SHORT).show();
                createDefaultSPValue(true);
                workPath = defaultPath;
            }
        }
        FtpServerFactory serverFactory = new FtpServerFactory();
        BaseUser baseUser = new BaseUser();
        baseUser.setName(userName);
        baseUser.setPassword(password);
        baseUser.setHomeDirectory(workPath);
        baseUser.setEnabled(true);

        List<Authority> authorities = new ArrayList<>();
        authorities.add(new WritePermission());
        baseUser.setAuthorities(authorities);
        try {
            serverFactory.getUserManager().save(baseUser);
        } catch (Exception e) {
            Log.e("startFtpServer", "serverFactory userManager save baseUser error");
            Toast.makeText(getApplicationContext(), "??????baseUser??????", Toast.LENGTH_SHORT).show();
            return false;
        }
        ListenerFactory listenerFactory = new ListenerFactory();
        if (ipAddress != null) {
            listenerFactory.setServerAddress(ipAddress);
        }
        listenerFactory.setPort(port);
        Listener listener = listenerFactory.createListener();
        serverFactory.addListener("default", listener);
        try {
            if (ftpServer != null) {
                ftpServer.stop();
            }
            ftpServer = serverFactory.createServer();
            ftpServer.start();
            Log.d("startFtpServer", "??????FTPServer??????");
            Toast.makeText(getApplicationContext(), "??????FTPServer??????", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "??????FTPServer??????", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }


    private boolean stopFtpServer() {
        try {
            if (ftpServer != null && !ftpServer.isStopped()) {
                ftpServer.stop();
                Toast.makeText(getApplicationContext(), "?????????FTP??????", Toast.LENGTH_SHORT).show();
                Log.d("stopFtpServer", "ftpServer stop successful");
            } else {
                Log.e("stopFtpServer", "ftpServer already stopped");
            }
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("stopFtpServer", "ftpServer stop error");
            Toast.makeText(getApplicationContext(), "??????FTP????????????", Toast.LENGTH_SHORT).show();
            return false;
        }
        return true;
    }

    // ??????
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int itemId = item.getItemId();
        if (itemId == R.id.menuSetting) {
            Intent intent = new Intent(MainActivity.this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menuAbout) {
            Intent intent = new Intent(MainActivity.this, AboutActivity.class);
            startActivity(intent);
            return true;
        } else if (itemId == R.id.menuQuit) {
            if (isStartFtp) {
                builder.show();
            } else {
                finish();
                System.exit(0);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if ((System.currentTimeMillis() - exitTime) > 2000) {
            Toast.makeText(getApplicationContext(), "????????????????????????", Toast.LENGTH_SHORT).show();
            exitTime = System.currentTimeMillis();
        } else {
            if (isStartFtp) {
                builder.show();
            } else {
                finish();
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (null != ftpServer) {
            ftpServer.stop();
            ftpServer = null;
        }
    }

    private void initDialog() {
        builder = new AlertDialog.Builder(this)
                .setTitle("??????").setMessage("????????????????????????????????????Ftp?????????????????????????????????????????????")
                .setPositiveButton("??????", (dialog, which) -> {
                    stopFtpServer();
                    finish();
                    dialog.dismiss();
                })
                .setNegativeButton("??????", (dialog, which) -> dialog.dismiss());
    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.btnSwitch) {
            if (!isMountSdCard()) {
                Toast.makeText(getApplicationContext(), "?????????SD??????????????????Ftp??????", Toast.LENGTH_SHORT).show();
                return;
            }
            if (isStartFtp) {
                boolean isStop = stopFtpServer();
                if (isStop) {
                    isStartFtp = false;
                    btnSwitch.setText("??????");
                    imageView.setImageResource(R.drawable.wifi2);
                    tvTipText.setText("");
                }
            } else {
                String ipAddress = getIPAddress(getApplicationContext());
                if (ipAddress == null) {
                    Toast.makeText(getApplicationContext(), "??????IP????????????", Toast.LENGTH_SHORT).show();
                }
                boolean isStart = startFtpServer(ipAddress);
                if (isStart) {
                    isStartFtp = true;
                    btnSwitch.setText("??????");
                    imageView.setImageResource(R.drawable.wifi1);
                    String tipTxt = "";
                    if (ipAddress == null) {
                        tipTxt = "ftp://?????????IP(???????????????????????????):2221";
                    } else {
                        tipTxt = "ftp://" + ipAddress + ":2221";
                    }
                    tvTipText.setText(tipTxt);
                }

            }
        }
    }


    public static String getIPAddress(Context context) {
        try {
            NetworkInfo info = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();
            if (info != null && info.isConnected()) {
                if (info.getType() == ConnectivityManager.TYPE_MOBILE) {//????????????2G/3G/4G??????
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
                        return null;
                    }
                } else if (info.getType() == ConnectivityManager.TYPE_WIFI) {//????????????????????????
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    WifiInfo wifiInfo = wifiManager.getConnectionInfo();
                    return intIP2StringIP(wifiInfo.getIpAddress());
                }
            } else {
                Log.e("getIPAddress", "?????????????????????,??????????????????");
                Toast.makeText(context.getApplicationContext(), "?????????????????????,??????????????????", Toast.LENGTH_SHORT).show();
                return null;
            }

        } catch (Exception e) {
            e.printStackTrace();
            Log.e("getIPAddress", "??????IP????????????");
        }
        return null;
    }

    public static String intIP2StringIP(int ip) {
        return (ip & 0xFF) + "." + ((ip >> 8) & 0xFF) + "." + ((ip >> 16) & 0xFF) + "." + (ip >> 24 & 0xFF);
    }

    public boolean isMountSdCard() {
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            Log.e("isMountSdCard", "?????????SD???");
        }
        return Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED);
    }
}