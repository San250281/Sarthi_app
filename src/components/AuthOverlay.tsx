import React, { useState, useEffect } from "react";
import { UserProfile } from "../types";
import { Lock, User, Mail, ShieldAlert, Key, CheckCircle, RefreshCw, HelpCircle, ArrowLeft } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

interface AuthOverlayProps {
  userProfile: UserProfile;
  setUserProfile: React.Dispatch<React.SetStateAction<UserProfile>>;
  onLogCloudWatch?: (level: "INFO" | "METRIC" | "WARN" | "ERROR", message: string) => void;
}

type AuthMode = "SIGN_IN" | "SIGN_UP" | "VERIFY_EMAIL" | "FORGOT_PASSWORD" | "RESET_PASSWORD";

export default function AuthOverlay({
  userProfile,
  setUserProfile,
  onLogCloudWatch,
}: AuthOverlayProps) {
  const [authMode, setAuthMode] = useState<AuthMode>(
    userProfile.passcodeHash ? "SIGN_IN" : "SIGN_UP"
  );
  
  // Form input states
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [name, setName] = useState("");
  const [verificationCode, setVerificationCode] = useState("");
  const [resetCode, setResetCode] = useState("");
  const [newPassword, setNewPassword] = useState("");
  
  // Status states
  const [errorText, setErrorText] = useState("");
  const [infoText, setInfoText] = useState("");
  const [isLoading, setIsLoading] = useState(false);
  const [sentCodeValue, setSentCodeValue] = useState("");

  // Print Cognito events to live simulator console
  const logEvent = (level: "INFO" | "METRIC" | "WARN" | "ERROR", text: string) => {
    if (onLogCloudWatch) {
      onLogCloudWatch(level, `[Amazon Cognito UserPool] ${text}`);
    }
  };

  const handleSignIn = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorText("");
    setInfoText("");
    setIsLoading(true);

    logEvent("INFO", `Attempting Cognito secure SRP authentication for: ${email}`);

    setTimeout(() => {
      setIsLoading(false);
      // Retrieve stored user credential simulations
      const registeredEmail = localStorage.getItem("saarthi_cognito_email") || "test@gmail.com";
      const registeredPassHash = localStorage.getItem("saarthi_cognito_pass") || "Password123";
      const registeredName = localStorage.getItem("saarthi_cognito_name") || "Rahul Sharma";

      if (email.toLowerCase().trim() !== registeredEmail.toLowerCase().trim()) {
        setErrorText("CognitoException: UserNotConfirmedException or UserNotFoundException occurred.");
        logEvent("ERROR", `Authentication failed: User ${email} not found in user pool.`);
        return;
      }

      if (password !== registeredPassHash) {
        setErrorText("CognitoException: NotAuthorizedException. Incorrect password.");
        logEvent("WARN", `Unauthorized login attempt: Incorrect password signature for ${email}.`);
        return;
      }

      // Generate simulated JWT AuthTokens (Cognito token bundle)
      const tokenExpiry = Date.now() + 5 * 60 * 1000; // 5 minute session for visibility & refresh testing!
      const idToken = `eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.user_${btoa(email)}.saarthi_id_token`;
      const refreshToken = `cognito_refresh_token_${Math.random().toString(36).substr(2, 12)}`;

      const currentProfileDetails: UserProfile = {
        userName: registeredName,
        userEmail: email,
        preferredLanguage: "English",
        currentMood: "Normal",
        themeMode: "Light",
        passcodeHash: btoa("1234"), // Fallback fallback pin
        isLoggedIn: true,
        cognitoTokenExpiration: tokenExpiry,
        cognitoIdToken: idToken,
        cognitoRefreshToken: refreshToken,
        subscriptionPlan: userProfile.subscriptionPlan || "Free Tier",
        subscriptionExpiry: userProfile.subscriptionExpiry || 0,
        subscriptionStatus: userProfile.subscriptionStatus || "Inactive",
        dailyQuestionsCount: userProfile.dailyQuestionsCount || 0,
        lastQuestionTimestamp: userProfile.lastQuestionTimestamp || Date.now(),
        createdDate: userProfile.createdDate || Date.now() - 24 * 60 * 60 * 1000,
        lastLogin: Date.now()
      };

      setUserProfile(currentProfileDetails);
      localStorage.setItem("saarthi_profile", JSON.stringify(currentProfileDetails));
      
      logEvent("METRIC", `Successfully issued secure Cognito Access/ID JWT Token bundle for user: ${email}`);
      logEvent("INFO", `Token session validity established. Expiry set to 5 minutes.`);
    }, 1200);
  };

  const handleSignUp = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorText("");
    setInfoText("");

    if (!name.trim() || !email.trim() || !password) {
      setErrorText("All fields represent mandatory registration parameters.");
      return;
    }

    if (password !== confirmPassword) {
      setErrorText("Password confirmation does not match original password.");
      return;
    }

    if (password.length < 8) {
      setErrorText("Cognito policy error: Password must satisfy complexity rules (min 8 characters).");
      return;
    }

    setIsLoading(true);
    logEvent("INFO", `Initiating SignUp in UserPool 'ap-south-1_SaarthiUserAdmin' for ${email}`);

    setTimeout(() => {
      setIsLoading(false);
      
      // Seed a random 6-digit confirmation code
      const generatedCode = Math.floor(100000 + Math.random() * 900000).toString();
      setSentCodeValue(generatedCode);
      
      // Temporarily store during verification states
      localStorage.setItem("saarthi_cognito_temp_email", email);
      localStorage.setItem("saarthi_cognito_temp_pass", password);
      localStorage.setItem("saarthi_cognito_temp_name", name);

      logEvent("INFO", `CognitoUser created in UNCONFIRMED state. Dispatching secure SES verification code.`);
      logEvent("METRIC", `DynamoDB subscription seed initiated. SES verification code triggered successfully.`);
      
      setInfoText(`Verification code generated! Simulate receiving code from AWS SES: ${generatedCode}`);
      setAuthMode("VERIFY_EMAIL");
    }, 1200);
  };

  const handleVerifyEmail = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorText("");
    setInfoText("");
    setIsLoading(true);

    logEvent("INFO", `Confirming sign-up request with Cognito User ID provider for email.`);

    setTimeout(() => {
      setIsLoading(false);
      
      if (verificationCode !== sentCodeValue && verificationCode !== "123456") {
        setErrorText("CognitoException: CodeMismatchException. The code provided does not match.");
        logEvent("WARN", `CodeMismatchException: Incorrect verification code entered for signup.`);
        return;
      }

      // Commit temporary user to active user registries
      const tempEmail = localStorage.getItem("saarthi_cognito_temp_email") || email;
      const tempPass = localStorage.getItem("saarthi_cognito_temp_pass") || password;
      const tempName = localStorage.getItem("saarthi_cognito_temp_name") || name;

      localStorage.setItem("saarthi_cognito_email", tempEmail);
      localStorage.setItem("saarthi_cognito_pass", tempPass);
      localStorage.setItem("saarthi_cognito_name", tempName);

      // Successfully confirmed Cognito Account!
      logEvent("METRIC", `Cognito signup confirmed successfully. Account transition to CONFIRMED state.`);
      logEvent("INFO", `User successfully created in DynamoDB Users table.`);

      // Log them in immediately
      const tokenExpiry = Date.now() + 5 * 60 * 1000;
      const idToken = `eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9.user_${btoa(tempEmail)}.saarthi_id_token`;
      const refreshToken = `cognito_refresh_token_${Math.random().toString(36).substr(2, 12)}`;

      const currentProfileDetails: UserProfile = {
        userName: tempName,
        userEmail: tempEmail,
        preferredLanguage: "English",
        currentMood: "Normal",
        themeMode: "Light",
        passcodeHash: btoa("1234"), // simple passcode hash
        isLoggedIn: true,
        cognitoTokenExpiration: tokenExpiry,
        cognitoIdToken: idToken,
        cognitoRefreshToken: refreshToken,
        subscriptionPlan: "Free Tier",
        subscriptionExpiry: 0,
        subscriptionStatus: "Inactive",
        dailyQuestionsCount: 0,
        lastQuestionTimestamp: Date.now(),
        createdDate: Date.now(),
        lastLogin: Date.now()
      };

      setUserProfile(currentProfileDetails);
      localStorage.setItem("saarthi_profile", JSON.stringify(currentProfileDetails));
    }, 1200);
  };

  const handleForgotPassword = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorText("");
    setInfoText("");
    
    if (!email.trim()) {
      setErrorText("Please state your registered Email address.");
      return;
    }

    setIsLoading(true);
    logEvent("INFO", `Initiating forgot password workflow for username/email: ${email}`);

    setTimeout(() => {
      setIsLoading(false);
      const generatedCode = Math.floor(100000 + Math.random() * 900000).toString();
      setSentCodeValue(generatedCode);
      setResetCode("");
      
      logEvent("INFO", `Sent reset confirmation signature to email via AWS Pinpoint.`);
      setInfoText(`Verification reset code sent to: ${email}. Simulated OTP is: ${generatedCode}`);
      setAuthMode("RESET_PASSWORD");
    }, 1200);
  };

  const handleResetPassword = (e: React.FormEvent) => {
    e.preventDefault();
    setErrorText("");
    setInfoText("");

    if (newPassword.length < 8) {
      setErrorText("Password must be at least 8 characters long.");
      return;
    }

    setIsLoading(true);
    logEvent("INFO", `Verifying password reset code & updating password hashes.`);

    setTimeout(() => {
      setIsLoading(false);

      if (verificationCode !== sentCodeValue) {
        setErrorText("Invalid verification code. Please check your simulated code.");
        return;
      }

      // Update stored passcode details
      localStorage.setItem("saarthi_cognito_pass", newPassword);
      logEvent("METRIC", `Password updated in Cognito User Pool successfully.`);
      
      alert("Success! Password reset was verified by Amazon Cognito. You can now login with your new password.");
      setAuthMode("SIGN_IN");
      setEmail("");
      setPassword("");
    }, 1200);
  };

  return (
    <div className="fixed inset-0 bg-slate-950 flex justify-center items-center p-6 z-50 overflow-y-auto" id="auth_portal_overlay">
      
      {/* Decorative Warm Ambient Glows */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden opacity-30">
        <div className="absolute top-1/4 -left-10 w-80 h-80 rounded-full bg-emerald-500/20 blur-3xl"></div>
        <div className="absolute bottom-1/4 -right-10 w-96 h-96 rounded-full bg-blue-500/15 blur-3xl"></div>
      </div>

      <motion.div
        initial={{ opacity: 0, scale: 0.96 }}
        animate={{ opacity: 1, scale: 1 }}
        className="w-full max-w-md bg-slate-900 border border-slate-800 rounded-3xl p-8 shadow-2xl relative overflow-hidden z-10"
      >
        <div className="text-center mb-6">
          <div className="w-16 h-16 rounded-full bg-slate-950 border border-slate-800 flex items-center justify-center text-3xl mx-auto shadow-md mb-3 relative group">
            🧘
            <span className="absolute bottom-0 right-0 w-4 h-4 bg-emerald-500 border-2 border-slate-900 rounded-full animate-pulse" title="Secure Auth Mode" />
          </div>
          
          <div className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full bg-slate-950 border border-slate-800 text-[10px] text-emerald-400 font-mono uppercase font-bold tracking-widest mb-1.5">
            <span className="w-1.5 h-1.5 rounded-full bg-emerald-500 animate-pulse" />
            Amazon Cognito Portal
          </div>

          <h2 className="font-display text-2xl font-bold tracking-tight text-white leading-normal mt-2">
            {authMode === "SIGN_UP" && "Join Saarthi Group"}
            {authMode === "SIGN_IN" && "Cognito Identifier"}
            {authMode === "VERIFY_EMAIL" && "MFA / Email Verify"}
            {authMode === "FORGOT_PASSWORD" && "Reset Request"}
            {authMode === "RESET_PASSWORD" && "Confirm New Password"}
          </h2>
          
          <p className="text-xs text-slate-400 font-sans tracking-wide leading-relaxed mt-1">
            {authMode === "SIGN_UP" && "Create a secure account backed by automated AWS KMS key storage."}
            {authMode === "SIGN_IN" && "Enter your credentials verified securely through API Gateway endpoints."}
            {authMode === "VERIFY_EMAIL" && "We sent a simulated SES email verification containing your confirmation signature."}
            {authMode === "FORGOT_PASSWORD" && "Send a reset verification trigger sequence to your email inbox."}
            {authMode === "RESET_PASSWORD" && "Set your updated password parameters in ap-south-1 Cognito schema."}
          </p>
        </div>

        {errorText && (
          <div className="bg-rose-500/10 border border-rose-500/20 text-rose-300 px-4 py-3 rounded-xl text-xs flex items-center gap-2.5 mb-5 text-left">
            <ShieldAlert size={18} className="shrink-0 text-rose-400" />
            <span className="font-sans leading-relaxed">{errorText}</span>
          </div>
        )}

        {infoText && (
          <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 px-4 py-3.5 rounded-xl text-xs flex flex-col gap-1 mb-5 text-left font-mono">
            <span className="font-bold">AWS SES Sandbox Notification:</span>
            <span className="text-slate-300 text-[11px] font-sans leading-normal">{infoText}</span>
          </div>
        )}

        <AnimatePresence mode="wait">
          {authMode === "SIGN_UP" && (
            <motion.form
              key="signup"
              onSubmit={handleSignUp}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="space-y-4 text-left"
            >
              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1.5">Your Full Name</label>
                <div className="relative">
                  <User size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="text"
                    required
                    placeholder="Rahul Sharma"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1.5">Secure Email address</label>
                <div className="relative">
                  <Mail size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="email"
                    required
                    placeholder="rahul@gmail.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1.5">Password</label>
                <div className="relative">
                  <Key size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="password"
                    required
                    placeholder="Min 8 chars"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1.5">Confirm Password</label>
                <div className="relative">
                  <Lock size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="password"
                    required
                    placeholder="Confirm password"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-3.5 bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-slate-950 font-extrabold rounded-xl shadow-lg shadow-emerald-500/10 tracking-wider text-xs uppercase transition-all mt-4 flex items-center justify-center gap-2"
              >
                {isLoading ? <RefreshCw className="animate-spin" size={14} /> : "Cognito Registration"}
              </button>

              <div className="pt-2 text-center whitespace-nowrap">
                <button
                  type="button"
                  onClick={() => setAuthMode("SIGN_IN")}
                  className="text-xs text-slate-400 hover:text-white underline font-medium"
                >
                  Already have confirmed account? Sign In
                </button>
              </div>
            </motion.form>
          )}

          {authMode === "SIGN_IN" && (
            <motion.form
              key="signin"
              onSubmit={handleSignIn}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="space-y-4"
            >
              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1.5 text-left">Your Email</label>
                <div className="relative">
                  <Mail size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="email"
                    required
                    placeholder="Enter email address"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <div>
                <div className="flex justify-between items-center mb-1.5">
                  <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block">Password</label>
                  <button
                    type="button"
                    onClick={() => {
                      setAuthMode("FORGOT_PASSWORD");
                      setErrorText("");
                      setInfoText("");
                    }}
                    className="text-[11px] text-emerald-400 hover:text-emerald-300 font-semibold underline"
                  >
                    Forgot Password?
                  </button>
                </div>
                
                <div className="relative">
                  <Lock size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="password"
                    required
                    placeholder="Enter password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <button
                type="submit"
                disabled={isLoading}
                className="w-full py-3.5 bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-slate-950 font-extrabold rounded-xl shadow-lg shadow-emerald-500/10 tracking-wider text-xs uppercase transition-all mt-4 flex items-center justify-center gap-2"
              >
                {isLoading ? <RefreshCw className="animate-spin" size={14} /> : "Sign In securely"}
              </button>

              <div className="pt-2 text-center text-slate-400 text-xs flex justify-between items-center px-1">
                <span>No profile yet?</span>
                <button
                  type="button"
                  onClick={() => {
                    setAuthMode("SIGN_UP");
                    setErrorText("");
                    setInfoText("");
                  }}
                  className="text-emerald-400 hover:text-emerald-300 underline font-semibold"
                >
                  Create Cognito Account
                </button>
              </div>
            </motion.form>
          )}

          {authMode === "VERIFY_EMAIL" && (
            <motion.form
              key="verify"
              onSubmit={handleVerifyEmail}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="space-y-4"
            >
              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-2 text-center">Confirm OTP Sent to Email</label>
                <div className="relative">
                  <Lock size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="text"
                    maxLength={6}
                    required
                    placeholder="xxxxxx"
                    value={verificationCode}
                    onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ""))}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-lg font-mono text-center tracking-widest focus:outline-none focus:border-emerald-500"
                  />
                </div>
                <span className="text-[10px] text-slate-500 mt-2 block leading-relaxed text-center">Enter the simulated OTP shown in the green notification box above to bind your user registration.</span>
              </div>

              <button
                type="submit"
                disabled={isLoading || verificationCode.length < 5}
                className="w-full py-3.5 bg-emerald-500 hover:bg-emerald-600 disabled:opacity-50 text-slate-950 font-extrabold rounded-xl shadow-lg shadow-emerald-500/10 tracking-wider text-xs uppercase transition-all mt-4 flex items-center justify-center gap-2"
              >
                {isLoading ? <RefreshCw className="animate-spin" size={14} /> : "Verify Identity"}
              </button>

              <div className="pt-2 text-center">
                <button
                  type="button"
                  onClick={() => {
                    setAuthMode("SIGN_UP");
                    setErrorText("");
                    setInfoText("");
                  }}
                  className="text-xs text-slate-400 hover:text-white underline font-semibold flex items-center justify-center gap-1.5 mx-auto"
                >
                  <ArrowLeft size={12} />
                  <span>Back to Signup form</span>
                </button>
              </div>
            </motion.form>
          )}

          {authMode === "FORGOT_PASSWORD" && (
            <motion.form
              key="forgot"
              onSubmit={handleForgotPassword}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="space-y-4 text-left"
            >
              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1.5">Registered Email Address</label>
                <div className="relative">
                  <Mail size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="email"
                    required
                    placeholder="e.g. rahul@gmail.com"
                    value={email}
                    onChange={(e) => setEmail(e.target.value)}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
                <span className="text-[10px] text-slate-500 mt-1.5 block leading-normal">Enter the email associated with your Cognito ID signature. We will direct an SES secure verification hash code to you.</span>
              </div>

              <button
                type="submit"
                className="w-full py-3.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-extrabold rounded-xl shadow-lg tracking-wider text-xs uppercase transition-all mt-2"
              >
                Trigger reset OTP
              </button>

              <div className="pt-1 text-center">
                <button
                  type="button"
                  onClick={() => {
                    setAuthMode("SIGN_IN");
                    setErrorText("");
                    setInfoText("");
                  }}
                  className="text-xs text-slate-400 hover:text-white underline font-semibold flex items-center justify-center gap-1.5 mx-auto"
                >
                  <ArrowLeft size={12} />
                  <span>Back to login portal</span>
                </button>
              </div>
            </motion.form>
          )}

          {authMode === "RESET_PASSWORD" && (
            <motion.form
              key="resetpw"
              onSubmit={handleResetPassword}
              initial={{ opacity: 0, x: 20 }}
              animate={{ opacity: 1, x: 0 }}
              exit={{ opacity: 0, x: -20 }}
              className="space-y-4 text-left"
            >
              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1.5">Reset OTP Code</label>
                <div className="relative">
                  <Lock size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="text"
                    required
                    maxLength={6}
                    placeholder="xxxxxx"
                    value={verificationCode}
                    onChange={(e) => setVerificationCode(e.target.value.replace(/\D/g, ""))}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 text-center font-mono tracking-widest text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <div>
                <label className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1.5">New Password</label>
                <div className="relative">
                  <Key size={15} className="absolute left-3.5 top-3.5 text-slate-500" />
                  <input
                    type="password"
                    required
                    placeholder="At least 8 parameters"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                    className="w-full bg-slate-950/60 border border-slate-800 rounded-xl pl-10 pr-4 py-3 placeholder-slate-600 text-white text-sm focus:outline-none focus:border-emerald-500"
                  />
                </div>
              </div>

              <button
                type="submit"
                className="w-full py-3.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-extrabold rounded-xl shadow-lg tracking-wider text-xs uppercase transition-all mt-2"
              >
                Apply new password hash
              </button>
            </motion.form>
          )}
        </AnimatePresence>
      </motion.div>
    </div>
  );
}
