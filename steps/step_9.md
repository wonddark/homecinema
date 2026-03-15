I want you to implement **responsive layout helpers** for the KMP video downloader app so that screens can adapt to **mobile, tablet, and desktop** devices.

Please do the following:

1. In `shared/ui/util/PlatformUtils.kt`, define:

```kotlin
expect fun getDeviceType(): DeviceType

enum class DeviceType { MOBILE, TABLET, DESKTOP }
```

2. Provide **actual implementations** for each platform:

  - **Android**: determine device type based on screen width/density.
  - **Desktop**: return `DESKTOP` for all cases (or allow optional window width logic later).

3. Update screens (`DownloadScreen`) to **adapt layout** based on `getDeviceType()`:

  - Mobile → Column layout
  - Tablet → LazyVerticalGrid or multi-column layout
  - Desktop → Row layout or grid suitable for wide screens

4. Ensure this logic **works with Compose** and allows **shared atomic components** to be reused without changes.

The goal is to have a **responsive UI** that adapts automatically to different device types while maintaining a single **shared set of components**.
