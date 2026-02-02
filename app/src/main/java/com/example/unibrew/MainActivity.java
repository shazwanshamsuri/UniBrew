package com.example.unibrew;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Start with the Home (List) screen
        getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, new HomeFragment()).commit();

        bottomNav.setOnItemSelectedListener(item -> {
            Fragment selected = null;
            if (item.getItemId() == R.id.nav_home) {
                selected = new HomeFragment();
            } else if (item.getItemId() == R.id.navigation_map) {
                // Since you have 'activity_map_view.xml', I assume your class is named MapViewActivity
                Intent intent = new Intent(MainActivity.this, MapActivity.class);
                startActivity(intent);
                return false;
            }else if (item.getItemId() == R.id.nav_profile) {
                selected = new ProfileFragment(); // We create this next!

            }


            if (selected != null) {
                getSupportFragmentManager().beginTransaction().replace(R.id.fragment_container, selected).commit();
            }
            return true;
        });
    }
}