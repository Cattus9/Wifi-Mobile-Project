package com.project.inet_mobile;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

import com.airbnb.lottie.LottieAnimationView;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 3000; // 3 seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        LottieAnimationView lottieLoading = findViewById(R.id.lottie_loading);

        // Animasi Fade In Logo
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(logo, "alpha", 0f, 1f);
        fadeIn.setDuration(1000); // Durasi animasi 1 detik
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());
        fadeIn.start();

        // Lottie animation akan otomatis play karena autoPlay="true" di XML
        // Jika ingin kontrol manual, bisa gunakan:
        // lottieLoading.playAnimation();

        // Delay and then navigate to the login activity
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                startActivity(intent);
                finish(); // Close the splash activity
            }
        }, SPLASH_DURATION);
    }
}