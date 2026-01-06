package com.logan.heatmapview


import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import kotlin.math.abs
import kotlin.math.max

/**
 * 活动热力图组件 (ActivityHeatmapView)
 *
 * 这是一个用于展示频率数据的自定义 View，类似于 GitHub 的提交记录或健身打卡记录。
 *
 * ## 主要特性：
 * 1. **自适应布局**：自动测量左侧 Label 宽度，确保文字不被遮挡。
 * 2. **高度可配**：支持通过 XML (ahv前缀属性) 或代码配置颜色、间距、圆角和文字大小。
 * 3. **视觉增强**：支持方块的垂直线性渐变色。
 * 4. **标准兼容**：完全支持 Android 的 padding 属性 (paddingLeft, paddingTop 等)。
 * 5. **性能优化**：绘图计算外提，避免 onDraw 中频繁对象分配。
 * 6. **动态 X 轴**：支持动态设置列数 (columnCount)，由 headers 数量决定。
 *
 * ## 使用方式：
 * 在 XML 中引入全类名，并配置 ahv... 属性。
 * 在代码中调用 [setData] 方法填充数据。
 */
class ActivityHeatmapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    // ============================================================================================
    // 1. 数据模型定义
    // ============================================================================================

    /**
     * 单行数据模型
     * @param label 左侧显示的标签文本 (如 "Running", "Reading")
     * @param activeIndices 该行中处于激活状态的列索引集合 (0代表第一列)
     */
    data class RowData(val label: String, val activeIndices: Set<Int>)

    // 内部数据持有
    private var rowDataList: List<RowData> = emptyList()
    // 默认的底部表头 (周一到周日)
    private var columnHeaders: List<String> = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    // ============================================================================================
    // 2. 绘图属性 (Paint & Config)
    // ============================================================================================

    // 文字画笔 (抗锯齿)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    // 方块画笔 (抗锯齿)
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // --- 颜色配置 (默认值) ---
    private var activeColorStart = Color.parseColor("#116329")   // 激活色-顶
    private var activeColorEnd = Color.parseColor("#2DA44E")     // 激活色-底
    private var inactiveColorStart = Color.parseColor("#222222") // 未激活-顶
    private var inactiveColorEnd = Color.parseColor("#222222")   // 未激活-底

    // --- 尺寸配置 (默认值，初始化时会转为 px) ---
    private var cellGap = dpToPx(8f)          // 方块间距
    private var cellCornerRadius = dpToPx(4f) // 方块圆角

    // Y轴 (Label) 相关
    private var labelGridGap = dpToPx(10f)
    private var labelTextColor = Color.WHITE
    private var labelTextSize = spToPx(14f)

    // X轴 (Header) 相关
    private var headerGridGap = dpToPx(10f)
    private var headerTextColor = Color.GRAY
    private var headerTextSize = spToPx(12f)

    // --- 内部计算缓存变量 ---
    private var cellSide = 0f          // 计算出的每个方块边长
    private var maxLabelWidth = 0f     // 左侧 Label 区域的最大宽度
    private var bottomHeaderHeight = 0f // 底部 Header 区域的总高度

    // 从 val 改为 var，默认为 12
    private var columnCount = 12

    // ============================================================================================
    // 3. 初始化 (解析 XML 属性)
    // ============================================================================================
    init {
        attrs?.let {
            val typedArray = context.obtainStyledAttributes(it, R.styleable.ActivityHeatmapView)

            // --- 1. 解析颜色 ---
            activeColorStart = typedArray.getColor(R.styleable.ActivityHeatmapView_ahvActiveColorStart, activeColorStart)
            activeColorEnd = typedArray.getColor(R.styleable.ActivityHeatmapView_ahvActiveColorEnd, activeColorStart)
            inactiveColorStart = typedArray.getColor(R.styleable.ActivityHeatmapView_ahvInactiveColorStart, inactiveColorStart)
            inactiveColorEnd = typedArray.getColor(R.styleable.ActivityHeatmapView_ahvInactiveColorEnd, inactiveColorStart)

            // --- 2. 解析 Y轴 (Label) ---
            labelGridGap = typedArray.getDimension(R.styleable.ActivityHeatmapView_ahvLabelGridGap, labelGridGap)
            labelTextColor = typedArray.getColor(R.styleable.ActivityHeatmapView_ahvLabelTextColor, labelTextColor)
            labelTextSize = typedArray.getDimension(R.styleable.ActivityHeatmapView_ahvLabelTextSize, labelTextSize)

            // --- 3. 解析 X轴 (Header) ---
            headerGridGap = typedArray.getDimension(R.styleable.ActivityHeatmapView_ahvHeaderGridGap, headerGridGap)
            headerTextColor = typedArray.getColor(R.styleable.ActivityHeatmapView_ahvHeaderTextColor, headerTextColor)
            headerTextSize = typedArray.getDimension(R.styleable.ActivityHeatmapView_ahvHeaderTextSize, headerTextSize)

            // --- 4. 解析通用尺寸 ---
            cellGap = typedArray.getDimension(R.styleable.ActivityHeatmapView_ahvCellGap, cellGap)
            cellCornerRadius = typedArray.getDimension(R.styleable.ActivityHeatmapView_ahvCellCornerRadius, cellCornerRadius)

            typedArray.recycle()
        }
    }

    // ============================================================================================
    // 4. 公共 API
    // ============================================================================================

    /**
     * 设置显示数据
     * @param data 行数据列表
     * @param headers (可选) 自定义底部 X 轴的文字列表。
     * 如果传入 headers，将根据 headers 的数量自动更新 columnCount。
     */
    fun setData(data: List<RowData>, headers: List<String>? = null) {
        this.rowDataList = data
        if (headers != null) {
            this.columnHeaders = headers
            // 列数跟随表头数量动态变化
            this.columnCount = headers.size
        }
        // 数据变更可能影响行数和宽度计算，必须重新请求布局
        requestLayout()
        invalidate()
    }

    // --- 动态配置 Setters (支持链式调用或独立调用) ---

    fun setLabelTextSize(sizePx: Float) {
        this.labelTextSize = sizePx
        requestLayout()
        invalidate()
    }

    fun setHeaderTextSize(sizePx: Float) {
        this.headerTextSize = sizePx
        requestLayout()
        invalidate()
    }

    fun setLabelGridGap(gapInPx: Float) {
        this.labelGridGap = gapInPx
        requestLayout()
        invalidate()
    }

    fun setHeaderGridGap(gapInPx: Float) {
        this.headerGridGap = gapInPx
        requestLayout()
        invalidate()
    }

    // ============================================================================================
    // 5. 测量逻辑 (onMeasure)
    // ============================================================================================

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = MeasureSpec.getSize(widthMeasureSpec)
        // 计算内容实际可用宽度 (总宽 - 左右内边距)
        val availableWidth = totalWidth - paddingLeft - paddingRight

        // --- A. 计算左侧 Label 占用宽度 ---
        textPaint.textSize = labelTextSize
        maxLabelWidth = 0f
        // 遍历找到最长的文字宽度，避免截断
        rowDataList.forEach {
            val w = textPaint.measureText(it.label)
            if (w > maxLabelWidth) maxLabelWidth = w
        }
        // 如果有数据，左侧宽度 = 文字宽 + 间距
        val leftAreaWidth = if (rowDataList.isNotEmpty()) maxLabelWidth + labelGridGap else 0f

        // --- B. 计算网格方块大小 ---
        val gridAvailableWidth = availableWidth - leftAreaWidth
        // 确保宽度不为负数
        val validGridWidth = max(0f, gridAvailableWidth)

        // 增加 columnCount > 0 的安全检查，防止除零异常
        if (columnCount > 0) {
            // 方块边长 = (可用宽 - (列数-1)*间距) / 列数
            cellSide = (validGridWidth - ((columnCount - 1) * cellGap)) / columnCount
        } else {
            cellSide = 0f
        }

        // --- C. 计算底部 Header 高度 ---
        textPaint.textSize = headerTextSize
        val fontMetrics = textPaint.fontMetrics
        // 文字纯高度 = Abs(Top) + Abs(Bottom)
        val textHeight = abs(fontMetrics.ascent) + abs(fontMetrics.descent)
        bottomHeaderHeight = headerGridGap + textHeight

        // --- D. 计算 View 总高度 ---
        val rowCount = max(rowDataList.size, 0)
        val contentHeight = if (rowCount > 0) {
            // 内容高 = (行数 * 边长) + (行间距) + 底部高度
            (rowCount * cellSide) + ((rowCount - 1) * cellGap) + bottomHeaderHeight
        } else {
            0f
        }

        // 最终高度 = 内容高 + 上下内边距
        val finalHeight = contentHeight + paddingTop + paddingBottom
        setMeasuredDimension(totalWidth, finalHeight.toInt())
    }

    // ============================================================================================
    // 6. 绘制逻辑 (onDraw)
    // ============================================================================================

    override fun onDraw(canvas: Canvas) {
        if (rowDataList.isEmpty()) return

        // --- 性能优化：将不变的 Metrics 计算移出循环 ---

        // 1. 预计算 Label 的垂直居中偏移量
        textPaint.textSize = labelTextSize
        var fontMetrics = textPaint.fontMetrics
        // 居中公式偏移量
        val labelBaselineOffset = -(fontMetrics.bottom + fontMetrics.top) / 2

        // 2. 预计算 Header 的基线偏移量
        textPaint.textSize = headerTextSize
        fontMetrics = textPaint.fontMetrics
        val headerAscentAbs = abs(fontMetrics.ascent)

        // --- 开始遍历绘制 ---
        rowDataList.forEachIndexed { rowIndex, row ->
            // 计算当前行的 Y 坐标 (注意加上 paddingTop)
            val topY = paddingTop + rowIndex * (cellSide + cellGap)
            val bottomY = topY + cellSide

            // --- 步骤 1: 绘制左侧 Label ---
            textPaint.color = labelTextColor
            textPaint.textSize = labelTextSize
            textPaint.textAlign = Paint.Align.LEFT

            // 文字 Y = 方块中心 + 偏移量
            val textBaseY = topY + cellSide / 2 + labelBaselineOffset
            canvas.drawText(row.label, paddingLeft.toFloat(), textBaseY, textPaint)

            // --- 步骤 2: 绘制右侧网格 ---
            for (colIndex in 0 until columnCount) {
                // 计算当前列的 X 坐标 (注意加上 paddingLeft 和 左侧区域宽)
                val leftArea = if (maxLabelWidth > 0) maxLabelWidth + labelGridGap else 0f
                val leftX = paddingLeft + leftArea + colIndex * (cellSide + cellGap)
                val rightX = leftX + cellSide

                // 2.1 绘制方块 (Cell)
                val isActive = row.activeIndices.contains(colIndex)
                val startColor = if (isActive) activeColorStart else inactiveColorStart
                val endColor = if (isActive) activeColorEnd else inactiveColorEnd

                // 处理渐变色逻辑
                if (startColor != endColor) {
                    // 仅当颜色不同时创建渐变 Shader
                    cellPaint.shader = LinearGradient(
                        0f, topY, 0f, bottomY,
                        startColor, endColor,
                        Shader.TileMode.CLAMP
                    )
                } else {
                    cellPaint.shader = null
                    cellPaint.color = startColor
                }

                canvas.drawRoundRect(leftX, topY, rightX, bottomY, cellCornerRadius, cellCornerRadius, cellPaint)

                // 2.2 绘制底部 Header (仅在最后一行绘制)
                if (rowIndex == rowDataList.size - 1) {
                    textPaint.color = headerTextColor
                    textPaint.textSize = headerTextSize
                    textPaint.textAlign = Paint.Align.CENTER

                    val headerText = columnHeaders.getOrNull(colIndex) ?: ""
                    // Header Y = 方块底部 + 间距 + Ascent
                    val baselineY = bottomY + headerGridGap + headerAscentAbs

                    canvas.drawText(headerText, leftX + cellSide / 2, baselineY, textPaint)
                }
            }
        }
    }

    // ============================================================================================
    // 7. 辅助工具方法
    // ============================================================================================

    /** 将 dp 转换为 px */
    private fun dpToPx(dp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    }

    /** 将 sp 转换为 px */
    private fun spToPx(sp: Float): Float {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
    }
}