/*
 * Copyright 2023 Samsung Electronics Co., Ltd. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.samsung.android.ecgmonitor;

import static android.content.pm.PackageManager.PERMISSION_DENIED;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;

import com.samsung.android.ecgmonitor.databinding.ActivityMainBinding;
import com.samsung.android.service.health.tracking.ConnectionListener;
import com.samsung.android.service.health.tracking.HealthTracker;
import com.samsung.android.service.health.tracking.HealthTrackerException;
import com.samsung.android.service.health.tracking.HealthTrackingService;
import com.samsung.android.service.health.tracking.data.DataPoint;
import com.samsung.android.service.health.tracking.data.HealthTrackerType;
import com.samsung.android.service.health.tracking.data.ValueKey;


import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends Activity {

    private final String APP_TAG = "ECG Monitor";
    private String permission;
    private final Handler ecgHandler = new Handler(Looper.getMainLooper());
    private final AtomicBoolean isMeasurementRunning = new AtomicBoolean(false);
    private final AtomicReference<Float> curEcg = new AtomicReference<>();
    private final int MEASUREMENT_DURATION = 60000;
    private final int MEASUREMENT_TICK = 1000;
    private final AtomicBoolean leadOff = new AtomicBoolean(true);

    private final HealthTracker.TrackerEventListener ecgListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.isEmpty())
                return;
            final int isLeadOff = list.get(0).getValue(ValueKey.EcgSet.LEAD_OFF);
            final int NO_CONTACT = 5;
            if (isLeadOff == NO_CONTACT) {
                leadOff.set(true);
                return;
            } else
                leadOff.set(false);
            curEcg.set(list.get(list.size() - 1).getValue(ValueKey.EcgSet.ECG_MV)); // График ЭКГ кек
        }

        @Override
        public void onFlushCompleted() {
            Log.i(APP_TAG, " onFlushCompleted called");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(APP_TAG, " onError called");
            if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.NoPermission), Toast.LENGTH_SHORT).show());
            }
            if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show());
            }
        }
    };
    private final HealthTracker.TrackerEventListener heartTrackerListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.isEmpty())
                return;
            for (DataPoint data : list) {
                final int status = data.getValue(ValueKey.HeartRateSet.HEART_RATE_STATUS);
                int heartRateValue = data.getValue(ValueKey.HeartRateSet.HEART_RATE);
                Log.i("heartTrackerListener", "status " + status + "heartRateValue " + heartRateValue);

            }
        }

        @Override
        public void onFlushCompleted() {
            Log.i(APP_TAG, " onFlushCompleted called");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(APP_TAG, " onError called");
            if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.NoPermission), Toast.LENGTH_SHORT).show());
            }
            if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show());
            }
        }
    };
    private final HealthTracker.TrackerEventListener sweatTrackerListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.isEmpty())
                return;
            for (DataPoint data : list) {
                final int status = data.getValue(ValueKey.SweatLossSet.STATUS);
                float SWEAT_LOSS = data.getValue(ValueKey.SweatLossSet.SWEAT_LOSS);

                Log.i("sweatTrackerListener", "status " + status + "SWEAT_LOSS " + SWEAT_LOSS);

            }
        }

        @Override
        public void onFlushCompleted() {
            Log.i(APP_TAG, " onFlushCompleted called");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(APP_TAG, " onError called");
            if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.NoPermission), Toast.LENGTH_SHORT).show());
            }
            if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show());
            }
        }
    };
    private final HealthTracker.TrackerEventListener spo2TrackerListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.isEmpty())
                return;
            for (DataPoint data : list) {
                final int status = data.getValue(ValueKey.SpO2Set.STATUS);
                float SPO2 = data.getValue(ValueKey.SpO2Set.SPO2);
                float HEART_RATE = data.getValue(ValueKey.SpO2Set.HEART_RATE);
                float ACCURACY_FLAG = data.getValue(ValueKey.SpO2Set.ACCURACY_FLAG);
                Log.i("spo2TrackerListener", "status " + status + "SPO2 " + SPO2 + "HEART_RATE " + HEART_RATE + "ACCURACY_FLAG " + ACCURACY_FLAG);
            }

        }

        @Override
        public void onFlushCompleted() {
            Log.i(APP_TAG, " onFlushCompleted called");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(APP_TAG, " onError called");
            if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.NoPermission), Toast.LENGTH_SHORT).show());
            }
            if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show());
            }
        }
    };
    private final HealthTracker.TrackerEventListener skinTemperatureTrackerListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.isEmpty())
                return;
            for (DataPoint data : list) {
                final int status = data.getValue(ValueKey.SkinTemperatureSet.STATUS);
                float AMBIENT_TEMPERATURE = data.getValue(ValueKey.SkinTemperatureSet.AMBIENT_TEMPERATURE);
                float OBJECT_TEMPERATURE = data.getValue(ValueKey.SkinTemperatureSet.OBJECT_TEMPERATURE);
                Log.i("skinTemperatureTrackerListener", "status " + status + "AMBIENT_TEMPERATURE " + AMBIENT_TEMPERATURE + "OBJECT_TEMPERATURE " + OBJECT_TEMPERATURE);
            }
        }

        @Override
        public void onFlushCompleted() {
            Log.i(APP_TAG, " onFlushCompleted called");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(APP_TAG, " onError called");
            if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.NoPermission), Toast.LENGTH_SHORT).show());
            }
            if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show());
            }
        }
    };
    private final HealthTracker.TrackerEventListener edaTrackerListener = new HealthTracker.TrackerEventListener() {
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.isEmpty())
                return;
            for (DataPoint data : list) {
                final int status = data.getValue(ValueKey.EdaSet.STATUS);
                float SKIN_CONDUCTANCE = data.getValue(ValueKey.EdaSet.SKIN_CONDUCTANCE);
                Log.i("edaTrackerListener", "status " + status + "SKIN_CONDUCTANCE " + SKIN_CONDUCTANCE);
            }
        }

        @Override
        public void onFlushCompleted() {
            Log.i(APP_TAG, " onFlushCompleted called");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(APP_TAG, " onError called");
            if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.NoPermission), Toast.LENGTH_SHORT).show());
            }
            if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show());
            }
        }
    };
    private final HealthTracker.TrackerEventListener acccelerometrTrackerListener = new HealthTracker.TrackerEventListener() {

        // фильтрация гравитации
        private final float[] gravity = new float[3];
        private final float[] linearAcc = new float[3];

        private float lastDynMag = 0f;
        private long lastStepTime = 0L;
        private int stepCount = 0;

        // настройки — потом подберёшь по логам
        private static final float STEP_THRESHOLD = 1.2f;        // порог «силы шага»
        private static final long STEP_MIN_INTERVAL_MS = 300;    // мин. интервал между шагами
        private static final long STEP_MAX_INTERVAL_MS = 2000;   // макс. интервал (дольше — не считаем как шаг)
        private static final float GRAVITY_ALPHA = 0.9f;         // фильтр гравитации
        @Override
        public void onDataReceived(@NonNull List<DataPoint> list) {
            if (list.isEmpty())
                return;
            for (DataPoint data : list) {
                final int ACCELEROMETER_X = data.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_X);
                final int ACCELEROMETER_Y = data.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_Y);
                final int ACCELEROMETER_Z = data.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_Z);

                float ax = data.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_X);
                float ay = data.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_Y);
                float az = data.getValue(ValueKey.AccelerometerSet.ACCELEROMETER_Z);

                // 1. Оценка гравитации (низкочастотный фильтр)
                gravity[0] = GRAVITY_ALPHA * gravity[0] + (1 - GRAVITY_ALPHA) * ax;
                gravity[1] = GRAVITY_ALPHA * gravity[1] + (1 - GRAVITY_ALPHA) * ay;
                gravity[2] = GRAVITY_ALPHA * gravity[2] + (1 - GRAVITY_ALPHA) * az;

                // 2. Динамическое ускорение (без g)
                linearAcc[0] = ax - gravity[0];
                linearAcc[1] = ay - gravity[1];
                linearAcc[2] = az - gravity[2];

                float dynMag = (float) Math.sqrt(
                        linearAcc[0] * linearAcc[0] +
                                linearAcc[1] * linearAcc[1] +
                                linearAcc[2] * linearAcc[2]
                );

                // 3. Время из DataPoint (лучше, чем System.currentTimeMillis)
                long now = data.getTimestamp(); // если timestamp в нс — делишь на 1_000_000 для мс
                // long now = data.getTimestamp() / 1_000_000L;

                // 4. Антидребезг по времени
                long dt = now - lastStepTime;
                if (dt < STEP_MIN_INTERVAL_MS || dt > STEP_MAX_INTERVAL_MS) {
                    lastDynMag = dynMag;
                    return;
                }

                // 5. Поиск пересечения порога снизу вверх (локальный пик)
                if (lastDynMag < STEP_THRESHOLD && dynMag >= STEP_THRESHOLD) {
                    stepCount++;
                    lastStepTime = now;

                    Log.d("edaTrackerListener", "step=" + stepCount +
                            " dynMag=" + dynMag +
                            " dt=" + dt);

                    // если хочешь пробрасывать в общий логгер:
                    // trackerDataSubject.notifyMetric("STEPS", stepCount);
                }

                lastDynMag = dynMag;

                Log.i("edaTrackerListener", "ACCELEROMETER_X " + ACCELEROMETER_X + "ACCELEROMETER_Y " + ACCELEROMETER_Y + "ACCELEROMETER_Z " + ACCELEROMETER_Z);
            }
        }

        @Override
        public void onFlushCompleted() {
            Log.i(APP_TAG, " onFlushCompleted called");
        }

        @Override
        public void onError(HealthTracker.TrackerError trackerError) {
            Log.i(APP_TAG, " onError called");
            if (trackerError == HealthTracker.TrackerError.PERMISSION_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.NoPermission), Toast.LENGTH_SHORT).show());
            }
            if (trackerError == HealthTracker.TrackerError.SDK_POLICY_ERROR) {
                runOnUiThread(() -> Toast.makeText(getApplicationContext(),
                        getString(R.string.SDKPolicyError), Toast.LENGTH_SHORT).show());
            }
        }
    };
    private boolean permissionGranted = false;
    private TextView mTextView;
    private Button mButMeasure;
    private ActivityMainBinding binding;
    private HealthTrackingService healthTrackingService = null;
    private HealthTracker ecgTracker = null;
    private HealthTracker heartTracker = null;
    private HealthTracker accelerometerTracker = null;
    private HealthTracker sweatTracker = null;
    private HealthTracker spo2Tracker = null;
    private HealthTracker skinTemperatureTracker = null;
    private HealthTracker edaTracker = null;


    CountDownTimer countDownTimer = new CountDownTimer(MEASUREMENT_DURATION, MEASUREMENT_TICK) {
        @Override
        public void onTick(long timeLeft) {
            if (isMeasurementRunning.get()) {
                if (leadOff.get()) {
                    runOnUiThread(() -> binding.txtOutput.setText(R.string.outputWarning));
                } else {
                    final String measureValue = getString(R.string.MeasurementUpdate, timeLeft / 1000, String.format(Locale.ENGLISH, "%.2f", curEcg.get()));
                    runOnUiThread(() -> binding.txtOutput.setText(measureValue));
                }
            }
        }

        @Override
        public void onFinish() {

            ecgTracker.unsetEventListener();
            heartTracker.unsetEventListener();
            //  sweatTracker.unsetEventListener();
            spo2Tracker.unsetEventListener();
            skinTemperatureTracker.unsetEventListener();
            edaTracker.unsetEventListener();

            isMeasurementRunning.set(false);
            runOnUiThread(() ->
            {
                getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                if (leadOff.get())
                    binding.txtOutput.setText(R.string.MeasurementFailed);
                else {
                    final String finalEcgStr = getString(R.string.MeasurementSuccessful, String.format(Locale.ENGLISH, "%.2f", curEcg.get()));
                    binding.txtOutput.setText(finalEcgStr);
                }
                binding.butStart.setText(R.string.RepeatMeasurement);
            });
        }
    };
    private boolean connected = false;
    private final ConnectionListener connectionListener = new ConnectionListener() {
        @Override
        public void onConnectionSuccess() {

            Log.i(APP_TAG, "Connected");
            Toast.makeText(getApplicationContext(), getString(R.string.ConnectedToHS), Toast.LENGTH_SHORT).show();
            checkCapabilities();
            connected = true;
            List<HealthTrackerType> supported = healthTrackingService.getTrackingCapability().getSupportHealthTrackerTypes();

            ecgTracker = healthTrackingService.getHealthTracker(HealthTrackerType.ECG_ON_DEMAND);
            heartTracker = healthTrackingService.getHealthTracker(HealthTrackerType.HEART_RATE_CONTINUOUS);
            // sweatTracker = healthTrackingService.getHealthTracker(HealthTrackerType.SWEAT_LOSS);
            spo2Tracker = healthTrackingService.getHealthTracker(HealthTrackerType.SPO2_ON_DEMAND);
            skinTemperatureTracker = healthTrackingService.getHealthTracker(HealthTrackerType.SKIN_TEMPERATURE_ON_DEMAND);
            edaTracker = healthTrackingService.getHealthTracker(HealthTrackerType.EDA_CONTINUOUS);
            accelerometerTracker = healthTrackingService.getHealthTracker(HealthTrackerType.ACCELEROMETER_CONTINUOUS);

        }

        @Override
        public void onConnectionEnded() {
            Log.i(APP_TAG, "Disconnected");
        }

        @Override
        public void onConnectionFailed(HealthTrackerException e) {
            if (e.getErrorCode() == HealthTrackerException.OLD_PLATFORM_VERSION || e.getErrorCode() == HealthTrackerException.PACKAGE_NOT_INSTALLED)
                Toast.makeText(getApplicationContext(), getString(R.string.NoHealthPlatformError), Toast.LENGTH_LONG).show();
            if (e.hasResolution()) {
                e.resolve(MainActivity.this);
            } else {
                Log.e(APP_TAG, "Could not connect to Health Services: " + e.getMessage());
                runOnUiThread(() -> Toast.makeText(getApplicationContext(), getString(R.string.ConnectionError), Toast.LENGTH_LONG).show());
            }
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        mTextView = binding.txtOutput;
        mButMeasure = binding.butStart;
        mButMeasure.setOnClickListener(unused -> startMeasurement());

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
            permission = getString(R.string.additionalHealthDataPermission);
        } else {
            permission = Manifest.permission.BODY_SENSORS;
        }

        if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_DENIED)
            requestPermissions(new String[]{permission}, 0);
        else
            permissionGranted = true;
        try {
            healthTrackingService = new HealthTrackingService(connectionListener, getApplicationContext());
            healthTrackingService.connectService();

        } catch (Throwable t) {
            final String msg = t.getMessage();
            Log.e(APP_TAG, msg == null ? "" : msg);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (ecgTracker != null)
            ecgTracker.unsetEventListener();
        isMeasurementRunning.set(false);
        ecgHandler.removeCallbacksAndMessages(null);
        if (healthTrackingService != null)
            healthTrackingService.disconnectService();
    }

    private void checkCapabilities() {
        final List<HealthTrackerType> availableTrackers = healthTrackingService.getTrackingCapability().getSupportHealthTrackerTypes();
        if (!availableTrackers.contains(HealthTrackerType.ECG_ON_DEMAND)) {
            Toast.makeText(getApplicationContext(), getString(R.string.NoECGSupport), Toast.LENGTH_LONG).show();
            Log.e(APP_TAG, "Device does not support ECG tracking");
            finish();
        }
    }

    long startDate = 0L;

    private void startMeasurement() {
        startDate = System.currentTimeMillis();
        if (ActivityCompat.checkSelfPermission(getApplicationContext(), permission) == PackageManager.PERMISSION_DENIED)
            requestPermissions(new String[]{permission}, 0);
        if (!permissionGranted) {
            Log.i(APP_TAG, "Could not get permissions. Terminating measurement");
            return;
        }
        if (!connected) {
            Toast.makeText(getApplicationContext(), getString(R.string.ConnectionError), Toast.LENGTH_SHORT).show();
            return;
        }
        if (!isMeasurementRunning.get()) {
            mTextView.setText(R.string.outputMeasuring);
            mButMeasure.setText(R.string.stop);
            isMeasurementRunning.set(true);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
//             ecgHandler.post(() -> ecgTracker.setEventListener(ecgListener)); // сложный
//            ecgHandler.post(() -> heartTracker.setEventListener(heartTrackerListener));
//            //ecgHandler.post(() -> sweatTracker.setEventListener(sweatTrackerListener));
//            // ecgHandler.post(() -> spo2Tracker.setEventListener(spo2TrackerListener));
//             ecgHandler.post(() -> skinTemperatureTracker.setEventListener(skinTemperatureTrackerListener));
//           ecgHandler.post(() -> edaTracker.setEventListener(edaTrackerListener));
            ecgHandler.post(() -> accelerometerTracker.setEventListener(acccelerometrTrackerListener));

//            ecgTracker.unsetEventListener();
//            heartTracker.unsetEventListener();
//            sweatTracker.unsetEventListener();
//            spo2Tracker.unsetEventListener();
//            skinTemperatureTracker.unsetEventListener();
//            edaTracker.unsetEventListener();

            final Thread uiUpdateThread = new Thread(() -> countDownTimer.start());
            uiUpdateThread.start();
        } else {
            if (ecgTracker != null)
                ecgTracker.unsetEventListener();
            ecgHandler.removeCallbacksAndMessages(null);
            isMeasurementRunning.set(false);
            mButMeasure.setText(R.string.start);
            mTextView.setText(R.string.outputStart);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 0) {
            permissionGranted = true;
            for (int i = 0; i < permissions.length; ++i) {
                if (grantResults[i] == PERMISSION_DENIED) {
                    //User denied permissions twice - permanent denial:
                    if (!shouldShowRequestPermissionRationale(permissions[i])) {
                        Toast.makeText(getApplicationContext(), getString(R.string.PermissionDeniedPermanently), Toast.LENGTH_LONG).show();
                    }
                    //User denied permissions once:
                    else {
                        Toast.makeText(getApplicationContext(), getString(R.string.PermissionDeniedRationale), Toast.LENGTH_LONG).show();
                    }
                    permissionGranted = false;
                    break;
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}

