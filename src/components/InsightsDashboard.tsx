import React, { useState } from "react";
import { MoodRecord, ReflectionSummary, CloudWatchLog } from "../types";
import { Smile, Sparkles, TrendingUp, Calendar, BookOpen, AlertCircle, RefreshCw, CheckCircle, Terminal, Database, Server, Cpu, Trash2 } from "lucide-react";
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer } from "recharts";

interface InsightsDashboardProps {
  moodRecords: MoodRecord[];
  reflections: ReflectionSummary[];
  onAddMood: (mood: string, confidence: number, clarity: number, notes: string) => void;
  userName: string;
  cloudWatchLogs: CloudWatchLog[];
  onClearLogs: () => void;
}

export default function InsightsDashboard({
  moodRecords,
  reflections,
  onAddMood,
  userName,
  cloudWatchLogs,
  onClearLogs,
}: InsightsDashboardProps) {
  // Mood check-in local states
  const [selectedMood, setSelectedMood] = useState("Normal");
  const [selectedConfidence, setSelectedConfidence] = useState(6);
  const [selectedClarity, setSelectedClarity] = useState(6);
  const [moodNotes, setMoodNotes] = useState("");
  const [moodAddedMsg, setMoodAddedMsg] = useState(false);

  // Growth report synthesizer local states
  const [reportMarkdown, setReportMarkdown] = useState("");
  const [isGeneratingReport, setIsGeneratingReport] = useState(false);
  
  // Log filter
  const [logFilter, setLogFilter] = useState<"ALL" | "INFO" | "METRIC" | "ERROR_WARN">("ALL");

  const moodEmojis: { [key: string]: string } = {
    Excited: "🤩",
    Normal: "🙂",
    Confused: "🤔",
    Stressed: "😰",
    Unmotivated: "🥱",
    Sad: "😔",
  };

  const colors: { [key: string]: string } = {
    Excited: "bg-amber-500",
    Normal: "bg-emerald-500",
    Confused: "bg-purple-500",
    Stressed: "bg-rose-500",
    Unmotivated: "bg-amber-700",
    Sad: "bg-indigo-600",
  };

  // Submit Mood check-in
  const handleMoodSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    onAddMood(selectedMood, selectedConfidence, selectedClarity, moodNotes.trim());
    setMoodNotes("");
    setMoodAddedMsg(true);
    setTimeout(() => setMoodAddedMsg(false), 3000);
  };

  // Build gorgeous chart data by sorting mood outputs chronologically
  const sortedMoodRecords = [...moodRecords].sort((a, b) => a.timestamp - b.timestamp);
  
  const chartData = sortedMoodRecords.map((m) => {
    const d = new Date(m.timestamp);
    const dateLabel = d.toLocaleDateString("en-US", { month: "short", day: "numeric" });
    return {
      date: dateLabel,
      Confidence: m.confidence,
      Clarity: m.clarity,
      Emoji: moodEmojis[m.mood] || "🙂",
    };
  });

  // Call API to generate report
  const handleGenerateReport = async () => {
    setIsGeneratingReport(true);
    try {
      const res = await fetch("/api/generate-report", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          moodRecords: sortedMoodRecords,
          reflections: reflections,
          userName: userName || "Friend",
        }),
      });

      if (!res.ok) {
        throw new Error("Failed upstream report assembly call.");
      }

      const data = await res.json();
      setReportMarkdown(data.report);
    } catch (error: any) {
      console.error(error);
      alert("A problem occurred compiling report summary blocks: " + error.message);
    } finally {
      setIsGeneratingReport(false);
    }
  };

  // Filtered CloudWatch logs
  const filteredLogs = cloudWatchLogs.filter((log) => {
    if (logFilter === "ALL") return true;
    if (logFilter === "INFO") return log.level === "INFO";
    if (logFilter === "METRIC") return log.level === "METRIC";
    if (logFilter === "ERROR_WARN") return log.level === "ERROR" || log.level === "WARN";
    return true;
  });

  return (
    <div className="flex flex-col h-full bg-slate-900 rounded-3xl border border-slate-800 p-6 md:p-8 overflow-y-auto scroller shadow-xl" id="insights_dashboard_root">
      
      {/* Header Description */}
      <div className="mb-6 border-b border-slate-800 pb-5 text-left">
        <h2 className="font-display text-2xl md:text-3xl font-bold tracking-tight text-white flex items-center gap-3">
          <TrendingUp className="text-emerald-400" />
          <span>Insights & AWS Dev Monitors</span>
        </h2>
        <p className="text-xs font-sans text-slate-450 mt-1.5 tracking-wide leading-relaxed">
          Log daily mental checkpoint indicators, visualize emotional consistency metrics over time, and inspect real-time serverless logs generated across the AWS Cloud Stack.
        </p>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 mb-6 items-stretch">
        
        {/* Card Left: Daily Check-in Logging Form */}
        <div className="bg-slate-950/40 p-5 rounded-2xl border border-slate-850 lg:col-span-4" id="mood_checkin_form">
          <h3 className="font-display font-semibold text-base text-white mb-4 text-left">
            Daily check-in
          </h3>

          <form onSubmit={handleMoodSubmit} className="space-y-4 text-left">
            {/* Mood selection Grid */}
            <div>
              <label className="text-[10px] uppercase text-slate-500 font-bold tracking-wider block mb-2">How are you feeling?</label>
              <div className="grid grid-cols-3 gap-1.5">
                {Object.keys(moodEmojis).map((moodName) => {
                  const isSelected = selectedMood === moodName;
                  return (
                    <button
                      key={moodName}
                      type="button"
                      onClick={() => setSelectedMood(moodName)}
                      className={`p-2.5 rounded-xl flex flex-col items-center justify-center border transition-all text-center ${
                        isSelected
                          ? `border-emerald-400 ${colors[moodName]}/10 text-white font-bold`
                          : "border-slate-850 bg-slate-900/60 text-slate-400 hover:border-slate-800"
                      }`}
                    >
                      <span className="text-xl">{moodEmojis[moodName]}</span>
                      <span className="text-[9px] mt-1 truncate tracking-wide font-medium">{moodName}</span>
                    </button>
                  );
                })}
              </div>
            </div>

            {/* Slider Metrics: Confidence & Clarity */}
            <div className="space-y-3 pt-1">
              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-[10px] uppercase text-slate-400 font-bold tracking-wider block">Self-Confidence Level</label>
                  <span className="font-mono text-xs font-bold text-emerald-400 block">{selectedConfidence}/10</span>
                </div>
                <input
                  type="range"
                  min="1"
                  max="10"
                  value={selectedConfidence}
                  onChange={(e) => setSelectedConfidence(Number(e.target.value))}
                  className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-emerald-500"
                />
              </div>

              <div>
                <div className="flex justify-between items-center mb-1">
                  <label className="text-[10px] uppercase text-slate-400 font-bold tracking-wider block">Mental Clarity Level</label>
                  <span className="font-mono text-xs font-bold text-teal-400 block">{selectedClarity}/10</span>
                </div>
                <input
                  type="range"
                  min="1"
                  max="10"
                  value={selectedClarity}
                  onChange={(e) => setSelectedClarity(Number(e.target.value))}
                  className="w-full h-1 bg-slate-800 rounded-lg appearance-none cursor-pointer accent-teal-500"
                />
              </div>
            </div>

            {/* Reflection optional notes */}
            <div>
              <label className="text-[10px] uppercase text-slate-400 font-bold tracking-wider block mb-2">Short notes (optional)</label>
              <input
                type="text"
                placeholder="A bit tense today because of deadlines..."
                value={moodNotes}
                onChange={(e) => setMoodNotes(e.target.value)}
                className="w-full bg-slate-900 border border-slate-850 rounded-xl px-3 py-2 placeholder-slate-600 text-slate-250 text-xs focus:outline-none focus:border-emerald-500 text-left"
              />
            </div>

            {moodAddedMsg && (
              <div className="bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 py-1.5 px-3 rounded-lg flex items-center gap-2 text-[10px]">
                <CheckCircle size={14} className="shrink-0" />
                <span>Daily mood logged successfully!</span>
              </div>
            )}

            <button
              type="submit"
              className="w-full py-2.5 bg-emerald-500 hover:bg-emerald-600 text-slate-950 font-bold rounded-xl transition-all text-xs uppercase tracking-wider flex items-center justify-center gap-1.5 leading-none"
            >
              <span>Commit metrics log</span>
            </button>
          </form>
        </div>

        {/* Card Right: Analytics Graphing Zone */}
        <div className="bg-slate-950/40 p-5 rounded-2xl border border-slate-850 lg:col-span-8 flex flex-col justify-between" id="emotional_chart_visuals">
          <div>
            <h3 className="font-display font-semibold text-base text-white mb-1.5 text-left">
              Emotional progression trends
            </h3>
            <p className="text-xs text-slate-400 leading-normal tracking-wide text-left">
              Tracking your Confidence vs Clarity indexes across your commit entries chronologically.
            </p>
          </div>

          <div className="h-44 w-full mt-4 flex items-center justify-center bg-slate-950/10 p-2 border border-slate-900/60 rounded-xl">
            {chartData.length < 2 ? (
              <div className="text-center p-4 bg-slate-900/10 rounded-xl w-full flex flex-col justify-center items-center h-full">
                <AlertCircle size={22} className="text-slate-500 mb-1" />
                <p className="text-slate-500 text-[11px]">Not enough trend log coordinates yet. Record at least 2 check-ins to view graphs.</p>
              </div>
            ) : (
              <ResponsiveContainer width="100%" height="100%">
                <LineChart
                  data={chartData}
                  margin={{ top: 10, right: 10, left: -25, bottom: 0 }}
                >
                  <CartesianGrid strokeDasharray="3 3" stroke="#1c2535" opacity={0.5} />
                  <XAxis dataKey="date" stroke="#64748b" fontSize={10} tickLine={false} />
                  <YAxis domain={[1, 10]} stroke="#64748b" fontSize={10} tickLine={false} />
                  <Tooltip
                    contentStyle={{
                      backgroundColor: "#030712",
                      borderColor: "#1e293b",
                      borderRadius: "12px",
                      color: "#f8fafc",
                    }}
                  />
                  <Legend verticalAlign="top" height={28} fontSize={10} iconSize={8} />
                  <Line
                    name="Self-Confidence"
                    type="monotone"
                    dataKey="Confidence"
                    stroke="#10b981"
                    strokeWidth={2}
                    dot={{ r: 3, strokeWidth: 1.5 }}
                    activeDot={{ r: 5 }}
                  />
                  <Line
                    name="Mental Clarity"
                    type="monotone"
                    dataKey="Clarity"
                    stroke="#0d9488"
                    strokeWidth={2}
                    dot={{ r: 3, strokeWidth: 1.5 }}
                    activeDot={{ r: 5 }}
                  />
                </LineChart>
              </ResponsiveContainer>
            )}
          </div>

          {/* Quick stats summarizing block */}
          <div className="grid grid-cols-2 gap-3 mt-4">
            <div className="bg-slate-900/40 p-3 rounded-xl border border-slate-850 text-left">
              <span className="text-[9px] text-slate-500 font-sans tracking-widest uppercase block">Reflections Published</span>
              <div className="flex items-baseline gap-1 mt-0.5">
                <span className="text-lg font-bold font-sans text-white">{reflections.length}</span>
                <span className="text-xs text-slate-400">entries</span>
              </div>
            </div>

            <div className="bg-slate-900/40 p-3 rounded-xl border border-slate-850 text-left">
              <span className="text-[9px] text-slate-500 font-sans tracking-widest uppercase block">Mood Checkpoints</span>
              <div className="flex items-baseline gap-1 mt-0.5">
                <span className="text-lg font-bold font-sans text-white">{moodRecords.length}</span>
                <span className="text-xs text-slate-400">check-ins</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* section Middle: Growth report generator block */}
      <div className="bg-slate-950/20 p-5 rounded-2xl border border-slate-850 text-left mb-6" id="report_generator_panel">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center p-1 gap-4">
          <div>
            <h3 className="font-display font-semibold text-base text-white flex items-center gap-2">
              <Sparkles className="text-emerald-400 shrink-0" size={16} />
              <span>AI Progression Recovery Synthesis Report</span>
            </h3>
            <p className="text-[11px] text-slate-400 leading-normal mt-1 max-w-xl">
              Compile personal mood check-ins and reflection logs and command Gemini-3.5-Flash to synthesize a structured recovery report.
            </p>
          </div>

          <button
            onClick={handleGenerateReport}
            disabled={isGeneratingReport || reflections.length === 0}
            className={`py-2 px-4 rounded-xl font-semibold tracking-wide flex items-center gap-2 transition-all text-xs border shrink-0 ${
              reflections.length === 0 || isGeneratingReport
                ? "bg-slate-800 text-slate-500 border-slate-700/50 cursor-not-allowed"
                : "bg-emerald-500/10 border-emerald-500/20 text-emerald-400 font-bold hover:bg-emerald-500/20"
            }`}
          >
            {isGeneratingReport ? <RefreshCw className="animate-spin text-emerald-400" size={12} /> : <Calendar size={12} />}
            <span>{isGeneratingReport ? "Compiling..." : "Generate report"}</span>
          </button>
        </div>

        {/* Display completed report */}
        {reportMarkdown && (
          <div className="mt-4 bg-slate-950/60 border border-slate-850 p-5 rounded-xl text-slate-300 text-xs leading-relaxed max-w-none font-sans whitespace-pre-wrap">
            <div className="flex items-center justify-between border-b border-slate-900 pb-2.5 mb-3">
              <span className="text-[10px] text-slate-500 font-mono tracking-wide">Saarthi Cloud Mentorship Synthesis Report</span>
              <button
                onClick={() => setReportMarkdown("")}
                className="text-slate-500 hover:text-white hover:bg-slate-900 text-[9px] font-bold uppercase tracking-wider py-0.5 px-2 rounded border border-slate-850"
              >
                Clear Sheet
              </button>
            </div>
            {reportMarkdown}
          </div>
        )}
      </div>

      {/* section Bottom: Real-time CloudWatch Console Monitor Window */}
      <div className="bg-slate-950 border border-slate-800 rounded-2xl p-5 text-left flex flex-col" id="live_aws_cloudwatch_logger">
        <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center border-b border-slate-850 pb-3 mb-4 gap-3">
          <div className="flex items-center gap-2">
            <Terminal className="text-emerald-400" size={18} />
            <div>
              <h3 className="font-display font-semibold text-sm text-white flex items-center gap-1.5">
                <span>AWS CloudWatch Logs</span>
                <span className="text-[9px] uppercase px-1.5 py-0.5 bg-emerald-500/10 text-emerald-400 rounded-md font-mono font-bold">ap-south-1</span>
              </h3>
              <p className="text-[10px] text-slate-500 font-sans tracking-wide">Real-time AWS Solution Architecture execution logs</p>
            </div>
          </div>

          {/* Quick Metrics Indicators */}
          <div className="flex gap-2 text-[9px] font-mono items-center">
            <div className="bg-slate-900 px-2 py-1 rounded border border-slate-800 text-slate-400 flex items-center gap-1">
              <Server size={10} className="text-slate-500" />
              <span>Lambda latency:</span>
              <span className="text-emerald-400 font-bold font-mono">284ms</span>
            </div>
            <div className="bg-slate-900 px-2 py-1 rounded border border-slate-800 text-slate-400 flex items-center gap-1">
              <Database size={10} className="text-slate-500" />
              <span>DynamoDB state:</span>
              <span className="text-teal-400 font-bold font-mono">CONNECTED</span>
            </div>
          </div>
        </div>

        {/* Filters and Control panel */}
        <div className="flex justify-between items-center mb-3">
          <div className="flex gap-1.5">
            {(["ALL", "INFO", "METRIC", "ERROR_WARN"] as const).map((mode) => (
              <button
                key={mode}
                onClick={() => setLogFilter(mode)}
                className={`text-[9px] font-bold px-2.5 py-1 rounded transition-all uppercase tracking-wider border ${
                  logFilter === mode
                    ? "bg-emerald-500/10 border-emerald-500/30 text-emerald-400"
                    : "bg-transparent border-transparent text-slate-500 hover:text-slate-300"
                }`}
              >
                {mode === "ERROR_WARN" ? "Errors & Warnings" : mode}
              </button>
            ))}
          </div>

          <button
            onClick={onClearLogs}
            className="flex items-center gap-1 text-[9px] text-slate-400 hover:text-rose-400 transition-all font-bold uppercase tracking-wider"
            title="Clear Log Terminal"
          >
            <Trash2 size={12} />
            <span>Clear console</span>
          </button>
        </div>

        {/* Scrolling logs console */}
        <div className="h-44 bg-slate-950 rounded-lg p-3 border border-slate-900 font-mono text-[10px] leading-relaxed overflow-y-auto scroller-hidden divide-y divide-slate-900">
          {filteredLogs.length === 0 ? (
            <p className="text-slate-700 italic text-center py-10 font-mono">-- No AWS console logs dispatched currently --</p>
          ) : (
            filteredLogs.map((log) => {
              let tagColor = "text-blue-400";
              if (log.level === "METRIC") tagColor = "text-purple-400";
              if (log.level === "WARN") tagColor = "text-amber-400 animate-pulse";
              if (log.level === "ERROR") tagColor = "text-rose-400 animate-pulse font-bold";

              return (
                <div key={log.id} className="py-1.5 flex gap-2 items-start font-mono text-left select-all hover:bg-slate-900/40">
                  <span className="text-slate-600 shrink-0 font-mono">[{new Date(log.timestamp).toISOString().split("T")[1].slice(0, 8)}]</span>
                  <span className={`${tagColor} shrink-0 uppercase font-mono font-bold w-12`}>[{log.level}]</span>
                  <span className="text-slate-300 font-mono leading-normal whitespace-pre-wrap flex-1">{log.message}</span>
                </div>
              );
            })
          )}
        </div>
        <div className="mt-2 text-[9px] text-slate-650 flex justify-between font-mono">
          <span>Active streams: ap-south-1.cognito / ap-south-1.lambda / ap-south-1.dynamodb</span>
          <span>Buffer: 50 rows maximum</span>
        </div>
      </div>

    </div>
  );
}
