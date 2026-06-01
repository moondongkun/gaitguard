# GaitGuard

**Real-time gait & posture monitoring on Android with an optimized on-device DNN.**

GaitGuard is a mobile app for *Mobile Computing and Its Applications (Spring 2026, 4190.406B)* mini-project. It senses the phone's inertial sensors while you walk, runs an optimized Temporal Convolutional Network (TCN) entirely on-device, and visualizes fatigue, fall-risk, and gait-quality metrics in real time. A camera-based posture analyzer complements the gait pipeline.

---

## 1. Goal & Usage Scenario

Falls and fatigue-related gait degradation are a major risk for older adults and rehabilitation patients. GaitGuard turns an ordinary smartphone (carried in a trouser pocket) into a continuous gait monitor that:

- estimates a **fatigue score** and **fall-risk level** from walking patterns,
- surfaces a **fall alert** (with a caregiver SMS hook) when risk is high,
- lets the user run a guided **6-Minute Walk Test** and review trends over time.

Typical scenario: an at-risk user keeps the phone in their pocket during daily walking. GaitGuard continuously processes the motion stream and warns of elevated fall risk, while a clinician or caregiver can review the session dashboard.

> Note: the fatigue/fall-risk outputs are a research proxy validated on public datasets, **not** a clinical diagnosis.

---

## 2. Key Idea

1. **Sense** the 6-axis IMU stream (3-axis accelerometer + 3-axis gyroscope) at 50 Hz, with no need to persist raw data.
2. **Process** it into 4-second windows (200 samples, 50% overlap) plus 12 hand-crafted gait features (cadence, inter-step interval CV, symmetry, harmonic ratio, trunk tilt, etc.).
3. **Infer** on-device with an optimized **TCN** that outputs fatigue (0–1), a 3-class fall-risk distribution, and a gait-symmetry score.
4. **Visualize** results live: waveform, fatigue gauge, fall-risk indicator, terrain context, and historical dashboards.

The model is compressed with **pruning + Android-compatible INT8 quantization** so it runs in real time with a small on-device artifact — the core optimization requirement of the assignment.

---

## 3. Model & Optimization

The training/optimization pipeline lives in [`model/train.py`](model/train.py). It downloads the datasets, trains the network, applies pruning and quantization, and emits the deployable `.tflite` model plus the benchmark table.

- **Architecture:** compact Temporal Convolutional Network (Conv1D blocks with explicit causal padding) over the 4-second IMU window, with the 12 engineered features fused in a secondary input head.
- **Baseline for comparison:** a full LSTM classifier.
- **Pruning:** polynomial sparsity schedule from 50% → 85%, then pruning wrappers are stripped.
- **Quantization:** dynamic-range post-training INT8 stores eligible weights as INT8, keeps activations and app-facing input/output tensors float32, and uses low TFLite op versions for Android runtime compatibility.

### Before / After Optimization

| Metric | Full LSTM (baseline) | Pruned TCN | Pruned TCN INT8 |
|---|---|---|---|
| Accuracy — WISDM v1.1 hold-out | 84.1% | 60.0% | 60.0% |
| Accuracy — UCI-HAR walking transfer | 39.6% | 40.0% | 40.0% |
| Model size | 0.14 MB | 0.04 MB | 0.02 MB |
| Size reduction vs baseline | — | 70% | **83%** |
| Inference latency (Galaxy S21) | 45 ms | 18 ms | 18 ms |

Optimization cuts model size by **83%** while keeping the live Android model fully on-device with float32 app I/O and dynamic-range INT8 weight storage. Full notes are in [`model/benchmark.md`](model/benchmark.md).

### Datasets

- **Primary:** WISDM v1.1 walking segments (`model/WISDM_ar_v1.1/`).
- **Secondary / cross-dataset sanity check:** UCI-HAR walking subset (`model/UCI HAR Dataset/`).
- **Fatigue-proxy labeling:** first 60% of each walking stream = fresh/normal proxy, last 40% = fatigue proxy.
- **Split:** deterministic stratified 80/20 hold-out.

---

## 4. App Functionality

Built with Kotlin + Jetpack Compose. Key screens:

- **Live** — real-time gait monitoring: fatigue gauge, fall-risk indicator, vertical-acceleration waveform, cadence/symmetry readouts, and a fall alert dialog with caregiver-SMS hook.
- **Posture** — camera-based posture analysis with an AR skeleton overlay and scan, plus a posture result/health view.
- **Walk Test** — guided 6-Minute Walk Test with a countdown timer and progress arc.
- **History / Dashboard** — global gait score and per-session trends over time.
- **Metrics / Calibration / Settings** — gravity-vector calibration, alert configuration, and local-data clearing.

On-device ML lives in `app/src/main/java/com/gaitguard/app/ml/` (`GaitInferenceEngine`, `PostureAnalyzer`); signal processing and gait feature extraction live in `app/src/main/java/com/gaitguard/app/sensor/`. The TFLite model loads with a GPU delegate and gracefully falls back to CPU.

---

## 5. Build & Install

**Run the prebuilt APK (fastest):** install one of the APKs in [`apk/`](apk/) on an Android device:

```
adb install apk/GaitGuard-final-debug.apk
```

**Build from source:**

```
./gradlew assembleDebug      # produces app/build/outputs/apk/debug/
```

Requires Android Studio (recent stable), Android SDK, and a device/emulator with camera and motion sensors. The TFLite model `app/src/main/assets/gait_model.tflite` is bundled in the repo.

**Retrain / re-optimize the model:**

```
python model/train.py        # writes app/src/main/assets/gait_model.tflite + model/benchmark.md
```

---

## 6. Repository Structure

```
app/                 Android Studio app module (Kotlin + Compose)
  src/main/java/com/gaitguard/app/
    ml/              On-device inference (GaitInferenceEngine, PostureAnalyzer)
    sensor/          IMU sensing, signal processing, gait features
    ui/screens/      Live, Posture, WalkTest, History, Metrics, Settings, ...
    data/            Models, DB, repositories
  src/main/assets/   gait_model.tflite (deployed optimized model)
model/               train.py, benchmark.md, optimized .tflite variants, datasets
apk/                 Final APK builds
docs/                Final presentation slides + script (see below)
```

---

## 7. Demo

A short demo (screenshots/video) of the Live monitor, Posture analyzer, Walk Test, and History dashboard is included in the final presentation. See [`docs/`](docs/) for the slides and the presentation script.

---

## 8. Submission Checklist (per project guidelines)

- [x] Source code (Android Studio project) — `app/`
- [x] Model optimization code — `model/train.py`
- [x] Before/after optimization benchmark — `model/benchmark.md`
- [x] Final APK — `apk/`
- [x] Final presentation slides — `docs/`
- [x] README
- [x] TA (@jinbrighten) invited as collaborator
