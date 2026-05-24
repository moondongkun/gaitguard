# GaitGuard — Android App Icon

Drop these files into your Android Studio project's `app/src/main/res/` folder.
The folder structure below maps 1-to-1.

## Files
```
res/
├── mipmap-mdpi/
│   ├── ic_launcher.png                 48×48
│   ├── ic_launcher_round.png           48×48
│   ├── ic_launcher_foreground.png     108×108
│   └── ic_launcher_background.png     108×108
├── mipmap-hdpi/                        (72 / 162)
├── mipmap-xhdpi/                       (96 / 216)
├── mipmap-xxhdpi/                     (144 / 324)
├── mipmap-xxxhdpi/                    (192 / 432)
├── mipmap-anydpi-v26/
│   ├── ic_launcher.xml                 adaptive icon descriptor
│   └── ic_launcher_round.xml           same, for the round alias
└── values/
    └── ic_launcher_background.xml      <color name="ic_launcher_background">#3069AB</color>
```

## Play Store
`play-store/ic_launcher-playstore.png` — 512×512, for the Google Play console listing.

## How to use in Android Studio
1. In the Project window switch to **Project** view.
2. Drag each folder into `app/src/main/res/` (or copy the files manually).
3. Android will pick the correct density automatically.
4. The `mipmap-anydpi-v26/` XML files turn on **adaptive icons** on Android 8.0+
   — the launcher renders the foreground (spine) over the solid blue background
   inside whatever shape the device's launcher uses (circle, squircle, etc.).

## Colors
- Background blue: `#3069AB`
- Vertebrae cream:  `#F9EFE6`
- Accent orange:    `#EA684E`
