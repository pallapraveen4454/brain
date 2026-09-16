const { onCall, onRequest, HttpsError } = require("firebase-functions/v2/https");
const { defineSecret } = require("firebase-functions/params");
const admin = require("firebase-admin");

if (!admin.apps.length) {
  admin.initializeApp();
}

// Server-side Gemini API key secret
const geminiApiKeySecret = defineSecret("GEMINI_API_KEY");

const PRIMARY_MODEL = "gemini-3.6-flash";
const FALLBACK_MODEL = "gemini-3.5-flash";

// Server-side in-memory rate limiting: 20 requests/minute per UID
const userTimestamps = new Map();
const RATE_LIMIT_WINDOW_MS = 60 * 1000;
const MAX_REQUESTS_PER_WINDOW = 20;

function checkRateLimit(uid) {
  const now = Date.now();
  const timestamps = userTimestamps.get(uid) || [];
  const validTimestamps = timestamps.filter((t) => now - t < RATE_LIMIT_WINDOW_MS);
  if (validTimestamps.length >= MAX_REQUESTS_PER_WINDOW) {
    return false;
  }
  validTimestamps.push(now);
  userTimestamps.set(uid, validTimestamps);

  // Periodic cleanup
  if (userTimestamps.size > 2000) {
    for (const [key, ts] of userTimestamps.entries()) {
      if (ts.every((t) => now - t >= RATE_LIMIT_WINDOW_MS)) {
        userTimestamps.delete(key);
      }
    }
  }
  return true;
}

function resolveServerApiKey() {
  if (process.env.GEMINI_API_KEY && process.env.GEMINI_API_KEY.trim()) {
    return process.env.GEMINI_API_KEY.trim();
  }
  try {
    const val = geminiApiKeySecret.value();
    if (val && val.trim()) {
      return val.trim();
    }
  } catch (e) {
    // Secret may not be defined in local non-cloud environment
  }
  return "";
}

/**
 * Executes server-side call to Google Generative Language API with model fallback:
 * Primary: gemini-3.6-flash
 * Fallback: gemini-3.5-flash
 */
async function executeGeminiRequest(payload, isQuiz = false) {
  const apiKey = resolveServerApiKey();
  if (!apiKey) {
    console.error("Gemini API key is not configured in server environment or secret manager.");
    throw new HttpsError(
      "failed-precondition",
      "AI service is currently unavailable. Please verify API configuration."
    );
  }

  const attempts = [
    { model: PRIMARY_MODEL, timeoutMs: isQuiz ? 35000 : 25000, isFallback: false },
    { model: FALLBACK_MODEL, timeoutMs: isQuiz ? 30000 : 15000, isFallback: true }
  ];

  let lastError = null;

  for (const attempt of attempts) {
    const { model, timeoutMs, isFallback } = attempt;
    const url = `https://generativelanguage.googleapis.com/v1beta/models/${model}:generateContent?key=${apiKey}`;

    const controller = new AbortController();
    const timeoutHandle = setTimeout(() => controller.abort(), timeoutMs);

    try {
      const response = await fetch(url, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          "x-goog-api-key": apiKey
        },
        body: JSON.stringify(payload),
        signal: controller.signal
      });
      clearTimeout(timeoutHandle);

      const status = response.status;
      const body = await response.text();

      if (response.ok && body) {
        const json = JSON.parse(body);
        return { json, modelUsed: model };
      }

      console.warn(`[GeminiBackend] Model ${model} returned HTTP ${status}: ${body.substring(0, 200)}`);

      // Authentication or leaked-key error: DO NOT retry fallback
      const isAuthError =
        status === 401 ||
        status === 403 ||
        (status === 400 && (body.includes("API_KEY") || body.includes("API key")));
      if (isAuthError) {
        throw new HttpsError(
          "permission-denied",
          "AI service is currently unavailable. Please verify API configuration."
        );
      }

      // Quota exceeded
      if (status === 429 || body.includes("RESOURCE_EXHAUSTED")) {
        throw new HttpsError(
          "resource-exhausted",
          "AI service is busy right now. Please wait a moment and try again."
        );
      }

      // For model-specific error or 5xx, try fallback model if not already on fallback
      lastError = new Error(`HTTP ${status} from ${model}`);
    } catch (err) {
      clearTimeout(timeoutHandle);
      if (err instanceof HttpsError) {
        throw err;
      }
      console.warn(`[GeminiBackend] ${isFallback ? "Fallback" : "Primary"} model ${model} error: ${err.message}`);
      lastError = err;
    }
  }

  throw new HttpsError(
    "unavailable",
    "AI service encountered a temporary error. Please try again."
  );
}

function buildQuizPrompt(topic) {
  return `You are an expert trivia master and quiz creator. Generate exactly 10 distinct, high-quality, multiple-choice quiz questions specifically and exclusively about the topic: "${topic}".

STRICT QUALITY REQUIREMENTS:
1. TOPIC-SPECIFIC KNOWLEDGE:
   - Every single question, its correct answer, and all 3 distractors MUST be factually and deeply grounded in "${topic}".
   - For historical topics (e.g. World War II), ask about real battles, commanders, treaties, strategies, dates, alliances, and causes.
   - For entertainment/fiction topics (e.g. Marvel Cinematic Universe), ask about specific characters, Infinity Stones, weapons, movie plots, actors, directors, and lore.
   - For science topics (e.g. Quantum Physics), ask about real principles, particles, equations, experiments, and scientists.
   - For sports topics (e.g. Football World Cup), ask about tournaments, records, legendary players, rules, and memorable matches.

2. FORBIDDEN GENERIC TEMPLATES & DISTRACTORS:
   - DO NOT use generic template questions like:
     * "Which milestone significantly transformed the study of [topic]?"
     * "How does [topic] impact modern global developments?"
     * "Which factor is most crucial for future advancements in [topic]?"
     * "Which core concept is fundamental when studying [topic]?"
   - DO NOT use generic academic distractors like:
     * "Methodological Innovations", "The Ban on Research", "Stagnation of Ideas", "Complete Disregard of Facts", "Ignoring Facts", "Random Speculation".
   - Every distractor must be a genuine, plausible, topic-relevant alternative.

3. QUESTION & ANSWER DIVERSITY:
   - Mix question styles (e.g. key figures, chronology/milestones, technical mechanisms, identification, cause & effect, defining quotes/artifacts).
   - Do not ask about the same entity twice.
   - Exactly 4 options per question.
   - Exactly 1 correct answer.
   - Distribute the correct answer position randomly across the 4 options (roughly an equal mix of 0, 1, 2, and 3).
   - Include a concise, 1-2 sentence explanation of why the correct answer is true.

OUTPUT FORMAT:
Output strictly valid JSON matching this schema without Markdown formatting:
{
  "questions": [
    {
      "questionText": "What specific factual question here?",
      "options": ["Option A", "Option B", "Option C", "Option D"],
      "correctOptionIndex": 0,
      "explanation": "Clear factual explanation."
    }
  ]
}`;
}

// -------------------------------------------------------------
// Firebase Callable Functions (Secured by Firebase Authentication)
// -------------------------------------------------------------

/**
 * AI Quick Answer Callable Function
 */
exports.geminiQuickAnswer = onCall(
  {
    secrets: [geminiApiKeySecret],
    cors: true,
    maxInstances: 10
  },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError(
        "unauthenticated",
        "User must be authenticated to use BrainQuizAI AI features."
      );
    }
    const uid = request.auth.uid;

    if (!checkRateLimit(uid)) {
      throw new HttpsError(
        "resource-exhausted",
        "AI service is busy right now. Please wait a moment and try again."
      );
    }

    const data = request.data || {};
    const question = typeof data.question === "string" ? data.question.trim() : "";
    if (!question) {
      throw new HttpsError("invalid-argument", "Question cannot be empty.");
    }
    if (question.length > 500) {
      throw new HttpsError("invalid-argument", "Question is too long (maximum 500 characters).");
    }

    const rawHistory = Array.isArray(data.recentHistory) ? data.recentHistory : [];
    const boundedHistory = rawHistory.slice(-2);

    const contents = [];
    for (const turn of boundedHistory) {
      const u = typeof turn.user === "string" ? turn.user.trim().substring(0, 500) : "";
      const m = typeof turn.model === "string" ? turn.model.trim().substring(0, 500) : "";
      if (u && m) {
        contents.push({ role: "user", parts: [{ text: u }] });
        contents.push({ role: "model", parts: [{ text: m }] });
      }
    }
    contents.push({ role: "user", parts: [{ text: question }] });

    const payload = {
      contents,
      systemInstruction: {
        parts: [
          {
            text: "You are BrainQuizAI Quick Answer, an intelligent, factual, and direct AI assistant. Provide concise, accurate, and direct answers in the user's language. Keep answers informative yet brief. Do not format as a quiz."
          }
        ]
      },
      generationConfig: {
        temperature: 0.4,
        topP: 0.9,
        maxOutputTokens: 384
      }
    };

    const { json, modelUsed } = await executeGeminiRequest(payload, false);
    const candidates = json && json.candidates;
    if (candidates && candidates.length > 0) {
      const content = candidates[0].content;
      if (content && content.parts && content.parts.length > 0) {
        const text = content.parts.map((p) => p.text || "").join("").trim();
        if (text) {
          return { answer: text, model: modelUsed };
        }
      }
    }

    throw new HttpsError("internal", "Couldn't get an answer right now. Please try again.");
  }
);

/**
 * AI Quiz Generator Callable Function
 */
exports.geminiQuizGenerator = onCall(
  {
    secrets: [geminiApiKeySecret],
    cors: true,
    maxInstances: 10
  },
  async (request) => {
    if (!request.auth || !request.auth.uid) {
      throw new HttpsError(
        "unauthenticated",
        "User must be authenticated to use BrainQuizAI AI features."
      );
    }
    const uid = request.auth.uid;

    if (!checkRateLimit(uid)) {
      throw new HttpsError(
        "resource-exhausted",
        "AI service is busy right now. Please wait a moment and try again."
      );
    }

    const data = request.data || {};
    const topic = typeof data.topic === "string" ? data.topic.trim() : "";
    if (!topic) {
      throw new HttpsError("invalid-argument", "Topic cannot be empty.");
    }
    if (topic.length > 120) {
      throw new HttpsError("invalid-argument", "Topic is too long (maximum 120 characters).");
    }

    const prompt = buildQuizPrompt(topic);
    const payload = {
      contents: [
        {
          parts: [{ text: prompt }]
        }
      ],
      generationConfig: {
        temperature: 0.75,
        topP: 0.95,
        responseMimeType: "application/json"
      }
    };

    const { json, modelUsed } = await executeGeminiRequest(payload, true);
    const candidates = json && json.candidates;
    if (candidates && candidates.length > 0) {
      const content = candidates[0].content;
      if (content && content.parts && content.parts.length > 0) {
        const rawText = content.parts[0].text || "";
        return { rawJson: rawText, model: modelUsed };
      }
    }

    throw new HttpsError("internal", "Failed to generate questions. Please try again.");
  }
);

/**
 * Unified Callable Function
 */
exports.geminiAiService = onCall(
  {
    secrets: [geminiApiKeySecret],
    cors: true,
    maxInstances: 10
  },
  async (request) => {
    const operation = request.data?.operation || request.data?.type;
    if (operation === "quiz_generator") {
      return exports.geminiQuizGenerator.run(request);
    }
    return exports.geminiQuickAnswer.run(request);
  }
);

/**
 * Secured HTTPS REST endpoint (validates Firebase Auth ID Token)
 */
exports.geminiApi = onRequest(
  {
    secrets: [geminiApiKeySecret],
    cors: true,
    maxInstances: 10
  },
  async (req, res) => {
    if (req.method !== "POST") {
      res.status(405).json({ error: "Method not allowed. Use POST." });
      return;
    }

    // Authenticate via Bearer Token
    const authHeader = req.headers.authorization || "";
    if (!authHeader.startsWith("Bearer ")) {
      res.status(401).json({ error: "Unauthorized: Missing Firebase ID token." });
      return;
    }

    const idToken = authHeader.split("Bearer ")[1];
    let decodedToken;
    try {
      decodedToken = await admin.auth().verifyIdToken(idToken);
    } catch (e) {
      res.status(401).json({ error: "Unauthorized: Invalid or expired Firebase ID token." });
      return;
    }

    const uid = decodedToken.uid;
    if (!checkRateLimit(uid)) {
      res.status(429).json({ error: "AI service is busy right now. Please wait a moment and try again." });
      return;
    }

    const body = req.body || {};
    const operation = body.operation || body.type || "quick_answer";

    try {
      if (operation === "quick_answer") {
        const question = typeof body.question === "string" ? body.question.trim() : "";
        if (!question) {
          res.status(400).json({ error: "Question cannot be empty." });
          return;
        }
        const rawHistory = Array.isArray(body.recentHistory) ? body.recentHistory : [];
        const boundedHistory = rawHistory.slice(-2);
        const contents = [];
        for (const turn of boundedHistory) {
          const u = typeof turn.user === "string" ? turn.user.trim().substring(0, 500) : "";
          const m = typeof turn.model === "string" ? turn.model.trim().substring(0, 500) : "";
          if (u && m) {
            contents.push({ role: "user", parts: [{ text: u }] });
            contents.push({ role: "model", parts: [{ text: m }] });
          }
        }
        contents.push({ role: "user", parts: [{ text: question }] });

        const payload = {
          contents,
          systemInstruction: {
            parts: [
              {
                text: "You are BrainQuizAI Quick Answer, an intelligent, factual, and direct AI assistant. Provide concise, accurate, and direct answers in the user's language. Keep answers informative yet brief. Do not format as a quiz."
              }
            ]
          },
          generationConfig: {
            temperature: 0.4,
            topP: 0.9,
            maxOutputTokens: 384
          }
        };

        const { json, modelUsed } = await executeGeminiRequest(payload, false);
        const candidates = json && json.candidates;
        const text = candidates?.[0]?.content?.parts?.map((p) => p.text || "").join("").trim();
        if (text) {
          res.json({ success: true, answer: text, model: modelUsed });
          return;
        }
        res.status(500).json({ error: "Empty AI response." });
      } else if (operation === "quiz_generator") {
        const topic = typeof body.topic === "string" ? body.topic.trim() : "";
        if (!topic) {
          res.status(400).json({ error: "Topic cannot be empty." });
          return;
        }
        const prompt = buildQuizPrompt(topic);
        const payload = {
          contents: [{ parts: [{ text: prompt }] }],
          generationConfig: {
            temperature: 0.75,
            topP: 0.95,
            responseMimeType: "application/json"
          }
        };
        const { json, modelUsed } = await executeGeminiRequest(payload, true);
        const candidates = json && json.candidates;
        const rawJson = candidates?.[0]?.content?.parts?.[0]?.text || "";
        res.json({ success: true, rawJson, model: modelUsed });
      } else {
        res.status(400).json({ error: `Unknown operation: ${operation}` });
      }
    } catch (err) {
      console.error("[GeminiBackend] Request error:", err);
      const statusCode = err.code === "permission-denied" ? 403 : err.code === "resource-exhausted" ? 429 : 500;
      res.status(statusCode).json({ error: err.message || "Internal server error" });
    }
  }
);
