package com.example.myapplication.ui.home

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.myapplication.R
import com.example.myapplication.data.database.BookDatabase
import com.example.myapplication.data.repository.BookRepository
import com.example.myapplication.databinding.FragmentBySubjectBinding
import com.example.myapplication.ui.main.SubjectsAdapter
import com.example.myapplication.ui.viewmodel.BookViewModel
import com.example.myapplication.ui.viewmodel.BookViewModelFactory
import kotlinx.coroutines.launch

class BySubjectFragment : Fragment() {

    private lateinit var subjectsAdapter: SubjectsAdapter

    private val bookViewModel: BookViewModel by viewModels {
        BookViewModelFactory(BookRepository(BookDatabase.getDatabase(requireContext()).bookDao()))
    }

    private var _binding: FragmentBySubjectBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBySubjectBinding.inflate(inflater, container, false)
        val root: View = binding.root

        subjectsAdapter = SubjectsAdapter(mutableListOf())
        subjectsAdapter.setOnItemClickListener { subjectName: String ->
            findNavController().navigate(R.id.navigation_home, Bundle().apply {
                putString("filter", subjectName)
            })
        }

        binding.recyclerViewBySubject.apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = subjectsAdapter
        }

        bookViewModel.allBooks.observe(viewLifecycleOwner) { books ->
            viewLifecycleOwner.lifecycleScope.launch {
                val subjects = bookViewModel.getAllSubjects()
                val subjectNames = subjects.map { it.subjectName }
                println("BySubjectFragment: Observed books, found subjects: $subjectNames")
                subjectsAdapter.addSubjects(subjectNames)
                subjectsAdapter.notifyDataSetChanged()
            }
        }

        return root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}