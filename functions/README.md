# BrainQuizAI Firebase Cloud Functions (Gemini Server-Side Backend)

This folder contains the secure production-grade server-side architecture for BrainQuizAI Gemini AI features:
- **AI Quick Answer** (`geminiQuickAnswer`)
- **AI Quiz Generator** (`geminiQuizGenerator`)
- **Unified Service** (`geminiAiService`)
- **REST Endpoint** (`geminiApi`)

## Security Architecture
The Android client APK contains **zero** Gemini API keys or secrets.
All AI operations are executed server-side.
Requests must be accompanied by an authenticated Firebase User ID token (`request.auth`).
Rate limiting is enforced at 20 requests/minute per UID.

## Setting the Server-Side Secret in Production
To configure the Gemini API key in Firebase Secret Manager:
```bash
firebase functions:secrets:set GEMINI_API_KEY
```

For local Firebase Emulator testing:
Create `functions/.env.local` (ignored by Git):
```bash
GEMINI_API_KEY=your_key_here
```
