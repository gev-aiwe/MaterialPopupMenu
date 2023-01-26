package com.aiwe.app

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.Group
import androidx.core.view.isVisible
import com.aiwe.app.databinding.ActivityMainBinding
import com.github.aiwe.materialpopupmenu.MaterialPopupMenu
import com.github.aiwe.materialpopupmenu.MaterialPopupMenuBuilder
import com.github.aiwe.materialpopupmenu.popupMenu

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: Adapter

    private val mapOfUsers = mutableMapOf<Model, List<User>>()

    private var popupMenu: MaterialPopupMenu? = null

    private val handler = Handler(Looper.getMainLooper())

    private val runnable: PopupRunnable = object : PopupRunnable() {

        override fun run() {
            updateDynamicSections(users)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        adapter = Adapter { model, itemView ->
            val users = mapOfUsers[model]
            if (!users.isNullOrEmpty()) {
                showSimplePopup(users, itemView)
//                showDynamicPopup(users, itemView)
            }
        }
        binding.recyclerView.adapter = adapter
        initAdapter()
    }

    private fun initAdapter() {
        val modelList = mutableListOf<Model>()
        for (i in 0..19) {
            val model = Model("Item $i")
            modelList.add(model)
            val users = mutableListOf<User>()
            for (j in 0..i) {
                val user = User("Name $j")
                users.add(user)
            }
            mapOfUsers[model] = users
        }
        adapter.items = modelList
    }

    private fun showSimplePopup(users: List<User>, itemView: View) {
        popupMenu = popupMenu(true) {
            style = R.style.Widget_MPM_Menu_CustomBackground
            dropdownGravity = Gravity.END or Gravity.BOTTOM
            dropDownVerticalOffset = 0
            dropDownHorizontalOffset = 0
            section {
                users.forEach { user ->
                    customItem {
                        this.layoutResId = R.layout.user_item_rv
                        callback = { Toast.makeText(this@MainActivity, "Click ${user.name}", Toast.LENGTH_SHORT).show() }
                        viewBoundCallback = {
                            it.findViewById<Group>(R.id.group).isVisible = true
                            it.findViewById<ProgressBar>(R.id.progress).isVisible = false
                            it.findViewById<TextView>(R.id.title).text = user.name
                        }
                    }
                }
            }

        }
        popupMenu?.show(this, itemView)
    }

    private fun showDynamicPopup(users: List<User>, itemView: View) {
        handler.removeCallbacks(runnable)
        popupMenu = popupMenu(needDrawAnchor = true) {
            style = R.style.Widget_MPM_Menu_CustomBackground
            dropdownGravity = Gravity.END or Gravity.BOTTOM
            dropDownVerticalOffset = 0
            dropDownHorizontalOffset = 0
            section {
                customItem {
                    this.layoutResId = R.layout.user_item_rv
                    viewBoundCallback = {
                        it.findViewById<Group>(R.id.group).isVisible = false
                        it.findViewById<ProgressBar>(R.id.progress).isVisible = true
                    }
                }
            }
        }
        popupMenu?.show(this, itemView)
        runnable.users = users
        handler.postDelayed(runnable, 2000L)
    }

    private fun updateDynamicSections(users: List<User>) {
        val section = MaterialPopupMenuBuilder.SectionHolder()
        users.forEach { user ->
            section.customItem {
                this.layoutResId = R.layout.user_item_rv
                callback = { Toast.makeText(this@MainActivity, "Click ${user.name}", Toast.LENGTH_SHORT).show() }
                viewBoundCallback = {
                    it.findViewById<Group>(R.id.group).isVisible = true
                    it.findViewById<ProgressBar>(R.id.progress).isVisible = false
                    it.findViewById<TextView>(R.id.title).text = user.name
                }
            }
        }
        popupMenu?.updateSections(listOf(section))
    }
}