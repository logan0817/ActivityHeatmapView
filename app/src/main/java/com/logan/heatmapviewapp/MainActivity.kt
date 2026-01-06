package com.logan.heatmapviewapp

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import com.logan.heatmapview.ActivityHeatmapView
import com.logan.heatmapviewapp.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class MainActivity : AppCompatActivity() {
    companion object {
        val TAG = "MainActivityTAG"
    }

    private var _binding: ActivityMainBinding? = null
    private val binding get() = _binding!!

    // 模拟当前显示的周偏移量：0 代表本周，-1 代表上周，1 代表下周
    private var weekOffsetOfView1 = 0
    private var weekOffsetOfView2 = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        _binding = ActivityMainBinding.inflate(layoutInflater)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(binding.root)
        setupInsets()
        setListeners()

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
        supportActionBar?.title = MainActivity::class.java.simpleName
    }

    @SuppressLint("SetTextI18n")
    private fun setListeners() {
        // 按钮点击事件
        binding.btnPrevWeek1.setOnClickListener {
            weekOffsetOfView1--
            updateUI1()
        }

        binding.btnNextWeek1.setOnClickListener {
            weekOffsetOfView1++
            updateUI1()
        }
        // 按钮点击事件
        binding.btnPrevWeek2.setOnClickListener {
            weekOffsetOfView2--
            updateUI2()
        }

        binding.btnNextWeek2.setOnClickListener {
            weekOffsetOfView2++
            updateUI2()
        }
    }


    private fun updateUI1() {
        val calendar = Calendar.getInstance()
        val firstDayOfWeek = Calendar.MONDAY
        // 设置到当前周的起始日
        calendar.firstDayOfWeek = firstDayOfWeek
        calendar.add(Calendar.WEEK_OF_YEAR, weekOffsetOfView1)
        calendar.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)

        val sdf = SimpleDateFormat("MMM dd", Locale.ENGLISH)
//        val sdf = SimpleDateFormat("dd.MM.yyyy.", Locale.ENGLISH)
        val startDate = sdf.format(calendar.time)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = sdf.format(calendar.time)

        binding.tvDateRange1.text = "$startDate - $endDate"


        val labels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
        // 2. 生成并设置动态行数的数据
        // 传入自定义 X 轴标签
        val dateLabels = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")

        val data = labels.map {
            // 1. 随机决定这一行有几天是激活的
            val activeCount = (1..12).random()

            // 2. 生成 0-12 的列表，打乱顺序，取前 activeCount 个，并转为 Set
            val randomIndices = (0..12).shuffled().take(activeCount).toSet()

            ActivityHeatmapView.RowData(it, randomIndices)
        }

        binding.activityHeatmapView1.setData(data, headers = dateLabels)

    }
    private fun updateUI2() {
        val calendar = Calendar.getInstance()
        val firstDayOfWeek = Calendar.MONDAY
        // 设置到当前周的起始日
        calendar.firstDayOfWeek = firstDayOfWeek
        calendar.add(Calendar.WEEK_OF_YEAR, weekOffsetOfView2)
        calendar.set(Calendar.DAY_OF_WEEK, firstDayOfWeek)

        val sdf = SimpleDateFormat("dd.MM.yyyy.", Locale.ENGLISH)
        val startDate = sdf.format(calendar.time)
        calendar.add(Calendar.DAY_OF_WEEK, 6)
        val endDate = sdf.format(calendar.time)

        binding.tvDateRange2.text = "$startDate - $endDate"


        val labels = listOf("Pulse", "Track", "Lift", "Strength")
        // 2. 生成并设置动态行数的数据
        // 传入自定义 X 轴标签
        val dateLabels = listOf("M", "T", "W", "T", "F", "S", "S")

        val data = labels.map {
            // 1. 随机决定这一行有几天是激活的
            val activeCount = (1..7).random()

            // 2. 生成 0-12 的列表，打乱顺序，取前 activeCount 个，并转为 Set
            val randomIndices = (0..7).shuffled().take(activeCount).toSet()

            ActivityHeatmapView.RowData(it, randomIndices)
        }

        binding.activityHeatmapView2.setData(data, headers = dateLabels)

    }
}
