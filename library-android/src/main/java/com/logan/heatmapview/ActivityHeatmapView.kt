package com.logan.heatmapview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.util.AttributeSet
import android.util.TypedValue
import android.view.MotionEvent
import android.view.View
import kotlin.math.abs
import kotlin.math.max

/**
 * **ActivityHeatmapView**
 *
 * 一个高度可定制、高性能的 Android 热力图组件（类似于 GitHub 的贡献图）。
 * 适用于展示活跃度、频率分布或其他基于网格的时间序列数据。
 *
 * ### 主要特性：
 * 1. **灵活布局**：支持 X 轴（顶部/底部）和 Y 轴（左侧/右侧）位置的自由配置。
 * 2. **智能适配**：自动计算列数、标签宽度和布局偏移，完美适配 Padding。
 * 3. **高性能**：优化的绘图逻辑，采用 Shader 缓存与对象复用，减少 GC。
 * 4. **强交互**：内置点击事件监听，支持获取点击位置的数据。
 * 5. **视觉增强**：支持垂直渐变色、圆角以及通过 Adapter 完全自定义单元格绘制。
 * 6. **极简 API**：提供泛型方法 [setData] 简化数据绑定.
 *
 * @author Logan
 */
class ActivityHeatmapView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    /**
     * 单元格点击监听器。
     */
    fun interface OnCellClickListener {
        /**
         * 当单元格被点击时触发。
         *
         * @param rowIndex 点击的行索引 (0 ~ rowCount-1)
         * @param colIndex 点击的列索引 (0 ~ columnCount-1)
         * @param data 该单元格绑定的数据对象 (如果该位置无数据则为 null)
         */
        fun onCellClick(rowIndex: Int, colIndex: Int, data: Any?)
    }

    /**
     * 颜色适配器。
     * 用于根据单元格的具体数据值，动态决定颜色（实现真正的“热力”深浅效果）。
     */
    fun interface ColorAdapter {
        /**
         * 获取单元格颜色。
         *
         * @param data 单元格数据
         * @return 颜色值的 Int (ARGB)。如果返回 null，则组件会使用默认配置的 active/inactive 颜色。
         */
        fun getCellColor(data: Any?): Int?
    }

    /**
     * 自定义单元格绘制适配器。
     * 当你需要绘制文字、图标或其他复杂内容到单元格内时实现此接口。
     */
    interface CellAdapter {
        /**
         * 绘制单元格内容。
         *
         * @param canvas 画布
         * @param cellRect 当前单元格的绘制区域 (RectF)
         * @param rowIndex 行索引
         * @param colIndex 列索引
         * @param data 单元格数据
         */
        fun onDrawCell(canvas: Canvas, cellRect: RectF, rowIndex: Int, colIndex: Int, data: Any?)
    }

    /** Y 轴标签位置枚举 */
    enum class LabelPosition(val value: Int) {
        LEFT(0), RIGHT(1);

        companion object {
            fun fromInt(value: Int) = entries.find { it.value == value } ?: LEFT
        }
    }

    /** X 轴表头位置枚举 */
    enum class HeaderPosition(val value: Int) {
        TOP(0), BOTTOM(1);

        companion object {
            fun fromInt(value: Int) = entries.find { it.value == value } ?: BOTTOM
        }
    }

    /**
     * 内部使用的标准行数据模型。
     * 使用 private 封装，避免对外暴露复杂的 Map 结构。
     *
     * @param label 行标题
     * @param cellData 列索引与数据的映射表 (Map<ColumnIndex, Data>)，用于 O(1) 查找。
     */
    private data class RowData(val label: String, val cellData: Map<Int, Any>)

    // 内部数据持有
    private var rowDataList: List<RowData> = emptyList()
    private var columnHeaders: List<String> = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

    // 回调引用
    private var cellAdapter: CellAdapter? = null
    private var colorAdapter: ColorAdapter? = null
    private var onCellClickListener: OnCellClickListener? = null

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cellPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    // 默认颜色配置
    private var activeColorStart = Color.parseColor("#116329")
    private var activeColorEnd = Color.parseColor("#2DA44E")
    private var inactiveColorStart = Color.parseColor("#222222")
    private var inactiveColorEnd = Color.parseColor("#222222")

    // 尺寸配置
    private var cellGap = dpToPx(8f)
    private var cellCornerRadius = dpToPx(4f)
    private var labelGridGap = dpToPx(10f)
    private var labelTextColor = Color.WHITE
    private var labelTextSize = spToPx(14f)
    private var labelPosition = LabelPosition.LEFT
    private var headerGridGap = dpToPx(10f)
    private var headerTextColor = Color.GRAY
    private var headerTextSize = spToPx(12f)
    private var headerPosition = HeaderPosition.BOTTOM

    // 内部计算缓存 (在 onMeasure 中计算，供 onDraw 使用)
    private var cellSide = 0f
    private var maxLabelWidth = 0f
    private var labelAreaTotalWidth = 0f
    private var headerAreaTotalHeight = 0f
    private var columnCount = 12

    // 布局偏移量 (用于 Touch 事件坐标转换和绘制偏移)
    private var gridOffsetX = 0f
    private var gridOffsetY = 0f

    // 临时对象 (避免在 onDraw 中频繁 GC)
    private val tempCellRect = RectF()

    init {
        attrs?.let {
            val ta = context.obtainStyledAttributes(it, R.styleable.ActivityHeatmapView)

            // 解析颜色
            activeColorStart = ta.getColor(R.styleable.ActivityHeatmapView_ahvActiveColorStart, activeColorStart)
            activeColorEnd = if (ta.hasValue(R.styleable.ActivityHeatmapView_ahvActiveColorEnd)) {
                ta.getColor(R.styleable.ActivityHeatmapView_ahvActiveColorEnd, activeColorStart)
            } else activeColorStart

            inactiveColorStart = ta.getColor(R.styleable.ActivityHeatmapView_ahvInactiveColorStart, inactiveColorStart)
            inactiveColorEnd = if (ta.hasValue(R.styleable.ActivityHeatmapView_ahvInactiveColorEnd)) {
                ta.getColor(R.styleable.ActivityHeatmapView_ahvInactiveColorEnd, inactiveColorStart)
            } else inactiveColorStart

            // 解析尺寸与位置
            labelGridGap = ta.getDimension(R.styleable.ActivityHeatmapView_ahvLabelGridGap, labelGridGap)
            labelTextColor = ta.getColor(R.styleable.ActivityHeatmapView_ahvLabelTextColor, labelTextColor)
            labelTextSize = ta.getDimension(R.styleable.ActivityHeatmapView_ahvLabelTextSize, labelTextSize)
            labelPosition = LabelPosition.fromInt(ta.getInt(R.styleable.ActivityHeatmapView_ahvLabelPosition, labelPosition.value))

            headerGridGap = ta.getDimension(R.styleable.ActivityHeatmapView_ahvHeaderGridGap, headerGridGap)
            headerTextColor = ta.getColor(R.styleable.ActivityHeatmapView_ahvHeaderTextColor, headerTextColor)
            headerTextSize = ta.getDimension(R.styleable.ActivityHeatmapView_ahvHeaderTextSize, headerTextSize)
            headerPosition = HeaderPosition.fromInt(ta.getInt(R.styleable.ActivityHeatmapView_ahvHeaderPosition, headerPosition.value))

            cellGap = ta.getDimension(R.styleable.ActivityHeatmapView_ahvCellGap, cellGap)
            cellCornerRadius = ta.getDimension(R.styleable.ActivityHeatmapView_ahvCellCornerRadius, cellCornerRadius)

            ta.recycle()
        }
    }

    /**
     * **设置数据 (泛型入口)**
     *
     * 自动将您的业务列表 `List<T>` 转换为组件内部所需的结构。
     *
     * @param items 您的业务数据列表 (例如 `List<User>`)
     * @param labelExtractor 从 T 中提取行标题的 Lambda (例如 `{ user.name }`)
     * @param dataExtractor 从 T 中提取该行数据列表的 Lambda (例如 `{ user.records }`)
     * * @param indexMapper (可选) 确定数据属于第几列。
     * - 如果传入：根据规则计算 (适用于稀疏数据，如按日期存储)。
     * - 如果不传：默认使用 List 的下标 (0, 1, 2...) (适用于连续数据，如固定7天的数组)。
     * 返回的索引必须 >= 0，否则该数据会被忽略。
     * @param headers (可选) 自定义 X 轴表头。若不传则保持当前表头。传入时会自动更新列数。
     */
    fun <T, D> setData(
        items: List<T>,
        labelExtractor: (T) -> String,
        dataExtractor: (T) -> List<D>,
        indexMapper: ((D) -> Int)? = null,
        headers: List<String>? = null
    ) {
        val internalData = items.map { item ->
            val label = labelExtractor(item)
            val details = dataExtractor(item)

            // 将 List 转换为 Map 以优化后续查找性能
            val map = mutableMapOf<Int, Any>()
            details.forEachIndexed { listIndex, detailItem ->
                // 如果提供了 mapper，就用 mapper 算；否则直接用 listIndex
                val colIndex = indexMapper?.invoke(detailItem) ?: listIndex
                if (colIndex >= 0) {
                    map[colIndex] = detailItem as Any
                }
            }
            RowData(label, map)
        }
        updateViewData(internalData, headers)
    }

    /**
     * 内部更新数据并刷新 UI。
     */
    private fun updateViewData(data: List<RowData>, headers: List<String>? = null) {
        this.rowDataList = data
        if (headers != null) {
            this.columnHeaders = headers
            this.columnCount = headers.size
        }
        requestLayout()
        invalidate()
    }

    /** 设置单元格点击监听器 */
    fun setOnCellClickListener(listener: OnCellClickListener?) {
        this.onCellClickListener = listener
    }

    /** 设置自定义单元格内容绘制器 */
    fun setCellAdapter(adapter: CellAdapter?) {
        this.cellAdapter = adapter; invalidate()
    }

    /** 设置动态颜色适配器 */
    fun setColorAdapter(adapter: ColorAdapter?) {
        this.colorAdapter = adapter; invalidate()
    }

    // --- 样式配置 Setters (支持链式调用的写法风格) ---

    fun setLabelPosition(pos: LabelPosition) {
        this.labelPosition = pos; requestLayout(); invalidate()
    }

    fun setHeaderPosition(pos: HeaderPosition) {
        this.headerPosition = pos; requestLayout(); invalidate()
    }

    fun setLabelTextSize(sizePx: Float) {
        this.labelTextSize = sizePx; requestLayout(); invalidate()
    }

    fun setHeaderTextSize(sizePx: Float) {
        this.headerTextSize = sizePx; requestLayout(); invalidate()
    }

    fun setLabelGridGap(gapInPx: Float) {
        this.labelGridGap = gapInPx; requestLayout(); invalidate()
    }

    fun setHeaderGridGap(gapInPx: Float) {
        this.headerGridGap = gapInPx; requestLayout(); invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        // 如果没有设置监听器，则不消耗事件，允许父布局拦截
        if (onCellClickListener == null) return super.onTouchEvent(event)

        if (event.action == MotionEvent.ACTION_UP) {
            handleCellClick(event.x, event.y)
            performClick()
        }
        return true
    }

    override fun performClick(): Boolean = super.performClick()

    /**
     * 处理点击坐标转换
     */
    private fun handleCellClick(x: Float, y: Float) {
        if (rowDataList.isEmpty() || cellSide <= 0) return

        // 1. 坐标系转换 (减去 Padding 和 布局偏移)
        val effectiveX = x - paddingLeft - gridOffsetX
        val effectiveY = y - paddingTop - gridOffsetY

        // 2. 网格整体边界检查
        val gridWidth = columnCount * (cellSide + cellGap) - cellGap
        val gridHeight = rowDataList.size * (cellSide + cellGap) - cellGap
        if (effectiveX < 0 || effectiveX > gridWidth || effectiveY < 0 || effectiveY > gridHeight) return

        // 3. 计算点击的行列索引
        val colIndex = (effectiveX / (cellSide + cellGap)).toInt()
        val rowIndex = (effectiveY / (cellSide + cellGap)).toInt()

        // 4. 精确判定 (确保点在方块内，而不是点在间隙里)
        if (rowIndex in rowDataList.indices && colIndex in 0 until columnCount) {
            val cellLeft = colIndex * (cellSide + cellGap)
            val cellTop = rowIndex * (cellSide + cellGap)

            if (effectiveX >= cellLeft && effectiveX <= cellLeft + cellSide &&
                effectiveY >= cellTop && effectiveY <= cellTop + cellSide
            ) {

                val data = rowDataList[rowIndex].cellData[colIndex]
                onCellClickListener?.onCellClick(rowIndex, colIndex, data)
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val totalWidth = MeasureSpec.getSize(widthMeasureSpec)
        val availableWidth = totalWidth - paddingLeft - paddingRight

        // A. 测量 Label 区域宽度
        textPaint.textSize = labelTextSize
        maxLabelWidth = 0f
        rowDataList.forEach {
            val w = textPaint.measureText(it.label)
            if (w > maxLabelWidth) maxLabelWidth = w
        }
        labelAreaTotalWidth = if (rowDataList.isNotEmpty()) maxLabelWidth + labelGridGap else 0f

        // B. 计算方块边长
        val gridAvailableWidth = availableWidth - labelAreaTotalWidth
        val validGridWidth = max(0f, gridAvailableWidth)
        cellSide = if (columnCount > 0) {
            (validGridWidth - ((columnCount - 1) * cellGap)) / columnCount
        } else 0f

        // C. 测量 Header 区域高度
        textPaint.textSize = headerTextSize
        val fm = textPaint.fontMetrics
        headerAreaTotalHeight = headerGridGap + abs(fm.ascent) + abs(fm.descent)

        // D. 预计算偏移量 (供 onDraw 和 onTouchEvent 共享)
        gridOffsetX = if (labelPosition == LabelPosition.LEFT) labelAreaTotalWidth else 0f
        gridOffsetY = if (headerPosition == HeaderPosition.TOP) headerAreaTotalHeight else 0f

        // E. 计算 View 总高度
        val rowCount = max(rowDataList.size, 0)
        val contentGridHeight = if (rowCount > 0) {
            (rowCount * cellSide) + ((rowCount - 1) * cellGap)
        } else 0f
        val finalHeight = contentGridHeight + headerAreaTotalHeight + paddingTop + paddingBottom
        setMeasuredDimension(totalWidth, finalHeight.toInt())
    }

    override fun onDraw(canvas: Canvas) {
        if (rowDataList.isEmpty()) return

        // 预计算字体 Metrics (垂直居中/基线)
        textPaint.textSize = labelTextSize
        var fm = textPaint.fontMetrics
        val labelBaselineOffset = -(fm.bottom + fm.top) / 2

        textPaint.textSize = headerTextSize
        fm = textPaint.fontMetrics
        val headerAscentAbs = abs(fm.ascent)

        rowDataList.forEachIndexed { rowIndex, row ->
            val gridTopY = paddingTop + gridOffsetY + rowIndex * (cellSide + cellGap)
            val gridBottomY = gridTopY + cellSide

            // --- 1. 绘制行标题 (Label) ---
            textPaint.color = labelTextColor
            textPaint.textSize = labelTextSize
            val textBaseY = gridTopY + cellSide / 2 + labelBaselineOffset

            if (labelPosition == LabelPosition.LEFT) {
                textPaint.textAlign = Paint.Align.LEFT
                canvas.drawText(row.label, paddingLeft.toFloat(), textBaseY, textPaint)
            } else {
                textPaint.textAlign = Paint.Align.RIGHT
                canvas.drawText(row.label, (width - paddingRight).toFloat(), textBaseY, textPaint)
            }

            // --- 2. 准备默认渐变 Shader (每行复用以提升性能) ---
            val activeShader = if (activeColorStart != activeColorEnd) {
                LinearGradient(0f, gridTopY, 0f, gridBottomY, activeColorStart, activeColorEnd, Shader.TileMode.CLAMP)
            } else null
            val inactiveShader = if (inactiveColorStart != inactiveColorEnd) {
                LinearGradient(0f, gridTopY, 0f, gridBottomY, inactiveColorStart, inactiveColorEnd, Shader.TileMode.CLAMP)
            } else null

            // --- 3. 绘制列 (网格单元格) ---
            for (colIndex in 0 until columnCount) {
                val leftX = paddingLeft + gridOffsetX + colIndex * (cellSide + cellGap)
                val rightX = leftX + cellSide
                tempCellRect.set(leftX, gridTopY, rightX, gridBottomY)

                val cellData = row.cellData[colIndex]
                val hasData = cellData != null

                // 颜色策略：ColorAdapter > 默认配置
                val dynamicColor = colorAdapter?.getCellColor(cellData)
                if (dynamicColor != null) {
                    cellPaint.shader = null
                    cellPaint.color = dynamicColor
                } else {
                    if (hasData) {
                        cellPaint.shader = activeShader
                        if (activeShader == null) cellPaint.color = activeColorStart
                    } else {
                        cellPaint.shader = inactiveShader
                        if (inactiveShader == null) cellPaint.color = inactiveColorStart
                    }
                }

                canvas.drawRoundRect(tempCellRect, cellCornerRadius, cellCornerRadius, cellPaint)

                // 回调自定义绘制
                if (hasData) {
                    cellAdapter?.onDrawCell(canvas, tempCellRect, rowIndex, colIndex, cellData)
                }

                // --- 4. 绘制列标题 (Header) ---
                // 仅在紧邻 Header 的那一行绘制
                val shouldDrawHeader = if (headerPosition == HeaderPosition.TOP) (rowIndex == 0) else (rowIndex == rowDataList.size - 1)
                if (shouldDrawHeader) {
                    textPaint.color = headerTextColor
                    textPaint.textSize = headerTextSize
                    textPaint.textAlign = Paint.Align.CENTER
                    val headerText = columnHeaders.getOrNull(colIndex) ?: ""

                    val headerY = if (headerPosition == HeaderPosition.TOP) {
                        paddingTop.toFloat() + headerAscentAbs
                    } else {
                        gridBottomY + headerGridGap + headerAscentAbs
                    }
                    canvas.drawText(headerText, leftX + cellSide / 2, headerY, textPaint)
                }
            }
        }
    }

    // --- 工具方法 ---
    private fun dpToPx(dp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, resources.displayMetrics)
    private fun spToPx(sp: Float) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, resources.displayMetrics)
}