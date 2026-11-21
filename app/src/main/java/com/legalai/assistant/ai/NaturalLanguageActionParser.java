package com.legalai.assistant.ai;

import com.legalai.assistant.data.model.TaskItem;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NaturalLanguageActionParser {

    public static class ParsedAction {
        public String type; // "TASK" or "NOTE"
        public String title;
        public String description;
        public String dueDate;
        public String priority;

        public ParsedAction(String type, String title, String description, String dueDate, String priority) {
            this.type = type;
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.priority = priority;
        }
    }

    public static List<ParsedAction> parseActions(String text) {
        List<ParsedAction> actions = new ArrayList<>();
        if (text == null || text.isEmpty()) return actions;

        // 1. Explicit Action tags: [ACTION: CREATE_TASK Title | Description | DueDate | Priority]
        Pattern actionPattern = Pattern.compile("\\[ACTION:\\s*CREATE_TASK\\s*([^|]+)\\|([^|]+)\\|([^|]+)\\|([^\\]]+)\\]", Pattern.CASE_INSENSITIVE);
        Matcher matcher = actionPattern.matcher(text);
        while (matcher.find()) {
            String title = matcher.group(1).trim();
            String desc = matcher.group(2).trim();
            String date = matcher.group(3).trim();
            String prio = matcher.group(4).trim();
            actions.add(new ParsedAction("TASK", title, desc, date, prio));
        }

        // 2. Intelligent statutory detection for Indian Law deadlines:
        String lower = text.toLowerCase();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();

        if (lower.contains("15 days") && (lower.contains("138") || lower.contains("cheque") || lower.contains("notice"))) {
            cal.add(Calendar.DAY_OF_YEAR, 15);
            actions.add(new ParsedAction("TASK",
                    "Sec 138 NI Act: 15-Day Payment Cure Window",
                    "Statutory 15-day period for drawer to make payment before criminal complaint arises.",
                    sdf.format(cal.getTime()),
                    "High"));
        } else if (lower.contains("30 days") && (lower.contains("notice") || lower.contains("statutory"))) {
            cal.add(Calendar.DAY_OF_YEAR, 30);
            actions.add(new ParsedAction("TASK",
                    "Statutory Notice Deadline / Filing Limitation",
                    "Mandatory 30-day statutory timeline for dispatch or filing under Indian limitation rules.",
                    sdf.format(cal.getTime()),
                    "High"));
        } else if (lower.contains("bail") && (lower.contains("anticipatory") || lower.contains("bnss") || lower.contains("crpc"))) {
            cal.add(Calendar.DAY_OF_YEAR, 3);
            actions.add(new ParsedAction("TASK",
                    "File Anticipatory Bail Application (BNSS § 482)",
                    "Prepare and file application before Sessions / High Court with case diary annexures.",
                    sdf.format(cal.getTime()),
                    "High"));
        }

        return actions;
    }
}
