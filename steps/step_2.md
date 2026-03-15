I want you to implement the **domain models** for the video downloader app in the `shared/domain` module. These models will be pure Kotlin, with no platform dependencies, and will be used by all platforms.  

Please create the following classes:

1. **VideoInfo**
   - `id: String` → unique video identifier
   - `title: String`
   - `duration: Long` → duration in seconds
   - `formats: List<VideoFormat>` → list of available video/audio formats

2. **VideoFormat**
   - `formatId: String` → format code
   - `resolution: String` → e.g., "1080p", "720p"
   - `size: Long?` → optional file size in bytes

3. **DownloadTask**
   - `id: String` → unique task identifier
   - `url: String` → original video URL
   - `format: String?` → selected format
   - `state: DownloadState` → current download state, default is `QUEUED`

4. **DownloadState** (enum)
   - Values: `QUEUED`, `DOWNLOADING`, `COMPLETED`, `FAILED`

Make sure the classes are **serializable with kotlinx.serialization** to enable saving/loading later.  
Generate the Kotlin source files in `shared/domain` with proper package declarations, imports, and basic constructors.