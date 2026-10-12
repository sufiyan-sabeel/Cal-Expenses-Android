package com.umaiz.calexpenses.ui.notes

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.umaiz.calexpenses.CalExpensesApp
import com.umaiz.calexpenses.MainActivity
import com.umaiz.calexpenses.R
import com.umaiz.calexpenses.databinding.FragmentNotesBinding

class NotesFragment : Fragment() {

    private var _b: FragmentNotesBinding? = null
    private val b get() = _b!!
    private val repo get() = (requireActivity().application as CalExpensesApp).repository
    private lateinit var adapter: NoteAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View = FragmentNotesBinding.inflate(inflater, container, false).also { _b = it }.root

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        (activity as? MainActivity)?.supportActionBar?.title = getString(R.string.nav_notes)

        adapter = NoteAdapter(emptyList()) { n -> NoteSheet(n).show(childFragmentManager, "note") }
        b.recycler.layoutManager = LinearLayoutManager(requireContext())
        b.recycler.adapter = adapter
        b.fabAdd.setOnClickListener { NoteSheet(null).show(childFragmentManager, "note") }

        repo.notes.observe(viewLifecycleOwner) { list ->
            adapter.submit(list)
            b.tvEmpty.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
        }
    }

    override fun onDestroyView() { super.onDestroyView(); _b = null }
}
