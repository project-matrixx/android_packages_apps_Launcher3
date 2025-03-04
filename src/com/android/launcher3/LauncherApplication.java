/*
 * Copyright (C) 2023 The Android Open Source Project
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
package com.android.launcher3;

import android.app.Application;

import com.android.launcher3.dagger.DaggerLauncherAppComponent;
import com.android.launcher3.dagger.LauncherAppComponent;
import com.android.launcher3.dagger.LauncherBaseAppComponent;

/**
 * Main application class for Launcher
 */
public class LauncherApplication extends Application {

    private LauncherBaseAppComponent mAppComponent;
    @Override
    public void onCreate() {
        super.onCreate();
        MainProcessInitializer.initialize(this);
        initDagger();
    }

    public LauncherAppComponent getAppComponent() {
        // Since supertype setters will return a supertype.builder and @Component.Builder types
        // must not have any generic types.
        // We need to cast mAppComponent to {@link LauncherAppComponent} since appContext()
        // method is defined in the super class LauncherBaseComponent#Builder.
        return (LauncherAppComponent) mAppComponent;
    }

    protected void initDagger() {
        mAppComponent = DaggerLauncherAppComponent.builder().appContext(this).build();
    }
}
