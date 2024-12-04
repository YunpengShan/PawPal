package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.adapter.RvPetsAdapter
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.FragmentHomeBinding
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.Pets

class HomeFragment : Fragment() {
    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private lateinit var petsList: ArrayList<Pets>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var rvAdapter: RvPetsAdapter
    private lateinit var firebaseAuth: FirebaseAuth

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)

        firebaseAuth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        val userId = firebaseAuth.currentUser?.uid

        if (userId == null) {
            showAlertDialog("Error", "User not logged in!")
            return binding.root
        }

        petsList = arrayListOf()

        rvAdapter = RvPetsAdapter(petsList)
        binding.rvPets.layoutManager = LinearLayoutManager(context)
        binding.rvPets.setHasFixedSize(true)
        binding.rvPets.adapter = rvAdapter

        binding.btnAdd.setOnClickListener {
            findNavController().navigate(R.id.action_homeFragment_to_addFragment)
        }

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
            showAlertDialog("Error", "User not logged in!")
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
                rvAdapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                showAlertDialog("Error", "Error fetching pets: ${e.message}")
            }
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
