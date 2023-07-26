package five.ec1cff.scrcpy.launcher

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import five.ec1cff.scrcpy.*
import five.ec1cff.scrcpy.databinding.ActivityLauncherBinding
import five.ec1cff.scrcpy.databinding.ViewRecentTaskItemBinding
import five.ec1cff.scrcpy.util.ActivityTaskManagerWrapper
import five.ec1cff.scrcpy.util.ActivityTaskManagerWrapper.startActivity
import kotlin.properties.Delegates

class RecentTaskListAdapter(
    private val launcher: ActivityLauncher) : RecyclerView.Adapter<RecentTaskListAdapter.ViewHolder>() {

    private val taskList: MutableList<TaskViewModel.RecentTask> = mutableListOf()

    fun updateList(newList: List<TaskViewModel.RecentTask>) {
        taskList.clear()
        taskList.addAll(newList)
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val title: TextView
        val detail: TextView
        val icon: ImageView
        val root: View

        init {
            ViewRecentTaskItemBinding.bind(view).let {
                title = it.title
                detail = it.detail
                root = it.root
                icon = it.icon
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder =
        LayoutInflater.from(parent.context)
        .inflate(R.layout.view_recent_task_item, parent, false)
        .let { ViewHolder(it) }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val task = taskList[position]
        holder.apply {
            title.text = task.label
            detail.text = "${task.taskId}"
            icon.setImageDrawable(task.icon)
            root.setOnClickListener {
                launcher.startTask(task.taskInfo)
            }
        }
    }

    override fun getItemCount(): Int = taskList.size
}

class ActivityLauncher(var displayId: Int) {
    fun startTask(task: ActivityManager.RecentTaskInfo) {
        // ActivityTaskManagerWrapper.startTaskOnDisplay(taskId, displayId)
        App.taskController.startRecentTaskOnDisplay(task, displayId)
    }
}

class LauncherActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_DISPLAY_ID = "five.ec1cff.launcher.DISPLAY_ID"

        fun showLauncherOnDisplay(displayId: Int) {
            // val taskId = ScrcpyShizukuService.launcherTasks.get(displayId)
            // if (taskId == null || taskId == -1) {
                val launcher = Intent()
                launcher.putExtra(EXTRA_DISPLAY_ID, displayId)
                launcher.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                launcher.component = ComponentName(
                    BuildConfig.APPLICATION_ID,
                    LauncherActivity::class.java.name
                )
                val options = ActivityOptions.makeBasic()
                options.launchDisplayId = displayId
                startActivity(launcher, options)
            // } else {
            //     ActivityTaskManagerWrapper.startTaskOnDisplay(taskId, displayId)
            // }
        }
    }

    private lateinit var binding: ActivityLauncherBinding
    private lateinit var adapter: RecentTaskListAdapter
    private lateinit var launcher: ActivityLauncher
    private lateinit var model: TaskViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        model = ViewModelProvider(this)[TaskViewModel::class.java]
        binding = ActivityLauncherBinding.inflate(layoutInflater)
        launcher = ActivityLauncher(ActivityTaskManagerWrapper.getDisplayIdForActivity(this))
        adapter = RecentTaskListAdapter(launcher)
        binding.recentTaskList.let {
            it.adapter = adapter
            it.layoutManager = LinearLayoutManager(
                this, LinearLayoutManager.VERTICAL, false
            )
        }
        binding.refresh.setOnRefreshListener {
            updateTasks {
                binding.refresh.isRefreshing = false
            }
        }
        val view = binding.root
        setContentView(view)
    }

    private fun updateTasks(callback: (() -> Unit)? = null) {
        model.loadRecentTasks(this) {
            runOnUiThread {
                adapter.updateList(model.recentTasks)
                if (callback != null) callback()
            }
        }
    }

    override fun onResume() {
        super.onResume()
        launcher.displayId = ActivityTaskManagerWrapper.getDisplayIdForActivity(this)
        updateTasks()
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    override fun onBackPressed() {
    }
}