package org.frknkrc44.hma_oss.ui.activity

import android.os.Bundle
import androidx.navigation.findNavController
import icu.nullptr.hidemyapplist.service.ServiceClient
import org.frknkrc44.hma_oss.BuildConfig
import org.frknkrc44.hma_oss.R
import org.frknkrc44.hma_oss.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {
    var readyToKill: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = this.findNavController(R.id.nav_host_fragment)
        return navController.navigateUp() || super.onSupportNavigateUp()
    }

    override fun onDestroy() {
        if (readyToKill) {
            ServiceClient.forceStop(BuildConfig.APPLICATION_ID)
        } else {
            readyToKill = true
        }

        super.onDestroy()
    }
}