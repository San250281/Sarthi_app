export interface ChatMessage {
  id: string;
  conversationId: string; // Linked conversation id
  role: "user" | "saarthi";
  text: string;
  timestamp: number;
  isVoice?: boolean;
  language?: string;
  agentId?: string; // Which agent replied
  tokenCount?: number; // AWS token metric simulation
}

export interface Conversation {
  id: string;
  title: string;
  createdDate: number;
  updatedDate: number;
  summary?: string; // Cache the conversation context summary
}

export interface MemoryItem {
  id: string;
  category: "Name" | "Goal" | "Habit" | "Interest" | "Preference" | "Milestone" | "Gratitude" | "Other";
  key: string;
  value: string;
  timestamp: number;
}

export interface UserProfile {
  userName: string;
  userEmail: string;
  preferredLanguage: string; // "Auto", "English", "Hindi", "Hinglish"
  currentMood: string;
  themeMode: "Auto" | "Light" | "Dark";
  passcodeHash: string; // secure hash
  isLoggedIn: boolean;
  cognitoTokenExpiration?: number; // expiry timestamp of session
  cognitoIdToken?: string; // JWT token
  cognitoRefreshToken?: string; // Refresh token
  subscriptionPlan: string; // "Free Tier" | "Weekly Explorer" | "Monthly Companion" | "Yearly Zen"
  subscriptionExpiry: number; // millisecond timestamp
  subscriptionStatus: "Active" | "Inactive" | "Expired";
  dailyQuestionsCount: number; // 10 limits for Free Tier
  lastQuestionTimestamp: number; // For daily quotas resetting
  createdDate: number;
  lastLogin: number;
}

export interface MoodRecord {
  id: string;
  mood: string; // "Excited", "Normal", "Confused", "Stressed", "Unmotivated", "Sad"
  confidence: number; // 1 to 10
  clarity: number; // 1 to 10
  notes: string;
  timestamp: number;
}

export interface ReflectionSummary {
  id: string;
  originalText: string;
  detectedFeelings: string;
  keyTakeaways: string; // formatted markdown bullet points
  actionableSteps: string; // formatted markdown bullet points
  mainMoodEmoji: string;
  timestamp: number;
}

export interface AIAgent {
  id: string;
  name: string;
  pricePerMin: number;
  avatarEmoji: string;
  description: string;
  greeting: string;
  systemPromptExtension: string;
}

export type SaarthiState = "IDLE" | "LISTENING" | "THINKING" | "SPEAKING";

export interface CloudWatchLog {
  id: string;
  timestamp: number;
  level: "INFO" | "METRIC" | "WARN" | "ERROR";
  message: string;
}
