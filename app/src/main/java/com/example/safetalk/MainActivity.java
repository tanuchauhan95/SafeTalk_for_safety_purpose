package com.example.safetalk;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.telephony.SmsManager;
import android.util.Base64;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.security.Key;
import java.util.ArrayList;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

public class MainActivity extends AppCompatActivity {

    Button sosBtn, voiceBtn;

    // 🔐 SECRET KEY (must be 16 characters)
    private static final String SECRET_KEY = "1234567890123456";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        sosBtn = findViewById(R.id.sosBtn);
        voiceBtn = findViewById(R.id.voiceBtn);

        // 🚨 SOS BUTTON
        sosBtn.setOnClickListener(v -> sendSOS());

        // 🎤 VOICE BUTTON
        voiceBtn.setOnClickListener(v -> startVoiceRecognition());

        // 🔐 Request permissions at start
        requestPermissions();
    }

    // 🔐 PERMISSION REQUEST
    private void requestPermissions() {
        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.SEND_SMS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.RECORD_AUDIO
        }, 1);
    }

    // 🎤 Start Voice Recognition
    private void startVoiceRecognition() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say 'help' to trigger SOS");

        startActivityForResult(intent, 1);
    }

    // 🔐 AES ENCRYPTION
    private String encrypt(String data) {
        try {
            Key key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.ENCRYPT_MODE, key);

            byte[] encrypted = cipher.doFinal(data.getBytes());
            return Base64.encodeToString(encrypted, Base64.DEFAULT);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 🔓 AES DECRYPTION (for testing)
    private String decrypt(String data) {
        try {
            Key key = new SecretKeySpec(SECRET_KEY.getBytes(), "AES");
            Cipher cipher = Cipher.getInstance("AES");
            cipher.init(Cipher.DECRYPT_MODE, key);

            byte[] decoded = Base64.decode(data, Base64.DEFAULT);
            byte[] decrypted = cipher.doFinal(decoded);

            return new String(decrypted);

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // 🚨 SEND SOS
    private void sendSOS() {

        LocationManager lm = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        try {
            if (ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions();
                return;
            }

            Location location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location == null) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }

            if (location != null) {

                double lat = location.getLatitude();
                double lon = location.getLongitude();

                String originalMessage = "SOS! Help me. My location: https://maps.google.com/?q="
                        + lat + "," + lon;

                String encryptedMessage = encrypt(originalMessage);

                if (encryptedMessage == null) {
                    Toast.makeText(this, "Encryption failed", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                    requestPermissions();
                    return;
                }

                SmsManager smsManager = SmsManager.getDefault();
                smsManager.sendTextMessage("916397184848", null, encryptedMessage, null, null);

                String decrypted = decrypt(encryptedMessage);

                Toast.makeText(this,
                        "SOS Sent!\n\nEncrypted:\n" + encryptedMessage +
                                "\n\nDecrypted:\n" + decrypted,
                        Toast.LENGTH_LONG).show();

            } else {
                Toast.makeText(this, "Location not found", Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Error sending SOS", Toast.LENGTH_SHORT).show();
        }
    }

    // 🎤 VOICE RESULT
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == 1 && resultCode == RESULT_OK && data != null) {

            ArrayList<String> result =
                    data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);

            if (result != null && !result.isEmpty()) {

                String spokenText = result.get(0).toLowerCase();

                Toast.makeText(this, spokenText, Toast.LENGTH_LONG).show();

                if (spokenText.contains("help")) {
                    Toast.makeText(this, "Voice SOS Triggered!", Toast.LENGTH_SHORT).show();
                    sendSOS();
                } else {
                    Toast.makeText(this, "Wrong code", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
}