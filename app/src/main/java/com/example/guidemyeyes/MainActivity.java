package com.example.guidemyeyes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.navigation.NavController;
import androidx.navigation.NavDirections;
import androidx.navigation.fragment.NavHostFragment;

import android.os.Bundle;
import android.preference.PreferenceFragment;
import android.view.MenuItem;
import android.widget.Button;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_GuideMyEyes);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //Declare Fragments
        HomeFragment homeFragment = new HomeFragment();
        SettingsFragment settingsFragment = new SettingsFragment();
        GuideFragment guideFragment = new GuideFragment();

        FragmentTransaction fragmentTransaction = getSupportFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.fragment_frame_layout, homeFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        bottomNavigationView.setOnNavigationItemSelectedListener(
                item -> {
                    Fragment selectedFragment = null;
                    switch (item.getItemId()) {
                        case R.id.menuHome: {
                            selectedFragment = homeFragment;
                            break;
                        }
                        case R.id.menuSetting: {
                            selectedFragment = settingsFragment;
                            break;
                        }
                    }
                    if (selectedFragment != null && !selectedFragment.isVisible()) {
                        getSupportFragmentManager().beginTransaction().addToBackStack(null).replace(R.id.fragment_frame_layout,selectedFragment).commit();
                    }
                    return true;
                });
    }
}