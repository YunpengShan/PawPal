package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.adapter.RvPetsAdapter
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentSearchBinding
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.Pets
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import androidx.appcompat.widget.SearchView
import com.google.firebase.firestore.FirebaseFirestore


class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var petList: ArrayList<Pets>
    private lateinit var adapter: RvPetsAdapter
    private lateinit var userId: String

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

        // Handle search functionality
        binding.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterPets(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
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
                Log.e("Firestore", "Error fetching pets: ${exception.message}")
            }
    }

    private fun filterPets(query: String?) {
        val filteredList = petList.filter {
            it.name?.contains(query!!, true) == true || it.breed?.contains(query!!, true) == true
        }
        adapter = RvPetsAdapter(ArrayList(filteredList))
        binding.recyclerViewPets.adapter = adapter
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
