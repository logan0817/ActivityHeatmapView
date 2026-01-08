# ActivityHeatmapView

中文文档 [Chinese Document](./README.md)

# ActivityHeatmapView

A highly customizable, high-performance Android heatmap component (similar to GitHub's Contribution Graph).
It is suitable for displaying activity levels, frequency distributions, or other grid-based time-series data.

## ✨ Key Features

* **Flexible Layout**: Supports free configuration of X-axis (Top/Bottom) and Y-axis (Left/Right) positions.
* **Smart Adaptation**: Automatically calculates column count, label width, and layout offsets; perfectly adapts to Padding.
* **Easy-to-use API**: Provides a generic `setData` method to directly bind business list data.
* **Strong Interactivity**: Built-in click event listener.
* **Visual Enhancements**: Supports dynamic heatmap colors (`ColorAdapter`) and custom cell drawing (`CellAdapter`).
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
    implementation 'io.github.logan0817:ActivityHeatmapView:1.0.1' // Replace with the latest version shown in the badge above
    ```

## Demo

<img src="demo.jpg" width="350" />

> 1. Demo.apk [点击下载](apk/app-debug.apk)

## Usage: ActivityHeatmapView


    <com.logan.heatmapview.ActivityHeatmapView
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
        app:ahvLabelPosition="left"
    
        app:ahvHeaderGridGap = "8dp"
        app:ahvHeaderTextColor = "#888888"
        app:ahvHeaderTextSize = "12sp"
        app:ahvHeaderPosition="bottom"/>


### 2. Data Binding
Use the generic setData method to pass your business data list directly.

```kotlin
// 1. // 1. Assume your business data structure is as follows
data class DailyStep(val day: Int, val count: Int)
data class UserData(val name: String, val history: List<DailyStep>)

// 2. Prepare data
val userList = listOf(
    UserData(name = "Logan", history = listOf(DailyStep(day = 1, count = 5000))),
    UserData(name = "Allen",  history = listOf(DailyStep(day = 1, count = 7000), DailyStep(day = 3, count = 3000))),
    UserData(name = "Levi",  history = listOf(DailyStep(day = 2, count = 8000), DailyStep(day = 5, count = 9000))),
    UserData(name = "Nicely",  history = listOf())
)

val weekHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// 3. Bind data
heatmapView.setData(
    items = userList,
    labelExtractor = { user -> user.name },       // Extract row label
    dataExtractor = { user -> user.history },     // Extract data list for the row
    // Scenario A: Must pass indexMapper (sparse/unordered data, e.g., date day=1 maps to index=0)
    // Scenario B: Can omit indexMapper (continuous/fixed data, defaults to List index)
    indexMapper = { step -> step.day - 1 },
    headers = weekHeaders                          // Set headers, the component automatically sets column count based on header count
)
```

## Advanced Usage

### 1. Interaction: Click Event Listener

    heatmapView.setOnCellClickListener { rowIndex, colIndex, data ->
        val stepData = data as? DailyStep
        val msg = if (stepData != null) {
            "Day ${colIndex + 1}: ${stepData.count} steps"
        } else {
            "No record for this day"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


### Visuals: Dynamic Heatmap Colors (ColorAdapter)
Display different shades of color based on data values to achieve a true "heatmap" effect.. Refer to the DEMO code for examples.

    heatmapView.setColorAdapter { data ->
    val stepData = data as? DailyStep ?: return@setColorAdapter null
    
        // Example: Return varying opacity of green based on step count
        val ratio = (stepData.count / 20000.0).coerceIn(0.2, 1.0)
        val alpha = (255 * ratio).toInt()
        Color.argb(alpha, 0, 255, 0) // Return dynamically calculated ARGB color
    }


### 3. Custom Drawing: Cell Content (CellAdapter)
Draw icons, text, or any arbitrary content inside the blocks. Refer to the DEMO code for examples.

    heatmapView.setCellAdapter(object : ActivityHeatmapView.CellAdapter {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 24f; textAlign = Paint.Align.CENTER
        }
    
        override fun onDrawCell(canvas: Canvas, cellRect: RectF, rowIndex: Int, colIndex: Int, data: Any?) {
            val stepData = data as? DailyStep ?: return
            // Example: If steps exceed 5000, draw a star in the center of the cell
            if (stepData.count > 5000) {
                val baseline = cellRect.centerY() - (paint.descent() + paint.ascent()) / 2
                canvas.drawText("★", cellRect.centerX(), baseline, paint)
            }
        }
    })


## Attributes & Description

| *Attribute Name* |  *Value Type* |            *Description*             |
| :-------------------- | :-----------: |:------------------------------------:|
| ahvActiveColorStart   |     color     |       Active Gradient - Start        |
| ahvActiveColorEnd     |     color     |        Active Gradient - End         |
|                       |               |                                      |
| ahvInactiveColorStart |     color     |      Inactive Gradient - Start       |
| ahvInactiveColorEnd   |     color     |       Inactive Gradient - End        |
|                       |               |                                      |
| ahvCellGap            |   dimension   |        Spacing between cells         |
| ahvCellCornerRadius   |   dimension   |          Cell corner radius          |
|                       |               |                                      |
| ahvLabelGridGap       |   dimension   |          Y-Axis (Label) Gap          |
| ahvLabelTextColor     |     color     |         Y-Axis (Label) Color         |
| ahvLabelTextSize      |   dimension   |       Y-Axis (Label) Text Size       |
| ahvLabelPosition      | dimension |  Y-Axis Label position: left, right  |
|                       |               |                                      |
| ahvHeaderGridGap      |   dimension   |         X-Axis (Header) Gap          |
| ahvHeaderTextColor    |     color     |        X-Axis (Header) Color         |
| ahvHeaderTextSize     |   dimension   |      X-Axis (Header) Text Size       |
| ahvHeaderPosition     | dimension | X-Axis Header position: top , bottom |


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