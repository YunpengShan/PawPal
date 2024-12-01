package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
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


class SearchFragment : Fragment() {
    private var _binding: FragmentSearchBinding? = null
    private val binding get() = _binding!!

    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var firebaseDatabase: DatabaseReference
    private lateinit var petList: ArrayList<Pets>
    private lateinit var adapter: RvPetsAdapter

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSearchBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()

        val userId = firebaseAuth.currentUser?.uid
        firebaseDatabase = FirebaseDatabase.getInstance().getReference("users/$userId/pets")

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
        firebaseDatabase.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                petList.clear()
                for (data in snapshot.children) {
                    val pet = data.getValue(Pets::class.java)
                    if (pet != null) {
                        petList.add(pet)
                    }
                }
                adapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                // Handle error
            }
        })
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
