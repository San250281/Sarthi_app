import express from "express";
import path from "path";
import dotenv from "dotenv";
import { createServer as createViteServer } from "vite";
import { GoogleGenAI, Type } from "@google/genai";

dotenv.config();

const app = express();
const PORT = 3000;

app.use(express.json());

// Lazy-initialize client to prevent crashing on boot if key is missing
let aiClient: GoogleGenAI | null = null;
const getAiClient = (): GoogleGenAI => {
  if (!aiClient) {
    const key = process.env.GEMINI_API_KEY;
    if (!key || key === "MY_GEMINI_API_KEY") {
      throw new Error("GEMINI_API_KEY environment variable is required to power Saarthi's intelligent companion.");
    }
    aiClient = new GoogleGenAI({
      apiKey: key,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        }
      }
    });
  }
  return aiClient;
};

// --- API Endpoints ---

// API 1: Chat/Voice conversation
app.post("/api/chat", async (req, res) => {
  try {
    const { message, history, systemPrompt, isVoice, conversationSummary } = req.body;
    const ai = getAiClient();

    // Map history elements into standard contents format
    // Keep it minimal to optimize token payload & latency
    const recentHistory = history || [];
    const contents = recentHistory.map((h: { role: string; text: string }) => ({
      role: h.role === "saarthi" ? "model" : "user",
      parts: [{ text: h.text }]
    }));

    // Add current message to layout contents
    contents.push({
      role: "user",
      parts: [{ text: message }]
    });

    // If there is an existing conversation summary, we prepend it to the background context
    let formattedPrompt = systemPrompt || "You are Saarthi, a warm, non-judgmental emotionally intelligent AI companion.";
    if (conversationSummary) {
      formattedPrompt = `Background Context Summary of previous conversation turns:\n"${conversationSummary}"\n\n${formattedPrompt}`;
    }

    // Force strict Hinglish voice constraints directly on the backend to guarantee small, optimized tokens and low-latency natural flow
    if (isVoice) {
      formattedPrompt = `You are Saarthi. You are in a live voice conversation with the user.
Your absolute goal is to engage like an intelligent friend, mentor, and trusted companion, NEVER like a generic chatbot.
Your voice and response style must sound like a warm, supportive, and natural human being.

Personality Traits: Warm, friendly, intelligent, supportive, calm, deeply emotional and empathetic.

Strict Conversation Rules:
- Answer in 1 to 3 short, natural sentences.
- Avoid robotic language or formal jargon.
- Speak naturally in "Hinglish" or English as per user prompt context.
- Keep response lengths strictly under 40-60 words to avoid robotic sounding TTS synthesis.
- Pehle user ki baat politely acknowledge karo, fir answer do, fir conversation naturally aage badhao (maybe with a quick companion follow-up question).
- Never use bullet points, numbered lists, or long technical explanations.
- Avoid all robotic prefixes like: "Certainly", "Of course", "Based on my analysis", "Main aapki chinta samajhta hoon".

Hinglish Styling:
Use natural conversational Hinglish words (e.g. "Ye idea kaafi accha lag raha hai", "Mujhe lagta hai issue latency ka ho sakta hai", "Chalo dekhte hain ise kaise solve karein").
Avoid formal Hindi translations like "Kripya jankari dein" or "Main aapki samasya samajhta hoon".

Use the following specific Response Formula:
1. Acknowledge: User ke voice emotional status or feeling ko notice karke validate karo.
2. Answer: Direct, warm reply with exactly one core idea.
3. Continue: Ask a casual conversational follow-up to pass back the mic naturally.

${formattedPrompt}`;
    }

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: contents,
      config: {
        systemInstruction: formattedPrompt,
        temperature: 0.7,
      }
    });

    res.json({ response: response.text });
  } catch (error: any) {
    console.error("Error in /api/chat:", error);
    res.status(500).json({ error: error?.message || "Failed to generate AI response." });
  }
});

// API 5: Summarize history for conversational memory optimization
app.post("/api/summarize-history", async (req, res) => {
  const { history, existingSummary } = req.body || {};
  try {
    if (!history || history.length === 0) {
      return res.json({ summary: existingSummary || "" });
    }
    const ai = getAiClient();

    const textToSummarize = history
      .map((h: { role: string; text: string }) => `${h.role === "saarthi" ? "Saarthi" : "User"}: ${h.text}`)
      .join("\n");

    const prompt = `You are an automated background summarizer for Saarthi. Your job is to update or generate a one-sentence summary of the conversation context so far.
Keep it extremely concise (under 20 words) and focus on the user's current topic.

Existing summary: "${existingSummary || "None"}"
New conversation turns to incorporate:
${textToSummarize}

Write only the updated one-sentence summary. No introductory/concluding text or fluff.`;

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: prompt,
      config: {
        systemInstruction: "You are a concise background conversational summarizer. Return only the updated one-sentence summary.",
        temperature: 0.2,
      }
    });

    res.json({ summary: response.text?.trim() || existingSummary || "" });
  } catch (error: any) {
    console.error("Error in /api/summarize-history:", error);
    res.json({ summary: existingSummary || "" });
  }
});

// API 2: Extract key memories
app.post("/api/extract-memories", async (req, res) => {
  try {
    const { message, aiResponse, existingMemories } = req.body;
    const ai = getAiClient();

    const prompt = `A user said: "${message}"\nThe AI companion replied: "${aiResponse}"\n
Existing facts recorded or known so far: ${JSON.stringify(existingMemories || [])}

Analyze the user's latest statement and detect if they explicitly mentioned any details about themselves that should be remembered to provide deep personalized emotional support in subsequent chats.
If they did, extract them into atomic facts. Avoid duplicating or contradicting existing facts.
Valid Categories:
- "Name" (their display name)
- "Goal" (a positive objective they are pursuing)
- "Habit" (routines or daily practices they perform or want to start)
- "Interest" (hobbies or things they like)
- "Preference" (themes, speaking speed, language preference, etc.)
- "Milestone" (dates, achievements, victories)
- "Other" (any other key factual detail)

Only extract facts explicitly shared in this turn. If no new facts were mentioned, return an empty list.`;

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: prompt,
      config: {
        systemInstruction: "You are an automated analytical layer for Saarthi that extracts core personal user memories in a perfectly structured list.",
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.ARRAY,
          items: {
            type: Type.OBJECT,
            properties: {
              category: {
                type: Type.STRING,
                description: "Must be one of: Name, Goal, Habit, Interest, Preference, Milestone, Other"
              },
              key: {
                type: Type.STRING,
                description: "A very brief 1-3 word title or keyword of the fact, e.g. Sleep Routine, Dog Name, Math Exam"
              },
              value: {
                type: Type.STRING,
                description: "A summary of the fact, e.g. Loves to drink chamomile tea before bedtime, Has a puppy named Bruno, Preparing for SAT math"
              }
            },
            required: ["category", "key", "value"]
          }
        },
        temperature: 0.2,
      }
    });

    const parsed = JSON.parse(response.text || "[]");
    res.json({ memories: parsed });
  } catch (error: any) {
    console.error("Error in /api/extract-memories:", error);
    res.json({ memories: [] }); // Soft fail: return empty memories list rather than breaking
  }
});

// API 3: Reflection Summarizing
app.post("/api/generate-reflection", async (req, res) => {
  try {
    const { text } = req.body;
    if (!text || !text.trim()) {
      return res.status(400).json({ error: "No reflection text provided." });
    }
    const ai = getAiClient();

    const prompt = `The user recorded this reflection about their emotional state, thoughts, or day:\n"${text}"\n\nPerform a warm, caring analysis and write a structured emotional feedback object. Keep the feedback highly encouraging, gentle, non-clinical, and brief. Add bullet points for takeaways and actionable footsteps. Choose an appropriate single emoji representing their primary mood.`;

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: prompt,
      config: {
        systemInstruction: "You are Saarthi's analytical journaling companion. Analyze user journals, detect feelings, note takeaways and suggest actionable support instructions.",
        responseMimeType: "application/json",
        responseSchema: {
          type: Type.OBJECT,
          properties: {
            detectedFeelings: {
              type: Type.STRING,
              description: "A gentle sentence describing what emotional feelings, anxiety, calm, or mood they seemed to express."
            },
            keyTakeaways: {
              type: Type.STRING,
              description: "A bulleted list of 2-3 key takeaways or observations from their words."
            },
            actionableSteps: {
              type: Type.STRING,
              description: "A bulleted list of 1-2 actionable, positive baby-steps or supportive focus instruction for today."
            },
            mainMoodEmoji: {
              type: Type.STRING,
              description: "One single emoji matching their mood, e.g. 🌅, 🌱, 🌧️, 🌊, ✨, 🌸"
            }
          },
          required: ["detectedFeelings", "keyTakeaways", "actionableSteps", "mainMoodEmoji"]
        },
        temperature: 0.4
      }
    });

    const parsed = JSON.parse(response.text || "{}");
    res.json(parsed);
  } catch (error: any) {
    console.error("Error in /api/generate-reflection:", error);
    res.status(500).json({ error: error?.message || "Failed to process reflection." });
  }
});

// API 4: Personalized Synthesis & Progression Report
app.post("/api/generate-report", async (req, res) => {
  try {
    const { moodRecords, reflections, userName } = req.body;
    const ai = getAiClient();

    const prompt = `You are Saarthi, an emotionally intelligent, warm AI companion.
Generate a beautiful, inspiring progression/growth feedback report for user: ${userName || "Friend"}.
They have recorded these journals and emotional checklists over the past few days.

Mood History logs:
${JSON.stringify(moodRecords || [])}

Journals/Reflections logs:
${JSON.stringify(reflections || [])}

Synthesize these data points into a beautifully formatted, structured Markdown report.
Structure requirements:
1. **Introduction**: A warm, encouraging welcome greeting noticing their progress, consistency, or vulnerability.
2. **Emotional Analysis**: Gentle, loving analysis of their mood swings or calming patterns over the past week.
3. **Key Victories**: Highlight small details they've mentioned as milestones, or consistency in journaling.
4. **Actionable Roadmap**: Highlight 3 personalized mindfulness exercises, habits, or micro-goals tailored exactly to their logs.

Make the tone extremely warm, supportive, motivating, and full of deep empathy. Do not sound clinical or use robotic jargon. Keep it compact but highly meaningful! Use bold typography and lists to make it beautiful in Markdown.`;

    const response = await ai.models.generateContent({
      model: "gemini-3.5-flash",
      contents: prompt,
      config: {
        systemInstruction: "You are Saarthi, an emotionally intelligent voice companion helping humans move forward in life with clarity, comfort, and positive focus.",
        temperature: 0.7
      }
    });

    res.json({ report: response.text });
  } catch (error: any) {
    console.error("Error in /api/generate-report:", error);
    res.status(500).json({ error: error?.message || "Failed to generate report." });
  }
});

// --- Vite and Production serving setup ---

async function startServer() {
  if (process.env.NODE_ENV !== "production") {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: "spa",
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), "dist");
    app.use(express.static(distPath));
    app.get("*", (req, res) => {
      res.sendFile(path.join(distPath, "index.html"));
    });
  }

  app.listen(PORT, "0.0.0.0", () => {
    console.log(`Saarthi fullstack web server running on port ${PORT}`);
  });
}

startServer();
