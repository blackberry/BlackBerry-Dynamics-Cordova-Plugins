# cordova-plugin-bbd-media-capture
BlackBerry Dynamics Cordova Media Capture plugin. It is a fork of [cordova-plugin-media-capture@3.0.4-dev](https://github.com/apache/cordova-plugin-media-capture). 

This plugin enables you to capture audio, video and images using device's microphone or camera and store them within the BlackBerry Dynamics secure container using a similar JavaScript API to the original plugin.

## Preconditions
`cordova-plugin-bbd-media-capture` is dependent on `cordova-plugin-bbd-base` and `cordova-plugin-bbd-file` plugins.

## Installation
`cordova plugin add git+https://github.com/blackberry/blackberry-dynamics-cordova-plugins#media-capture`

## Uninstallation
`cordova plugin rm cordova-plugin-bbd-media-capture`

## Supported Platforms
- iOS
- Android

### Limitations
Due to changes in permissions from Android 11 only image capture is supported when targeting Android API Level 30 or above.
- `navigator.device.capture.captureVideo()` is not supported on Android 11+
- `navigator.device.capture.captureAudio()` is not supported on Android 11+

## Usage
Please take a look at the original examples [here](https://github.com/apache/cordova-plugin-media-capture).

## Constants
#### navigator.device.capture.storageLocation
Use this constant to get platform specific Media storage root location via `resolveLocalFileSystemURL` from `cordova-plugin-bbd-file`.
```javascript
resolveLocalFileSystemURL(navigator.device.capture.storageLocation,
    function(directoryEntry) {
        var mediaDirectoryReader = directoryEntry.createReader();
        mediaDirectoryReader.readEntries(function (entries) {
            console.log("Media storage entries: ", entries);
        }, function(error) {
            console.log("Error: ", error);
        });
    }, function(error) {
        console.log("resolveLocalFileSystemURL error: ", error)
    }
);
```

## Media Capture + File
The BlackBerry Dynamics Cordova Media Capture plugin works together with BlackBerry Dynamics Cordova File plugin which was extended and provides new file system for media files - `LocalFileSystem.MEDIA`.

#### LocalFileSystem.MEDIA
Use this file system type to resolve media files stored in BlackBerry Dynamics secure container.
- Audio files that were captured using `navigator.device.capture.captureAudio()` method are store within `LocalFileSystem.MEDIA` in `audio` sub-folder.
- Video files that were captured using `navigator.device.capture.captureVideo()` method are store within `LocalFileSystem.MEDIA` in `video` sub-folder.
- Images that were captured using `navigator.device.capture.captureImage()` method are store within `LocalFileSystem.MEDIA` in `images` sub-folder.

```javascript
requestFileSystem(LocalFileSystem.MEDIA, 0, function (fileSystem) {
    // reading audio files from secure container
    fileSystem.root.getDirectory('/audio', {create: false, exclusive: false}, function (dirEntry) {
        var directoryReader = dirEntry.createReader();
        directoryReader.readEntries(function (audioFiles) {
            for (var i = 0; i < audioFiles.length; i++) {
                var mediaFile = audioFiles[i];
                console.log(mediaFile); // MediaFile
                mediaFile.file(function(file){
                    console.log(file); // FileEntry
                }, function (error) { console.log(error) });
            }
        }, function (error) { console.log(error) });
    }, function (error) { console.log(error) });
    
    // reading video files from secure container
    fileSystem.root.getDirectory('/video', {create: false, exclusive: false}, function (dirEntry) {
        var directoryReader = dirEntry.createReader();
        directoryReader.readEntries(function (videoFiles) {
            for (var i = 0; i < videoFiles.length; i++) {
                var mediaFile = videoFiles[i];
                console.log(mediaFile); // MediaFile
                mediaFile.file(function(file){
                    console.log(file); // FileEntry
                }, function (error) { console.log(error) });
            }
        }, function (error) { console.log(error) });
    }, function (error) { console.log(error) });
    
    // reading images from secure container
    fileSystem.root.getDirectory('/images', {create: false, exclusive: false}, function (dirEntry) {
        var directoryReader = dirEntry.createReader();
        directoryReader.readEntries(function (images) {
            for (var i = 0; i < images.length; i++) {
                var mediaFile = images[i];
                console.log(mediaFile); // MediaFile
                mediaFile.file(function(file){
                    console.log(file); // FileEntry
                }, function (error) { console.log(error) });
            }
        }, function (error) { console.log(error) });
    }, function (error) { console.log(error) });
}, function (error) { console.log(error) });
```

## License

Apache 2.0 License

## Disclaimer

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
