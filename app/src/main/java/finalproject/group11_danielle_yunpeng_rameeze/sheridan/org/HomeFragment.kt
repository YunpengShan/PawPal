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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.toObject
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.Pets
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.adapter.RvPetsAdapter
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentHomeBinding

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var petsList: ArrayList<Pets>
    private lateinit var firestore: FirebaseFirestore
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
        firestore = FirebaseFirestore.getInstance()
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

    override fun onResume() {
        super.onResume()
        fetchData() // Re-fetch data and update the RecyclerView
    }


    private fun fetchData() {
        val userId = firebaseAuth.currentUser?.uid

        if (userId.isNullOrEmpty()) {
            Toast.makeText(context, "User not logged in!", Toast.LENGTH_SHORT).show()
            return
        }
        firestore.collection("pets")
            .whereEqualTo("ownerID", userId)
            .get()
            .addOnSuccessListener { result ->
                petsList.clear()
                for (doc in result) {
                    var pet = doc.toObject(Pets::class.java)
                    pet.id = doc.id
                    pet?.let { petsList.add(it) }
                }
                // Notify adapter of data change
                rvAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error fetching pets: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
