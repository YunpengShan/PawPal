package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.adapter.RvPetsAdapter
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentSearchBinding
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.Pets

class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var petList: ArrayList<Pets>
    private lateinit var adapter: RvPetsAdapter
    private lateinit var userId: String
    private var currentQuery: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()

        userId = firebaseAuth.currentUser?.uid.toString()
        firestore = FirebaseFirestore.getInstance()

        setupRecyclerView()
        fetchData()

        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                currentQuery = query
                filterPets(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                currentQuery = newText
                filterPets(newText)
                return true
            }
        })

        return binding.root
    }

    private fun setupRecyclerView() {
        petList = arrayListOf()
        adapter = RvPetsAdapter(petList)

        binding.recyclerViewPets.layoutManager = LinearLayoutManager(context)
        binding.recyclerViewPets.adapter = adapter
    }

    private fun fetchData() {
        firestore.collection("pets")
            .whereEqualTo("ownerID", userId) // Filter pets by the owner's user ID
            .get()
            .addOnSuccessListener { querySnapshot ->
                petList.clear() // Clear the current list to avoid duplicates
                for (document in querySnapshot) {
                    val pet = document.toObject(Pets::class.java) // Convert Firestore document to Pets object
                    petList.add(pet) // Add the pet to the list
                }
                adapter.notifyDataSetChanged() // Notify the adapter of dataset changes
            }
            .addOnFailureListener { exception ->
                // Handle error
            }
    }

    private fun filterPets(query: String?) {
        val filteredList = petList.filter {
            it.name?.contains(query!!, true) == true || it.breed?.contains(query!!, true) == true
        }
        adapter = RvPetsAdapter(ArrayList(filteredList))
        binding.recyclerViewPets.adapter = adapter
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString("currentQuery", currentQuery)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            currentQuery = it.getString("currentQuery")
            currentQuery?.let { query ->
                binding.searchView.setQuery(query, false)
                filterPets(query)
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
