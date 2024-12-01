package finalproject.group11_danielle_yunpeng_rameeze.sheridan.org

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.drawerlayout.widget.DrawerLayout
import androidx.navigation.NavController
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.google.android.material.navigation.NavigationView
import finalproject.group11_danielle_yunpeng_rameeze.sheridan.org.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var navController: NavController
    private lateinit var appBarConfiguration: AppBarConfiguration

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Set Toolbar as ActionBar
        setSupportActionBar(binding.toolbar)

        // Initialize NavHostFragment and NavController
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.fragmentContainerView) as NavHostFragment
        navController = navHostFragment.navController

        // Initialize DrawerLayout and NavigationView
        val drawerLayout: DrawerLayout = binding.drawerLayout
        val navigationView: NavigationView = binding.navView

        // Configure AppBar with DrawerLayout
        appBarConfiguration = AppBarConfiguration(
            setOf(R.id.homeFragment),
            drawerLayout
        )
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration)
        NavigationUI.setupWithNavController(navigationView, navController)

        // Handle navigation drawer menu item clicks
        navigationView.setNavigationItemSelectedListener { menuItem ->
            when (menuItem.itemId) {
                R.id.nav_home -> {
                    // Navigate to Home
                    navController.navigate(R.id.homeFragment)
                }
                R.id.nav_logout -> {
                    // Handle Logout
                    Toast.makeText(this, "Logged Out", Toast.LENGTH_SHORT).show()
                    navController.navigate(R.id.signInFragment)
                }
                R.id.nav_search -> {
                    navController.navigate(R.id.searchFragment)
                }
                R.id.nav_statistics -> {
                    navController.navigate(R.id.statisticsFragment)
                }
                R.id.nav_about -> {
                    navController.navigate(R.id.aboutFragment)
                }
            }
            drawerLayout.closeDrawers() // Close the navigation drawer after an item is selected
            true
        }

        // Hide back button in SignInFragment and SignUpFragment
        navController.addOnDestinationChangedListener { _, destination, _ ->
            if (destination.id == R.id.signInFragment || destination.id == R.id.signUpFragment) {
                supportActionBar?.setDisplayHomeAsUpEnabled(false) // Hide the back button
            } else {
                supportActionBar?.setDisplayHomeAsUpEnabled(true) // Show the back button
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        return NavigationUI.navigateUp(navController, appBarConfiguration) || super.onSupportNavigateUp()
    }
}
