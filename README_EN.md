# ActivityHeatmapView

中文文档 [Chinese Document](./README.md)

* **ActivityHeatmapView**
* This is a custom View used to display frequency data, similar to GitHub's contribution graph or fitness workout logs.

## Installation

### Gradle:

1. Add the remote repository to your Project's **build.gradle** or **settings.gradle**:

    ```gradle
    repositories {
        // ...
        mavenCentral()
    }
    ```

2. Add the dependency to your Module's **build.gradle**:

   [![Maven Central](https://img.shields.io/maven-central/v/io.github.logan0817/ActivityHeatmapView.svg?label=Latest%20Release)](https://central.sonatype.com/artifact/io.github.logan0817/ActivityHeatmapView)

    ```gradle
    implementation 'io.github.logan0817:ActivityHeatmapView:1.0.0' // Replace with the latest version shown in the badge above
    ```

## Demo

<img src="demo.jpg" width="350" />

> 1. Demo.apk [点击下载](apk/app-debug.apk)

## Usage: ShimmerView

```xml
<com.logan.shinningviewapp.ActivityHeatmapView
    android:id = "@+id/activityHeatmapView"
    android:layout_width = "match_parent"
    android:layout_height = "wrap_content"

    app:ahvInactiveColorEnd = "#2B2A2C"
    app:ahvInactiveColorStart = "#2B2A2C"

    app:ahvActiveColorStart = "#ED1C91"
    app:ahvActiveColorEnd = "#FF6FBF"

    app:ahvCellGap = "9dp"
    app:ahvCellCornerRadius = "4dp"

    app:ahvLabelGridGap = "10dp"
    app:ahvLabelTextColor = "#FFFFFF"
    app:ahvLabelTextSize = "14sp"

    app:ahvHeaderGridGap = "8dp"
    app:ahvHeaderTextColor = "#888888"
    app:ahvHeaderTextSize = "12sp" />
```

## activityHeatmapView.setData Usage Example

```kotlin
// Generate and set data with dynamic row counts
// Pass in custom X-axis labels
val dateLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
val data = labels.map {
    //The example logic simulation here is for reference.
    // 1. The number of days this row is active is randomly determined.
    val activeCount = (1..12).random()
    // 2. Generate a list from 0 to 12, shuffle it, take the first activeCount elements, and convert them to a Set.
    val randomIndices = (0..12).shuffled().take(activeCount).toSet()
    ActivityHeatmapView.RowData(it, randomIndices)
}
activityHeatmapView.setData(data, headers = dateLabels)
```

## Attributes & Description

| *Attribute Name* |  *Value Type* |       *Description* |
| :-------------------- | :-----------: | :-----------------------: |
| ahvActiveColorStart   |     color     |  Active Gradient - Start  |
| ahvActiveColorEnd     |     color     |   Active Gradient - End   |
|                       |               |                           |
| ahvInactiveColorStart |     color     | Inactive Gradient - Start |
| ahvInactiveColorEnd   |     color     |  Inactive Gradient - End  |
|                       |               |                           |
| ahvCellGap            |   dimension   |   Spacing between cells   |
| ahvCellCornerRadius   |   dimension   |    Cell corner radius     |
|                       |               |                           |
| ahvLabelGridGap       |   dimension   |    Y-Axis (Label) Gap     |
| ahvLabelTextColor     |     color     |   Y-Axis (Label) Color    |
| ahvLabelTextSize      |   dimension   | Y-Axis (Label) Text Size  |
|                       |               |                           |
| ahvHeaderGridGap      |   dimension   |    X-Axis (Header) Gap    |
| ahvHeaderTextColor    |     color     |   X-Axis (Header) Color   |
| ahvHeaderTextSize     |   dimension   | X-Axis (Header) Text Size |


### If you have any questions, feel free to open an issue.
### If you find this helpful, please give it a star to support the author!

### License

```text
MIT License

Copyright (c) 2025 Logan Gan

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```