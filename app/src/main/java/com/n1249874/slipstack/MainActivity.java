package com.n1249874.slipstack;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

import com.google.android.material.navigation.NavigationBarView;
import com.n1249874.slipstack.databinding.ActivityMainBinding;
import com.n1249874.slipstack.fragments.HomeFragment;
import com.n1249874.slipstack.fragments.SettingsFragment;
import com.n1249874.slipstack.fragments.SplitFragment;
import com.n1249874.slipstack.fragments.TrendsFragment;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        if (savedInstanceState == null) {
            loadFragment(new HomeFragment());
        }

        NavigationBarView.OnItemSelectedListener navigationListener = item -> {
            Fragment selectedFragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                selectedFragment = new HomeFragment();
            } else if (itemId == R.id.nav_trends) {
                selectedFragment = new TrendsFragment();
            } else if (itemId == R.id.nav_split) {
                selectedFragment = new SplitFragment();
            } else if (itemId == R.id.nav_settings) {
                selectedFragment = new SettingsFragment();
            }

            if (selectedFragment != null) {
                loadFragment(selectedFragment);
                return true;
            }
            return false;
        };
        binding.bottomNavigation.setOnItemSelectedListener(navigationListener);

        java.util.concurrent.Executors.newSingleThreadExecutor().execute(() -> {
            try {
                com.n1249874.slipstack.database.AppDatabase db = com.n1249874.slipstack.database.AppDatabase.getInstance(this);
                androidx.sqlite.db.SupportSQLiteDatabase sqlite = db.getOpenHelper().getWritableDatabase();
                sqlite.execSQL("PRAGMA foreign_keys = ON;");
                sqlite.execSQL("DELETE FROM line_items WHERE receiptId NOT IN (SELECT id FROM receipts)");
            } catch (Exception ignored) {}
        });
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}