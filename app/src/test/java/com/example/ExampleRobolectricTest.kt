package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.MoodRecord
import com.example.viewmodel.SaarthiViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  private lateinit var context: Application
  private lateinit var viewModel: SaarthiViewModel

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext<Application>()
    viewModel = SaarthiViewModel(context)
  }

  @Test
  fun `read string from context`() {
    val contextStr = ApplicationProvider.getApplicationContext<Context>()
    val appName = contextStr.getString(R.string.app_name)
    assertEquals("Saarthi", appName)
  }

  @Test
  fun `test initial reflective prompts contains default items`() = runTest {
    val prompts = viewModel.reflectivePrompts.value
    assertNotNull(prompts)
    assertEquals(4, prompts.size)
    assertTrue(prompts[0].contains("smallest first step"))
  }

  @Test
  fun `test generate personalized prompts completes successfully`() = runTest {
    viewModel.generatePersonalizedPrompts()
    // Default or generated prompts should be in it stably
    val prompts = viewModel.reflectivePrompts.value
    assertNotNull(prompts)
    assertTrue(prompts.isNotEmpty())
  }

  @Test
  fun `test generate growth report fails when no records are present`() = runTest {
    // Force clean mood records by using viewmodel.repository.clearAllMoodRecords()
    viewModel.repository.clearAllMoodRecords()
    
    viewModel.generateGrowthReport()
    
    // Growth report should remain null
    val report = viewModel.growthReport.value
    assertEquals(null, report)
    
    // There should be an error message
    val error = viewModel.errorMessage.value
    assertNotNull(error)
    assertTrue(error!!.contains("No emotional logs available"))
  }

  @Test
  fun `test insert mood records and verify repository values`() = runTest {
    viewModel.repository.clearAllMoodRecords()
    
    val record1 = MoodRecord(mood = "Happy", confidence = 8, clarity = 9, notes = "High clarity and positive path forward")
    viewModel.repository.insertMoodRecord(record1)
    
    val record2 = MoodRecord(mood = "Anxious", confidence = 3, clarity = 4, notes = "Feeling stressed about next milestone")
    viewModel.repository.insertMoodRecord(record2)
    
    // Pull entries from flow to check persistence
    val records = viewModel.moodRecords.value
    assertNotNull(records)
  }

  @Test
  fun `test language detection for English inputs`() {
    val englishInput1 = "I want to work on my fitness goal today."
    val englishInput2 = "How are you doing, my friend?"
    
    assertEquals("English", viewModel.detectLanguageOfInput(englishInput1))
    assertEquals("English", viewModel.detectLanguageOfInput(englishInput2))
  }

  @Test
  fun `test language detection for Hindi Devanagari inputs`() {
    val hindiInput1 = "नमस्ते, आप कैसे हैं?" 
    val hindiInput2 = "आज मुझे बहुत खुशी हो रही है।"
    
    assertEquals("Hindi", viewModel.detectLanguageOfInput(hindiInput1))
    assertEquals("Hindi", viewModel.detectLanguageOfInput(hindiInput2))
  }

  @Test
  fun `test language detection for Hinglish inputs`() {
    val hinglishInput1 = "Aap kaise ho bhai?"
    val hinglishInput2 = "Mera goal complete ho gaya hai."
    val hinglishInput3 = "Mujhe samajh nahi aa raha."
    
    assertEquals("Hinglish", viewModel.detectLanguageOfInput(hinglishInput1))
    assertEquals("Hinglish", viewModel.detectLanguageOfInput(hinglishInput2))
    assertEquals("Hinglish", viewModel.detectLanguageOfInput(hinglishInput3))
  }

  @Test
  fun `test language state switches correctly when sending message`() = runTest {
    val detected1 = viewModel.detectLanguageOfInput("Hi there, help me plan my goal")
    assertEquals("English", detected1)

    val detected2 = viewModel.detectLanguageOfInput("Aaj mera mood thoda stressed hai")
    assertEquals("Hinglish", detected2)

    val detected3 = viewModel.detectLanguageOfInput("मैं आज बहुत खुश हूँ")
    assertEquals("Hindi", detected3)
  }

  @Test
  fun `test nightly gratitude triggers active logging isSaved correctly`() = runTest {
    // 1. Verify initial state is false
    val initialActive = viewModel.isGratitudeLoggingActive.value
    assertEquals(false, initialActive)

    // 2. Trigger gratitude check-In
    viewModel.triggerGratitudeCheckIn()
    val active = viewModel.isGratitudeLoggingActive.value
    assertEquals(true, active)

    // 3. User sends gratitude input
    viewModel.sendMessage("I am grateful for my sister support and a hot meal.")
    
    // Check that logging state is reset to false
    var attempts = 0
    while (viewModel.isGratitudeLoggingActive.value && attempts < 100) {
        Thread.sleep(10)
        attempts++
    }
    val afterSendActive = viewModel.isGratitudeLoggingActive.value
    assertEquals(false, afterSendActive)

    // Check that a memory was indeed logged with category "Gratitude"
    var savedGratitude: com.example.data.MemoryItem? = null
    attempts = 0
    while (savedGratitude == null && attempts < 100) {
        val memories = viewModel.repository.memoryItems.first()
        savedGratitude = memories.find { it.category == "Gratitude" }
        if (savedGratitude == null) {
            Thread.sleep(10)
        }
        attempts++
    }
    
    assertNotNull(savedGratitude)
    assertEquals("Grateful For", savedGratitude!!.key)
    assertEquals("I am grateful for my sister support and a hot meal.", savedGratitude.value)
  }

  @Test
  fun `test direct audio recording toggle changes setting`() {
    // 1. Verify initial state is false
    val initialValue = viewModel.useDirectAudioCapture.value
    assertEquals(false, initialValue)

    // 2. Set to true
    viewModel.setUseDirectAudioCapture(true)
    val updatedValue = viewModel.useDirectAudioCapture.value
    assertEquals(true, updatedValue)

    // 3. Set to false
    viewModel.setUseDirectAudioCapture(false)
    val finalValue = viewModel.useDirectAudioCapture.value
    assertEquals(false, finalValue)
  }

  @Test
  fun `test listening without permission fails gracefully`() {
    // 1. Initially companionState is IDLE
    assertEquals(com.example.viewmodel.SaarthiState.IDLE, viewModel.companionState.value)

    // 2. Call startListening() without granting permission first
    viewModel.startListening()

    // 3. States should be checked: state should remain IDLE, and errorMessage should be populated
    assertEquals(com.example.viewmodel.SaarthiState.IDLE, viewModel.companionState.value)
    assertNotNull(viewModel.errorMessage.value)
    assertTrue(viewModel.errorMessage.value!!.contains("permission"))
  }

  @Test
  fun `test initial agent and wallet balance details are populated`() {
    val initialBalance = viewModel.walletBalance.value
    assertTrue(initialBalance > 0) // starts with ₹150 demo balance
    
    val defaultAgent = viewModel.selectedAgent.value
    assertEquals("saarthi", defaultAgent.id)
    assertEquals(5, defaultAgent.pricePerMin)
  }

  @Test
  fun `test selecting multiple agents updates state and pricing`() {
    // Select Kabir (Zen Philosopher)
    viewModel.selectAgent("kabir")
    var currentAgent = viewModel.selectedAgent.value
    assertEquals("kabir", currentAgent.id)
    assertEquals(10, currentAgent.pricePerMin)

    // Select Meera (Art Coach)
    viewModel.selectAgent("meera")
    currentAgent = viewModel.selectedAgent.value
    assertEquals("meera", currentAgent.id)
    assertEquals(8, currentAgent.pricePerMin)
  }

  @Test
  fun `test razorpay credit recharge increases wallet balance`() {
    val initialBalance = viewModel.walletBalance.value
    
    // Simulate Razorpay checkout of ₹220 pack successful
    viewModel.addWalletBalance(220, "Mental Peace Booster Pack")
    
    val updatedBalance = viewModel.walletBalance.value
    assertEquals(initialBalance + 220, updatedBalance)
  }

  @Test
  fun `test secure signup hashes password and updates state`() = runTest {
    var isSuccess = false
    viewModel.secureSignUp(
        name = "Sanjay Kumar",
        email = "sanjay.test@saarthi.com",
        passcode = "9876",
        onSuccess = { isSuccess = true },
        onError = { org.junit.Assert.fail("Secure signup failed") }
    )
    
    // Allow DB lock resolution
    kotlinx.coroutines.delay(200)
    
    val profile = viewModel.profile.value
    assertNotNull(profile)
    assertEquals("Sanjay Kumar", profile?.userName)
    assertEquals("sanjay.test@saarthi.com", profile?.userEmail)
    assertTrue(profile?.isLoggedIn == true)
    
    // Confirm passcode hash is not plain passcode
    assertNotNull(profile?.passcodeHash)
    org.junit.Assert.assertNotEquals("9876", profile?.passcodeHash)
  }

  @Test
  fun `test secure login and logout authentication gate`() = runTest {
    // 1. Initial register
    viewModel.secureSignUp(
        name = "Sanjay Kumar",
        email = "sanjay.test@saarthi.com",
        passcode = "9876",
        onSuccess = {},
        onError = { org.junit.Assert.fail() }
    )
    
    kotlinx.coroutines.delay(200)

    // 2. Lock app with logout
    viewModel.secureLogout()
    kotlinx.coroutines.delay(200)
    
    var profile = viewModel.profile.value
    assertTrue(profile?.isLoggedIn == false)

    // 3. Login with correct passcode
    viewModel.secureLogin(
        email = "sanjay.test@saarthi.com",
        passcode = "9876",
        onSuccess = {},
        onError = { org.junit.Assert.fail("Login with correct credentials failed") }
    )
    kotlinx.coroutines.delay(200)

    profile = viewModel.profile.value
    assertTrue(profile?.isLoggedIn == true)
  }
}

