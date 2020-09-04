package com.dede.tester.ui.svn

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.dede.tester.R
import com.dede.tester.ext.findCoordinator
import com.dede.tester.ext.getDownloadFile
import com.dede.tester.ext.installApk
import com.dede.tester.ext.isApk
import com.dede.tester.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_svn.*
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.SVNNodeKind
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SVNFragment : Fragment(), Runnable {

    private val svnViewModel: SVNViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels({ requireActivity() })

    private lateinit var defaultSvnUrl: SVNURL
    private lateinit var authenticationManager: ISVNAuthenticationManager

    private var currentSvnUrl: SVNURL
        get() = (recycler_view?.tag as? SVNURL) ?: defaultSvnUrl
        set(value) {
            recycler_view?.tag = value
        }

    private val backCallback = object : OnBackPressedCallback(true) {
        override fun handleOnBackPressed() {
            val svnurl = currentSvnUrl
            val canBack = if (svnurl == defaultSvnUrl) false else !svnViewModel.isRoot
            if (!canBack) {
                isEnabled = false
                showBackSnackBar()
                return
            }
            loadSVNTree(svnurl.removePathTail())// 加载上一级
        }
    }

    private val handler = Handler()

    override fun run() {
        backCallback.isEnabled = true
    }

    private fun showBackSnackBar() {
        handler.removeCallbacks(this)
        handler.postDelayed(this, 1000)
        Snackbar.make(findCoordinator(), "再按一次退出", Snackbar.LENGTH_SHORT).show()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        requireActivity().onBackPressedDispatcher.addCallback(this, backCallback)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val svnConfig =
            arguments?.getParcelable<SVNConfigViewModel.SVNConfig>(SVNConfigFragment.EXTRA_SVN_CONFIG)
        val noConfig = svnConfig == null || svnConfig.isEmpty()
        if (noConfig) {
            findNavController().popBackStack(R.id.nav_svn, true)
            findNavController().navigate(R.id.nav_svn_config)
        }
        val url = svnConfig?.svnUrl ?: SVNConfigFragment.DEFAULT_URL
        defaultSvnUrl = SVNURL.parseURIEncoded(url)
        authenticationManager = CustomAuthManager(svnConfig?.user, svnConfig?.password)

        svnViewModel.list.observe(viewLifecycleOwner, Observer {
            refresh_layout.isRefreshing = false
            (recycler_view.adapter as SVNTreeAdapter).refresh(it)
        })
        svnViewModel.error.observe(viewLifecycleOwner, Observer {
            bt_config.visibility = View.VISIBLE
            if (noConfig) {// 没有配置
                return@Observer
            }
            // 出错喽
            Toast.makeText(requireContext(), it.localizedMessage, Toast.LENGTH_LONG).show()
        })
        mainViewModel.sortType.observe(viewLifecycleOwner, Observer {
            svnViewModel.sortType = it// 排序方式更新
            refresh()
        })
        return inflater.inflate(R.layout.fragment_svn, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        recycler_view.adapter = SVNTreeAdapter()
        val drawable =
            ContextCompat.getDrawable(requireContext(), R.drawable.shape_divider_vertical)
        val itemDecoration = DividerItemDecoration(context, DividerItemDecoration.VERTICAL)
        itemDecoration.setDrawable(drawable!!)
        recycler_view.addItemDecoration(itemDecoration)
        refresh_layout.setColorSchemeResources(R.color.colorAccent, R.color.colorPrimary)
        refresh_layout.setOnRefreshListener {
            refresh()
        }

        bt_config.setOnClickListener {
            findNavController().navigate(R.id.action_svn_to_svn_config)
        }

        // 加载默认列表
        loadSVNTree(defaultSvnUrl)
    }

    /**
     * 刷新列表
     */
    private fun refresh() {
        loadSVNTree(currentSvnUrl)
    }

    private fun loadSVNTree(svnUrl: SVNURL) {
        bt_config.visibility = View.GONE
        mainViewModel.subTitle.value = svnUrl.path
        currentSvnUrl = svnUrl
        refresh_layout.isRefreshing = true
        svnViewModel.loadDirEntry(authenticationManager, svnUrl)
    }

    inner class SVNTreeAdapter : RecyclerView.Adapter<SVNTreeHolder>() {

        fun refresh(new: List<SVNDirEntry>) {
            data.clear()
            data.addAll(new)
            notifyDataSetChanged()
        }

        private val data = ArrayList<SVNDirEntry>()

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SVNTreeHolder {
            return SVNTreeHolder(parent)
        }

        override fun getItemCount(): Int {
            return data.size
        }

        private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

        private fun formatDate(date: Date): String {
            return format.format(date)
        }

        override fun onBindViewHolder(holder: SVNTreeHolder, position: Int) {
            val svnDirEntry = data[position]
            holder.tv_name.text = svnDirEntry.name
            val size = Formatter.formatFileSize(holder.itemView.context, svnDirEntry.size)
            val date = formatDate(svnDirEntry.date)
            val author = svnDirEntry.author
            val revision = svnDirEntry.revision
            when {
                svnViewModel.isParentSvnDirEntry(svnDirEntry) -> {
                    holder.tv_name.text = ".."
                    holder.tv_info.text = "返回上一层"
                    holder.iv_type.setImageResource(R.drawable.ic_folder)
                }
                svnDirEntry.kind == SVNNodeKind.DIR -> {
                    // 文件夹不显示大小
                    holder.tv_info.text = "$author   r${revision}\n${date}"
                    holder.iv_type.setImageResource(R.drawable.ic_folder)
                }
                else -> {
                    holder.tv_info.text = "$size   $author   r${revision}\n${date}"
                    holder.iv_type.setImageResource(R.drawable.ic_file)
                }
            }
            holder.itemView.setOnClickListener {
                when (svnDirEntry.kind) {
                    SVNNodeKind.DIR -> {
                        loadSVNTree(svnDirEntry.url)
                    }
                    SVNNodeKind.FILE -> {
                        download(svnDirEntry)
                    }
                    else -> {

                    }
                }
            }
        }
    }

    private fun download(svnDirEntry: SVNDirEntry) {
        if (svnViewModel.downloadStatus.value == true) {
            Snackbar.make(findCoordinator(), "有任务正在下载", Snackbar.LENGTH_SHORT).show()
            return
        }

        val context = requireContext()
        val downloadFile = context.getDownloadFile(svnDirEntry.name)
        if (!downloadFile.exists()) {
            AlertDialog.Builder(context)
                .setTitle("提示")
                .setMessage("是否下载${svnDirEntry.name}？")
                .setNegativeButton("取消", null)
                .setPositiveButton("下载") { _, _ ->
                    svnViewModel.download(context, authenticationManager, svnDirEntry)
                }
                .create()
                .show()
            return
        }

        val builder = AlertDialog.Builder(context)
            .setTitle("提示")
            .setMessage("${svnDirEntry.name}文件已存在，是否重新下载？")
            .setPositiveButton("重新下载") { _, _ ->
                svnViewModel.download(context, authenticationManager, svnDirEntry)
            }
        if (downloadFile.isApk(context)) {
            builder.setNegativeButton("直接安装") { _, _ ->
                installApk(context, downloadFile)
            }
        }

        builder.create().show()
    }

    class SVNTreeHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_svn_tree, parent, false)
    ) {
        val tv_name = itemView.findViewById<TextView>(R.id.tv_name)
        val tv_info = itemView.findViewById<TextView>(R.id.tv_info)
        val iv_type = itemView.findViewById<ImageView>(R.id.iv_type)
    }

}
