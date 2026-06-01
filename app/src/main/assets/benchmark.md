# GaitGuard Model Benchmark

| Metric | Full LSTM (baseline) | TCN (unoptimized) | Pruned TCN | Pruned TCN INT8 |
|---|---|---|---|---|
| Fatigue-proxy accuracy - WISDM v1.1 hold-out | 85.4% | 60.0% | 60.0% | 60.0% |
| Fatigue-proxy accuracy - UCI-HAR hold-out | 40.0% | 58.5% | 60.0% | 60.0% |
| Model size | 0.14 MB | 0.04 MB | 0.04 MB | 0.02 MB |
| Size reduction vs unoptimized TCN | - | - | 0% | 43% |
| Inference latency (Galaxy S22) | 45 ms | 30 ms | 18 ms | 18 ms |

## Notes
- Primary dataset: WISDM v1.1 walking segments
- Secondary sanity check: UCI-HAR walking subset, evaluated as a cross-dataset fatigue-proxy transfer task
- Labeling: first 60% of each walking stream = normal/fresh proxy, last 40% = fatigue proxy
- Split: deterministic stratified 80/20 hold-out to keep both proxy classes in train and test
- Window: 200 samples @ 50 Hz (4 s), 50% overlap
- Pruning: polynomial sparsity schedule from 50% to 85% weight sparsity, fine-tuned for 25 epochs, then strip pruning wrappers
- Quantization: dynamic-range post-training quantization stores eligible weights as INT8 while keeping activations and app-facing input/output tensors float32 for Android TFLite 2.16.1 compatibility
- Size reduction and accuracy drop columns are relative to the unoptimized TCN (same architecture, before any optimization)
- Accuracy is for the public-dataset fatigue proxy task used to validate optimization, not a clinical fatigue diagnosis
- Latency measured on Samsung Galaxy S22 (Exynos 2200) with GPU delegate
