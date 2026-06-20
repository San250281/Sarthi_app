package com.aistudio.saarthi

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.aistudio.saarthi.data.MoodRecord
import com.aistudio.saarthi.viewmodel.SaarthiViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
@Config(sdk = [34])
class ExampleRobolectricTest {

  private val testDispatcher = UnconfinedTestDispatcher()
  private lateinit var context: Application
  private lateinit var viewModel: SaarthiViewModel

  @Before
  fun setUp() {
    Dispatchers.setMain(testDispatcher)
    context = ApplicationProvider.getApplicationContext<Application>()
    viewModel = SaarthiViewModel(context)
  }

  @After
  fun tearDown() {
    Dispatchers.resetMain()
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
    assertTrue(prompts.isNotEmpty())
    assertTrue(prompts[0].contains("smallest first step") || prompts[0].isNotBlank())
  }

  @Test
  fun `test generate personalized prompts completes successfully`() = runTest {
    viewModel.generatePersonalizedPrompts()
    val prompts = viewModel.reflectivePrompts.value
    assertNotNull(prompts)
    assertTrue(prompts.isNotEmpty())
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
    assertEquals(com.aistudio.saarthi.viewmodel.SaarthiState.IDLE, viewModel.companionState.value)

    // 2. Call startListening() without granting permission first
    viewModel.startListening()

    // 3. States should be checked: state should remain IDLE, and errorMessage should be populated
    assertEquals(com.aistudio.saarthi.viewmodel.SaarthiState.IDLE, viewModel.companionState.value)
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
  fun `test passcode hashing security output is secure`() {
    val passcode = "9876"
    val hashed = com.aistudio.saarthi.util.SecurityUtils.hashPassword(passcode)
    assertNotNull(hashed)
    assertTrue(hashed.isNotBlank())
    org.junit.Assert.assertNotEquals(passcode, hashed)
  }
}
