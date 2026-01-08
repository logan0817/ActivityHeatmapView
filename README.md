英文文档 [English Document](./README_EN.md)

# ActivityHeatmapView (活动热力图组件)

这是一个高度可定制、高性能的 Android 热力图组件（类似于 GitHub 的 Contribution Graph）。
适用于展示活跃度、频率分布或其他基于网格的时间序列数据。

## ✨ 主要特性

* **灵活布局**：支持 X 轴（顶部/底部）和 Y 轴（左侧/右侧）位置的自由配置。
* **智能适配**：自动计算列数、标签宽度和布局偏移，完美适配 Padding。
* **易用 API**：提供泛型 `setData` 方法，直接绑定业务列表数据。
* **强交互**：内置点击事件监听。
* **视觉增强**：支持动态热力颜色（ColorAdapter）、自定义单元格绘制（CellAdapter）
* 
## 引入

### Gradle:

1. 在Project的 **build.gradle** 或 **setting.gradle** 中添加远程仓库

    ```gradle
    repositories {
        //
        mavenCentral()
    }
    ```

2. 在Module的 **build.gradle** 中添加依赖项
   [![Maven Central](https://img.shields.io/maven-central/v/io.github.logan0817/ActivityHeatmapView.svg?label=Latest%20Release)](https://central.sonatype.com/artifact/io.github.logan0817/ActivityHeatmapView)

    ```gradle
   implementation 'io.github.logan0817:ActivityHeatmapView:1.0.1' // 替换为上方徽章显示的最新版本
    ```

## 效果展示

<img src="demo.jpg" width="350" />

> Demo.apk [点击下载](apk/app-debug.apk)

## 🚀 基础用法

### 1. XML 布局配置

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
        app:ahvHeaderPosition="bottom" />

### 2. 代码数据绑定
使用泛型 setData 方法，直接传入您的业务数据列表。

```kotlin
// 1. 假设您的业务数据结构如下
data class DailyStep(val day: Int, val count: Int)
data class UserData(val name: String, val history: List<DailyStep>)

// 2. 准备数据
val userList = listOf(
    UserData(name = "Logan", history = listOf(DailyStep(day = 1, count = 5000))),
    UserData(name = "Allen",  history = listOf(DailyStep(day = 1, count = 7000), DailyStep(day = 3, count = 3000))),
    UserData(name = "Levi",  history = listOf(DailyStep(day = 2, count = 8000), DailyStep(day = 5, count = 9000))),
    UserData(name = "Nicely",  history = listOf())
)

val weekHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

// 3. 绑定数据
heatmapView.setData(
    items = userList,
    labelExtractor = { user -> user.name },       // 提取行标题
    dataExtractor = { user -> user.history },     // 提取该行的数据列表
    // 场景 A：必须传 indexMapper (稀疏/乱序数据, 如日期 day=1 对应 index=0)
    // 场景 B：可以不传 indexMapper (连续/固定数据, 默认使用 List 下标)
    indexMapper = { step -> step.day - 1 },
    headers = weekHeaders                          // 设置表头，组件会自动根据表头数量设置列数
)
```

## 进阶用法

### 1. 交互：点击事件监听

    heatmapView.setOnCellClickListener { rowIndex, colIndex, data ->
        val stepData = data as? DailyStep
        val msg = if (stepData != null) {
            "第${colIndex + 1}天: ${stepData.count}步"
        } else {
            "该日无记录"
        }
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }


### 2视觉：动态热力颜色 (ColorAdapter)
根据数据值的大小显示不同深浅的颜色，实现真正的“热力”效果,可以参考DEMO代码示例。

    heatmapView.setColorAdapter { data ->
    val stepData = data as? DailyStep ?: return@setColorAdapter null
    
        // 示例：根据步数返回不同透明度的绿色
        val ratio = (stepData.count / 20000.0).coerceIn(0.2, 1.0)
        val alpha = (255 * ratio).toInt()
        Color.argb(alpha, 0, 255, 0) // 返回动态计算的 ARGB 颜色
    }


### 3. 自定义绘制：方块内容 (CellAdapter)
在方块内部绘制图标、文字等任意内容，可以参考DEMO代码示例。

    heatmapView.setCellAdapter(object : ActivityHeatmapView.CellAdapter {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE; textSize = 24f; textAlign = Paint.Align.CENTER
        }
    
        override fun onDrawCell(canvas: Canvas, cellRect: RectF, rowIndex: Int, colIndex: Int, data: Any?) {
            val stepData = data as? DailyStep ?: return
            // 示例：步数超过 5000，在格子中间画一颗星
            if (stepData.count > 5000) {
                val baseline = cellRect.centerY() - (paint.descent() + paint.ascent()) / 2
                canvas.drawText("★", cellRect.centerX(), baseline, paint)
            }
        }
    })

## 控件参数及含义

| *参数名*                 |    *参数取值* |           *参数含义*           |
|-----------------------|----------:|:--------------------------:|
| ahvActiveColorStart   |     color |          激活渐变色-顶           |
| ahvActiveColorEnd     |     color |          激活渐变色-底           |
|                       |           |                            |
| ahvInactiveColorStart |     color |          未激活渐变色-顶          |
| ahvInactiveColorEnd   |     color |          未激活渐变色-底          |
|                       |           |                            |
| ahvCellGap            | dimension |           方块之间间距           |
| ahvCellCornerRadius   | dimension |           方块圆角大小           |
|                       |           |                            |
| ahvLabelGridGap       | dimension |        Y轴 (Label)间距        |
| ahvLabelTextColor     |     color |       Y轴 (Label)文字颜色       |
| ahvLabelTextSize      | dimension |       Y轴 (Label)文字大小       |
| ahvLabelPosition      | dimension | Y轴 标签位置: left(左), right(右) |
|                       |           |                            |
| ahvHeaderGridGap      | dimension |       X轴 (Header)间距        |
| ahvHeaderTextColor    |     color |       X轴 (Header)颜色        |
| ahvHeaderTextSize     | dimension |      X轴 (Header)文字大小       |
| ahvHeaderPosition     | dimension | X轴 表头位置: top(顶), bottom(底) |


### 如果你有任何疑问可以留言。
### 如果对你有帮助，可以赏个star支持一下作者。

### License

```
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
