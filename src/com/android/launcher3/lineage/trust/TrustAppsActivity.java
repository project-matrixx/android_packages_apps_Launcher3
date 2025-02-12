/*
 * Copyright (C) 2019-2024 The LineageOS Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.android.launcher3.lineage.trust;

import static com.android.launcher3.lineage.trust.db.TrustComponent.Kind.HIDDEN;
import static com.android.launcher3.lineage.trust.db.TrustComponent.Kind.PROTECTED;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.launcher3.LauncherAppState;
import com.android.launcher3.LauncherPrefs;
import com.android.launcher3.R;
import com.android.launcher3.Utilities;
import com.android.launcher3.lineage.trust.db.TrustComponent;

import com.google.android.material.color.DynamicColors;

import com.android.settingslib.collapsingtoolbar.CollapsingToolbarBaseActivity;

import java.util.List;

public class TrustAppsActivity extends CollapsingToolbarBaseActivity implements
        TrustAppsAdapter.Listener,
        LoadTrustComponentsTask.Callback,
        UpdateItemTask.UpdateCallback {

    private static final String KEY_TRUST_ONBOARDING = "pref_trust_onboarding";

    private RecyclerView mRecyclerView;
    private LinearLayout mLoadingView;
    private ProgressBar mProgressBar;

    private TrustAppsAdapter mAdapter;
    private AppLockHelper mAppLockHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstance) {
        super.onCreate(savedInstance);

        setupEdgeToEdge();
        setContentView(R.layout.activity_hidden_apps);
        DynamicColors.applyToActivityIfAvailable(this);
        setTheme(com.android.settingslib.widget.theme.R.style.Theme_SubSettingsBase);

        mRecyclerView = findViewById(R.id.hidden_apps_list);
        mLoadingView = findViewById(R.id.hidden_apps_loading);
        mLoadingView.setVisibility(View.VISIBLE);
        mProgressBar = findViewById(R.id.hidden_apps_progress_bar);

        ViewCompat.setOnApplyWindowInsetsListener(mRecyclerView, (view, insets) -> {
            Insets systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            view.setPadding(
                view.getPaddingLeft(),
                systemInsets.top,
                view.getPaddingRight(),
                systemInsets.bottom
            );
            return WindowInsetsCompat.CONSUMED;
        });

        final boolean hasSecureKeyguard = Utilities.hasSecureKeyguard(this);
        mAdapter = new TrustAppsAdapter(this, this, hasSecureKeyguard);
        mAppLockHelper = AppLockHelper.getInstance(this);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mRecyclerView.setItemAnimator(new DefaultItemAnimator());
        mRecyclerView.setAdapter(mAdapter);

        showOnBoarding(false);

        new LoadTrustComponentsTask(mAppLockHelper, getPackageManager(), this, this).execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater menuInflater = getMenuInflater();
        menuInflater.inflate(R.menu.menu_trust_apps, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            finish();
            return true;
        } else if (id == R.id.menu_trust_help) {
            showOnBoarding(true);
            return true;
        } else {
            return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onHiddenItemChanged(@NonNull TrustComponent component) {
        new UpdateItemTask(mAppLockHelper, this, HIDDEN).execute(component);
    }

    @Override
    public void onProtectedItemChanged(@NonNull TrustComponent component) {
        new UpdateItemTask(mAppLockHelper, this, PROTECTED).execute(component);
    }

    @Override
    public void onUpdated(boolean result) {
        LauncherAppState.INSTANCE.executeIfCreated(app -> app.getModel().forceReload());
    }

    @Override
    public void onLoadListProgress(int progress) {
        mProgressBar.setProgress(progress);
    }

    @Override
    public void onLoadCompleted(List<TrustComponent> result) {
        mLoadingView.setVisibility(View.GONE);
        mRecyclerView.setVisibility(View.VISIBLE);
        mAdapter.update(result);
    }

    private void setupEdgeToEdge() {
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content),
                (v, windowInsets) -> {
                    Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());
                    // Apply the insets paddings to the view.
                    v.setPadding(insets.left, insets.top, insets.right, insets.bottom);

                    // Return CONSUMED if you don't want the window insets to keep being
                    // passed down to descendant views.
                    return WindowInsetsCompat.CONSUMED;
                });
    }

    private void showOnBoarding(boolean forceShow) {
        SharedPreferences preferenceManager = LauncherPrefs.getPrefs(this);
        if (!forceShow && preferenceManager.getBoolean(KEY_TRUST_ONBOARDING, false)) {
            return;
        }

        preferenceManager.edit()
                .putBoolean(KEY_TRUST_ONBOARDING, true)
                .apply();

        new AlertDialog.Builder(this)
                .setView(R.layout.dialog_trust_welcome)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }
}
