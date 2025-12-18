package com.samsung.android.ecgmonitor;

public class StepDetector {
    // Константы для настройки алгоритма
    private static final float STEP_THRESHOLD = 1.5f; // Порог для обнаружения шага
    private static final long STEP_DELAY_NS = 250000000L; // Минимальная задержка между шагами (250 мс)

    // Переменные для отслеживания состояния
    private long lastStepTimeNs = 0;
    private float lastMagnitude = 0;
    private int stepCount = 0;
    private boolean isUpwardPeak = false;

    // Фильтр для сглаживания значений
    private static final int FILTER_SIZE = 5;
    private float[] magnitudeBuffer = new float[FILTER_SIZE];
    private int bufferIndex = 0;

    public StepDetector() {
        // Инициализация буфера
        for (int i = 0; i < FILTER_SIZE; i++) {
            magnitudeBuffer[i] = 0;
        }
    }

    public void processSensorData(float ax, float ay, float az, long timestampNs) {
        // 1. Вычисляем общее ускорение (исключая гравитацию)
        float magnitude = (float) Math.sqrt(ax * ax + ay * ay + az * az);

        // 2. Применяем фильтр низких частот для сглаживания
        magnitudeBuffer[bufferIndex] = magnitude;
        bufferIndex = (bufferIndex + 1) % FILTER_SIZE;

        float filteredMagnitude = 0;
        for (float value : magnitudeBuffer) {
            filteredMagnitude += value;
        }
        filteredMagnitude /= FILTER_SIZE;

        // 3. Определяем, является ли текущее значение пиком
        if (filteredMagnitude < lastMagnitude && lastMagnitude >= STEP_THRESHOLD) {
            // Обнаружен пик (переход от максимума к минимуму)
            if (isUpwardPeak) {
                // Проверяем временную задерку между шагами
                if (timestampNs - lastStepTimeNs > STEP_DELAY_NS) {
                    stepCount++;
                    lastStepTimeNs = timestampNs;
                    isUpwardPeak = false;

                    // Вы можете добавить здесь логирование или callback
                    // Log.d("StepDetector", "Step detected! Total: " + stepCount);
                }
            }
        } else if (filteredMagnitude > lastMagnitude) {
            // Начался подъем к пику
            isUpwardPeak = true;
        }

        lastMagnitude = filteredMagnitude;
    }

    public int getStepCount() {
        return stepCount;
    }

    public void resetStepCount() {
        stepCount = 0;
        lastStepTimeNs = 0;
        lastMagnitude = 0;
        isUpwardPeak = false;
    }
}
