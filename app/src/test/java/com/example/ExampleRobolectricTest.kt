package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.viewmodel.RideViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("PulaRide", appName)
  }

  @Test
  fun `test driver compliance flow and offline gating`() {
    val app = ApplicationProvider.getApplicationContext<Application>()
    val viewModel = RideViewModel(app)

    // Initial state: not compliant
    assertEquals("NOT_COMPLIANT", viewModel.complianceStatus.value)
    assertFalse(viewModel.driverOnline.value)

    // Try going online: should trigger compliance warning and stay offline
    viewModel.setDriverOnline(true)
    assertTrue(viewModel.complianceWarningTriggered.value)
    assertFalse(viewModel.driverOnline.value)

    // Clear warning
    viewModel.setComplianceWarningTriggered(false)
    assertFalse(viewModel.complianceWarningTriggered.value)

    // Simulate approval sandbox action
    viewModel.simulateVerificationApproval()
    assertEquals("COMPLIANT", viewModel.complianceStatus.value)

    // Try going online now: should succeed
    viewModel.setDriverOnline(true)
    assertTrue(viewModel.driverOnline.value)
    assertFalse(viewModel.complianceWarningTriggered.value)

    // Simulate rejection sandbox action
    viewModel.simulateVerificationRejection("License is expired")
    assertEquals("NOT_COMPLIANT", viewModel.complianceStatus.value)
    // Driver should be forced back to offline
    assertFalse(viewModel.driverOnline.value)
  }
}
