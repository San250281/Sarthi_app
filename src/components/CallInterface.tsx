import React, { useState, useEffect, useRef } from "react";
import { AIAgent, SaarthiState, ChatMessage, UserProfile, Conversation } from "../types";
import { Mic, MicOff, PhoneOff, Volume2, VolumeX, MessageSquare, Send, Sparkles, User, ShieldAlert, Folder, Plus, Trash2, Edit3, Check, X, RefreshCw, Layers } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

interface CallInterfaceProps {
  activeAgent: AIAgent;
  agentsList: AIAgent[];
  onSelectAgent: (id: string) => void;
  companionState: SaarthiState;
  setCompanionState: (state: SaarthiState) => void;
  walletBalance: number;
  setWalletBalance: React.Dispatch<React.SetStateAction<number>>;
  messages: ChatMessage[];
  onSendMessage: (text: string, isVoice?: boolean) => Promise<any>;
  ttsEnabled: boolean;
  setTtsEnabled: (enabled: boolean) => void;
  userProfile: UserProfile;
  
  // New multiple conversation manager states
  conversations: Conversation[];
  activeConversationId: string;
  onSelectConversation: (id: string) => void;
  onCreateConversation: (title: string) => void;
  onDeleteConversation: (id: string) => void;
  onRenameConversation: (id: string, newTitle: string) => void;
  onLogCloudWatch?: (level: "INFO" | "METRIC" | "WARN" | "ERROR", msg: string) => void;
}

export default function CallInterface({
  activeAgent,
  agentsList,
  onSelectAgent,
  companionState,
  setCompanionState,
  walletBalance,
  setWalletBalance,
  messages,
  onSendMessage,
  ttsEnabled,
  setTtsEnabled,
  userProfile,

  conversations,
  activeConversationId,
  onSelectConversation,
  onCreateConversation,
  onDeleteConversation,
  onRenameConversation,
  onLogCloudWatch,
}: CallInterfaceProps) {
  const [isInCall, setIsInCall] = useState(false);
  const [isKeyboardMode, setIsInKeyboardMode] = useState(false);
  const [typedMessage, setTypedMessage] = useState("");
  const [callDuration, setCallDuration] = useState(0);
  const [partialSpeech, setPartialSpeech] = useState("");
  const [isMuted, setIsMuted] = useState(false);

  // Performance metrics tracking structures
  const [metrics, setMetrics] = useState({
    speechTimeMs: 0,
    geminiTimeMs: 0,
    ttsTimeMs: 0,
    totalResponseMs: 0,
    avgResponseMs: 0,
    successfulTurns: 0,
    failedTurns: 0,
  });

  const speechStartTsRef = useRef<number>(0);

  // Conversation Sidebar toggles
  const [roomsPanelOpen, setRoomsPanelOpen] = useState(true);
  const [editingConvId, setEditingConvId] = useState<string | null>(null);
  const [editingTitleVal, setEditingTitleVal] = useState("");

  // Input voice assistant states
  const [isInputListening, setIsInputListening] = useState(false);
  const inputRecognitionRef = useRef<any>(null);

  // Audio Context waveform visualizer simulator
  const [volumeBarHeights, setVolumeBarBars] = useState<number[]>([15, 15, 15, 15, 15, 15, 15, 15]);

  // Web Speech API refs for CALL voice
  const recognitionRef = useRef<any>(null);
  const durationTimerRef = useRef<NodeJS.Timeout | null>(null);
  const debitTimerRef = useRef<NodeJS.Timeout | null>(null);
  const scrollRef = useRef<HTMLDivElement>(null);

  // Auto Scroll Chat
  useEffect(() => {
    if (scrollRef.current) {
      scrollRef.current.scrollTop = scrollRef.current.scrollHeight;
    }
  }, [messages, partialSpeech]);

  // Handle Dynamic Call Cost Deducting
  useEffect(() => {
    if (isInCall && companionState === "LISTENING") {
      const costPerSecond = activeAgent.pricePerMin / 10; // boosted speed for better tracking visualization
      
      debitTimerRef.current = setInterval(() => {
        setWalletBalance((prev) => {
          if (prev <= 0) return 0;
          return Math.max(0, Number((prev - costPerSecond).toFixed(2)));
        });
      }, 1000);
    } else {
      if (debitTimerRef.current) clearInterval(debitTimerRef.current);
    }

    return () => {
      if (debitTimerRef.current) clearInterval(debitTimerRef.current);
    };
  }, [isInCall, companionState, activeAgent]);

  // Hang up on zero wallet balance safely inside regular react effect flow
  useEffect(() => {
    if (isInCall && walletBalance <= 0) {
      handleHangUp();
      alert(`Call Disconnected: Wallet reached zero! Recharge in the Wallet tab to resume talking with ${activeAgent.name}.`);
    }
  }, [walletBalance, isInCall, activeAgent.name]);

  // Track Call Duration
  useEffect(() => {
    if (isInCall) {
      durationTimerRef.current = setInterval(() => {
        setCallDuration((prev) => prev + 1);
      }, 1000);
    } else {
      if (durationTimerRef.current) clearInterval(durationTimerRef.current);
      setCallDuration(0);
    }
    return () => {
      if (durationTimerRef.current) clearInterval(durationTimerRef.current);
    };
  }, [isInCall]);

  // Simulate Orb / Waveform motion
  useEffect(() => {
    let interval: NodeJS.Timeout;
    if (isInCall) {
      interval = setInterval(() => {
        setVolumeBarBars((prev) => {
          return prev.map(() => {
            if (companionState === "SPEAKING") {
              return Math.floor(Math.random() * 50) + 15;
            } else if (companionState === "LISTENING") {
              return Math.floor(Math.random() * 25) + 10;
            } else if (companionState === "THINKING") {
              return Math.floor(Math.random() * 15) + 10;
            }
            return 10;
          });
        });
      }, 100);
    }
    return () => clearInterval(interval);
  }, [isInCall, companionState]);

  // Native Speech Synthesis with custom metrics tracking and voice quality tuning
  const playTTSWithMetrics = (text: string, speechTime: number, geminiTime: number) => {
    if (!ttsEnabled || !window.speechSynthesis) {
      setCompanionState("LISTENING");
      if (isInCall && !isMuted) {
        startSpeechRecognition();
      }
      return;
    }
    
    // Ensure we are not listening while speaking to prevent feedback or echo loops
    stopSpeechRecognition();
    window.speechSynthesis.cancel();
    
    const cleanText = text.replace(/[*#_`~]/g, "").trim();
    const utterance = new SpeechSynthesisUtterance(cleanText);
    
    // Autodetect the highest-fidelity English neural or natural audio voice
    const voices = window.speechSynthesis.getVoices();
    const selectedVoice = voices.find(v => v.lang.startsWith("en") && v.name.toLowerCase().includes("natural")) ||
                        voices.find(v => v.lang.startsWith("en") && v.name.toLowerCase().includes("neural")) ||
                        voices.find(v => v.lang.startsWith("en") && v.name.toLowerCase().includes("google")) ||
                        voices.find(v => v.lang.startsWith("en")) ||
                        voices[0];

    if (selectedVoice) {
      utterance.voice = selectedVoice;
    }

    // Voice Engine Tuning (Targeting speechRate = 0.95, pitch = 1.05 with agent profiles)
    if (activeAgent.id === "shanti") {
      utterance.rate = 0.85; // slightly slower for tranquil advice
      utterance.pitch = 1.10; 
    } else if (activeAgent.id === "kabir") {
      utterance.rate = 0.90; // philosophical pacing
      utterance.pitch = 0.95; 
    } else {
      utterance.rate = 0.95; // default low-latency conversational rate
      utterance.pitch = 1.05; // warm natural tone
    }

    const ttsStart = Date.now();

    utterance.onstart = () => {
      setCompanionState("SPEAKING");
      const ttsTime = Date.now() - ttsStart;
      const totalTime = speechTime + geminiTime + ttsTime;
      
      setMetrics((prev) => {
        const nextTurns = prev.successfulTurns + 1;
        return {
          ...prev,
          speechTimeMs: speechTime,
          geminiTimeMs: geminiTime,
          ttsTimeMs: ttsTime,
          totalResponseMs: totalTime,
          successfulTurns: nextTurns,
          avgResponseMs: Math.round(((prev.avgResponseMs * prev.successfulTurns) + totalTime) / nextTurns),
        };
      });

      if (onLogCloudWatch) {
        onLogCloudWatch("METRIC", `speech_time_ms:${speechTime} | gemini_time_ms:${geminiTime} | tts_time_ms:${ttsTime} | total_response_ms:${totalTime}`);
      }
    };

    utterance.onend = () => {
      setCompanionState("LISTENING");
      if (isInCall && !isMuted) {
        startSpeechRecognition();
      }
    };

    utterance.onerror = (e) => {
      console.warn("Neural TTS flow error:", e);
      setCompanionState("LISTENING");
      if (isInCall && !isMuted) {
        startSpeechRecognition();
      }
    };

    window.speechSynthesis.speak(utterance);
  };

  const playTTS = (text: string) => {
    playTTSWithMetrics(text, 0, 1000);
  };

  // Start Call loop
  const handleStartCall = async () => {
    if (walletBalance < activeAgent.pricePerMin) {
      alert(`Insufficient funds! Talking with ${activeAgent.name} costs ₹${activeAgent.pricePerMin}/min. Balance: ₹${walletBalance}. Replenish in the Wallet Store tab.`);
      return;
    }

    setIsInCall(true);
    setCompanionState("THINKING");
    stopSpeechRecognition();

    // Instead of starting recognition immediately, the AI speaks the greeting first
    setTimeout(() => {
      playTTS(activeAgent.greeting);
    }, 400);
  };

  const handleHangUp = () => {
    setIsInCall(false);
    setCompanionState("IDLE");
    setPartialSpeech("");
    if (window.speechSynthesis) {
      window.speechSynthesis.cancel();
    }
    stopSpeechRecognition();
  };

  // Setup Call speech recognition loops with VAD & Error mappings
  const startSpeechRecognition = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      console.warn("SpeechRecognition API is not supported in this browser.");
      return;
    }

    setCompanionState("LISTENING");
    stopSpeechRecognition();

    const rec = new SpeechRecognition();
    rec.continuous = true;
    rec.interimResults = true;
    rec.lang = "en-IN"; // Target Multilingual English-Indian conversational model

    // Voice Activity Detection (VAD) soundstart tracking
    rec.onsoundstart = () => {
      speechStartTsRef.current = Date.now();
    };

    rec.onresult = (event: any) => {
      let interim = "";
      let final = "";

      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) {
          final += event.results[i][0].transcript;
        } else {
          interim += event.results[i][0].transcript;
        }
      }

      if (interim) {
        setPartialSpeech(interim);
      }

      if (final && final.trim()) {
        const textToSubmit = final.trim();
        setPartialSpeech("");
        
        // Calculate dynamic Speech Recognition duration
        const speechTime = speechStartTsRef.current > 0 ? (Date.now() - speechStartTsRef.current) : 950;
        
        // Immediately STOP recognizing while we send and process to prevent echo
        stopSpeechRecognition();

        if (window.speechSynthesis) {
          window.speechSynthesis.cancel();
        }

        setCompanionState("THINKING");
        
        const geminiStart = Date.now();
        onSendMessage(textToSubmit, true)
          .then((botReply) => {
            const geminiTime = Date.now() - geminiStart;
            
            if (botReply) {
              playTTSWithMetrics(botReply, speechTime, geminiTime);
            } else {
              setCompanionState("LISTENING");
              startSpeechRecognition();
            }
          })
          .catch((err) => {
            setMetrics(m => ({ ...m, failedTurns: m.failedTurns + 1 }));
            if (onLogCloudWatch) {
              onLogCloudWatch("ERROR", `AWS Lambda: Downstream chat processing failure: ${err.message}`);
            }
            setCompanionState("LISTENING");
            startSpeechRecognition();
          });
      }
    };

    rec.onerror = (e: any) => {
      console.warn("Speech Recognition Error code:", e.error);
      
      const mappedErrorMap: Record<string, string> = {
        "no-speech": "ERROR_SPEECH_TIMEOUT",
        "audio-capture": "ERROR_CLIENT",
        "not-allowed": "ERROR_NOT_ALLOWED",
        "network": "ERROR_NETWORK",
        "aborted": "ERROR_CLIENT"
      };
      
      const friendlyError = mappedErrorMap[e.error] || "ERROR_CLIENT";
      if (onLogCloudWatch) {
        onLogCloudWatch("WARN", `SpeechRecognition: Encountered ${friendlyError} (${e.error}). Recovering speech path.`);
      }

      setMetrics((prev) => ({ ...prev, failedTurns: prev.failedTurns + 1 }));

      if (["no-speech", "network", "aborted"].includes(e.error) && isInCall && !isMuted) {
        setTimeout(() => {
          if (isInCall && !isMuted && companionState === "LISTENING") {
            try {
              rec.start();
            } catch (_) {}
          }
        }, 400);
      }
    };

    rec.onend = () => {
      if (isInCall && !isMuted && companionState === "LISTENING") {
        try {
          rec.start();
        } catch (_) {}
      }
    };

    recognitionRef.current = rec;
    rec.start();
  };

  const stopSpeechRecognition = () => {
    if (recognitionRef.current) {
      recognitionRef.current.onend = null;
      recognitionRef.current.onerror = null;
      try {
        recognitionRef.current.stop();
      } catch (_) {}
      recognitionRef.current = null;
    }
  };

  const toggleMute = () => {
    if (isMuted) {
      setIsMuted(false);
      startSpeechRecognition();
    } else {
      setIsMuted(true);
      stopSpeechRecognition();
    }
  };

  // Keyboard Message Submission
  const handleKeyboardSend = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!typedMessage.trim()) return;

    if (walletBalance < activeAgent.pricePerMin) {
      alert(`Insufficient balance! Talking with ${activeAgent.name} costs ₹${activeAgent.pricePerMin}/min. Wallet balance is ₹${walletBalance}. Please recharge.`);
      return;
    }

    const query = typedMessage.trim();
    setTypedMessage("");
    setCompanionState("THINKING");

    await onSendMessage(query, false);
    
    // Play synthesis of the freshly added response
    const messagesStore = JSON.parse(localStorage.getItem("saarthi_messages") || "[]");
    const latestReply = messagesStore
      .filter((m: any) => m.role === "saarthi" && m.conversationId === activeConversationId)
      .pop()?.text;
    
    if (latestReply) {
      playTTS(latestReply);
    } else {
      setCompanionState("IDLE");
    }
  };

  // Input Box Voice-To-Text / Autocomplete trigger
  const toggleInputVoiceRecognition = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert("Custom speech input assistant is not fully supported in your current browser. Try typing manual reflections.");
      return;
    }

    if (isInputListening) {
      // Turn off
      if (inputRecognitionRef.current) {
        inputRecognitionRef.current.stop();
      }
      setIsInputListening(false);
    } else {
      // Turn on
      const rec = new SpeechRecognition();
      rec.continuous = false; // Stop after user stops speaking
      rec.interimResults = true;
      rec.lang = "en-IN"; // English-Hinglish-Hindi support

      rec.onresult = (event: any) => {
        let text = "";
        for (let i = 0; i < event.results.length; ++i) {
          text += event.results[i][0].transcript;
        }
        setTypedMessage(text);
      };

      rec.onerror = (e: any) => {
        console.error("Speech Input assistant error:", e);
        setIsInputListening(false);
      };

      rec.onend = () => {
        setIsInputListening(false);
      };

      inputRecognitionRef.current = rec;
      setIsInputListening(true);
      rec.start();
    }
  };

  const formatTime = (secs: number) => {
    const mins = Math.floor(secs / 60);
    const remainingSecs = secs % 60;
    return `${mins.toString().padStart(2, "0")}:${remainingSecs.toString().padStart(2, "0")}`;
  };

  const getStateText = () => {
    switch (companionState) {
      case "LISTENING":
        return "Listening to your thoughts...";
      case "THINKING":
        return `${activeAgent.name} is reflecting...`;
      case "SPEAKING":
        return `${activeAgent.name} is speaking...`;
      default:
        return "Connected Companion";
    }
  };

  // Filter messages for current active conversation only
  const activeConversationMessages = messages.filter(
    (m) => m.conversationId === activeConversationId
  );

  const startEditingRoom = (c: Conversation) => {
    setEditingConvId(c.id);
    setEditingTitleVal(c.title);
  };

  const saveEditingRoom = (id: string) => {
    onRenameConversation(id, editingTitleVal);
    setEditingConvId(null);
  };

  return (
    <div className="flex flex-col lg:flex-row h-full bg-slate-900 text-slate-100 rounded-3xl border border-slate-800 overflow-hidden shadow-2xl relative" id="call_interface_root">
      
      {/* Background Ambient Glows */}
      <div className="absolute inset-0 pointer-events-none overflow-hidden opacity-30">
        <div className="absolute top-1/4 left-1/3 w-80 h-80 rounded-full bg-emerald-500/20 blur-3xl"></div>
        <div className="absolute bottom-1/3 right-1/4 w-96 h-96 rounded-full bg-teal-500/10 blur-3xl"></div>
      </div>

      {/* LEFT ACCORDION PANEL: Saved DynamoDB Conversation Rooms List */}
      <AnimatePresence>
        {roomsPanelOpen && (
          <motion.div
            initial={{ width: 0, opacity: 0 }}
            animate={{ width: "280px", opacity: 1 }}
            exit={{ width: 0, opacity: 0 }}
            className="border-r border-slate-800 bg-slate-950/80 backdrop-blur-md flex flex-col justify-between shrink-0 h-full z-20 overflow-hidden text-left"
            id="dynamodb_rooms_panel"
          >
            <div className="p-4 flex flex-col h-full">
              
              {/* Rooms Header */}
              <div className="flex justify-between items-center pb-3 border-b border-slate-850 mb-4">
                <div className="flex items-center gap-2">
                  <Layers size={15} className="text-emerald-400" />
                  <span className="text-xs uppercase font-extrabold text-white tracking-widest font-mono">DynamoDB Rooms</span>
                </div>
                <button
                  onClick={() => onCreateConversation(`Reflection Room #${conversations.length + 1}`)}
                  className="p-1 rounded bg-emerald-500 hover:bg-emerald-600 text-slate-950 transition-all font-bold"
                  title="Create New room"
                >
                  <Plus size={14} />
                </button>
              </div>

              {/* Rooms Lists */}
              <div className="flex-1 overflow-y-auto space-y-1.5 scroller pr-1">
                {conversations.map((c) => {
                  const isActive = c.id === activeConversationId;
                  const isEditing = c.id === editingConvId;

                  return (
                    <div
                      key={c.id}
                      onClick={() => !isEditing && onSelectConversation(c.id)}
                      className={`p-3 rounded-xl border transition-all cursor-pointer relative group flex flex-col ${
                        isActive
                          ? "bg-emerald-500/[0.04] border-emerald-500/40"
                          : "border-slate-850 bg-slate-900/15 hover:border-slate-800"
                      }`}
                    >
                      {isEditing ? (
                        <div className="flex items-center gap-1.5 w-full">
                          <input
                            type="text"
                            value={editingTitleVal}
                            onClick={(e) => e.stopPropagation()}
                            onChange={(e) => setEditingTitleVal(e.target.value)}
                            className="bg-slate-950 border border-slate-800 text-xs px-2 py-1 rounded text-white focus:outline-none focus:border-emerald-500 w-full"
                          />
                          <button
                            onClick={(e) => { e.stopPropagation(); saveEditingRoom(c.id); }}
                            className="p-1 rounded text-emerald-400 hover:bg-slate-900"
                          >
                            <Check size={12} />
                          </button>
                          <button
                            onClick={(e) => { e.stopPropagation(); setEditingConvId(null); }}
                            className="p-1 rounded text-slate-500 hover:bg-slate-900"
                          >
                            <X size={12} />
                          </button>
                        </div>
                      ) : (
                        <div className="flex justify-between items-center w-full">
                          <span className="text-xs font-bold text-slate-100 truncate pr-5 font-sans">
                            {c.title}
                          </span>
                          
                          {/* Inner tools */}
                          <div className="opacity-0 group-hover:opacity-100 flex items-center gap-1 transition-all shrink-0">
                            <button
                              onClick={(e) => { e.stopPropagation(); startEditingRoom(c); }}
                              className="p-1 rounded text-slate-400 hover:text-white"
                              title="Rename Room"
                            >
                              <Edit3 size={11} />
                            </button>
                            <button
                              onClick={(e) => { e.stopPropagation(); onDeleteConversation(c.id); }}
                              className="p-1 rounded text-slate-400 hover:text-rose-400"
                              title="Delete Room"
                            >
                              <Trash2 size={11} />
                            </button>
                          </div>
                        </div>
                      )}
                      
                      <div className="flex justify-between items-center mt-1 text-[9px] font-mono text-slate-500 leading-none">
                        <span>{new Date(c.createdDate).toLocaleDateString(undefined, {month: "short", day: "numeric"})}</span>
                        {isActive && <span className="text-emerald-400 font-extrabold uppercase font-mono tracking-widest text-[8px]">Open</span>}
                      </div>
                    </div>
                  );
                })}
              </div>

              {/* Status footer */}
              <div className="pt-2 border-t border-slate-850 mt-2 text-[9px] font-mono text-slate-600">
                <span>Total Active Collections: {conversations.length}</span>
              </div>
            </div>
          </motion.div>
        )}
      </AnimatePresence>

      {/* RIGHT PANEL: Voice Calling Screen & Transcription logs */}
      <div className="flex-1 flex flex-col h-full z-10 overflow-hidden relative">
        
        {/* Top Banner: Dynamic Cost indicators */}
        <div className="flex justify-between items-center px-6 py-4 bg-slate-950/80 border-b border-slate-800/60 z-10">
          <div className="flex items-center gap-3">
            <button
              onClick={() => setRoomsPanelOpen(!roomsPanelOpen)}
              className="p-2 bg-slate-900 hover:bg-slate-850 border border-slate-800 rounded-xl text-slate-400 hover:text-emerald-400 transition-all text-xs flex items-center gap-1.5 font-bold uppercase font-mono"
              title="Toggle Room List Drawer"
            >
              <Folder size={14} className="text-emerald-500" />
              <span>Rooms</span>
            </button>
            
            <div className="flex items-center gap-2">
              <span className="text-lg">{activeAgent.avatarEmoji}</span>
              <div className="text-left">
                <h3 className="font-display font-semibold tracking-tight text-white leading-none">{activeAgent.name}</h3>
                <span className="text-[10px] text-slate-400 font-sans tracking-wide">₹{activeAgent.pricePerMin}/min debited dynamically</span>
              </div>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <div className="bg-slate-900 border border-slate-800 rounded-full py-1 px-3 flex items-center gap-1.5">
              <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-pulse"></span>
              <span className="font-mono text-xs font-bold text-emerald-400">
                ₹{walletBalance.toFixed(2)}
              </span>
            </div>

            <button
              onClick={() => setTtsEnabled(!ttsEnabled)}
              className={`p-2 rounded-xl transition-all border ${
                ttsEnabled 
                  ? "bg-emerald-500/10 text-emerald-400 border-emerald-500/20" 
                  : "bg-slate-900 text-slate-400 border-slate-800"
              }`}
            >
              {ttsEnabled ? <Volume2 size={15} /> : <VolumeX size={15} />}
            </button>
          </div>
        </div>

        {/* Selected Companion Tabs Selector */}
        {!isInCall && (
          <div className="p-4 bg-slate-950/40 border-b border-slate-800/30 flex gap-2 overflow-x-auto scroller-hidden z-10" id="agent_tabs_nav">
            {agentsList.map((agent) => {
              const isSelected = agent.id === activeAgent.id;
              return (
                <button
                  key={agent.id}
                  onClick={() => {
                    onSelectAgent(agent.id);
                    if (window.speechSynthesis) window.speechSynthesis.cancel();
                  }}
                  className={`py-2 px-3 rounded-xl flex items-center gap-2 text-xs font-semibold whitespace-nowrap transition-all border shrink-0 ${
                    isSelected
                      ? "bg-emerald-500 text-slate-950 border-emerald-450 font-bold shadow-lg shadow-emerald-500/10"
                      : "bg-slate-950 text-slate-300 border-slate-800 hover:border-slate-700"
                  }`}
                >
                  <span>{agent.avatarEmoji}</span>
                  <span>{agent.name}</span>
                  <span className={`px-1 py-0.5 rounded text-[9px] ${
                    isSelected ? "bg-slate-950/20 text-slate-950" : "bg-slate-900 text-slate-400"
                  }`}>₹{agent.pricePerMin}/m</span>
                </button>
              );
            })}
          </div>
        )}

        {/* Primary Display Portal */}
        <div className="flex-1 flex flex-col justify-center items-center p-6 relative overflow-hidden z-10">
          {!isInCall ? (
            <motion.div
              initial={{ opacity: 0, y: 15 }}
              animate={{ opacity: 1, y: 0 }}
              className="text-center max-w-md bg-slate-950/60 p-6 rounded-2xl border border-slate-800/80 shadow-xl"
              id="pre_call_lobby"
            >
              <div className="w-16 h-16 rounded-full bg-slate-900 flex items-center justify-center text-3xl mb-3 mx-auto border border-emerald-500/20 shadow-lg relative">
                <span className="absolute -bottom-1 -right-1 text-xs bg-emerald-500 rounded-full p-0.5 leading-none">🌱</span>
                {activeAgent.avatarEmoji}
              </div>

              <h2 className="font-display text-xl font-bold text-white tracking-tight leading-normal">
                Meet {activeAgent.name}
              </h2>
              <p className="text-xs font-sans text-slate-300 mt-1.5 tracking-wide leading-relaxed">
                {activeAgent.description}
              </p>

              <div className="my-5 grid grid-cols-2 gap-3 text-left">
                <div className="bg-slate-900/80 p-2.5 rounded-xl border border-slate-800/60">
                  <span className="text-[8px] text-slate-500 block uppercase tracking-widest leading-none">Database Backup</span>
                  <span className="text-[11px] font-bold text-white mt-1 block">AWS DynamoDB Sync</span>
                </div>
                <div className="bg-slate-900/80 p-2.5 rounded-xl border border-slate-800/60">
                  <span className="text-[8px] text-slate-500 block uppercase tracking-widest leading-none">Cost Schedule</span>
                  <span className="text-[11px] font-mono font-bold text-emerald-400 mt-1 block">₹{activeAgent.pricePerMin}/min</span>
                </div>
              </div>

              {walletBalance < activeAgent.pricePerMin && (
                <div className="mb-4 flex items-center gap-2 bg-rose-500/10 border border-rose-500/20 text-rose-300 p-3 rounded-xl text-xs text-left">
                  <ShieldAlert size={18} className="shrink-0 text-rose-400" />
                  <p className="leading-relaxed">Your wallet balance is below ₹{activeAgent.pricePerMin}/min. top up in the Wallet tab to proceed.</p>
                </div>
              )}

              <button
                onClick={handleStartCall}
                disabled={walletBalance < activeAgent.pricePerMin}
                className={`w-full py-3 rounded-xl font-semibold tracking-wide flex items-center justify-center gap-2 text-xs uppercase tracking-wide transition-all ${
                  walletBalance >= activeAgent.pricePerMin
                    ? "bg-gradient-to-r from-emerald-500 to-teal-500 text-slate-950 font-bold hover:scale-[1.01] shadow-xl shadow-emerald-500/10"
                    : "bg-slate-800 text-slate-500 cursor-not-allowed border border-slate-700/50"
                }`}
              >
                <Mic size={14} />
                <span>Begin reflection session</span>
              </button>
            </motion.div>
          ) : (
            <div className="w-full h-full flex flex-col items-center justify-between" id="active_voice_portal">
              <div className="flex-1 flex flex-col justify-center items-center w-full relative">
                <span className="text-[10px] text-slate-500 font-sans tracking-widest uppercase mb-1.5 font-mono">
                  Call Elapsed: {formatTime(callDuration)}
                </span>

                <div className="relative w-40 h-36 flex items-center justify-center mb-4">
                  <AnimatePresence>
                    {companionState === "LISTENING" && (
                      <motion.div
                        initial={{ scale: 0.8, opacity: 0 }}
                        animate={{ scale: 1.25, opacity: 0.12 }}
                        exit={{ scale: 0.8, opacity: 0 }}
                        transition={{ duration: 1.5, repeat: Infinity, ease: "easeInOut" }}
                        className="absolute w-36 h-36 bg-teal-500 rounded-full"
                      />
                    )}
                    {companionState === "SPEAKING" && (
                      <motion.div
                        animate={{ scale: [1, 1.2, 1], opacity: [0.12, 0.35, 0.12] }}
                        transition={{ duration: 1, repeat: Infinity, ease: "easeInOut" }}
                        className="absolute w-40 h-40 bg-emerald-500 rounded-full"
                      />
                    )}
                    {companionState === "THINKING" && (
                      <motion.div
                        animate={{ rotate: 360 }}
                        transition={{ duration: 4, repeat: Infinity, ease: "linear" }}
                        className="absolute w-32 h-32 border border-dashed border-emerald-400/40 rounded-full"
                      />
                    )}
                  </AnimatePresence>

                  <div className="w-24 h-24 bg-slate-950 rounded-full border border-emerald-500 flex items-center justify-center text-4xl shadow-2xl z-10 relative">
                    <span>{activeAgent.avatarEmoji}</span>
                    {companionState === "THINKING" && (
                      <div className="absolute inset-0 rounded-full border-t border-emerald-400 animate-spin"></div>
                    )}
                  </div>
                </div>

                <h2 className="font-display text-lg font-bold text-white tracking-tight leading-normal h-8">
                  {getStateText()}
                </h2>

                <div className="flex gap-1 h-8 items-center justify-center mt-2">
                  {volumeBarHeights.map((ht, idx) => (
                    <motion.div
                      key={idx}
                      animate={{ height: ht }}
                      transition={{ ease: "easeInOut" }}
                      className="w-1 rounded-full bg-gradient-to-t from-emerald-500 to-teal-400 opacity-80"
                    />
                  ))}
                </div>
              </div>

              {/* Latency Benchmark Report HUD */}
              <div className="w-full max-w-xl grid grid-cols-4 gap-2 mb-3 bg-slate-950/80 border border-slate-800/80 rounded-xl p-3 backdrop-blur-sm shadow-xl" id="latency_benchmark_hud">
                <div className="text-center">
                  <span className="text-[8px] text-slate-500 uppercase block tracking-wider mb-0.5 font-mono">Speech Recognition</span>
                  <span className="text-xs font-mono font-bold text-teal-400">{metrics.speechTimeMs ? `${metrics.speechTimeMs} ms` : "—"}</span>
                </div>
                <div className="text-center border-l border-slate-800">
                  <span className="text-[8px] text-slate-500 uppercase block tracking-wider mb-0.5 font-mono">Gemini Lambda</span>
                  <span className="text-xs font-mono font-bold text-emerald-400">{metrics.geminiTimeMs ? `${metrics.geminiTimeMs} ms` : "—"}</span>
                </div>
                <div className="text-center border-l border-slate-800">
                  <span className="text-[8px] text-slate-500 uppercase block tracking-wider mb-0.5 font-mono">TTS Generation</span>
                  <span className="text-xs font-mono font-bold text-amber-400">{metrics.ttsTimeMs ? `${metrics.ttsTimeMs} ms` : "—"}</span>
                </div>
                <div className="text-center border-l border-slate-800">
                  <span className="text-[8px] text-slate-500 uppercase block tracking-wider mb-0.5 font-mono">Total Latency</span>
                  <span className="text-xs font-mono font-bold text-rose-400">{metrics.totalResponseMs ? `${metrics.totalResponseMs} ms` : "—"}</span>
                </div>
              </div>

              <div className="w-full max-w-xl flex justify-between items-center px-4 py-1.5 mb-3 bg-slate-900/60 border border-slate-850 rounded-lg text-[9px] font-mono text-slate-400">
                <span className="flex items-center gap-1">
                  <span className="w-1.5 h-1.5 bg-emerald-400 rounded-full animate-pulse"></span>
                  Avg conversation response time: <strong className="text-white">{metrics.avgResponseMs ? `${metrics.avgResponseMs} ms` : "Calculating..."}</strong>
                </span>
                <span>Active loop turns: <strong className="text-emerald-400">{metrics.successfulTurns}</strong> | Failures: <strong className="text-rose-400">{metrics.failedTurns}</strong></span>
              </div>

              {/* Transcription Logs overlay Console */}
              <div className="w-full max-w-xl h-28 bg-slate-950/60 border border-slate-800/60 rounded-xl p-4 mb-4 flex flex-col backdrop-blur-sm pr-2">
                <div className="text-[9px] text-slate-500 font-sans tracking-wide uppercase mb-1 flex justify-between">
                  <span>Conversation Transcript</span>
                  <span>{activeConversationMessages.length} elements loaded</span>
                </div>
                
                <div className="flex-1 overflow-y-auto text-[11px] space-y-1.5 pr-1 text-left" ref={scrollRef}>
                  {activeConversationMessages.map((msg) => (
                    <div key={msg.id} className="flex gap-1.5 leading-normal">
                      <span className="font-mono text-[9px] uppercase font-bold shrink-0 text-slate-500 mt-0.5">
                        {msg.role === "saarthi" ? activeAgent.name : "You"}:
                      </span>
                      <span className={msg.role === "saarthi" ? "text-slate-200 font-sans" : "text-emerald-300 font-sans"}>
                        {msg.text}
                      </span>
                    </div>
                  ))}

                  {partialSpeech && (
                    <div className="flex gap-1.5 text-slate-400 italic">
                      <span className="font-mono text-[9px] uppercase font-bold shrink-0 mt-0.5">Spoken...</span>
                      <span>{partialSpeech}</span>
                    </div>
                  )}

                  {activeConversationMessages.length === 0 && !partialSpeech && (
                    <p className="text-slate-500 italic text-center mt-2">Say something or click the typewriter to exchange queries...</p>
                  )}
                </div>
              </div>

              {/* Active voice control bar */}
              <div className="flex items-center gap-3 bg-slate-950/85 p-2.5 rounded-2xl border border-slate-800/80 z-20">
                <button
                  onClick={toggleMute}
                  className={`p-3 rounded-xl transition-all ${
                    isMuted 
                      ? "bg-rose-500/10 text-rose-400 border border-rose-500/20" 
                      : "bg-slate-900 text-slate-300 border border-slate-800 hover:bg-slate-800"
                  }`}
                >
                  {isMuted ? <MicOff size={16} /> : <Mic size={16} />}
                </button>

                <button
                  onClick={handleHangUp}
                  className="bg-rose-600 hover:bg-rose-700 text-white font-bold p-3 rounded-xl shadow-xl shadow-rose-600/15"
                >
                  <PhoneOff size={18} />
                </button>

                <button
                  onClick={() => setIsInKeyboardMode(!isKeyboardMode)}
                  className={`p-3 rounded-xl transition-all ${
                    isKeyboardMode 
                      ? "bg-emerald-500/10 text-emerald-400 border border-emerald-500/20" 
                      : "bg-slate-900 text-slate-300 border border-slate-800 hover:bg-slate-800"
                  }`}
                >
                  <MessageSquare size={16} />
                </button>
              </div>
            </div>
          )}
        </div>

        {/* Keyboard Input modal drawer */}
        <AnimatePresence>
          {isKeyboardMode && (
            <motion.div
              initial={{ opacity: 0, y: 100 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: 100 }}
              className="absolute bottom-0 inset-x-0 p-5 bg-slate-950/95 border-t border-slate-800 rounded-t-2xl z-30 text-left"
            >
              <div className="flex justify-between items-center mb-3">
                <div className="flex items-center gap-2">
                  <Sparkles size={14} className="text-emerald-400" />
                  <h4 className="font-display font-semibold text-white text-xs">Chat room input: {conversations.find(c => c.id === activeConversationId)?.title || "Active"}</h4>
                </div>
                <button
                  onClick={() => setIsInKeyboardMode(false)}
                  className="text-[10px] text-slate-400 hover:text-white bg-slate-900 px-2.5 py-1 rounded-full text-center"
                >
                  Close Input
                </button>
              </div>

              <form onSubmit={handleKeyboardSend} className="flex gap-2">
                <div className="relative flex-1">
                  <input
                    type="text"
                    value={typedMessage}
                    onChange={(e) => setTypedMessage(e.target.value)}
                    placeholder={
                      isInputListening 
                        ? "(Listening... Speak clearly)" 
                        : `Type your safe emotional reflections to ${activeAgent.name}...`
                    }
                    className="w-full bg-slate-900 border border-slate-800 rounded-xl pl-4 pr-12 py-3 placeholder-slate-500 text-white text-xs focus:outline-none focus:border-emerald-500"
                  />
                  
                  {/* Dynamic Voice Autocomplete speech-to-text input button! */}
                  <button
                    type="button"
                    onClick={toggleInputVoiceRecognition}
                    className={`absolute right-3 top-2.5 p-1.5 rounded-lg transition-all ${
                      isInputListening
                        ? "bg-rose-500 text-white animate-pulse"
                        : "text-slate-400 hover:text-emerald-400 hover:bg-slate-850"
                    }`}
                    title={isInputListening ? "Stop listening speech" : "Type with Voice (Speech Recognition)"}
                  >
                    <Mic size={14} />
                  </button>
                </div>

                <button
                  type="submit"
                  disabled={!typedMessage.trim()}
                  className="bg-emerald-500 hover:bg-emerald-600 disabled:opacity-40 disabled:cursor-not-allowed text-slate-950 font-bold px-4 py-3 rounded-xl flex items-center justify-center shrink-0"
                >
                  <Send size={15} />
                </button>
              </form>
              
              <div className="mt-2 text-[9px] text-slate-500 font-sans flex items-center gap-1">
                <span>Supports multi-lingual speech-recognition autocomplete. Tap microphone icon to speak.</span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>

    </div>
  );
}
