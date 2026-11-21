# LegalBot - Legal AI Assistant

LegalBot (NyayaAI) is an Android application designed for Indian Law research, BNS / IPC statutory mapping, bail provisions analysis, and legal notice drafting.

## Features
- **Statutory Analysis:** Analysis across BNS 2023, BNSS 2023, BSA 2023, IPC & CrPC with verifiable citations.
- **AI Models Supported:** Google Gemini, Groq (Llama 3 / DeepSeek), OpenAI GPT-4o.
- **Document & Media Scanner:** OCR text extraction for legal briefs and documents.
- **Voice Assistant:** Native speech-to-text and text-to-speech support for legal queries.
- **Legal Tools:** BNS/IPC mapper, Bail & CrPC/BNSS guides, and Notice Drafter.

## Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17+ / JBR Java 25
- Android SDK 35

## Getting Started
1. Clone the repository:
   ```bash
   git clone https://github.com/krushna9506/Legal_Bot.git
   ```
2. Open in Android Studio and sync Gradle.
3. Configure API keys in `local.properties`:
   ```properties
   GEMINI_API_KEY=your_gemini_api_key
   GROQ_API_KEY=your_groq_api_key
   OPENAI_API_KEY=your_openai_api_key
   ```
4. Build and run on an Android Virtual Device (AVD) or physical device.
