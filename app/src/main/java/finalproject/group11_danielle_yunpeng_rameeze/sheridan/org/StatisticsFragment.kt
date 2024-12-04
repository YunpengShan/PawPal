package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class StatisticsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvDogCount: TextView
    private lateinit var tvCatCount: TextView
    private lateinit var tvBirdCount: TextView
    private lateinit var tvReptileCount: TextView
    private lateinit var tvFishCount: TextView
    private lateinit var auth: FirebaseAuth // FirebaseAuth instance

    // Variables to store pet counts
    private var dogCount = 0
    private var catCount = 0
    private var birdCount = 0
    private var reptileCount = 0
    private var fishCount = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Initialize the UI components
        tvDogCount = view.findViewById(R.id.tvDogCount)
        tvCatCount = view.findViewById(R.id.tvCatCount)
        tvBirdCount = view.findViewById(R.id.tvBirdCount)
        tvReptileCount = view.findViewById(R.id.tvReptileCount)
        tvFishCount = view.findViewById(R.id.tvFishCount)

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // If savedInstanceState is null, fetch data from Firestore
        if (savedInstanceState == null) {
            getPetDistribution()
        }

        return view
    }

    private fun getPetDistribution() {
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            db.collection("pets")
                .whereEqualTo("ownerID", currentUserId)
                .get()
                .addOnSuccessListener { petSnapshot ->
                    dogCount = 0
                    catCount = 0
                    birdCount = 0
                    reptileCount = 0
                    fishCount = 0

                    for (petDocument in petSnapshot) {
                        val petType = petDocument.getString("type")
                        when (petType?.uppercase()) {
                            "DOG" -> dogCount++
                            "CAT" -> catCount++
                            "BIRD" -> birdCount++
                            "REPTILE" -> reptileCount++
                            "FISH" -> fishCount++
                        }
                    }

                    // Update the TextViews with the pet counts
                    updatePetCounts()
                }
                .addOnFailureListener { exception ->
                    showAlertDialog("Error fetching pets: ${exception.message}")
                }
        } else {
            showAlertDialog("No user is signed in.")
        }
    }

    private fun updatePetCounts() {
        tvDogCount.text = "Dogs: $dogCount"
        tvCatCount.text = "Cats: $catCount"
        tvBirdCount.text = "Birds: $birdCount"
        tvReptileCount.text = "Reptiles: $reptileCount"
        tvFishCount.text = "Fish: $fishCount"
    }

    private fun showAlertDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        // Save the pet counts
        outState.putInt("dogCount", dogCount)
        outState.putInt("catCount", catCount)
        outState.putInt("birdCount", birdCount)
        outState.putInt("reptileCount", reptileCount)
        outState.putInt("fishCount", fishCount)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        savedInstanceState?.let {
            // Restore the pet counts
            dogCount = it.getInt("dogCount")
            catCount = it.getInt("catCount")
            birdCount = it.getInt("birdCount")
            reptileCount = it.getInt("reptileCount")
            fishCount = it.getInt("fishCount")

            // Update the TextViews with restored counts
            updatePetCounts()
        }
    }
}
