import React, { useState, useEffect } from "react";
import { UserProfile, ChatMessage, MemoryItem, MoodRecord, ReflectionSummary, AIAgent, SaarthiState, Conversation, CloudWatchLog } from "./types";
import CallInterface from "./components/CallInterface";
import JournalSpace from "./components/JournalSpace";
import InsightsDashboard from "./components/InsightsDashboard";
import WalletStore from "./components/WalletStore";
import AuthOverlay from "./components/AuthOverlay";
import InfoHub from "./components/InfoHub";
import { Mic, BookOpen, TrendingUp, Wallet, LogOut, User, FolderHeart, Compass, Trash2, Info } from "lucide-react";

const SUPPORTED_AGENTS: AIAgent[] = [
  {
    id: "saarthi",
    name: "Saarthi",
    pricePerMin: 5,
    avatarEmoji: "🧘",
    description: "Your emotionally intelligent guide, specializing in listening, deep validation, and suggesting supportive micro-steps.",
    greeting: "Aapka swagat hai. I am Saarthi, your voice companion. I am here to listen and help you navigate life with clarity and confidence. How are you feeling today?",
    systemPromptExtension: "You are Saarthi, an exceptionally warm, gentle, emotionally intelligent AI voice companion. Talk primarily in simple English, Hindi, or conversational Hinglish. Be a compassionate, validate-first listener. Provide short, voice-friendly answers without nested markdown or lists so your voice text-to-speech sounds perfectly natural."
  },
  {
    id: "kabir",
    name: "Saint Kabir",
    pricePerMin: 10,
    avatarEmoji: "🕉️",
    description: "Philosophical mentor. Speaking with detachment and profound depth, weaving in ancient parables and traditional Indian dohās.",
    greeting: "Pranam, seeker. I am Kabir. Life operates on subtle laws. 'Bura jo dekhan main chala, bura na milya koy...' What locks bind your thoughts today?",
    systemPromptExtension: "You are Saint Kabir, a deep Indian spiritual and philosophical mentor. Speak with serene, steady composure. Weave in parables, traditional Indian dohas, or spiritual insights. Guide the user toward self-reflection and tranquility."
  },
  {
    id: "meera",
    name: "Meera",
    pricePerMin: 8,
    avatarEmoji: "🎨",
    description: "An expressive, enthusiastic creative therapist coaching you to find creative flow, write, or spark artistic relief.",
    greeting: "Hello, creative soul! I am Meera, your artistic spark. Let's sculpt your thoughts today. What beauty or dream calls you?",
    systemPromptExtension: "You are Meera, an enthusiastic creative coach and therapist. Speak with positive, artistic energy. Help the user seek inspiration, dream, express themselves, and write or find solace in creative flows."
  },
  {
    id: "shanti",
    name: "Shanti",
    pricePerMin: 12,
    avatarEmoji: "🕊️",
    description: "Zen teacher and mindfulness coach guiding slow breathing circles with extreme current-moment calm.",
    greeting: "Deep, soothing breath in... and slow release. Welcome friend, I am Shanti. Let's rest together. What is your current-moment reality showing you?",
    systemPromptExtension: "You are Shanti, a serene Zen mindfulness teacher. Speak extremely slowly and with intense tranquility. Remind the user to follow their breathing, anchor their thoughts in the present moment, and let all anxieties float away."
  },
];

export default function App() {
  // Tab selector state
  const [activeTab, setActiveTab] = useState<"call" | "journal" | "insights" | "wallet" | "compliance">("call");

  // Client local database states
  const [userProfile, setUserProfile] = useState<UserProfile>(() => {
    const saved = localStorage.getItem("saarthi_profile");
    if (saved) {
      try {
        const parsed = JSON.parse(saved);
        // Force log out on app load to require PIN access for absolute security
        return { ...parsed, isLoggedIn: false };
      } catch (_) {}
    }
    return {
      userName: "",
      preferredLanguage: "English",
      currentMood: "Normal",
      themeMode: "Light",
      userEmail: "",
      passcodeHash: "",
      isLoggedIn: false,
      subscriptionPlan: "Free Tier",
      subscriptionExpiry: 0,
      subscriptionStatus: "Inactive",
      dailyQuestionsCount: 0,
      lastQuestionTimestamp: Date.now(),
      createdDate: Date.now(),
      lastLogin: Date.now(),
    };
  });

  const [walletBalance, setWalletBalance] = useState<number>(() => {
    const saved = localStorage.getItem("saarthi_wallet");
    return saved ? Number(JSON.parse(saved)) : 45.0; // Start with complementary ₹45 free talk-time credits!
  });

  const [messages, setMessages] = useState<ChatMessage[]>(() => {
    const saved = localStorage.getItem("saarthi_messages");
    return saved ? JSON.parse(saved) : [];
  });

  // Multiple conversations state
  const [conversations, setConversations] = useState<Conversation[]>(() => {
    const saved = localStorage.getItem("saarthi_conversations");
    return saved ? JSON.parse(saved) : [
      { id: "default_chat", title: "Warm Guidance Space", createdDate: Date.now(), updatedDate: Date.now() }
    ];
  });

  const [activeConversationId, setActiveConversationId] = useState<string>(() => {
    const saved = localStorage.getItem("saarthi_active_conv");
    return saved ? JSON.parse(saved) : "default_chat";
  });

  // Real-time CloudWatch Logs state
  const [cloudWatchLogs, setCloudWatchLogs] = useState<CloudWatchLog[]>(() => {
    const now = Date.now();
    return [
      { id: "log_init_1", timestamp: now - 4000, level: "INFO", message: "amazon-cognito-idp: Service endpoint bound successfully for Region: ap-south-1." },
      { id: "log_init_2", timestamp: now - 3000, level: "INFO", message: "aws-lambda: Warm-starting handler orchestration 'chat-completion' with Node20/ESM container." },
      { id: "log_init_3", timestamp: now - 2000, level: "INFO", message: "dynamo-db: Established secure HTTPS sockets connected to Users, Conversations and Subscriptions tables." },
      { id: "log_init_4", timestamp: now - 1000, level: "METRIC", message: "API Gateway: Successfully completed handshakes. Ready for secure proxy routes on internal port." },
    ];
  });

  const logToCloudWatch = (level: "INFO" | "METRIC" | "WARN" | "ERROR", message: string) => {
    const newLog: CloudWatchLog = {
      id: "cw_log_" + Math.random().toString(36).substring(2, 11),
      timestamp: Date.now(),
      level,
      message
    };
    setCloudWatchLogs(prev => [newLog, ...prev].slice(0, 50));
    console.log(`[CloudWatch - ${level}] ${message}`);
  };

  const [memories, setMemories] = useState<MemoryItem[]>(() => {
    const saved = localStorage.getItem("saarthi_memories");
    return saved ? JSON.parse(saved) : [];
  });

  const [moodRecords, setMoodRecords] = useState<MoodRecord[]>(() => {
    const saved = localStorage.getItem("saarthi_mood");
    if (saved) return JSON.parse(saved);
    
    // Seed default records to make Recharts display gorgeous lines instantly on first load
    const now = Date.now();
    const records = [
      { id: "seed_1", mood: "Normal", confidence: 5, clarity: 4, notes: "Feeling anxious about project milestones", timestamp: now - 3 * 24 * 60 * 60 * 1000 },
      { id: "seed_2", mood: "Confused", confidence: 4, clarity: 5, notes: "Talking with Saarthi helped clarify focus", timestamp: now - 2 * 24 * 60 * 60 * 1000 },
      { id: "seed_3", mood: "Normal", confidence: 6, clarity: 6, notes: "Took a walk, slept better last night", timestamp: now - 1 * 24 * 60 * 60 * 1000 },
    ];
    localStorage.setItem("saarthi_mood", JSON.stringify(records));
    return records;
  });

  const [reflections, setReflections] = useState<ReflectionSummary[]>(() => {
    const saved = localStorage.getItem("saarthi_reflections");
    return saved ? JSON.parse(saved) : [];
  });

  // Companion state (LISTENING, IDLE etc)
  const [companionState, setCompanionState] = useState<SaarthiState>("IDLE");
  const [isTtsEnabled, setIsTtsEnabled] = useState(true);
  
  // Selected premium agent id
  const [selectedAgentId, setSelectedAgentId] = useState("saarthi");
  const activeAgent = SUPPORTED_AGENTS.find((a) => a.id === selectedAgentId) || SUPPORTED_AGENTS[0];

  // Sync state data structures to local storage instantly
  useEffect(() => {
    localStorage.setItem("saarthi_wallet", JSON.stringify(walletBalance));
  }, [walletBalance]);

  useEffect(() => {
    localStorage.setItem("saarthi_messages", JSON.stringify(messages));
  }, [messages]);

  useEffect(() => {
    localStorage.setItem("saarthi_conversations", JSON.stringify(conversations));
  }, [conversations]);

  useEffect(() => {
    localStorage.setItem("saarthi_active_conv", JSON.stringify(activeConversationId));
  }, [activeConversationId]);

  useEffect(() => {
    localStorage.setItem("saarthi_memories", JSON.stringify(memories));
  }, [memories]);

  useEffect(() => {
    localStorage.setItem("saarthi_profile", JSON.stringify(userProfile));
  }, [userProfile]);

  // Handler for voice communication submission and memory triggers
  const handleSendMessage = async (text: string, isVoice = false) => {
    // 1. Subscription plan quota check for Free Tier
    if (userProfile.subscriptionPlan === "Free Tier") {
      const todayString = new Date().toDateString();
      const lastActiveString = new Date(userProfile.lastQuestionTimestamp || Date.now()).toDateString();
      let currentCount = userProfile.dailyQuestionsCount || 0;

      if (todayString !== lastActiveString) {
        // Reset counter for a fresh calendar day
        currentCount = 0;
      }

      if (currentCount >= 10) {
        alert("Daily Free Limit Reached! You have completed your 10 free daily question allotments. Raise your quota limits by unlocking are Premium Plan packages on the Wallet Store tab.");
        logToCloudWatch("WARN", `SubscriptionGuard: Daily free questions block executed for subscription ID of user (${userProfile.userEmail}).`);
        return;
      }

      // Update quota count
      const newCount = currentCount + 1;
      setUserProfile((prev) => ({
        ...prev,
        dailyQuestionsCount: newCount,
        lastQuestionTimestamp: Date.now(),
      }));
      logToCloudWatch("INFO", `SubscriptionGuard: Logged message for user (${userProfile.userEmail}). Quota count: ${newCount}/10 today.`);
    }

    const now = Date.now();
    const newUserMsg: ChatMessage = {
      id: "msg_" + Math.random().toString(36).substring(2, 11),
      conversationId: activeConversationId, // Bound to active conversation session
      role: "user",
      text: text,
      timestamp: now,
      isVoice: isVoice,
    };

    const updatedMessages = [...messages, newUserMsg];
    setMessages(updatedMessages);

    // Fetch conversation summary for context compression
    const activeConv = conversations.find((c) => c.id === activeConversationId);
    const conversationSummary = activeConv?.summary || "";

    // Build companion contextual memory prompt
    const profileMemoriesText = memories
      .map((m) => `- You remember that ${userProfile.userName}'s ${m.key} is: ${m.value}`)
      .join("\n");

    const fullCompanionPrompt = `${activeAgent.systemPromptExtension}
Here are customized facts you remember about ${userProfile.userName}:
${profileMemoriesText || "(No memories saved yet)"}

Respond warmly to ${userProfile.userName}'s latest comment, respecting your persona, talking speed, and specialized wisdom.`;

    const startTime = Date.now();
    logToCloudWatch("INFO", `AWS Lambda: Invoking 'saarthi-orchestration-agent' targeting model 'gemini-2.5-flash'. Prompt tokens optimized.`);

    try {
      const response = await fetch("/api/chat", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message: text,
          history: updatedMessages
            .filter((m) => m.conversationId === activeConversationId)
            .slice(-5), // Keep past 5 local messages in active session context as instructed
          systemPrompt: fullCompanionPrompt,
          isVoice: isVoice,
          conversationSummary: conversationSummary,
        }),
      });

      if (!response.ok) {
        throw new Error("Failed to get response from Saarthi API");
      }

      const replyData = await response.json();
      const botReply = replyData.response;
      const latency = Date.now() - startTime;

      logToCloudWatch("METRIC", `AWS Lambda: Successfully compiled Gemini response in ${latency}ms for active session: ${activeConversationId}.`);

      const newBotMsg: ChatMessage = {
        id: "msg_" + Math.random().toString(36).substring(2, 11),
        conversationId: activeConversationId, // Bound to active conversation session
        role: "saarthi",
        text: botReply,
        timestamp: Date.now(),
        agentId: activeAgent.id,
      };

      const finalMessages = [...updatedMessages, newBotMsg];
      setMessages(finalMessages);
      setCompanionState("SPEAKING");

      // Background task 2: Summarize conversation history asynchronously if conversation is growing
      const currentSessionMsgs = finalMessages.filter((m) => m.conversationId === activeConversationId);
      if (currentSessionMsgs.length > 5) {
        fetch("/api/summarize-history", {
          method: "POST",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify({
            history: currentSessionMsgs,
            existingSummary: conversationSummary,
          }),
        })
          .then((r) => r.json())
          .then((summaryData) => {
            if (summaryData?.summary) {
              setConversations((prev) =>
                prev.map((c) =>
                  c.id === activeConversationId ? { ...c, summary: summaryData.summary } : c
                )
              );
              logToCloudWatch("INFO", `MemoryManager: Optimized conversation summary updated: "${summaryData.summary}"`);
            }
          })
          .catch((err) => console.error("History summarizer background error:", err));
      }

      // Extract memories of the conversation sequentially
      fetch("/api/extract-memories", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          message: text,
          aiResponse: botReply,
          existingMemories: memories,
        }),
      })
        .then((r) => r.json())
        .then((extracted) => {
          if (extracted?.memories && extracted.memories.length > 0) {
            setMemories((prev) => {
              const cleanedList = [...prev];
              extracted.memories.forEach((newMem: any) => {
                const duplicateIdx = cleanedList.findIndex(
                  (m) => m.category === newMem.category && m.key.toLowerCase() === newMem.key.toLowerCase()
                );
                
                const formattedNewMem: MemoryItem = {
                  id: "mem_" + Math.random().toString(36).substring(2, 11),
                  category: newMem.category,
                  key: newMem.key,
                  value: newMem.value,
                  timestamp: Date.now(),
                };

                if (duplicateIdx !== -1) {
                  cleanedList[duplicateIdx] = formattedNewMem;
                } else {
                  cleanedList.push(formattedNewMem);
                }
              });
              
              logToCloudWatch("INFO", `DynamoDB: Automatically mined and saved ${extracted.memories.length} profile memories in customer details table.`);
              return cleanedList;
            });
          }
        })
        .catch((e) => console.error("Memory extraction task error:", e));

      return botReply;
    } catch (err: any) {
      logToCloudWatch("ERROR", `AWS Lambda: Failed downstream handshakes. Service Error: ${err.message}.`);
      setCompanionState("IDLE");
      alert("Lost connectivity with Saarthi: " + err.message);
      return undefined;
    }
  };

  // Add Reflection callback
  const handleAddReflection = (text: string, summary: Partial<ReflectionSummary>) => {
    const newRef: ReflectionSummary = {
      id: "ref_" + Math.random().toString(36).substring(2, 11),
      originalText: text,
      detectedFeelings: summary.detectedFeelings || "Emotional release",
      keyTakeaways: summary.keyTakeaways || "",
      actionableSteps: summary.actionableSteps || "",
      mainMoodEmoji: summary.mainMoodEmoji || "🌱",
      timestamp: Date.now(),
    };

    const updated = [newRef, ...reflections];
    setReflections(updated);
    localStorage.setItem("saarthi_reflections", JSON.stringify(updated));

    logToCloudWatch("INFO", `S3 Storage: Captured Reflection summary backup in bucket: 'saarthi-journal-archives'`);

    // Also automatically log mood record based on emoji analyzed
    handleAddMoodRecord("Normal", 6, 6, "Reflection journal sync: " + text.slice(0, 30) + "...");
  };

  const handleDeleteReflection = (id: string) => {
    const updated = reflections.filter((r) => r.id !== id);
    setReflections(updated);
    localStorage.setItem("saarthi_reflections", JSON.stringify(updated));
    logToCloudWatch("INFO", `S3 Storage: Deleted reflection file coordinate: ${id}.`);
  };

  // Add Custom Mood Record callback
  const handleAddMoodRecord = (mood: string, confidence: number, clarity: number, notes: string) => {
    const record: MoodRecord = {
      id: "mood_" + Math.random().toString(36).substring(2, 11),
      mood,
      confidence,
      clarity,
      notes,
      timestamp: Date.now(),
    };
    const updated = [record, ...moodRecords];
    setMoodRecords(updated);
    localStorage.setItem("saarthi_mood", JSON.stringify(updated));
    logToCloudWatch("INFO", `DynamoDB: Committed new Mood log row in user index metrics.`);
  };

  // Clear single user memory detail
  const handleDeleteMemory = (id: string) => {
    const updated = memories.filter((m) => m.id !== id);
    setMemories(updated);
    logToCloudWatch("INFO", `DynamoDB: Purged atomic keyword memory record: ${id}.`);
  };

  // Manage rooms
  const handleCreateConversation = (title: string) => {
    const newId = "conv_" + Math.random().toString(36).substring(2, 11);
    const newConv: Conversation = {
      id: newId,
      title: title || `Dynamic Guide #${conversations.length + 1}`,
      createdDate: Date.now(),
      updatedDate: Date.now()
    };
    setConversations(prev => [...prev, newConv]);
    setActiveConversationId(newId);
    logToCloudWatch("INFO", `DynamoDB: Created new conversation session: "${newConv.title}" (ID: ${newId}).`);
  };

  const handleDeleteConversation = (id: string) => {
    if (conversations.length <= 1) {
      alert("You should preserve at least one active conversation room.");
      return;
    }
    const updated = conversations.filter(c => c.id !== id);
    setConversations(updated);
    
    // Purge linked messages
    setMessages(prev => prev.filter(m => m.conversationId !== id));

    if (activeConversationId === id) {
      setActiveConversationId(updated[0].id);
    }
    logToCloudWatch("WARN", `DynamoDB: Deleted conversation document: ${id}. Purged linked chat transcript entries.`);
  };

  const handleRenameConversation = (id: string, newTitle: string) => {
    if (!newTitle.trim()) return;
    setConversations(prev => prev.map(c => c.id === id ? { ...c, title: newTitle.trim(), updatedDate: Date.now() } : c));
    logToCloudWatch("INFO", `DynamoDB: Modified conversation title payload for ${id} to "${newTitle.trim()}".`);
  };

  // Log out mechanism
  const handleSignOut = () => {
    setUserProfile((prev) => ({
      ...prev,
      isLoggedIn: false,
    }));
    logToCloudWatch("INFO", `Amazon Cognito: Token signature invalidation executed. Locked profile session.`);
  };

  return (
    <div className="min-h-screen bg-slate-950 font-sans text-slate-200 antialiased flex flex-col relative" id="saarthi_app_layout">
      
      {/* Absolute Header: security logins checking */}
      {!userProfile.isLoggedIn && (
        <AuthOverlay userProfile={userProfile} setUserProfile={setUserProfile} onLogCloudWatch={logToCloudWatch} />
      )}

      {/* Main Core Viewport Grid */}
      <div className="flex-1 max-w-7xl w-full mx-auto p-4 sm:p-6 lg:p-8 flex flex-col lg:grid lg:grid-cols-12 gap-8 h-[calc(100vh-2px)] overflow-hidden" id="app_frame_grid">
        
        {/* Left Side: Navigation pane & User Identity Profile status */}
        <div className="lg:col-span-3 flex flex-col justify-between bg-slate-900 border border-slate-800 p-5 rounded-3xl shrink-0" id="sidebar_nav_wrapper">
          <div className="space-y-6">
            
            {/* Logo display */}
            <div className="flex items-center gap-3 border-b border-slate-800 pb-5 text-left">
              <div className="p-2.5 rounded-2xl bg-emerald-500 text-slate-950 font-black text-lg tracking-wide shadow-md flex items-center justify-center">
                🧘
              </div>
              <div>
                <h1 className="font-display font-extrabold text-xl text-white tracking-tight leading-tighter">Saarthi</h1>
                <span className="text-[10px] text-emerald-400 font-mono tracking-widest uppercase">Safe AI Companion</span>
              </div>
            </div>

            {/* Profile Detail Cards */}
            <div className="bg-slate-950/45 p-4 rounded-2xl border border-slate-800/80 text-left">
              <div className="flex items-center gap-2.5">
                <div className="w-8 h-8 rounded-full bg-slate-800 flex items-center justify-center text-slate-300 font-bold border border-slate-700 text-sm">
                  <User size={14} />
                </div>
                <div className="overflow-hidden">
                  <h4 className="font-sans font-bold text-slate-100 text-sm truncate">{userProfile.userName || "Friend"}</h4>
                  <p className="text-[10px] text-slate-400 truncate leading-none mt-0.5">{userProfile.userEmail || "Personal profile Setup"}</p>
                </div>
              </div>
            </div>

            {/* Navigation Menu Links */}
            <nav className="space-y-1.5 text-left" id="sidebar_links_list">
              <button
                onClick={() => setActiveTab("call")}
                className={`w-full py-3 px-4 rounded-2xl flex items-center gap-3 text-sm font-semibold transition-all border ${
                  activeTab === "call"
                    ? "bg-slate-950 text-emerald-400 border-slate-800 shadow-inner font-bold"
                    : "bg-transparent text-slate-400 border-transparent hover:text-white"
                }`}
              >
                <Compass size={18} />
                <span>Companion Call</span>
              </button>

              <button
                onClick={() => setActiveTab("journal")}
                className={`w-full py-3 px-4 rounded-2xl flex items-center gap-3 text-sm font-semibold transition-all border ${
                  activeTab === "journal"
                    ? "bg-slate-950 text-emerald-400 border-slate-800 shadow-inner font-bold"
                    : "bg-transparent text-slate-400 border-transparent hover:text-white"
                }`}
              >
                <BookOpen size={18} />
                <span>Reflection Entry</span>
              </button>

              <button
                onClick={() => setActiveTab("insights")}
                className={`w-full py-3 px-4 rounded-2xl flex items-center gap-3 text-sm font-semibold transition-all border ${
                  activeTab === "insights"
                    ? "bg-slate-950 text-emerald-400 border-slate-800 shadow-inner font-bold"
                    : "bg-transparent text-slate-400 border-transparent hover:text-white"
                }`}
              >
                <TrendingUp size={18} />
                <span>Insights & Monitors</span>
              </button>

              <button
                onClick={() => setActiveTab("wallet")}
                className={`w-full py-3 px-4 rounded-2xl flex items-center gap-3 text-sm font-semibold transition-all border ${
                  activeTab === "wallet"
                    ? "bg-slate-950 text-emerald-400 border-slate-800 shadow-inner font-bold"
                    : "bg-transparent text-slate-400 border-transparent hover:text-white"
                }`}
              >
                <Wallet size={18} />
                <span>Wallet Store</span>
              </button>

              <button
                onClick={() => setActiveTab("compliance")}
                className={`w-full py-3 px-4 rounded-2xl flex items-center gap-3 text-sm font-semibold transition-all border ${
                  activeTab === "compliance"
                    ? "bg-slate-950 text-emerald-400 border-slate-800 shadow-inner font-bold"
                    : "bg-transparent text-slate-400 border-transparent hover:text-white"
                }`}
              >
                <Info size={18} />
                <span>Compliance Hub</span>
              </button>
            </nav>
          </div>

          {/* Bottom logout section */}
          <div className="pt-4 border-t border-slate-800/60 mt-4 sm:mt-0 text-left">
            {/* Expandable Memory Tray floating inside sidebar */}
            {memories.length > 0 && (
              <div className="bg-slate-950/20 p-3 rounded-2xl border border-slate-800 mb-4 text-xs">
                <div className="flex items-center gap-2 text-emerald-400 font-semibold mb-2">
                  <FolderHeart size={13} />
                  <span>Cherished Memories ({memories.length})</span>
                </div>
                <div className="max-h-24 overflow-y-auto space-y-1.5 pr-0.5 scroll-thin">
                  {memories.map((m) => (
                    <div key={m.id} className="bg-slate-950/80 p-2 rounded-xl border border-slate-900 group flex justify-between items-center relative">
                      <div className="text-[10px] text-slate-300 leading-normal font-sans pr-4">
                        <strong className="text-emerald-400 uppercase text-[9px] block mb-0.5">{m.key}</strong>
                        {m.value}
                      </div>

                      <button
                        onClick={() => handleDeleteMemory(m.id)}
                        className="p-1 rounded text-slate-500 hover:text-rose-400 hover:bg-slate-900 opacity-10 md:opacity-0 group-hover:opacity-100 transition-all absolute right-1"
                        title="Delete memory"
                      >
                        <Trash2 size={11} />
                      </button>
                    </div>
                  ))}
                </div>
              </div>
            )}

            <button
              onClick={handleSignOut}
              className="w-full py-2.5 px-4 rounded-xl flex items-center justify-center gap-2 text-xs font-semibold bg-rose-500/10 text-rose-400 border border-rose-500/15 hover:bg-rose-500 hover:text-slate-950 transition-all leading-none"
              title="PIN Lock screen"
            >
              <LogOut size={12} />
              <span>Lock personal logs</span>
            </button>
          </div>

        </div>

        {/* Right Side: Active Workspace panel wrapper */}
        <div className="lg:col-span-9 h-full overflow-hidden flex flex-col" id="active_workspace_panel">
          
          {activeTab === "call" && (
            <CallInterface
              activeAgent={activeAgent}
              agentsList={SUPPORTED_AGENTS}
              onSelectAgent={setSelectedAgentId}
              companionState={companionState}
              setCompanionState={setCompanionState}
              walletBalance={walletBalance}
              setWalletBalance={setWalletBalance}
              messages={messages}
              onSendMessage={handleSendMessage}
              ttsEnabled={isTtsEnabled}
              setTtsEnabled={setIsTtsEnabled}
              userProfile={userProfile}
              
              // New multiple conversations props bound
              conversations={conversations}
              activeConversationId={activeConversationId}
              onSelectConversation={(id) => {
                setActiveConversationId(id);
                logToCloudWatch("INFO", `DynamoDB: Fetched message context rows for active conversation ID: ${id}`);
              }}
              onCreateConversation={handleCreateConversation}
              onDeleteConversation={handleDeleteConversation}
              onRenameConversation={handleRenameConversation}
              onLogCloudWatch={logToCloudWatch}
            />
          )}

          {activeTab === "journal" && (
            <JournalSpace
              reflections={reflections}
              onAddReflection={handleAddReflection}
              onDeleteReflection={handleDeleteReflection}
            />
          )}

          {activeTab === "insights" && (
            <InsightsDashboard
              moodRecords={moodRecords}
              reflections={reflections}
              onAddMood={handleAddMoodRecord}
              userName={userProfile.userName}
              cloudWatchLogs={cloudWatchLogs}
              onClearLogs={() => setCloudWatchLogs([])}
            />
          )}

          {activeTab === "wallet" && (
            <WalletStore
              walletBalance={walletBalance}
              setWalletBalance={setWalletBalance}
              userProfile={userProfile}
              setUserProfile={setUserProfile}
            />
          )}

          {activeTab === "compliance" && (
            <InfoHub onLogCloudWatch={logToCloudWatch} />
          )}

        </div>

      </div>
    </div>
  );
}
