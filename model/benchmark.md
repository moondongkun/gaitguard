# GaitGuard Model Benchmark

| Metric | Full LSTM (baseline) | Pruned TCN | Pruned TCN INT8 |
|---|---|---|---|
| Fatigue-proxy accuracy - WISDM v1.1 stratified hold-out | 84.7% | 81.5% | 80.7% |
| Fatigue-proxy accuracy - UCI-HAR walking stratified hold-out | 40.4% | 40.0% | 39.6% |
| Model size | 0.14 MB | 0.08 MB | 0.05 MB |
| Size reduction vs baseline | - | 42% | 67% |
| Inference latency (Galaxy S21) | 45 ms | 18 ms | 11 ms |

## Notes
- Primary dataset: WISDM v1.1 walking segments
- Secondary sanity check: UCI-HAR walking subset, evaluated as a cross-dataset fatigue-proxy transfer task
- Labeling: first 60% of each walking stream = normal/fresh proxy, last 40% = fatigue proxy
- Split: deterministic stratified 80/20 hold-out to keep both proxy classes in train and test
- Window: 200 samples @ 50 Hz (4 s), 50% overlap
- Pruning: polynomial sparsity schedule from 50% to 85%, then strip pruning wrappers
- Quantization: representative post-training quantization is attempted first; if the local TFLite converter cannot calibrate SeparableConv1D, the script falls back to dynamic-range quantization with float input/output for Android compatibility
- Accuracy is for the public-dataset fatigue proxy task used to validate optimization, not a clinical fatigue diagnosis
- Latency measured on Samsung Galaxy S21 (Exynos 2100) with GPU delegate
