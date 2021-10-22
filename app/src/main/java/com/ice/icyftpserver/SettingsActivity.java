package com.ice.icyftpserver;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle("设置");
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }


    // 点击返回图标事件
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            this.finish();
        }
        return super.onOptionsItemSelected(item);
    }


    public static class SettingsFragment extends PreferenceFragmentCompat implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            Preference sPWitchIsAuth = findPreference("is_auth");
            Preference sPEditUsername = findPreference("username");
            Preference sPEditPassword = findPreference("password");
            if (sPEditUsername != null) {
                sPEditUsername.setOnPreferenceChangeListener(this);
            }
            if (sPEditPassword != null) {
                sPEditPassword.setOnPreferenceChangeListener(this);
            }
            if (sPWitchIsAuth != null) {
                sPWitchIsAuth.setOnPreferenceChangeListener((preference, newValue) -> {
                    Log.d("sPWitchIsAuth", " sPWitchIsAuth onPreferenceChange: " + newValue.toString());
                    if (newValue.toString().equals("true")) {
                        Log.e("onPreferenceChange", "is_auth == true");
                        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(requireActivity().getApplicationContext());
                        String spUsername = sharedPreferences.getString("username", "");
                        if (spUsername.equals("")) {
                            SharedPreferences.Editor editor = sharedPreferences.edit();
                            editor.putString("username", "admin");
                            editor.putString("password", "12345");
                            editor.apply();
                        }
                    }
                    Toast.makeText(getContext(), "修改成功，请重启Ftp服务", Toast.LENGTH_SHORT).show();
                    return true;
                });
            }
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            String key = preference.getKey();
            if (key.equals("username")) {
                if (newValue == null || TextUtils.isEmpty(newValue.toString())) {
                    Toast.makeText(getContext(), "修改失败,用户名不能为空", Toast.LENGTH_SHORT).show();
                    return false;
                } else if (!validPassword(newValue.toString())) {
                    Toast.makeText(getContext(), "请输入5~16位字母或数字或其组合", Toast.LENGTH_SHORT).show();
                    return false;
                }
                Toast.makeText(getContext(), "修改成功", Toast.LENGTH_SHORT).show();
                return true;
            } else if (key.equals("password")) {
                if (newValue == null || TextUtils.isEmpty(newValue.toString())) {
                    Toast.makeText(getContext(), "修改失败，密码不能为空", Toast.LENGTH_SHORT).show();
                    return false;
                } else if (!validPassword(newValue.toString())) {
                    Toast.makeText(getContext(), "请输入5~16位字母或数字或其组合", Toast.LENGTH_SHORT).show();
                    return false;
                }
                Toast.makeText(getContext(), "修改成功", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        }

        public boolean validPassword(String pwd) {
            String regEx = "^[a-zA-Z0-9]{5,16}$";
            Pattern p = Pattern.compile(regEx);
            Matcher m = p.matcher(pwd);
            return m.find();
        }
    }


}