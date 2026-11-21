package com.legalai.assistant.knowledge;

import java.util.HashMap;
import java.util.Map;

public class IndianLegalKnowledge {

    public static final String REFUSAL_MESSAGE =
            "⚖️ **Legal Domain Restriction**\n\n" +
            "I am **LegalBot**, an AI assistant dedicated **strictly to legal information, statutory provisions, court procedures, and law-related drafting** under the Indian legal system.\n\n" +
            "I cannot assist with general knowledge, coding, entertainment, sports, or non-legal topics.\n\n" +
            "**Please ask a question related to Indian Law:**\n" +
            "- 📜 **Statute Guidance:** BNS 2023, legacy IPC, BNSS 2023, CrPC, NI Act 138, IT Act 2000\n" +
            "- ⚖️ **Criminal & Civil Rights:** Bail rights (BNSS § 480/482), FIR registration, police powers\n" +
            "- 📑 **Legal Drafting:** Eviction notices, cheque bounce demand notices, consumer complaints\n" +
            "- 🔍 **Contract Audit:** Inspect agreements, covenants, indemnity, and breach clauses";

    public static final String GREETING_MESSAGE =
            "👋 **Hello! I am LegalBot.**\n\n" +
            "Your dedicated AI Legal Assistant specialized in the **Indian Legal System**, statutory codes, and court documentation.\n\n" +
            "**How can I assist your legal work today?**\n" +
            "- 📜 **Statute Mapping:** Cross-referencing BNS (2023) and legacy IPC (1860)\n" +
            "- ⚖️ **Procedural Law:** Bail under BNSS § 480/482/483, FIR & arrest guidelines\n" +
            "- 📑 **Document Drafting:** Sec 138 NI Act notices, Tenancy termination, Contract clauses\n" +
            "- 🔍 **Document Audit:** Review legal agreements, FIR photos, or charge sheets\n\n" +
            "*Feel free to ask a legal query or attach a legal document for instant review.*";

    public static final String SYSTEM_PROMPT =
            "CRITICAL DOMAIN RESTRICTION - STRICTLY INDIAN LAW ONLY:\n" +
            "You are LegalBot (NyayaAI), a court-ready, rigorous Legal Assistant specialized EXCLUSIVELY in the legal system of the Republic of India.\n\n" +
            "ABSOLUTE DOMAIN RESTRICTION - ZERO TOLERANCE FOR NON-LEGAL QUERIES:\n" +
            "1. You MUST ONLY discuss law, statutory provisions, court procedures, FIR/police investigations, bail rights, legal documentation, contract review, and Indian jurisprudence.\n" +
            "2. Under NO circumstances are you permitted to answer general knowledge questions, programming/code questions, cooking recipes, science, sports, entertainment, movies, general trivia, creative fiction, or casual chit-chat.\n" +
            "3. If the user asks ANY question or gives any prompt outside the domain of law, legal rights, disputes, or legal drafting, YOU MUST STRICTLY REFUSE TO ANSWER.\n" +
            "4. When refusing, your response MUST BE:\n" +
            "   \"I am LegalBot, an AI assistant dedicated strictly to legal information, statutory provisions, court procedures, and law-related drafting under the Indian legal system. I cannot assist with non-legal topics. Please ask a question related to law, statutes (e.g., BNS, IPC, BNSS, CrPC), bail rights, or legal documentation.\"\n" +
            "5. If the user sends a greeting (e.g., 'hello', 'hi', 'hey', 'namaste'), respond cordially as LegalBot, stating your strict role as an Indian Legal Assistant and asking how you can help with their legal case, statute research, or legal drafting.\n\n" +
            "LEGAL JURISDICTION & EXPERTISE:\n" +
            "1. The New Criminal Laws (effective July 1, 2024):\n" +
            "   - Bharatiya Nyaya Sanhita, 2023 (BNS) replacing Indian Penal Code, 1860 (IPC)\n" +
            "   - Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS) replacing Code of Criminal Procedure, 1973 (CrPC)\n" +
            "   - Bharatiya Sakshya Adhiniyam, 2023 (BSA) replacing Indian Evidence Act, 1872 (IEA)\n" +
            "2. Key Statutory Acts: Negotiable Instruments Act 1881 (Sec 138), Consumer Protection Act 2019, Transfer of Property Act 1882 / State Rent Control Acts, Arbitration and Conciliation Act 1996, Companies Act 2013, Information Technology Act 2000, Motor Vehicles Act, Family Law, Labor Laws.\n" +
            "3. Landmark Precedents of the Supreme Court of India and High Courts.\n\n" +
            "STRICT GROUNDING & ZERO-HALLUCINATION RULES:\n" +
            "- Always cite the corresponding BNS section alongside the legacy IPC section for cross-reference.\n" +
            "- Explicitly specify whether an offense is Bailable or Non-Bailable, Cognizable or Non-Cognizable, and Triable by which Court (e.g. Magistrate of the First Class or Court of Session).\n" +
            "- Never fabricate case citations or non-existent statutory clauses. If precedent is uncertain, clarify that judicial interpretation is evolving.\n" +
            "- For legal notice requests, format formal legal notices with structured cause of action, statutory notice period (e.g., 15 days under Sec 138 NI Act), demand, and reservation of remedies.\n" +
            "- Always conclude with a concise reminder that this is research assistance and not an advocate-client engagement under the Advocates Act, 1961.";

    private static final Map<String, String> IPC_TO_BNS_MAP = new HashMap<>();
    private static final Map<String, String> CRPC_TO_BNSS_MAP = new HashMap<>();

    static {
        // IPC to BNS mappings
        IPC_TO_BNS_MAP.put("302", "Section 103 BNS (Punishment for Murder - Death or Life Imprisonment + Fine)");
        IPC_TO_BNS_MAP.put("304A", "Section 106 BNS (Causing death by negligence / Rash driving)");
        IPC_TO_BNS_MAP.put("307", "Section 109 BNS (Attempt to murder)");
        IPC_TO_BNS_MAP.put("376", "Section 64 BNS (Punishment for Rape)");
        IPC_TO_BNS_MAP.put("379", "Section 303(2) BNS (Theft - Imprisonment up to 3 years or fine or both)");
        IPC_TO_BNS_MAP.put("420", "Section 318(4) BNS (Cheating and dishonestly inducing delivery of property - Up to 7 years + Fine)");
        IPC_TO_BNS_MAP.put("406", "Section 316 BNS (Criminal Breach of Trust)");
        IPC_TO_BNS_MAP.put("498A", "Section 85 & 86 BNS (Cruelty by Husband or relatives of husband)");
        IPC_TO_BNS_MAP.put("506", "Section 351 BNS (Criminal Intimidation)");
        IPC_TO_BNS_MAP.put("499", "Section 356 BNS (Defamation - includes community service option under BNS)");
        IPC_TO_BNS_MAP.put("124A", "Section 152 BNS (Acts endangering sovereignty, unity, and integrity of India)");
        IPC_TO_BNS_MAP.put("323", "Section 115(2) BNS (Voluntarily causing hurt)");
        IPC_TO_BNS_MAP.put("34", "Section 3(5) BNS (Joint liability / Common intention)");
        IPC_TO_BNS_MAP.put("120B", "Section 61(2) BNS (Criminal Conspiracy)");

        // CrPC to BNSS mappings
        CRPC_TO_BNSS_MAP.put("154", "Section 173 BNSS (Information in cognizable cases / FIR & Zero FIR / e-FIR)");
        CRPC_TO_BNSS_MAP.put("41", "Section 35 BNSS (When police may arrest without warrant)");
        CRPC_TO_BNSS_MAP.put("41A", "Section 35(3) BNSS (Notice of appearance before police officer)");
        CRPC_TO_BNSS_MAP.put("167", "Section 187 BNSS (Procedure when investigation cannot be completed in 24 hours / Police custody)");
        CRPC_TO_BNSS_MAP.put("437", "Section 480 BNSS (Bail in non-bailable offences by Magistrate)");
        CRPC_TO_BNSS_MAP.put("438", "Section 482 BNSS (Direction for grant of bail to person apprehending arrest / Anticipatory Bail)");
        CRPC_TO_BNSS_MAP.put("439", "Section 483 BNSS (Special powers of High Court or Court of Session regarding bail)");
        CRPC_TO_BNSS_MAP.put("173", "Section 193 BNSS (Report of police officer on completion of investigation / Chargesheet)");
        CRPC_TO_BNSS_MAP.put("82", "Section 84 BNSS (Proclamation for person absconding)");
    }

    public static boolean isGreeting(String query) {
        if (query == null) return false;
        String trimmed = query.trim().toLowerCase().replaceAll("[^a-zA-Z0-9 ]", "").trim();
        return trimmed.equals("hi") ||
               trimmed.equals("hello") ||
               trimmed.equals("hey") ||
               trimmed.equals("namaste") ||
               trimmed.equals("namaskar") ||
               trimmed.equals("good morning") ||
               trimmed.equals("good afternoon") ||
               trimmed.equals("good evening") ||
               trimmed.equals("who are you") ||
               trimmed.equals("what can you do") ||
               trimmed.equals("help") ||
               trimmed.equals("start") ||
               trimmed.equals("introduce yourself");
    }

    public static boolean hasLegalContext(String query) {
        if (query == null) return false;
        String lower = query.toLowerCase();
        String[] legalKeywords = {
            "law", "legal", "court", "judge", "justice", "statute", "section", "act",
            "bns", "ipc", "bnss", "crpc", "bsa", "cpc", "fir", "police", "arrest",
            "bail", "anticipatory", "custody", "warrant", "summons", "chargesheet",
            "magistrate", "session", "high court", "supreme court", "tribunal",
            "advocate", "lawyer", "attorney", "counsel", "client", "bar council",
            "cheque", "dishonour", "bounce", "138", "ni act", "promissory",
            "theft", "murder", "cheat", "fraud", "extortion", "defamation", "assault",
            "cruelty", "dowry", "498a", "302", "307", "376", "420", "506", "304",
            "tenant", "landlord", "rent", "eviction", "lease", "property", "deed",
            "registry", "possession", "dispute", "suit", "plaintiff", "defendant",
            "contract", "agreement", "breach", "indemnity", "damages", "liability",
            "consumer", "deficiency", "rera", "arbitrat", "mediation", "settlement",
            "divorce", "maintenance", "alimony", "custody", "marriage", "succession",
            "will", "probate", "inheritance", "partition", "affidavit", "power of attorney",
            "notary", "notice", "demand notice", "petition", "appeal", "revision", "writ",
            "pil", "quash", "stay", "injunction", "limitation", "cognizable", "bailable",
            "cyber", "it act", "copyright", "trademark", "patent", "ipr", "infringement",
            "tax", "gst", "company", "insolvency", "ibc", "nclt", "penalty", "fine",
            "imprisonment", "offence", "crime", "illegal", "unlawful", "rights", "accused",
            "victim", "witness", "trial", "evidence", "testimony", "confession", "investigation"
        };
        for (String kw : legalKeywords) {
            if (lower.contains(kw)) {
                return true;
            }
        }
        return false;
    }

    public static boolean isExplicitNonLegalQuery(String query) {
        if (query == null) return false;
        String lower = query.toLowerCase();

        // If query clearly possesses legal context, it is allowed
        if (hasLegalContext(query)) {
            return false;
        }

        String[] nonLegalTriggers = {
            "tell me a joke", "make me laugh", "funny joke",
            "write a poem", "write poem", "write a song", "sing a song", "lyrics of",
            "write code", "python code", "write a python", "java code", "javascript code",
            "html code", "css code", "react code", "c++ code", "php code", "function to sort",
            "recipe for", "how to cook", "how to make cake", "ingredients for",
            "who won the match", "cricket score", "football score", "world cup",
            "movie review", "who is the actor", "who acted in", "box office",
            "calculate ", "what is 2+", "what is 1+", "solve math",
            "weather in", "forecast for", "horoscope"
        };

        for (String trigger : nonLegalTriggers) {
            if (lower.contains(trigger)) {
                return true;
            }
        }
        return false;
    }

    public static String getStatuteMappingForQuery(String query) {
        if (query == null) return null;
        String lower = query.toLowerCase();

        for (Map.Entry<String, String> entry : IPC_TO_BNS_MAP.entrySet()) {
            if (lower.contains("ipc " + entry.getKey()) || lower.contains(entry.getKey() + " ipc") ||
                lower.contains("section " + entry.getKey())) {
                return "Legacy IPC § " + entry.getKey() + " ➔ " + entry.getValue();
            }
        }

        for (Map.Entry<String, String> entry : CRPC_TO_BNSS_MAP.entrySet()) {
            if (lower.contains("crpc " + entry.getKey()) || lower.contains(entry.getKey() + " crpc") ||
                lower.contains("section " + entry.getKey())) {
                return "Legacy CrPC § " + entry.getKey() + " ➔ " + entry.getValue();
            }
        }

        if (lower.contains("cheque") || lower.contains("138") || lower.contains("dishonour")) {
            return "Section 138, Negotiable Instruments Act, 1881";
        }
        if (lower.contains("bail") || lower.contains("anticipatory")) {
            return "BNSS § 480, 482 & 483 (Formerly CrPC § 437, 438, 439)";
        }
        if (lower.contains("fir") || lower.contains("zero fir")) {
            return "BNSS § 173 (Formerly CrPC § 154)";
        }
        return null;
    }

    public static String generateOfflineLegalResponse(String query) {
        if (query == null) return REFUSAL_MESSAGE;
        String lower = query.toLowerCase().trim();

        // 1. Direct greeting check
        if (isGreeting(query)) {
            return GREETING_MESSAGE;
        }

        // 2. Explicit non-legal check
        if (isExplicitNonLegalQuery(query)) {
            return REFUSAL_MESSAGE;
        }

        // 3. Cheque bounce / Sec 138 NI Act
        if (lower.contains("cheque") || lower.contains("138") || lower.contains("dishonour") || lower.contains("bounce")) {
            return "### Statutory Analysis: Section 138, Negotiable Instruments Act, 1881\n\n" +
                    "**1. Essential Ingredients for Offence:**\n" +
                    "- The cheque must be drawn for discharge of a legally enforceable debt or liability.\n" +
                    "- Presented to bank within validity period (3 months).\n" +
                    "- Cheque returned unpaid due to insufficiency of funds or exceeding arrangement.\n\n" +
                    "**2. Mandatory Timelines & Procedure:**\n" +
                    "- **Statutory Notice:** Demand notice must be issued in writing within **30 days** of receiving the return memo from bank.\n" +
                    "- **Grace Period:** Drawer gets **15 days** from date of receipt to make payment.\n" +
                    "- **Cause of Action:** If payment is not made within 15 days, cause of action arises.\n" +
                    "- **Complaint Filing:** Criminal complaint must be filed before Judicial Magistrate within **1 month** thereafter (Section 142 NI Act).\n\n" +
                    "**3. Penalty:** Imprisonment up to 2 years, or fine up to twice the cheque amount, or both.\n\n" +
                    "*To activate live LLM drafting and customized case laws, set your free Gemini or Groq API key in Settings.*";
        }

        // 4. Bail provisions under BNSS
        if (lower.contains("bail") || lower.contains("anticipatory") || lower.contains("438") || lower.contains("482")) {
            return "### Bail Provisions under Bharatiya Nagarik Suraksha Sanhita, 2023 (BNSS)\n\n" +
                    "**1. Regular Bail by Magistrate (Section 480 BNSS / Formerly Sec 437 CrPC):**\n" +
                    "- Applicable for non-bailable offences. Release is discretionary unless the offence carries death or life imprisonment, or the accused has prior repeat convictions.\n\n" +
                    "**2. Anticipatory Bail (Section 482 BNSS / Formerly Sec 438 CrPC):**\n" +
                    "- Filed before Court of Session or High Court when apprehending arrest for a non-bailable offence.\n" +
                    "- Key factors: Nature & gravity of accusation, role of applicant, possibility of fleeing justice.\n" +
                    "- Landmark Precedent: *Sushila Aggarwal v. State (NCT of Delhi) (2020 5 SCC 1)* - Protection can continue till end of trial.\n\n" +
                    "**3. Special Powers of High Court / Sessions Court (Section 483 BNSS / Formerly Sec 439 CrPC):**\n" +
                    "- Wide discretion to direct release on bail with conditions (surrender of passport, cooperation with IO).\n\n" +
                    "*Note: For personalized application drafting with live reasoning, configure your free API key in Settings.*";
        }

        // 5. BNS vs IPC Criminal code comparisons
        if (lower.contains("bns") || lower.contains("ipc") || lower.contains("cheat") || lower.contains("murder") || lower.contains("theft")) {
            StringBuilder sb = new StringBuilder();
            sb.append("### BNS (2023) vs IPC (1860) Criminal Law Reference\n\n");
            sb.append("The Bharatiya Nyaya Sanhita, 2023 came into full effect on **July 1, 2024**.\n\n");
            sb.append("| Offence | Legacy IPC Section | New BNS Section | Cognizability & Bail |\n");
            sb.append("| :--- | :--- | :--- | :--- |\n");
            sb.append("| Murder | Section 302 | **Section 103** | Cognizable / Non-Bailable |\n");
            sb.append("| Culpable Homicide | Section 304 | **Section 105** | Cognizable / Non-Bailable |\n");
            sb.append("| Causing death by negligence | Section 304A | **Section 106** | Cognizable / Bailable |\n");
            sb.append("| Cheating / Fraud | Section 420 | **Section 318(4)** | Cognizable / Non-Bailable |\n");
            sb.append("| Theft | Section 379 | **Section 303(2)** | Cognizable / Non-Bailable |\n");
            sb.append("| Criminal Breach of Trust | Section 406 | **Section 316** | Cognizable / Non-Bailable |\n");
            sb.append("| Cruelty by Husband/In-laws | Section 498A | **Section 85 & 86** | Cognizable / Non-Bailable |\n");
            sb.append("| Defamation | Section 499 | **Section 356** | Non-Cognizable / Bailable |\n\n");
            sb.append("FIRs registered after July 1, 2024 are charged under BNS sections, while crimes committed prior remain adjudicated under IPC.\n\n");
            sb.append("*Configure your free Gemini or Groq key in Settings for instant statute interpretation & petition drafting.*");
            return sb.toString();
        }

        // 6. Tenancy & Eviction
        if (lower.contains("rent") || lower.contains("tenant") || lower.contains("eviction")) {
            return "### Tenancy & Eviction Guidelines under Indian Law\n\n" +
                    "**1. Governing Framework:**\n" +
                    "- Regulated under respective State Rent Control Acts (e.g., Delhi Rent Control Act, Maharashtra Rent Control Act) and Transfer of Property Act, 1882 (Section 106).\n\n" +
                    "**2. Notice of Termination (Section 106 Transfer of Property Act):**\n" +
                    "- Monthly tenancy requires a **15-day notice** expiring with the end of a month of tenancy.\n" +
                    "- Agricultural or manufacturing leases require a **6-month notice**.\n\n" +
                    "**3. Valid Grounds for Eviction:**\n" +
                    "- Non-payment of rent despite statutory notice period.\n" +
                    "- Sub-letting without written landlord authorization.\n" +
                    "- Bona fide personal necessity of the landlord.\n" +
                    "- Substantial damage / alteration to premises.\n\n" +
                    "*Use the Notice Drafter chip above to draft a customized Tenancy Notice!*";
        }

        // 7. FIR, Police Arrest & Safeguards
        if (lower.contains("fir") || lower.contains("police") || lower.contains("arrest") || lower.contains("warrant")) {
            return "### FIR & Arrest Safeguards under BNSS, 2023\n\n" +
                    "**1. First Information Report (Section 173 BNSS / Formerly Sec 154 CrPC):**\n" +
                    "- Mandatory registration for cognizable offences.\n" +
                    "- **Zero FIR:** Can be lodged at any police station regardless of territorial jurisdiction.\n" +
                    "- **e-FIR:** Introduced under BNSS with digital submission within 3 days for verification.\n\n" +
                    "**2. Arrest Safeguards (Section 35 BNSS & *Arnesh Kumar* Guidelines):**\n" +
                    "- Offenses punishable up to 7 years require prior notice of appearance before arrest, unless exceptional conditions exist.\n" +
                    "- Arrest memo must be prepared and signed by at least one witness/family member.\n" +
                    "- Right to inform a friend or relative and right to medical examination (Section 53 BNSS).\n\n" +
                    "*Set an AI API key in Settings to analyze specific charge sheets or FIR texts.*";
        }

        // 8. General legal context check
        if (hasLegalContext(query)) {
            return "### LegalBot Procedural Guidance (Indian Law)\n\n" +
                    "I have reviewed your legal inquiry regarding: **" + query.trim() + "**\n\n" +
                    "**Key Indian Procedural Steps to Consider:**\n" +
                    "1. **Governing Statute:** Determine whether the matter is covered under the new criminal codes (BNS / BNSS / BSA, 2023) or civil / commercial enactments.\n" +
                    "2. **Limitation Period:** Verify timelines under the Limitation Act, 1963 (e.g., 3 years for civil recovery, 30 days for cheque return notices, 90 days for statutory appeals).\n" +
                    "3. **Jurisdiction & Forum:** Confirm whether the competent forum is the District Court, High Court, Consumer Commission, or Special Tribunal (NCLT/RERA/DRT).\n\n" +
                    "👉 **Document Audit:** You can attach camera documents, FIR photos, or agreements for OCR review and legal risk evaluation.";
        }

        // 9. If query lacks any legal context, strictly enforce domain restriction
        return REFUSAL_MESSAGE;
    }
}
