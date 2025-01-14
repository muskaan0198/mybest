package com.tft.selfbest.ui.fragments.activityLog

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tft.selfbest.R
import com.tft.selfbest.databinding.FragmentActivityLogBinding
import com.tft.selfbest.models.SelectedCategory
import com.tft.selfbest.network.NetworkResponse
import com.tft.selfbest.ui.adapter.*
import com.tft.selfbest.ui.dialog.ActivityLogFiltersDialog
import com.tft.selfbest.ui.fragments.statistics.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ActivityLogFragment(
    override var defaultCategories: MutableList<SelectedCategory>,
    var selectedPlatform: String,
    var selectedDuration: String,
    var startDate1: String,
    var endDate1: String,
    var firstTime: Boolean
) :
    Fragment(), View.OnClickListener,
    QueryResponseAdapter.ChangeStatusListener, ActivityLogFiltersDialog.FilterListener,
    AnsweredQueryAdapter.ChangeStatusListener, ForCategories {
    lateinit var binding: FragmentActivityLogBinding
    val viewModel by viewModels<StatisticsViewModel>()
    var categories: List<SelectedCategory> = listOf()
    lateinit var startDate: String
    val startDateCal = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    //var selectedPlatform = "Mobile"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentActivityLogBinding.inflate(layoutInflater)
        startDate = dateFormat.format(startDateCal.time)
//        setRadarChart()
        setClickListeners()
        binding.fabFilter.text = selectedPlatform
        binding.fabFilter.extend()
        binding.RadarChart.setTouchEnabled(false)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.fabFilter.shrink()
        }, 4000)
        //sample data
//        categories = listOf(
//            SelectedCategory("Development", false, 1.43),
//            SelectedCategory("Documentation", true, 521.577),
//            SelectedCategory("Management & Planning", true, 78.9),
//            SelectedCategory("Designing", true, 940.0),
//            SelectedCategory("Marketing & Sales", false, 379.0),
//            SelectedCategory("Collaboration", false, 39.0)
//        )

        viewModel.queryObserver.observe(viewLifecycleOwner) {
            if (it is NetworkResponse.Success) {
                binding.hoursSavedSp.text = it.data!!.hours_saved
            }
        }
        val adapter = activity?.let { ViewPagerQueryAdapter(it.supportFragmentManager, lifecycle) }
        binding.viewpager.adapter = adapter
        binding.tabLayout.tabMode = TabLayout.MODE_FIXED

        TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Asked"
                }
                1 -> {
                    tab.text = "Answered"
                }
                else -> {
                    tab.text = "Asked"
                }
            }

        }.attach()


        if(selectedDuration.equals("custom"))
            viewModel.getLogs(selectedPlatform, selectedDuration, startDate1, endDate1)
        else
            viewModel.getLogs(selectedPlatform, selectedDuration)
        viewModel.activityLogsObserver.observe(viewLifecycleOwner) {
            if (it is NetworkResponse.Success) {
                //binding.progress.visibility = View.GONE
                val list: ArrayList<RadarEntry> = ArrayList()
                val labels: MutableList<String> = mutableListOf()
                binding.hoursSaved.text = getTimeInFormat(it.data!!.focusTime)
                binding.timeSaved.text = getTimeInFormat(it.data.timeSaved)
                binding.distractedTime.text = getTimeInFormat(it.data.distractedTime)

                binding.breakTime.text = getTimeInFormat(it.data.defaultList.breakTime)
                binding.others.text = getTimeInFormat(it.data.defaultList.others)
                binding.distractionTime1.text = getTimeInFormat(it.data.defaultList.distraction)
                list.add(RadarEntry(it.data.defaultList.breakTime.toFloat()))
                labels.add("Break")
                list.add(RadarEntry(it.data.defaultList.others.toFloat()))
                labels.add("Others")
                list.add(RadarEntry(it.data.defaultList.distraction.toFloat()))
                labels.add("Distraction")

                if (it.data.selectedCategories != null) {
                    categories = it.data.selectedCategories
                    Log.e("Default Categories 0", getSelectedCategories().toString())
                    if (getSelectedCategories().isEmpty() && firstTime) {
                        val a = categories.sortedByDescending { it -> it.duration }
                            .take(3) as MutableList<SelectedCategory>
                        setSelectedCategories(categories.sortedByDescending { it -> it.duration }
                            .take(3) as MutableList<SelectedCategory>)
                        Log.e("Default Categories 0.1", a.toString())
                    }
                }else{
                    if (getSelectedCategories().isEmpty() && firstTime) {
                        setSelectedCategories(mutableListOf())
                    }
                }
                for (cat in getSelectedCategories()) {
                    list.add(RadarEntry(cat.duration.toFloat()))
                    labels.add(
                        if (cat.category.length >= 10) cat.category.substring(
                            0,
                            10
                        ) else cat.category
                    )
                }
                setRadarChart(list, labels)
                binding.categoryList.layoutManager = LinearLayoutManager(requireContext())
                Log.e("Selected Categories", getSelectedCategories().toString())
                binding.categoryList.adapter = CategoryAdapter(
                    binding.root.context,
                    getSelectedCategories().sortedByDescending { it -> it.duration }
                )

            }else if(it is NetworkResponse.Error){
               // binding.progress.visibility = View.GONE
                Toast.makeText(context, "Something went wrong", Toast.LENGTH_SHORT).show()
            }
        }

        return binding.root
    }

    private fun setClickListeners() {
        binding.workDen.setOnClickListener(this)
        binding.solutionPoint.setOnClickListener(this)
        //binding.selectCatContainer.setOnClickListener(this)
        binding.fabFilter.setOnClickListener(this)
    }

    private fun setRadarChart(list: ArrayList<RadarEntry>, labels: List<String>) {
        //binding.RadarChart.setBackgroundColor(Color.parseColor("#accbff"))
        binding.RadarChart.webLineWidth = 2f
        binding.RadarChart.webColor = Color.BLUE
        //binding.RadarChart.webLineWidthInner = 2f
        binding.RadarChart.webColorInner = Color.BLACK
        binding.RadarChart.webAlpha = 100

//        list.add(RadarEntry(100f))
//        list.add(RadarEntry(101f))
//        list.add(RadarEntry(102f))
//        list.add(RadarEntry(103f))
//        list.add(RadarEntry(104f))

        val radarDataSet = RadarDataSet(list, "List")
//        radarDataSet.setColors(ColorTemplate.MATERIAL_COLORS, 255)
        radarDataSet.color = Color.rgb(0, 255, 0)
        radarDataSet.fillColor = Color.rgb(0, 255, 0)
        radarDataSet.setDrawFilled(true)
        //radarDataSet.highLightColor = Color.rgb(0, 255, 0);
        radarDataSet.lineWidth = 2f
        radarDataSet.valueTextColor = Color.RED
        radarDataSet.valueTextSize = 14f
        radarDataSet.setDrawValues(false)
        radarDataSet.setDrawVerticalHighlightIndicator(true)
        //radarDataSet.setDrawCircles(true)

        val radarData = RadarData()
        radarData.addDataSet(radarDataSet)

//        val labels = listOf("Documenting", "Coding", "Break", "Distraction", "Other")
        val xAxis = binding.RadarChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textSize = 12f

        binding.RadarChart.data = radarData
        //binding.RadarChart.getAxisLeft().setDrawLabels(false);
        binding.RadarChart.yAxis.setDrawLabels(false);
        //binding.RadarChart.getXAxis().setDrawLabels(false);
        binding.RadarChart.description.text = ""
        binding.RadarChart.legend.isEnabled = false
        binding.RadarChart.animateY(2000)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.work_den -> {
                binding.solutionPointContainer.visibility = View.GONE
                binding.workDenContainer.visibility = View.VISIBLE
                binding.workDen.setTextColor(Color.parseColor("#FFFFFF"))
                binding.solutionPoint.setTextColor(Color.parseColor("#C8C8C8"))
                binding.workDen.setBackgroundResource(R.drawable.work_den_bg)
                binding.solutionPoint.setBackgroundColor(Color.parseColor("#FFFFFF"))
                binding.fabFilter.visibility = View.VISIBLE
            }

            R.id.solution_point -> {
                binding.workDenContainer.visibility = View.GONE
                binding.solutionPointContainer.visibility = View.VISIBLE
                binding.solutionPoint.setTextColor(Color.parseColor("#FFFFFF"))
                binding.workDen.setTextColor(Color.parseColor("#C8C8C8"))
                binding.workDen.setBackgroundColor(Color.parseColor("#FFFFFF"))
                binding.solutionPoint.setBackgroundResource(R.drawable.work_den_bg)
                binding.fabFilter.visibility = View.GONE
                viewModel.getQuery(startDate, "daily")
            }

//            R.id.select_cat_container -> {
//                if (binding.categoryContainer.isVisible) {
//                    binding.categoryContainer.visibility = View.GONE
//                } else {
//                    binding.categoryContainer.visibility = View.VISIBLE
//                }
//            }

            R.id.fab_filter -> {
                val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(
                    R.id.fragmentContainerView,
                    ActivityLogFiltersDialog(this, categories, getSelectedCategories())
                )
                transaction.addToBackStack(null)
                transaction.commit()
//                ActivityLogFiltersDialog(
//                    this
//                ).show(
//                    (activity as HomeActivity).supportFragmentManager,
//                    "ActivityLogFilter"
//                )
            }
        }
    }

    override fun changeStatus(id: Int, status: Int) {
        Log.e("Status ", "2")
        viewModel.updateStatus(id, status)
    }

    override fun filterData(platform: String, duration: String) {
        selectedPlatform = platform
        binding.fabFilter.text = selectedPlatform
        binding.fabFilter.extend()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.fabFilter.shrink()
        }, 4000)
        viewModel.getLogs(platform, duration)
    }

    override fun filterData(
        platform: String,
        duration: String,
        startDate: String,
        endDate: String
    ) {
        selectedPlatform = platform
        binding.fabFilter.text = selectedPlatform
        binding.fabFilter.extend()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.fabFilter.shrink()
        }, 4000)
        viewModel.getLogs(platform, duration, startDate, endDate)
    }

    private fun getTimeInFormat(time: Double): String {
        val ans: String
        val hours = time.toInt() / 3600
        val remainder = time.toInt() - hours * 3600
        val mins = remainder / 60
        val formatter = DecimalFormat("00");
        ans = "${formatter.format(hours)}h : ${formatter.format(mins)}m"
        return ans
    }
}
/*

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.viewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.mikephil.charting.data.RadarData
import com.github.mikephil.charting.data.RadarDataSet
import com.github.mikephil.charting.data.RadarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.utils.ColorTemplate
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tft.selfbest.R
import com.tft.selfbest.databinding.FragmentActivityLogBinding
import com.tft.selfbest.models.SelectedCategory
import com.tft.selfbest.network.NetworkResponse
import com.tft.selfbest.ui.adapter.*
import com.tft.selfbest.ui.dialog.ActivityLogFiltersDialog
import com.tft.selfbest.ui.fragments.statistics.StatisticsViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

@AndroidEntryPoint
class ActivityLogFragment(
    override var defaultCategories: MutableList<SelectedCategory>,
    var selectedPlatform: String,
    var selectedDuration: String,
    var startDate1: String,
    var endDate1: String,
    var firstTime: Boolean
) :
    Fragment(), View.OnClickListener,
    QueryResponseAdapter.ChangeStatusListener, ActivityLogFiltersDialog.FilterListener,
    AnsweredQueryAdapter.ChangeStatusListener, ForCategories {
    lateinit var binding: FragmentActivityLogBinding
    val viewModel by viewModels<StatisticsViewModel>()
    var categories: List<SelectedCategory> = listOf()
    lateinit var startDate: String
    val startDateCal = Calendar.getInstance()
    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.US)
    //var selectedPlatform = "Mobile"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        binding = FragmentActivityLogBinding.inflate(layoutInflater)
        startDate = dateFormat.format(startDateCal.time)
//        setRadarChart()
        setClickListeners()
        binding.fabFilter.text = selectedPlatform
        binding.fabFilter.extend()
        binding.RadarChart.setTouchEnabled(false)
        Handler(Looper.getMainLooper()).postDelayed({
            binding.fabFilter.shrink()
        }, 4000)
        //sample data
//        categories = listOf(
//            SelectedCategory("Development", false, 1.43),
//            SelectedCategory("Documentation", true, 521.577),
//            SelectedCategory("Management & Planning", true, 78.9),
//            SelectedCategory("Designing", true, 940.0),
//            SelectedCategory("Marketing & Sales", false, 379.0),
//            SelectedCategory("Collaboration", false, 39.0)
//        )

        viewModel.queryObserver.observe(viewLifecycleOwner) {
            if (it is NetworkResponse.Success) {
                binding.hoursSavedSp.text = it.data!!.hours_saved
            }
        }
        val adapter = activity?.let { ViewPagerQueryAdapter(it.supportFragmentManager, lifecycle) }
        binding.viewpager.adapter = adapter
        binding.tabLayout.tabMode = TabLayout.MODE_FIXED

        TabLayoutMediator(binding.tabLayout, binding.viewpager) { tab, position ->
            when (position) {
                0 -> {
                    tab.text = "Asked"
                }
                1 -> {
                    tab.text = "Answered"
                }
            }

        }.attach()


        if(selectedDuration.equals("custom"))
            viewModel.getLogs(selectedPlatform, selectedDuration, startDate1, endDate1)
        else
            viewModel.getLogs(selectedPlatform, selectedDuration)
        viewModel.activityLogsObserver.observe(viewLifecycleOwner) {
            if (it is NetworkResponse.Success) {
                val list: ArrayList<RadarEntry> = ArrayList()
                val labels: MutableList<String> = mutableListOf()
                binding.hoursSaved.text = getTimeInFormat(it.data!!.focusTime)
                binding.timeSaved.text = getTimeInFormat(it.data.timeSaved)
                binding.distractedTime.text = getTimeInFormat(it.data.distractedTime)

                binding.breakTime.text = getTimeInFormat(it.data.defaultList.breakTime)
                binding.others.text = getTimeInFormat(it.data.defaultList.others)
                binding.distractionTime1.text = getTimeInFormat(it.data.defaultList.distraction)
                list.add(RadarEntry(it.data.defaultList.breakTime.toFloat()))
                labels.add("Break")
                list.add(RadarEntry(it.data.defaultList.others.toFloat()))
                labels.add("Others")
                list.add(RadarEntry(it.data.defaultList.distraction.toFloat()))
                labels.add("Distraction")

                categories = it.data.selectedCategories
                Log.e("Default Categories 0", getSelectedCategories().toString())
                if (getSelectedCategories().isEmpty() && firstTime) {
                    val a = categories.sortedByDescending { it -> it.duration }
                        .take(3) as MutableList<SelectedCategory>
                    setSelectedCategories(categories.sortedByDescending { it -> it.duration }
                        .take(3) as MutableList<SelectedCategory>)
                    Log.e("Default Categories 0.1", a.toString())
                }
                for (cat in getSelectedCategories()) {
                    list.add(RadarEntry(cat.duration.toFloat()))
                    labels.add(cat.category.substring(0, 10))
                }
                setRadarChart(list, labels)
                binding.categoryList.layoutManager = LinearLayoutManager(requireContext())
                Log.e("Selected Categories", getSelectedCategories().toString())
                binding.categoryList.adapter = CategoryAdapter(
                    binding.root.context,
                    getSelectedCategories().sortedByDescending { it -> it.duration }
                )

            }
        }

        return binding.root
    }

    private fun setClickListeners() {
        binding.workDen.setOnClickListener(this)
        binding.solutionPoint.setOnClickListener(this)
        //binding.selectCatContainer.setOnClickListener(this)
        binding.fabFilter.setOnClickListener(this)
    }

    private fun setRadarChart(list: ArrayList<RadarEntry>, labels: List<String>) {
        //binding.RadarChart.setBackgroundColor(Color.parseColor("#accbff"))
        binding.RadarChart.webLineWidth = 2f
        binding.RadarChart.webColor = Color.BLUE
        //binding.RadarChart.webLineWidthInner = 2f
        binding.RadarChart.webColorInner = Color.BLACK
        binding.RadarChart.webAlpha = 100

//        list.add(RadarEntry(100f))
//        list.add(RadarEntry(101f))
//        list.add(RadarEntry(102f))
//        list.add(RadarEntry(103f))
//        list.add(RadarEntry(104f))

        val radarDataSet = RadarDataSet(list, "List")
//        radarDataSet.setColors(ColorTemplate.MATERIAL_COLORS, 255)
        radarDataSet.color = Color.rgb(0, 255, 0)
        radarDataSet.fillColor = Color.rgb(0, 255, 0)
        radarDataSet.setDrawFilled(true)
        //radarDataSet.highLightColor = Color.rgb(0, 255, 0);
        radarDataSet.lineWidth = 2f
        radarDataSet.valueTextColor = Color.RED
        radarDataSet.valueTextSize = 14f
        radarDataSet.setDrawValues(false)
        radarDataSet.setDrawVerticalHighlightIndicator(true)
        //radarDataSet.setDrawCircles(true)

        val radarData = RadarData()
        radarData.addDataSet(radarDataSet)

//        val labels = listOf("Documenting", "Coding", "Break", "Distraction", "Other")
        val xAxis = binding.RadarChart.xAxis
        xAxis.valueFormatter = IndexAxisValueFormatter(labels)
        xAxis.textSize = 12f

        binding.RadarChart.data = radarData
        //binding.RadarChart.getAxisLeft().setDrawLabels(false);
        binding.RadarChart.yAxis.setDrawLabels(false);
        //binding.RadarChart.getXAxis().setDrawLabels(false);
        binding.RadarChart.description.text = ""
        binding.RadarChart.legend.isEnabled = false
        binding.RadarChart.animateY(2000)
    }

    override fun onClick(p0: View?) {
        when (p0?.id) {
            R.id.work_den -> {
                binding.solutionPointContainer.visibility = View.GONE
                binding.workDenContainer.visibility = View.VISIBLE
                binding.workDen.setTextColor(Color.parseColor("#FFFFFF"))
                binding.solutionPoint.setTextColor(Color.parseColor("#C8C8C8"))
                binding.workDen.setBackgroundResource(R.drawable.work_den_bg)
                binding.solutionPoint.setBackgroundColor(Color.parseColor("#FFFFFF"))
                binding.fabFilter.visibility = View.VISIBLE
            }

            R.id.solution_point -> {
                binding.workDenContainer.visibility = View.GONE
                binding.solutionPointContainer.visibility = View.VISIBLE
                binding.solutionPoint.setTextColor(Color.parseColor("#FFFFFF"))
                binding.workDen.setTextColor(Color.parseColor("#C8C8C8"))
                binding.workDen.setBackgroundColor(Color.parseColor("#FFFFFF"))
                binding.solutionPoint.setBackgroundResource(R.drawable.work_den_bg)
                binding.fabFilter.visibility = View.GONE
                viewModel.getQuery(startDate, "daily")
            }

//            R.id.select_cat_container -> {
//                if (binding.categoryContainer.isVisible) {
//                    binding.categoryContainer.visibility = View.GONE
//                } else {
//                    binding.categoryContainer.visibility = View.VISIBLE
//                }
//            }

            R.id.fab_filter -> {
                val fragmentManager: FragmentManager = requireActivity().supportFragmentManager
                val transaction = fragmentManager.beginTransaction()
                transaction.replace(
                    R.id.fragmentContainerView,
                    ActivityLogFiltersDialog(this, categories, getSelectedCategories())
                )
                transaction.addToBackStack(null)
                transaction.commit()
//                ActivityLogFiltersDialog(
//                    this
//                ).show(
//                    (activity as HomeActivity).supportFragmentManager,
//                    "ActivityLogFilter"
//                )
            }
        }
    }

    override fun changeStatus(id: Int, status: Int) {
        Log.e("Status ", "2")
        viewModel.updateStatus(id, status)
    }

    override fun filterData(platform: String, duration: String) {
        selectedPlatform = platform
        binding.fabFilter.text = selectedPlatform
        binding.fabFilter.extend()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.fabFilter.shrink()
        }, 4000)
        viewModel.getLogs(platform, duration)
    }

    override fun filterData(
        platform: String,
        duration: String,
        startDate: String,
        endDate: String
    ) {
        selectedPlatform = platform
        binding.fabFilter.text = selectedPlatform
        binding.fabFilter.extend()
        Handler(Looper.getMainLooper()).postDelayed({
            binding.fabFilter.shrink()
        }, 4000)
        viewModel.getLogs(platform, duration, startDate, endDate)
    }

    private fun getTimeInFormat(time: Double): String {
        val ans: String
        val hours = time.toInt() / 3600
        val remainder = time.toInt() - hours * 3600
        val mins = remainder / 60
        val formatter = DecimalFormat("00");
        ans = "${formatter.format(hours)}h : ${formatter.format(mins)}m"
        return ans
    }
}

*/
