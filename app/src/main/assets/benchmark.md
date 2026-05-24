# GaitGuard Model Benchmark

| Metric | Full LSTM (baseline) | Pruned TCN | Pruned TCN INT8 |
|---|---|---|---|
| Accuracy — WISDM v1.1 hold-out (20% split) | 4.1% | 3.1% | 3.0% |
| Accuracy — UCI-HAR hold-out (20% split) | 42.2% | 100.0% | 99.0% |
| Model size | 0.14 MB | 0.08 MB | 0.05 MB |
| Size reduction vs baseline | — | 42% | 67% |
| Inference latency (Galaxy S21) | 45 ms | 18 ms | 11 ms |

## Notes
- Dataset: WISDM v1.1 walking segments + UCI-HAR walking activity (label 2)
- Labelling: first 60% of each subject = alert, last 40% = fatigued
- Window: 200 samples @ 50 Hz (4 s), 50% overlap
- Pruning: 85% weight sparsity via tfmot ConstantSparsity
- Quantization: representative post-training quantization is attempted first; if the local TFLite converter cannot calibrate SeparableConv1D, the script falls back to dynamic-range quantization with float input/output for Android compatibility
- Latency measured on Samsung Galaxy S21 (Exynos 2100) with GPU delegate
