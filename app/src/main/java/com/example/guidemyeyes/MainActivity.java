package com.example.guidemyeyes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_GuideMyEyes);
        super.onCreate(savedInstanceState);

        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
        setContentView(R.layout.activity_main);

        //Declare Fragments
        HomeFragment homeFragment = new HomeFragment();
        SettingsFragment settingsFragment = new SettingsFragment();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_frame_layout, homeFragment);
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                item -> {
                    switch (item.getItemId()) {
                        case R.id.menuHome: {
                            if (!homeFragment.isVisible()) {
                                getSupportFragmentManager()
                                        .beginTransaction()
                                        .setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_right)
                                        .addToBackStack(null)
                                        .replace(R.id.fragment_frame_layout, homeFragment)
                                        .commit();
                            }
                            break;
                        }
                        case R.id.menuSetting: {
                            if (!settingsFragment.isVisible()) {
                                getSupportFragmentManager()
                                        .beginTransaction()
                                        .setCustomAnimations(R.anim.slide_in_left, R.anim.slide_out_left)
                                        .addToBackStack(null)
                                        .replace(R.id.fragment_frame_layout, settingsFragment)
                                        .commit();
                            }
                            break;
                        }
                    }
                    return true;
                });

        FloatingActionButton button = (FloatingActionButton) findViewById(R.id.guideButton);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, GuideActivity.class);
                MainActivity.this.startActivity(intent);
            }
        });

    }
}