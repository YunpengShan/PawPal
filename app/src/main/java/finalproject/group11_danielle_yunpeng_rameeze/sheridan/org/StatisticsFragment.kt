package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.Fragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.model.FeedSchedule

class StatisticsFragment : Fragment() {

    private lateinit var db: FirebaseFirestore
    private lateinit var tvDogCount: TextView
    private lateinit var tvCatCount: TextView
    private lateinit var tvBirdCount: TextView
    private lateinit var tvReptileCount: TextView
    private lateinit var tvFishCount: TextView
    private lateinit var tvDogAvgFood: TextView
    private lateinit var tvCatAvgFood: TextView
    private lateinit var tvBirdAvgFood: TextView
    private lateinit var tvReptileAvgFood: TextView
    private lateinit var tvFishAvgFood: TextView
    private lateinit var auth: FirebaseAuth

    // Variables to store pet counts
    private var dogCount = 0
    private var catCount = 0
    private var birdCount = 0
    private var reptileCount = 0
    private var fishCount = 0

    private var dogTotalFood = 0.0
    private var catTotalFood = 0.0
    private var birdTotalFood = 0.0
    private var reptileTotalFood = 0.0
    private var fishTotalFood = 0.0

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

        tvDogAvgFood = view.findViewById(R.id.tvDogAvgFood)
        tvCatAvgFood = view.findViewById(R.id.tvCatAvgFood)
        tvBirdAvgFood = view.findViewById(R.id.tvBirdAvgFood)
        tvReptileAvgFood = view.findViewById(R.id.tvReptileAvgFood)
        tvFishAvgFood = view.findViewById(R.id.tvFishAvgFood)

        // Initialize Firebase instances
        db = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Fetch data from Firestore
        getPetDistribution()

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

                    dogTotalFood = 0.0
                    catTotalFood = 0.0
                    birdTotalFood = 0.0
                    reptileTotalFood = 0.0
                    fishTotalFood = 0.0

                    val feedSchedIds = mutableMapOf<String, String>()

                    for (petDocument in petSnapshot) {
                        val petType = petDocument.getString("type")
                        val feedSchedID = petDocument.getString("feedSchedID")

                        if (feedSchedID != null) {
                            feedSchedIds[feedSchedID] = petType ?: ""
                        }

                        when (petType?.uppercase()) {
                            "DOG" -> dogCount++
                            "CAT" -> catCount++
                            "BIRD" -> birdCount++
                            "REPTILE" -> reptileCount++
                            "FISH" -> fishCount++
                        }
                    }

                    Log.d("Statistics", "Pet Counts - Dogs: $dogCount, Cats: $catCount, Birds: $birdCount, Reptiles: $reptileCount, Fish: $fishCount")

                    // Now fetch all FeedSchedules
                    if (feedSchedIds.isNotEmpty()) {
                        db.collection("feedSched")
                            .whereIn(FieldPath.documentId(), feedSchedIds.keys.toList())
                            .get()
                            .addOnSuccessListener { feedSnapshot ->
                                for (feedDoc in feedSnapshot) {
                                    val feedSchedule = feedDoc.toObject(FeedSchedule::class.java)
                                    val amountStr = feedSchedule.amount

                                    Log.d("Statistics", "FeedSchedule ID: ${feedDoc.id}, Amount: $amountStr")

                                    // Corrected code: Use Regex object for regex replacement
                                    val numericAmountStr = amountStr?.replace(Regex("[^\\d.]"), "")
                                    val amount = numericAmountStr?.toDoubleOrNull() ?: 0.0

                                    Log.d("Statistics", "Original Amount String: $amountStr, Numeric Amount String: $numericAmountStr, Parsed Amount: $amount")

                                    // Get the pet type associated with this feed schedule
                                    val petType = feedSchedIds[feedDoc.id]

                                    when (petType?.uppercase()) {
                                        "DOG" -> dogTotalFood += amount
                                        "CAT" -> catTotalFood += amount
                                        "BIRD" -> birdTotalFood += amount
                                        "REPTILE" -> reptileTotalFood += amount
                                        "FISH" -> fishTotalFood += amount
                                    }
                                }

                                Log.d("Statistics", "Total Food Amounts - Dogs: $dogTotalFood, Cats: $catTotalFood, Birds: $birdTotalFood, Reptiles: $reptileTotalFood, Fish: $fishTotalFood")

                                // Update the TextViews with the pet counts and average food amounts
                                updatePetCountsAndAverages()
                            }
                            .addOnFailureListener { exception ->
                                showAlertDialog("Error", "Error fetching feed schedules: ${exception.message}")
                            }
                    } else {
                        // No feed schedules found
                        updatePetCountsAndAverages()
                    }
                }
                .addOnFailureListener { exception ->
                    showAlertDialog("Error", "Error fetching pets: ${exception.message}")
                }
        } else {
            showAlertDialog("Error", "No user is signed in.")
        }
    }

    private fun updatePetCountsAndAverages() {
        tvDogCount.text = "Dogs: $dogCount"
        tvCatCount.text = "Cats: $catCount"
        tvBirdCount.text = "Birds: $birdCount"
        tvReptileCount.text = "Reptiles: $reptileCount"
        tvFishCount.text = "Fish: $fishCount"

        val dogAvgFood = if (dogCount > 0) dogTotalFood / dogCount else 0.0
        val catAvgFood = if (catCount > 0) catTotalFood / catCount else 0.0
        val birdAvgFood = if (birdCount > 0) birdTotalFood / birdCount else 0.0
        val reptileAvgFood = if (reptileCount > 0) reptileTotalFood / reptileCount else 0.0
        val fishAvgFood = if (fishCount > 0) fishTotalFood / fishCount else 0.0

        Log.d("Statistics", "Average Food Amounts - Dogs: $dogAvgFood, Cats: $catAvgFood, Birds: $birdAvgFood, Reptiles: $reptileAvgFood, Fish: $fishAvgFood")

        val dogAvgFoodFormatted = String.format("%.2f", dogAvgFood)
        val catAvgFoodFormatted = String.format("%.2f", catAvgFood)
        val birdAvgFoodFormatted = String.format("%.2f", birdAvgFood)
        val reptileAvgFoodFormatted = String.format("%.2f", reptileAvgFood)
        val fishAvgFoodFormatted = String.format("%.2f", fishAvgFood)

        tvDogAvgFood.text = "Average Food Amount: $dogAvgFoodFormatted"
        tvCatAvgFood.text = "Average Food Amount: $catAvgFoodFormatted"
        tvBirdAvgFood.text = "Average Food Amount: $birdAvgFoodFormatted"
        tvReptileAvgFood.text = "Average Food Amount: $reptileAvgFoodFormatted"
        tvFishAvgFood.text = "Average Food Amount: $fishAvgFoodFormatted"
    }

    private fun showAlertDialog(title: String, message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}
