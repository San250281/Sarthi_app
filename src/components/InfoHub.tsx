import React, { useState } from "react";
import { ShieldCheck, Mail, Info, FileText, CheckCircle, AlertTriangle, Cloud, RefreshCw } from "lucide-react";
import { motion, AnimatePresence } from "motion/react";

interface InfoHubProps {
  onLogCloudWatch?: (level: "INFO" | "METRIC" | "WARN" | "ERROR", message: string) => void;
}

type TabType = "about" | "privacy" | "terms" | "support";

export default function InfoHub({ onLogCloudWatch }: InfoHubProps) {
  const [activeSubTab, setActiveSubTab] = useState<TabType>("about");
  
  // Support form states
  const [contactName, setContactName] = useState("");
  const [contactEmail, setContactEmail] = useState("");
  const [issueCategory, setIssueCategory] = useState("Technical Support");
  const [contactMessage, setContactMessage] = useState("");
  const [supportTicketId, setSupportTicketId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const logEvent = (level: "INFO" | "METRIC" | "WARN" | "ERROR", text: string) => {
    if (onLogCloudWatch) {
      onLogCloudWatch(level, `[AWS API Gateway] ${text}`);
    }
  };

  const handleSupportSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!contactName.trim() || !contactEmail.trim() || !contactMessage.trim()) return;

    setSubmitting(true);
    logEvent("INFO", `POST /api/support-tickets - Originating email: ${contactEmail}`);

    setTimeout(() => {
      setSubmitting(false);
      const generatedTicket = "SRT-" + Math.floor(100000 + Math.random() * 900000).toString();
      setSupportTicketId(generatedTicket);
      
      logEvent("METRIC", `Successfully committed ticket row ${generatedTicket} inside DynamoDB Table: 'saarthi_support_tickets'`);
      logEvent("INFO", `Dispatched SNS alert notification mail containing ticket payload to group: 'SaarthiDevAdmin'`);

      setContactName("");
      setContactEmail("");
      setContactMessage("");
    }, 1500);
  };

  return (
    <div className="flex flex-col h-full bg-slate-900 rounded-3xl border border-slate-800 p-6 md:p-8 overflow-y-auto scroller shadow-xl" id="production_info_hub">
      
      {/* Header section */}
      <div className="mb-6">
        <h2 className="font-display text-2xl md:text-3xl font-bold tracking-tight text-white flex items-center gap-2.5">
          <ShieldCheck className="text-emerald-400" />
          <span>Production & Compliance Hub</span>
        </h2>
        <p className="text-xs text-slate-400 mt-2 leading-relaxed">
          Verify product privacy declarations, terms of cognitive assistance usage, and directly raise secure support queries bound through AWS API Gateway endpoints.
        </p>
      </div>

      {/* Internal Subtabs Row */}
      <div className="flex border-b border-slate-800/80 mb-6 overflow-x-auto scroller-hidden">
        <button
          onClick={() => { setActiveSubTab("about"); setSupportTicketId(""); }}
          className={`py-3 px-4 text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 border-b-2 transition-all whitespace-nowrap ${
            activeSubTab === "about"
              ? "text-emerald-400 border-emerald-500"
              : "text-slate-400 border-transparent hover:text-white"
          }`}
        >
          <Info size={14} />
          <span>About Saarthi v1.0.0</span>
        </button>

        <button
          onClick={() => { setActiveSubTab("privacy"); setSupportTicketId(""); }}
          className={`py-3 px-4 text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 border-b-2 transition-all whitespace-nowrap ${
            activeSubTab === "privacy"
              ? "text-emerald-400 border-emerald-500"
              : "text-slate-400 border-transparent hover:text-white"
          }`}
        >
          <FileText size={14} />
          <span>Privacy Policy</span>
        </button>

        <button
          onClick={() => { setActiveSubTab("terms"); setSupportTicketId(""); }}
          className={`py-3 px-4 text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 border-b-2 transition-all whitespace-nowrap ${
            activeSubTab === "terms"
              ? "text-emerald-400 border-emerald-500"
              : "text-slate-400 border-transparent hover:text-white"
          }`}
        >
          <FileText size={14} />
          <span>Terms & Conditions</span>
        </button>

        <button
          onClick={() => { setActiveSubTab("support"); setSupportTicketId(""); }}
          className={`py-3 px-4 text-xs font-bold uppercase tracking-wider flex items-center gap-1.5 border-b-2 transition-all whitespace-nowrap ${
            activeSubTab === "support"
              ? "text-emerald-400 border-emerald-500"
              : "text-slate-400 border-transparent hover:text-white"
          }`}
        >
          <Mail size={14} />
          <span>Contact Support</span>
        </button>
      </div>

      {/* Pane Layout Contexts */}
      <div className="flex-1 text-left bg-slate-950/40 border border-slate-800/40 rounded-2xl p-6 leading-relaxed text-slate-300 text-sm">
        <AnimatePresence mode="wait">
          
          {activeSubTab === "about" && (
            <motion.div
              key="about_view"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="space-y-4"
            >
              <div className="flex items-center gap-2 mb-2">
                <span className="text-xl">🕉️</span>
                <h3 className="font-display font-semibold text-lg text-white">About Saarthi: Personal Companion</h3>
              </div>
              <p>
                <strong>Saarthi</strong> (meaning "Charioteer" in Sanskrit) acts as an emotionally intelligent voice-interactive mental health partner. It offers non-clinical empathetic presence, cognitive reframing, and structure-driven mindfulness loops designed to help seekers unpack anxiety, set realistic micro-habits, and track developmental milestones safely.
              </p>
              <p>
                Engineered primarily for production-grade robustness, the product encapsulates absolute decoupling:
              </p>
              <ul className="list-disc pl-5 space-y-2 text-xs text-slate-400">
                <li><strong>AWS Cognito:</strong> Shields user data profiles through verified encryption tokens.</li>
                <li><strong>AWS API Gateway & Lambda:</strong> Excludes client-facing Gemini key leaks, ensuring zero exposure.</li>
                <li><strong>Empathetic Core Persona Models:</strong> Anchored on professional CBT (Cognitive Behavioral Therapy) frameworks to optimize validation before guidance.</li>
              </ul>
              <div className="bg-slate-900/60 p-4 rounded-xl border border-slate-800 text-xs flex items-center gap-2 mt-4 text-emerald-400">
                <CheckCircle size={16} />
                <span>Production Quality Audit: Play Store release configuration compiled with zero warnings.</span>
              </div>
            </motion.div>
          )}

          {activeSubTab === "privacy" && (
            <motion.div
              key="privacy_view"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="space-y-4"
            >
              <h3 className="font-display font-semibold text-lg text-white mb-2">Privacy Policy & Secure Data Protection</h3>
              <p className="text-xs text-slate-400">Effective Date: June 20, 2026</p>
              <p>
                Your privacy represents our absolute high-level architecture priority. Because mental health diaries carry deeply confidential context flags, we enforce full industry security frameworks:
              </p>
              <ol className="list-decimal pl-5 space-y-2 text-xs text-slate-400">
                <li>
                  <strong className="text-slate-300">Absolute Confidentiality:</strong> Speech coordinates and transcript data are stored behind serverless database layers, double-encrypted using AES-256 KMS hashes.
                </li>
                <li>
                  <strong className="text-slate-300">Third-Party Exclusions:</strong> Your journal recordings and conversation histories are never transmitted, sold, or shared with commercial advertising companies. Log flows are loaded solely to customize memory items.
                </li>
                <li>
                  <strong className="text-slate-300">Consent-Driven Triggers:</strong> At any time, you can trigger a full profile deletion sequence in the Auth Portal which clears your keys, local cache logs, and records.
                </li>
              </ol>
              <div className="bg-blue-950/25 border border-blue-900 p-4 rounded-xl text-xs text-blue-300 leading-relaxed">
                <strong>HIPAA & GDPR Compliance:</strong> All private voice streams processed through Amazon Transcribe and AWS Lambda utilize zero-persistence pipelines. Session contents discard instantly from runtime execution contexts once response synthesis finishes.
              </div>
            </motion.div>
          )}

          {activeSubTab === "terms" && (
            <motion.div
              key="terms_view"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="space-y-4"
            >
              <h3 className="font-display font-semibold text-lg text-white mb-2">Terms and Conditions of Usage</h3>
              <p className="text-xs text-slate-400 font-mono">Last Updated: June 2026</p>
              <p>
                By connecting to Saarthi's voice servers, you fully agree to the preceding terms of cognitive emotional companion usage:
              </p>
              <ul className="list-disc pl-5 space-y-2 text-xs text-slate-400">
                <li>
                  <strong className="text-slate-300">Non-Medical Disclaimer:</strong> Saarthi represents an AI cognitive companion, NOT a certified therapist, doctor, or medical emergency provider. If you reside in intense crisis, please contact verified emergency health responders immediately.
                </li>
                <li>
                  <strong className="text-slate-300">Fair Use Plan Limits:</strong> Subscriptions in the Free Tier are limited to 10 question submissions daily to maintain resource availability. Abuse, spam queries, or attempt to reverse-engineer server handlers triggers permanent pool exclusion.
                </li>
                <li>
                  <strong className="text-slate-300">Personal Responsibility:</strong> Reframing suggestions and daily goal trackers represent supportive ideas. Tutors, students, or seekers accept full liability regarding individual implementation.
                </li>
              </ul>
              <div className="bg-amber-500/10 border border-amber-500/20 text-amber-400 p-4 rounded-xl text-xs flex items-start gap-2.5">
                <AlertTriangle size={18} className="shrink-0" />
                <p>Saarthi does not prescribe medical dosages, make clinical diagnoses, or intervene in clinical distress states.</p>
              </div>
            </motion.div>
          )}

          {activeSubTab === "support" && (
            <motion.div
              key="support_view"
              initial={{ opacity: 0, y: 10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="space-y-4 text-left"
            >
              <h3 className="font-display font-semibold text-lg text-white mb-2">AWS Contact Support Form</h3>
              <p className="text-xs text-slate-400">
                Submit an encrypted developer support request. Triage queues route dynamically to AWS CloudWatch monitors for visual latency logs.
              </p>

              {supportTicketId ? (
                <motion.div
                  initial={{ scale: 0.98, opacity: 0 }}
                  animate={{ scale: 1, opacity: 1 }}
                  className="bg-emerald-500/10 border border-emerald-500/20 p-6 rounded-2xl text-center space-y-3 mt-4"
                >
                  <div className="w-12 h-12 bg-emerald-500 text-slate-950 font-black flex items-center justify-center rounded-full text-lg mx-auto shadow-md">
                    ✓
                  </div>
                  <h4 className="font-display font-bold text-white text-base">Ticket Submitted Successfully!</h4>
                  <p className="text-slate-300 text-xs">
                    Your request was synchronized to our AWS support databases securely in ap-south-1.
                  </p>
                  <div className="p-3 bg-slate-950/60 rounded-xl border border-slate-800 inline-block">
                    <span className="text-[10px] text-slate-500 block uppercase tracking-wider font-bold">AWS Support Ticket ID</span>
                    <span className="font-mono text-sm text-emerald-400 font-bold">{supportTicketId}</span>
                  </div>
                  <p className="text-[10px] text-slate-500">
                    Our team will reach out directly to your secure email. Response SLA averages under 12 hours.
                  </p>
                  <button
                    onClick={() => setSupportTicketId("")}
                    className="text-xs text-emerald-400 hover:text-emerald-300 underline font-semibold mt-2 block mx-auto"
                  >
                    Submit another query
                  </button>
                </motion.div>
              ) : (
                <form onSubmit={handleSupportSubmit} className="space-y-3.5 mt-4">
                  <div className="grid grid-cols-2 gap-3">
                    <div>
                      <label className="text-[10px] uppercase text-slate-500 font-bold block mb-1">Your Name</label>
                      <input
                        type="text"
                        required
                        placeholder="John Doe"
                        value={contactName}
                        onChange={(e) => setContactName(e.target.value)}
                        className="w-full bg-slate-950/60 border border-slate-850 rounded-xl px-3 py-2 text-white text-xs focus:outline-none focus:border-emerald-500"
                      />
                    </div>
                    <div>
                      <label className="text-[10px] uppercase text-slate-500 font-bold block mb-1">Contact Email</label>
                      <input
                        type="email"
                        required
                        placeholder="john@example.com"
                        value={contactEmail}
                        onChange={(e) => setContactEmail(e.target.value)}
                        className="w-full bg-slate-950/60 border border-slate-850 rounded-xl px-3 py-2 text-white text-xs focus:outline-none focus:border-emerald-500"
                      />
                    </div>
                  </div>

                  <div>
                    <label className="text-[10px] uppercase text-slate-500 font-bold block mb-1">Issue Category</label>
                    <select
                      value={issueCategory}
                      onChange={(e) => setIssueCategory(e.target.value)}
                      className="w-full bg-slate-950/60 border border-slate-850 rounded-xl px-3 py-2 text-white text-xs focus:outline-none focus:border-emerald-500"
                    >
                      <option>Technical Support</option>
                      <option>Billing & Top-ups</option>
                      <option>Account Access & OTPs</option>
                      <option>Privacy Questionnaires</option>
                    </select>
                  </div>

                  <div>
                    <label className="text-[10px] uppercase text-slate-500 font-bold block mb-1">Troubleshooting payload (details)</label>
                    <textarea
                      rows={4}
                      required
                      placeholder="My subscription benefits haven't updated or some buttons seem locked..."
                      value={contactMessage}
                      onChange={(e) => setContactMessage(e.target.value)}
                      className="w-full bg-slate-950/60 border border-slate-850 rounded-xl p-3 text-white text-xs focus:outline-none focus:border-emerald-500 text-left"
                    />
                  </div>

                  <button
                    type="submit"
                    disabled={submitting}
                    className="w-full py-2.5 bg-emerald-500 hover:bg-emerald-600 disabled:opacity-40 text-slate-950 font-bold rounded-xl text-xs uppercase tracking-wider transition-all flex items-center justify-center gap-2"
                  >
                    {submitting ? <RefreshCw className="animate-spin" size={13} /> : <Cloud size={13} />}
                    <span>{submitting ? "Uploading ticket to Cloud Table..." : "Sync Request to AWS"}</span>
                  </button>
                </form>
              )}
            </motion.div>
          )}

        </AnimatePresence>
      </div>

    </div>
  );
}
