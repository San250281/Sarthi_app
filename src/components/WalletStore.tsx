import React, { useState } from "react";
import { UserProfile } from "../types";
import { CreditCard, Wallet, ShieldCheck, Sparkles, Check, ArrowRight, X, AlertCircle } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

interface WalletStoreProps {
  walletBalance: number;
  setWalletBalance: React.Dispatch<React.SetStateAction<number>>;
  userProfile: UserProfile;
  setUserProfile: React.Dispatch<React.SetStateAction<UserProfile>>;
}

interface RechargePackage {
  id: string;
  name: string;
  price: number;
  value: number;
  desc: string;
  isPopular?: boolean;
}

interface SubscriptionPlan {
  id: string;
  name: string;
  price: number;
  billing: string;
  benefits: string[];
}

export default function WalletStore({
  walletBalance,
  setWalletBalance,
  userProfile,
  setUserProfile,
}: WalletStoreProps) {
  const [razorpayModalOpen, setRazorpayModalOpen] = useState(false);
  const [selectedTxType, setSelectedTxType] = useState<"topup" | "subscription">("topup");
  const [selectedTopupPkg, setSelectedTopupPkg] = useState<RechargePackage | null>(null);
  const [selectedSubPlan, setSelectedSubPlan] = useState<SubscriptionPlan | null>(null);

  // Razorpay simulator loading state
  const [isProcessingPayment, setIsProcessingPayment] = useState(false);

  const topupPackages: RechargePackage[] = [
    { id: "pkg_30", name: "₹30 Top-up", price: 30, value: 30, desc: "Add ₹30 talk time credits" },
    { id: "pkg_50", name: "₹50 Top-up", price: 50, value: 50, desc: "Add ₹50 talk time credits" },
    { id: "pkg_100", name: "₹100 Value-Pack", price: 100, value: 120, desc: "Get ₹120 value (₹20 Bonus!)", isPopular: true },
  ];

  const subscriptionPlans: SubscriptionPlan[] = [
    {
      id: "sub_weekly",
      name: "Weekly Explorer",
      price: 49,
      billing: "billed weekly",
      benefits: ["Free consultations with Saarthi", "Reduced rates (₹1/min) for philosophers like Saint Kabir", "Detailed Daily AI growth sheets", "Micro-goals tracker system"],
    },
    {
      id: "sub_monthly",
      name: "Monthly Companion",
      price: 149,
      billing: "billed monthly",
      benefits: ["Free talk loops with Saarthi & Meera", "Flat Indian philosophic Kabir speaking at ₹2/min", "Premium synthesis charts unlocked", "Saved profile priority access"],
    },
    {
      id: "sub_yearly",
      name: "Yearly Zen Membership",
      price: 999,
      billing: "billed yearly",
      benefits: ["Unlimited voice-chat calls with ALL companion agents", "Exclusive daily Zen breathing routines", "Deep AI growth report compiles anytime", "Early alpha companion agents"],
    },
  ];

  // Open billing flow for Topups
  const triggerTopupPayment = (pkg: RechargePackage) => {
    setSelectedTxType("topup");
    setSelectedTopupPkg(pkg);
    setSelectedSubPlan(null);
    setRazorpayModalOpen(true);
  };

  // Open billing flow for Subscriptions
  const triggerSubPayment = (plan: SubscriptionPlan) => {
    setSelectedTxType("subscription");
    setSelectedSubPlan(plan);
    setSelectedTopupPkg(null);
    setRazorpayModalOpen(true);
  };

  // Simulate payment completion
  const handlePaymentSimulation = (success: boolean) => {
    setIsProcessingPayment(true);
    
    // Simulate Razorpay secure verifying delay
    setTimeout(() => {
      setIsProcessingPayment(false);
      setRazorpayModalOpen(false);

      if (success) {
        if (selectedTxType === "topup" && selectedTopupPkg) {
          setWalletBalance((prev) => prev + selectedTopupPkg.value);
          alert(`Success! Recharge of ₹${selectedTopupPkg.price} verified. Credited ₹${selectedTopupPkg.value} to your Saarthi wallet balance! New balance: ₹${(walletBalance + selectedTopupPkg.value).toFixed(2)}`);
        } else if (selectedTxType === "subscription" && selectedSubPlan) {
          // Grant plan access
          const expiryDate = Date.now() + 30 * 24 * 60 * 60 * 1000; // soft monthly timestamp
          setUserProfile((prev) => ({
            ...prev,
            subscriptionPlan: selectedSubPlan.name,
            subscriptionStatus: "Active",
            subscriptionExpiry: expiryDate,
          }));
          alert(`Payment Verified! You have successfully subscribed to the "${selectedSubPlan.name}" plan. Your premium benefits are immediately active on your profile.`);
        }
      } else {
        alert("Payment Aborted / Failed. Your payment transaction was canceled, no money was charged or added. Please try again.");
      }

      // Reset selection references
      setSelectedTopupPkg(null);
      setSelectedSubPlan(null);
    }, 1500);
  };

  return (
    <div className="flex flex-col h-full bg-slate-900 rounded-3xl border border-slate-800 p-6 md:p-8 overflow-y-auto scroller shadow-xl" id="wallet_store_root">
      
      {/* Header Description */}
      <div className="mb-8">
        <h2 className="font-display text-2xl md:text-3xl font-bold tracking-tight text-white flex items-center gap-3">
          <Wallet className="text-emerald-400" />
          <span>Wallet & Razorpay Store</span>
        </h2>
        <p className="text-sm font-sans text-slate-400 mt-2 tracking-wide leading-relaxed">
          Maintain your talk credits dynamically. Razorpay is safely set up in a fully sandbox-compliant simulator mode. Top up your balance or subscribe immediately to unlock unlimited companion loops.
        </p>
      </div>

      {/* Wallet Balance Board */}
      <div className="bg-slate-950/60 p-6 rounded-3xl border border-slate-800/80 mb-8 grid grid-cols-1 md:grid-cols-2 gap-6 items-center">
        <div className="text-left">
          <span className="text-xs uppercase text-slate-500 font-mono tracking-widest font-bold block">Current Balance</span>
          <div className="flex items-baseline gap-2.5 mt-2">
            <span className="text-4xl md:text-5xl font-mono font-bold text-white bg-clip-text">
              ₹{walletBalance.toFixed(2)}
            </span>
            <span className="text-sm font-semibold text-emerald-400">INR</span>
          </div>

          <div className="mt-4 flex items-center gap-2 text-xs text-slate-400">
            <ShieldCheck size={16} className="text-emerald-500" />
            <span>Securely managed offline-first transaction system</span>
          </div>
        </div>

        {/* Subscription state showcase */}
        <div className="bg-slate-900/60 p-4.5 rounded-2xl border border-slate-800 flex flex-col justify-between text-left h-full">
          <div>
            <span className="text-[10px] uppercase text-slate-500 font-bold tracking-widest block">Subscription Tier</span>
            <span className="text-lg font-bold text-white mt-1 block">{userProfile.subscriptionPlan}</span>
          </div>

          <div className="mt-3.5 pt-3 border-t border-slate-900/80 flex items-center justify-between text-xs">
            <span className="text-slate-400">Status</span>
            <span className={`px-2 py-0.5 rounded-full font-semibold text-[10px] ${
              userProfile.subscriptionStatus === "Active" 
                ? "bg-emerald-500/10 text-emerald-400" 
                : "bg-slate-800 text-slate-400"
            }`}>
              {userProfile.subscriptionStatus}
            </span>
          </div>
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-8 items-start">
        
        {/* Packages left: Topups */}
        <div className="lg:col-span-4 space-y-4 text-left">
          <h3 className="font-display font-semibold text-lg text-white mb-2 leading-none">
            Top-up vouchers
          </h3>
          <p className="text-xs text-slate-400 mb-4 leading-normal">
            Select standard packages to dynamically recharge your speech credits balance.
          </p>

          <div className="space-y-3">
            {topupPackages.map((pkg) => (
              <div
                key={pkg.id}
                onClick={() => triggerTopupPayment(pkg)}
                className={`p-4 rounded-2xl border transition-all cursor-pointer flex justify-between items-center bg-slate-950/40 hover:border-emerald-500/50 hover:bg-slate-950/60 relative overflow-hidden ${
                  pkg.isPopular ? "border-emerald-500/40 bg-slate-950/60" : "border-slate-800"
                }`}
              >
                {pkg.isPopular && (
                  <div className="absolute top-0 right-0 bg-emerald-500 text-slate-950 text-[8px] font-extrabold uppercase px-2 py-0.5 rounded-bl">
                    Best Value
                  </div>
                )}
                
                <div>
                  <h4 className="font-semibold text-white text-sm">{pkg.name}</h4>
                  <p className="text-xs text-slate-400 mt-1">{pkg.desc}</p>
                </div>

                <div className="font-mono text-sm font-bold text-emerald-400 shrink-0 bg-slate-900 border border-slate-800 py-1.5 px-3 rounded-xl flex items-center gap-1">
                  <span>Pay</span>
                  <span>₹{pkg.price}</span>
                </div>
              </div>
            ))}
          </div>
        </div>

        {/* Subscriptions right: Unlimited memberships */}
        <div className="lg:col-span-8 space-y-4 text-left">
          <h3 className="font-display font-semibold text-lg text-white mb-2 leading-none">
            Mentorship memberships
          </h3>
          <p className="text-xs text-slate-400 mb-4 leading-normal">
            Unlock premium features, detailed tracking summaries, and speak with specific premium agents!
          </p>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {subscriptionPlans.map((plan) => {
              const isSelected = userProfile.subscriptionPlan === plan.name;
              return (
                <div
                  key={plan.id}
                  className={`bg-slate-950/40 p-5 rounded-2xl border flex flex-col justify-between text-left h-full transition-all relative ${
                    isSelected ? "border-emerald-500 bg-slate-950/80 shadow-lg shadow-emerald-500/5" : "border-slate-800"
                  }`}
                >
                  <div>
                    <h4 className="font-display font-bold text-white text-base leading-tight">
                      {plan.name}
                    </h4>
                    
                    <div className="flex items-baseline gap-1 mt-3">
                      <span className="text-2xl font-mono font-bold text-emerald-400">₹{plan.price}</span>
                      <span className="text-[10px] text-slate-500 font-semibold uppercase font-sans">/{plan.billing.split(" ").pop()}</span>
                    </div>

                    <ul className="mt-5 space-y-2.5 text-xs text-slate-300">
                      {plan.benefits.map((benefit, idx) => (
                        <li key={idx} className="flex gap-2 leading-relaxed">
                          <Check size={14} className="text-emerald-500 shrink-0 mt-0.5" />
                          <span>{benefit}</span>
                        </li>
                      ))}
                    </ul>
                  </div>

                  <button
                    onClick={() => triggerSubPayment(plan)}
                    disabled={isSelected}
                    className={`w-full py-2.5 rounded-xl text-xs font-semibold mt-6 flex items-center justify-center gap-2 transition-all ${
                      isSelected
                        ? "bg-slate-800 text-slate-500 cursor-default border border-slate-700/40"
                        : "bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold"
                    }`}
                  >
                    <span>{isSelected ? "Active subscription" : "Unlock plan"}</span>
                    {!isSelected && <ArrowRight size={13} />}
                  </button>
                </div>
              );
            })}
          </div>
        </div>
      </div>

      {/* razorpay interactive payment simulator overlay modal */}
      <AnimatePresence>
        {razorpayModalOpen && (
          <div className="fixed inset-0 bg-black/75 flex items-center justify-center p-4 z-50 backdrop-blur-sm" id="pay_simulator_modal">
            <motion.div
              initial={{ scale: 0.95, opacity: 0 }}
              animate={{ scale: 1, opacity: 1 }}
              exit={{ scale: 0.95, opacity: 0 }}
              className="bg-[#0c1424] border border-[#1b2a47] rounded-3xl w-full max-w-md overflow-hidden shadow-2xl relative"
            >
              {/* Razorpay stylized header */}
              <div className="bg-[#101931] p-5 border-b border-[#1f2b4e] flex justify-between items-center text-left">
                <div className="flex items-center gap-3">
                  <div className="bg-blue-600 p-2 rounded-xl text-white font-black text-sm tracking-tighter"> R </div>
                  <div>
                    <h4 className="font-display font-bold text-white text-sm leading-normal">Razorpay Gateway</h4>
                    <p className="text-[10px] text-slate-400 font-sans uppercase tracking-widest leading-none mt-0.5">Secure sandbox simulator</p>
                  </div>
                </div>
                
                <button
                  onClick={() => setRazorpayModalOpen(false)}
                  className="p-1 rounded-full text-slate-400 hover:bg-slate-800 hover:text-white"
                >
                  <X size={18} />
                </button>
              </div>

              {/* invoice pricing summary layout */}
              <div className="p-6 text-left border-b border-[#172545]/60">
                <span className="text-[10px] text-slate-400 font-bold uppercase tracking-widest block">Merchant: Saarthi App Technologies</span>
                
                <div className="flex justify-between items-baseline mt-4 mb-2">
                  <span className="text-sm font-semibold text-slate-300">
                    {selectedTxType === "topup" ? selectedTopupPkg?.name : selectedSubPlan?.name}
                  </span>
                  <div className="flex items-baseline gap-0.5">
                    <span className="text-2xl font-mono font-bold text-white">
                      ₹{selectedTxType === "topup" ? selectedTopupPkg?.price : selectedSubPlan?.price}
                    </span>
                    <span className="text-xs text-slate-400 uppercase font-sans">INR</span>
                  </div>
                </div>

                <div className="bg-slate-950/60 p-3 rounded-xl border border-slate-800 text-xs text-slate-400 flex items-start gap-2.5">
                  <AlertCircle size={18} className="text-slate-500 shrink-0 mt-0.5" />
                  <p className="leading-relaxed">This simulates verification triggers of Razorpay payments. Choose one of the buttons below to test dynamic updates.</p>
                </div>
              </div>

              {/* CTA Action Bar */}
              <div className="p-6 space-y-3 bg-[#0d172b]">
                {isProcessingPayment ? (
                  <div className="text-center py-6">
                    <svg className="animate-spin h-8 w-8 text-blue-500 mx-auto mb-3" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                      <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                      <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.11 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                    </svg>
                    <p className="text-slate-300 text-xs font-semibold">Processing transaction secure check...</p>
                    <p className="text-slate-500 text-[10px] mt-1.5 font-mono">Do not close window or go back.</p>
                  </div>
                ) : (
                  <>
                    <button
                      onClick={() => handlePaymentSimulation(true)}
                      className="w-full py-3 bg-blue-600 hover:bg-blue-700 text-white font-bold rounded-xl flex items-center justify-center gap-2 text-sm shadow-xl shadow-blue-600/10 transition-all"
                    >
                      <ShieldCheck size={16} />
                      <span>Simulate SUCCESS (Sim payment verified)</span>
                    </button>

                    <button
                      onClick={() => handlePaymentSimulation(false)}
                      className="w-full py-3 bg-slate-900 hover:bg-slate-800 text-slate-300 border border-slate-800 font-semibold rounded-xl flex items-center justify-center gap-2 text-sm transition-all"
                    >
                      <span>Simulate FAILURE (Payment canceled)</span>
                    </button>
                  </>
                )}
              </div>
            </motion.div>
          </div>
        )}
      </AnimatePresence>
    </div>
  );
}
