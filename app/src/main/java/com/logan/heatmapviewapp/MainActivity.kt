package com.logan.heatmapviewapp

import android.annotation.SuppressLint
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.blue
import androidx.core.graphics.green
import androidx.core.graphics.red
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.logan.heatmapview.ActivityHeatmapView
import com.logan.heatmapviewapp.bean.sport.DailyStep
import com.logan.heatmapviewapp.bean.sport.UserData
import com.logan.heatmapviewapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import kotlin.random.Random

class MainActivity : AppCompatActivity() {

    // 使用 private const val 优于 companion object 里的变量
    private val binding by lazy { ActivityMainBinding.inflate(layoutInflater) }

    private val monthFormat = SimpleDateFormat("yyyy.MM", Locale.ENGLISH)
    private val dayFormat = SimpleDateFormat("MM.dd", Locale.ENGLISH)

    // 模拟周/月偏移量
    private var offsetView1 = 0 // View1 模拟月偏移
    private var offsetView2 = 0 // View2 模拟周偏移

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)

        setupInsets()
        setListeners()

        initActivityHeatmapView2Config()

        // 首次加载数据
        updateUI1()
        updateUI2()
    }

    private fun setupInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.toolbar) { view, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.updatePadding(top = insets.top)
            WindowInsetsCompat.CONSUMED
        }
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "ActivityHeatmapView"
    }

    /**
     * 初始化 View 的固定配置，避免在 updateUI 中重复创建对象
     */
    private fun initActivityHeatmapView2Config() {
        // 设置自定义绘制 (星星)
        // 提取 Paint 为成员变量或静态变量，避免在 onDrawCell 中重复创建
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.WHITE
            textSize = 24f
            textAlign = Paint.Align.CENTER
        }

        // 2. 预计算 4dp 的像素值 (用于底部边距)
        val bottomMarginPx = 4 * resources.displayMetrics.density

        binding.activityHeatmapView2.setCellAdapter(object : ActivityHeatmapView.CellAdapter {
            override fun onDrawCell(canvas: Canvas, cellRect: RectF, rowIndex: Int, colIndex: Int, data: Any?) {
                val stepData = data as? DailyStep ?: return

                // ---------------------------------------------------------
                // 绘制 A: 星星 (居中) - 仅步数 > 10000 显示
                // ---------------------------------------------------------
                if (stepData.count > 10000) {
                    textPaint.textSize = 24f // 星星字号
                    val fontMetrics = textPaint.fontMetrics
                    // 垂直居中计算公式
                    val baseline = cellRect.centerY() - (fontMetrics.bottom + fontMetrics.top) / 2
                    canvas.drawText("★", cellRect.centerX(), baseline, textPaint)
                }

                // ---------------------------------------------------------
                // 绘制 B: Steps (底部居中，距离底部 4dp)
                // ---------------------------------------------------------
                // 只有有步数才画，或者你可以决定 0 步是否画
                if (stepData.count > 0) {
                    textPaint.textSize = 16f // 步数字号 (建议比星星小一点)

                    // X轴: 方块中心
                    val x = cellRect.centerX()
                    // Y轴: 方块底部 - 4dp
                    val y = cellRect.bottom - bottomMarginPx

                    // 如果步数太长(比如 25000)，可以考虑转成 "25k" 格式，这里直接画数字
                    val text = stepData.count.toString()

                    canvas.drawText(text, x, y, textPaint)
                }
            }
        })

        // 设置点击事件
        binding.activityHeatmapView2.setOnCellClickListener { rowIndex, colIndex, data ->
            val stepData = data as? DailyStep
            Log.d("UserDailyData", "rowIndex=$rowIndex,colIndex=$colIndex,stepData.dayOfMonth=${stepData?.day},stepData.steps=${stepData?.count},")
            Toast.makeText(this, "rowIndex=$rowIndex,colIndex=$colIndex", Toast.LENGTH_SHORT).show()
        }

        // 设置动态颜色适配器
        binding.activityHeatmapView2.setColorAdapter { data ->
            val stepData = data as? DailyStep ?: return@setColorAdapter null

            // 步数颜色逻辑
            if (stepData.count < 10000) {
                return@setColorAdapter Color.TRANSPARENT // 步数太少显示灰色
            }
            // 根据步数计算深浅 (基准色 #ED1C91)
            val baseColor = Color.parseColor("#ED1C91")
            // 限制最大比例为 1.0，防止 Alpha 溢出
            val ratio = (stepData.count / 20000.0).coerceIn(0.2, 1.0)
            val alpha = (255 * ratio).toInt()

            Color.argb(alpha, baseColor.red, baseColor.green, baseColor.blue)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun setListeners() {
        // View 1 (月视图) 控制
        binding.btnPrevWeek1.setOnClickListener { offsetView1--; updateUI1() }
        binding.btnNextWeek1.setOnClickListener { offsetView1++; updateUI1() }

        // View 2 (周视图) 控制
        binding.btnPrevWeek2.setOnClickListener { offsetView2--; updateUI2() }
        binding.btnNextWeek2.setOnClickListener { offsetView2++; updateUI2() }
    }

    // 模拟月视图 (30天)
    private fun updateUI1() {
        // 1. 计算日期 (模拟按月切换)
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.MONTH, offsetView1)

        binding.tvDateRange1.text = monthFormat.format(calendar.time) // 显示 "2025 Oct"

        // 2. 生成数据
        val labels = listOf("Logan", "Allen", "Levi", "Nicely", "Vince", "Tim")
        val dataList = labels.map { name ->
            // 生成 1号到30号的数据
            // 随机挑选 15 天有数据
            val randomDays = (1..30).shuffled().take(15)
            val dailyRecords = randomDays.map { day ->
                DailyStep(day = day, count = (0..10000).random())
            }
            UserData(name = name, history = dailyRecords)
        }

        // 3. 设置给 View
        binding.activityHeatmapView1.setData(
            items = dataList,
            labelExtractor = { it.name },
            dataExtractor = { it.history },
            // indexMapper (可选) 确定数据属于第几列。根据规则计算 (适用于稀疏数据，如按日期存储)。
            indexMapper = { it.day - 1 },
            headers = (1..30).map { it.toString() } // 30列
        )
    }

    // 模拟周视图 (7天)


    private fun updateUI2() {
        // 1. 计算日期 (周)
        val calendar = Calendar.getInstance()
        calendar.firstDayOfWeek = Calendar.MONDAY
        calendar.add(Calendar.WEEK_OF_YEAR, offsetView2)

        // 算出本周一
        calendar.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        val startStr = dayFormat.format(calendar.time)

        // 算出本周日
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endStr = dayFormat.format(calendar.time)

        binding.tvDateRange2.text = "$startStr - $endStr"

        // 2. 生成数据
        // 模拟：生成周一(1)到周日(7)的数据
        val view2Labels = listOf("Logan", "Allen", "Levi", "Nicely")
        val dataList = view2Labels.map { name ->
            // 每天都生成数据，步数随机
            val dailyRecords = (1..7).map { dayOfWeek ->
                DailyStep(day = dayOfWeek, count = Random.nextInt(0, 25000))
            }
            UserData(name = name, history = dailyRecords)
        }

        val weekHeaders = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")

        // 3. 设置给 View (Adapter 已在 initViews 配置好，这里只传数据)
        binding.activityHeatmapView2.setData(
            items = dataList,
            labelExtractor = { it.name },
            dataExtractor = { it.history },
            // 如果没有 indexMapper：默认使用数据在 List 中的下标索引（适用于连续数据，比如列表里存了固定的周一到周日的7个数据）。
            indexMapper = null,
            headers = weekHeaders
        )
    }
}