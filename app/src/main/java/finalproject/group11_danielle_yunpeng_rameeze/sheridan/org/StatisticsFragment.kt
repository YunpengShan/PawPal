package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.Pets

class StatisticsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvDogCount: TextView
    private lateinit var tvCatCount: TextView
    private lateinit var tvBirdCount: TextView
    private lateinit var tvReptileCount: TextView
    private lateinit var tvFishCount: TextView
    private lateinit var auth: FirebaseAuth // FirebaseAuth instance

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_statistics, container, false)

        // Initialize the UI components
        tvDogCount = view.findViewById(R.id.tvDogCount)
        tvCatCount = view.findViewById(R.id.tvCatCount)
        tvBirdCount = view.findViewById(R.id.tvBirdCount)
        tvReptileCount = view.findViewById(R.id.tvReptileCount)
        tvFishCount = view.findViewById(R.id.tvFishCount)

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance() // Initialize FirebaseAuth

        // Call the method to get pet distribution for the current signed-in user
        getPetDistribution()

        return view
    }

    private fun getPetDistribution() {
        // Get the current signed-in user's UID
        val currentUserId = auth.currentUser?.uid

        if (currentUserId != null) {
            // Fetch the pets for the current signed-in user
            db.collection("users")
                .document(currentUserId) // Access the current user's document
                .collection("pets") // Access the pets subcollection
                .get()
                .addOnSuccessListener { petSnapshot ->
                    Log.d("StatisticsFragment", "Fetched pets for user $currentUserId: ${petSnapshot.size()} pets found")

                    var dogCount = 0
                    var catCount = 0
                    var birdCount = 0
                    var reptileCount = 0
                    var fishCount = 0

                    // Loop through the pets of this user
                    for (petDocument in petSnapshot) {
                        val pet = petDocument.toObject(Pets::class.java)
                        Log.d("StatisticsFragment", "Pet: $pet")

                        // Check if the pet type is not null or empty
                        pet?.let {
                            // Log pet type to verify
                            Log.d("StatisticsFragment", "Pet type: ${it.type}")

                            when (it.type) {
                                "Dog" -> dogCount++
                                "Cat" -> catCount++
                                "Bird" -> birdCount++
                                "Reptile" -> reptileCount++
                                "Fish" -> fishCount++
                                else -> Log.d("StatisticsFragment", "Unknown pet type: ${it.type}")
                            }
                        }
                    }

                    // Update the TextViews with the pet counts
                    tvDogCount.text = "Dogs: $dogCount"
                    tvCatCount.text = "Cats: $catCount"
                    tvBirdCount.text = "Birds: $birdCount"
                    tvReptileCount.text = "Reptiles: $reptileCount"
                    tvFishCount.text = "Fish: $fishCount"

                }
                .addOnFailureListener { exception ->
                    Log.e("StatisticsFragment", "Error fetching pets: ", exception)
                }
        } else {
            Log.e("StatisticsFragment", "No user is signed in.")
        }
    }
}
