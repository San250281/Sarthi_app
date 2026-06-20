import React, { useState, useRef, useEffect } from "react";
import { ReflectionSummary } from "../types";
import { BookOpen, Sparkles, Plus, Calendar, Clock, ChevronDown, ChevronUp, Trash2, Mic, MicOff, Smile } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

interface JournalSpaceProps {
  reflections: ReflectionSummary[];
  onAddReflection: (text: string, summary: Partial<ReflectionSummary>) => void;
  onDeleteReflection: (id: string) => void;
}

export default function JournalSpace({
  reflections,
  onAddReflection,
  onDeleteReflection,
}: JournalSpaceProps) {
  const [journalInput, setJournalInput] = useState("");
  const [isGenerating, setIsGenerating] = useState(false);
  const [expandedReflectionId, setExpandedReflectionId] = useState<string | null>(null);

  // Voice journaling triggers
  const [isListening, setIsListening] = useState(false);
  const recognitionRef = useRef<any>(null);

  // Setup Web Speech Recognition for voice journaling
  const toggleListening = () => {
    const SpeechRecognition = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognition) {
      alert("Speech recognition is not supported in this browser. Please type your thoughts.");
      return;
    }

    if (isListening) {
      stopListening();
    } else {
      startListening(SpeechRecognition);
    }
  };

  const startListening = (SpeechRecognition: any) => {
    const rec = new SpeechRecognition();
    rec.continuous = true;
    rec.interimResults = true;
    rec.lang = "en-IN";

    rec.onresult = (event: any) => {
      let final = "";
      for (let i = event.resultIndex; i < event.results.length; ++i) {
        if (event.results[i].isFinal) {
          final += event.results[i][0].transcript + " ";
        }
      }
      if (final) {
        setJournalInput((prev) => prev + final);
      }
    };

    rec.onerror = (e: any) => {
      console.error(e);
      stopListening();
    };

    rec.onend = () => {
      setIsListening(false);
    };

    recognitionRef.current = rec;
    setIsListening(true);
    rec.start();
  };

  const stopListening = () => {
    if (recognitionRef.current) {
      recognitionRef.current.stop();
      recognitionRef.current = null;
    }
    setIsListening(false);
  };

  useEffect(() => {
    return () => {
      if (recognitionRef.current) {
        recognitionRef.current.stop();
      }
    };
  }, []);

  // Submit Reflection to server API
  const handleGenerateReflection = async () => {
    if (!journalInput.trim()) return;

    setIsGenerating(true);
    try {
      const response = await fetch("/api/generate-reflection", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ text: journalInput.trim() }),
      });

      if (!response.ok) {
        throw new Error("Failed to process journal reflection with AI.");
      }

      const summaryData = await response.json();
      
      // Save reflection in local state & database
      onAddReflection(journalInput.trim(), {
        detectedFeelings: summaryData.detectedFeelings,
        keyTakeaways: summaryData.keyTakeaways,
        actionableSteps: summaryData.actionableSteps,
        mainMoodEmoji: summaryData.mainMoodEmoji || "🌱",
      });

      // Reset
      setJournalInput("");
    } catch (error: any) {
      console.error(error);
      alert(error?.message || "An issue occurred while summarizing the journal. Pls try again.");
    } finally {
      setIsGenerating(false);
    }
  };

  // Convert Epoch millis to gorgeous label
  const formatDate = (epoch: number) => {
    const date = new Date(epoch);
    return date.toLocaleDateString("en-US", {
      weekday: "short",
      month: "short",
      day: "numeric",
      year: "numeric",
    });
  };

  // Format epoch to HH:MM label
  const formatTime = (epoch: number) => {
    const date = new Date(epoch);
    return date.toLocaleTimeString("en-US", {
      hour: "2-digit",
      minute: "2-digit",
    });
  };

  return (
    <div className="flex flex-col h-full bg-slate-900 rounded-3xl border border-slate-800 p-6 md:p-8 overflow-y-auto scroller shadow-xl" id="journal_space_root">
      
      {/* Header Description */}
      <div className="mb-8">
        <h2 className="font-display text-2xl md:text-3xl font-bold tracking-tight text-white flex items-center gap-3">
          <BookOpen className="text-emerald-400" />
          <span>Journal & AI Reflection</span>
        </h2>
        <p className="text-sm font-sans text-slate-400 mt-2 tracking-wide leading-relaxed">
          Express your feelings, anxieties, or events of today. Saarthi will synthesize your thoughts, outline key takeaways, and suggest simple action loops.
        </p>
      </div>

      {/* Editor Block */}
      <div className="bg-slate-950/60 p-5 rounded-3xl border border-slate-800/80 mb-8 relative">
        <div className="flex justify-between items-center mb-3">
          <span className="text-xs uppercase font-sans tracking-widest text-slate-500 font-bold block text-left">
            Write your thoughts below
          </span>
          
          <button
            onClick={toggleListening}
            className={`py-1.5 px-3.5 rounded-full flex items-center gap-2 text-xs font-semibold transition-all border ${
              isListening
                ? "bg-rose-500/10 text-rose-400 border-rose-500/20 animate-pulse font-bold"
                : "bg-slate-900 text-slate-300 border-slate-800 hover:border-slate-700"
            }`}
          >
            {isListening ? <MicOff size={13} className="shrink-0" /> : <Mic size={13} className="shrink-0" />}
            <span>{isListening ? "Listening... click to STOP" : "Speak thoughts"}</span>
          </button>
        </div>

        <textarea
          rows={5}
          value={journalInput}
          onChange={(e) => setJournalInput(e.target.value)}
          placeholder="I'm feeling a bit restless about my milestones today. Work was stressful, but I managed to finish my priority goals..."
          className="w-full bg-slate-900/60 border border-slate-800 rounded-2xl p-4 placeholder-slate-500 text-white text-sm focus:outline-none focus:border-emerald-500 text-left resize-none"
        ></textarea>

        <div className="flex justify-between items-center mt-4">
          <span className="text-xs text-slate-500 font-mono">
            {journalInput.length} characters written
          </span>

          <button
            onClick={handleGenerateReflection}
            disabled={isGenerating || !journalInput.trim()}
            className={`py-3 px-6 rounded-xl font-semibold tracking-wide flex items-center gap-2.5 transition-all ${
              !journalInput.trim() || isGenerating
                ? "bg-slate-800 text-slate-500 border border-slate-700/50 cursor-not-allowed"
                : "bg-emerald-500 text-slate-950 font-bold hover:scale-[1.02] shadow-lg shadow-emerald-500/10"
            }`}
          >
            {isGenerating ? (
              <>
                <svg className="animate-spin h-4 w-4 text-slate-950 mr-1 inline-block" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.11 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                </svg>
                <span>Reflecting...</span>
              </>
            ) : (
              <>
                <Sparkles size={16} />
                <span>Publish reflection</span>
              </>
            )}
          </button>
        </div>
      </div>

      {/* Reflections Index Listing */}
      <div>
        <h3 className="font-display text-lg font-bold text-white mb-4 tracking-tight text-left">
          Reflection Logs ({reflections.length})
        </h3>

        {reflections.length === 0 ? (
          <div className="text-center py-12 bg-slate-950/20 rounded-2xl border border-dashed border-slate-800/80">
            <Smile className="mx-auto text-slate-500 mb-3" size={32} />
            <p className="text-slate-400 text-sm">No journal reflections recorded yet. Express your thoughts above!</p>
          </div>
        ) : (
          <div className="space-y-4">
            {reflections.map((ref) => {
              const isExpanded = expandedReflectionId === ref.id;
              return (
                <div
                  key={ref.id}
                  className="bg-slate-950/40 border border-slate-800 rounded-2xl overflow-hidden transition-all hover:border-slate-700/80"
                >
                  {/* Collapsed Header bar */}
                  <div
                    onClick={() => setExpandedReflectionId(isExpanded ? null : ref.id)}
                    className="p-5 flex items-center justify-between cursor-pointer select-none"
                  >
                    <div className="flex items-center gap-4">
                      {/* Mood Indicator circle */}
                      <div className="w-12 h-12 rounded-full bg-slate-900 border border-slate-800 flex items-center justify-center text-2xl shadow-inner">
                        <span>{ref.mainMoodEmoji}</span>
                      </div>
                      
                      <div className="text-left">
                        <span className="text-xs uppercase text-slate-500 font-mono tracking-wide block">
                          Journal reflection
                        </span>
                        <h4 className="font-sans font-semibold text-white text-sm mt-0.5 truncate max-w-sm md:max-w-md">
                          {ref.originalText}
                        </h4>
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <div className="text-right text-slate-500 text-xs hidden sm:block">
                        <div className="flex items-center gap-1">
                          <Calendar size={12} />
                          <span>{formatDate(ref.timestamp)}</span>
                        </div>
                        <div className="flex items-center justify-end gap-1 mt-0.5">
                          <Clock size={12} />
                          <span>{formatTime(ref.timestamp)}</span>
                        </div>
                      </div>

                      <button
                        onClick={(e) => {
                          e.stopPropagation();
                          if (confirm("Are you sure you want to delete this journal entry?")) {
                            onDeleteReflection(ref.id);
                          }
                        }}
                        className="p-2 rounded-lg text-slate-500 hover:text-rose-400 hover:bg-slate-900 transition-all"
                        title="Delete reflection"
                      >
                        <Trash2 size={15} />
                      </button>

                      <div className="p-1 rounded-full text-slate-400">
                        {isExpanded ? <ChevronUp size={18} /> : <ChevronDown size={18} />}
                      </div>
                    </div>
                  </div>

                  {/* Expanded AI Summary Analysis panel */}
                  <AnimatePresence>
                    {isExpanded && (
                      <motion.div
                        initial={{ height: 0, opacity: 0 }}
                        animate={{ height: "auto", opacity: 1 }}
                        exit={{ height: 0, opacity: 0 }}
                        className="bg-slate-950/80 border-t border-slate-900 px-5 pb-5 pt-4 space-y-5 text-left text-sm"
                      >
                        {/* Original Text Box */}
                        <div>
                          <span className="text-xs uppercase text-slate-500 font-bold block mb-1">Your original notes</span>
                          <p className="text-slate-300 font-sans italic bg-slate-900/40 p-3.5 rounded-xl border border-slate-900 text-xs leading-relaxed">
                            "{ref.originalText}"
                          </p>
                        </div>

                        <div className="grid grid-cols-1 md:grid-cols-2 gap-5">
                          {/* detected feeling */}
                          <div className="bg-slate-900/50 p-4 rounded-xl border border-slate-900">
                            <span className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1">Detected feelings</span>
                            <p className="text-emerald-300 mt-1">{ref.detectedFeelings}</p>
                          </div>

                          {/* Actionable focus steps */}
                          <div className="bg-slate-900/50 p-4 rounded-xl border border-slate-900">
                            <span className="text-xs uppercase text-teal-400 font-bold tracking-wider block mb-1">Mindful Action Loop</span>
                            <p className="text-slate-300 mt-1 whitespace-pre-line leading-relaxed font-sans">{ref.actionableSteps}</p>
                          </div>
                        </div>

                        {/* Takeaways details */}
                        <div className="bg-slate-900/50 p-4 rounded-xl border border-slate-900">
                          <span className="text-xs uppercase text-slate-400 font-bold tracking-wider block mb-1">Reflection Highlights</span>
                          <p className="text-slate-300 mt-1 whitespace-pre-line leading-relaxed font-sans">{ref.keyTakeaways}</p>
                        </div>
                      </motion.div>
                    )}
                  </AnimatePresence>
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}
