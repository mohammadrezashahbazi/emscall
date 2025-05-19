package com.example.emergencyapp;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.speech.tts.TextToSpeech;
import android.view.View;
import android.widget.Button;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FusedLocationProviderClient fusedLocationClient;
    private TextToSpeech tts;
    private double latitude = 0.0, longitude = 0.0;
    private static final String EMERGENCY_NUMBER = "115";  // عدد اورژانس خود را اینجا بنویسید

    // لانچر برای درخواست چند مجوز به‌صورت همزمان
    private final ActivityResultLauncher<String[]> requestPermsLauncher =
        registerForActivityResult(new RequestMultiplePermissions(), this::onPermsResult);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // ۱) init location & TTS
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
        tts = new TextToSpeech(this, status -> {
            if (status == TextToSpeech.SUCCESS) {
                tts.setLanguage(Locale.US);
            }
        });

        // ۲) دکمه‌ی اضطراری
        Button btn = findViewById(R.id.btnEmergency);
        btn.setOnClickListener(v -> startEmergencyProcedure());

        // ۳) درخواست اولیه مجوزها
        requestNecessaryPermissions();
    }

    private void requestNecessaryPermissions() {
        String[] perms = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.RECORD_AUDIO
        };
        boolean need = false;
        for (String p : perms) {
            if (ContextCompat.checkSelfPermission(this, p) != PackageManager.PERMISSION_GRANTED) {
                need = true;
                break;
            }
        }
        if (need) {
            requestPermsLauncher.launch(perms);
        }
    }

    private void onPermsResult(Map<String, Boolean> result) {
        // می‌توانید بررسی کنید که همه‌ی مجوزها داده شده‌اند یا نه
    }

    private void startEmergencyProcedure() {
        // ۱) گرفتن لوکیشن
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestNecessaryPermissions();
            return;
        }
        fusedLocationClient.getLastLocation()
            .addOnSuccessListener(loc -> {
                if (loc != null) {
                    latitude = loc.getLatitude();
                    longitude = loc.getLongitude();
                }
                // ۲) تماس و پخش پیام
                callAndSpeak();
            });
    }

    private void callAndSpeak() {
        // تماس
        Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + EMERGENCY_NUMBER));
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        startActivity(callIntent);

        //  پخش پیام پس از ۵ ثانیه (تا تماس برقرار شود)
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (am != null) {
                am.setMode(AudioManager.MODE_IN_CALL);
                am.setSpeakerphoneOn(true);
            }
            String text = "This is an emergency. Please send help. "
                        + "My location is latitude " + latitude
                        + " and longitude " + longitude + ".";
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "EMERGENCY_ID");
        }, 5000);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (tts != null) {
            tts.shutdown();
        }
    }
}
