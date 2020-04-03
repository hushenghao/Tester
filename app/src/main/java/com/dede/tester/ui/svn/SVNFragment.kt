package com.dede.tester.ui.svn

import android.os.Bundle
import android.text.format.Formatter
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
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
import com.dede.tester.ui.MainViewModel
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.fragment_svn.*
import org.tmatesoft.svn.core.SVNDirEntry
import org.tmatesoft.svn.core.SVNNodeKind
import org.tmatesoft.svn.core.SVNURL
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class SVNFragment : Fragment() {

    private val svnViewModel: SVNViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels({ requireActivity() })

    private lateinit var defaultSvnUrl: SVNURL
    private lateinit var authenticationManager: ISVNAuthenticationManager

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
        authenticationManager =
            BasicAuthenticationManager.newInstance(
                svnConfig?.user,
                svnConfig?.password?.toCharArray()
            )
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
            val svnurl = refresh_layout.tag as SVNURL
            loadSVNTree(svnurl)
        }

        bt_config.setOnClickListener {
            findNavController().popBackStack(R.id.nav_svn, true)
            findNavController().navigate(R.id.nav_svn_config, Bundle().apply {
                putBoolean(SVNConfigFragment.EXTRA_AUTO_LOGIN, false)
            })
        }

        // 加载默认列表
        loadSVNTree(defaultSvnUrl)
    }

    private fun loadSVNTree(svnUrl: SVNURL) {
        mainViewModel.subTitle.value = svnUrl.path
        refresh_layout.tag = svnUrl
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
                    holder.tv_info.text = "${author}   r${revision}\n${date}"
                    holder.iv_type.setImageResource(R.drawable.ic_folder)
                }
                else -> {
                    holder.tv_info.text = "${size}   ${author}   r${revision}\n${date}"
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

        val downloadFile = svnViewModel.getDownloadFile(requireContext(), svnDirEntry)
        if (!downloadFile.exists()) {
            svnViewModel.download(requireContext(), authenticationManager, svnDirEntry)
            return
        }

        AlertDialog.Builder(requireContext())
            .setTitle("提示")
            .setMessage("${svnDirEntry.name}文件已存在，是否重新下载？")
            .setNegativeButton("直接安装") { _, _ ->
                svnViewModel.install(requireContext(), downloadFile)
            }
            .setPositiveButton("重新下载") { _, _ ->
                svnViewModel.download(requireContext(), authenticationManager, svnDirEntry)
            }
            .create()
            .show()
    }

    class SVNTreeHolder(parent: ViewGroup) : RecyclerView.ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_svn_tree, parent, false)
    ) {
        val tv_name = itemView.findViewById<TextView>(R.id.tv_name)
        val tv_info = itemView.findViewById<TextView>(R.id.tv_info)
        val iv_type = itemView.findViewById<ImageView>(R.id.iv_type)
    }

}
