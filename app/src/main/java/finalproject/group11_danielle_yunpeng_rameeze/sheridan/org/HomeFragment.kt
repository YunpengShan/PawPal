package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.Pets
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.adapter.RvPetsAdapter
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var petsList: ArrayList<Pets>
    private lateinit var firebaseRef: DatabaseReference
    private lateinit var rvAdapter: RvPetsAdapter
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        // Initialize Firebase Auth, Database reference, and contact list
        firebaseAuth = FirebaseAuth.getInstance()
        val userId = firebaseAuth.currentUser?.uid

        if (userId == null) {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
            return binding.root
        }

        firebaseRef = FirebaseDatabase.getInstance().getReference("users/$userId/pets")
        petsList = arrayListOf()

        // Initialize RecyclerView and Adapter
        rvAdapter = RvPetsAdapter(petsList)
        binding.rvPets.layoutManager = LinearLayoutManager(context)
        binding.rvPets.setHasFixedSize(true)
        binding.rvPets.adapter = rvAdapter

        // FloatingActionButton click to navigate to AddFragment
        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addFragment)
        }

        // Fetch data from the database
        fetchData()

        return binding.root
    }

    private fun fetchData() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }

        firebaseRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                petsList.clear()
                if (snapshot.exists()) {
                    for (petSnap in snapshot.children) {
                        val pet = petSnap.getValue(Pets::class.java)
                        pet?.let { petsList.add(it) }
                    }
                }
                // Notify adapter of data change
                rvAdapter.notifyDataSetChanged()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(context, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        })
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
