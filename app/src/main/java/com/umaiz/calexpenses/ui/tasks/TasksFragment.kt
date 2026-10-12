package com.umaiz.calexpenses.ui.tasks

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.MainActivity
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.databinding.FragmentTasksBinding
import com.umaiz.calexpenses.util.Notifications
import kotlinx.coroutines.launch

class TasksFragment : Fragment() {

    private var _b: FragmentTasksBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository
    private lateinit var adapter: TaskAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentTasksBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.nav_tasks)

        adapter = TaskAdapter(
            emptyList(),
            onToggle = { t, checked -> lifecycleScope.launch { repo.updateTask(t.copy(done = checked)) } },
            onClick = { t -> TaskSheet(t).show(childFragmentManager, "task") },
            onDelete = { t ->
                MaterialAlertDialogBuilder(requireContext())
                    .setTitle("Delete task?")
                    .setNegativeButton(R.string.cancel, null)
                    .setPositiveButton(R.string.delete) { _, _ ->
                        Notifications.cancel(requireContext().applicationContext, t.id.toInt())
                        lifecycleScope.launch { repo.deleteTask(t) }
                    }.show()
            }
        )
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter
        b.fabAdd.setOnClickListener { TaskSheet(null).show(childFragmentManager, "task") }

        repo.tasks.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
