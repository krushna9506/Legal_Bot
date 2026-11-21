package com.legalai.assistant.util;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.TypefaceSpan;
import android.util.TypedValue;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.legalai.assistant.R;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class LegalTextFormatter {

    public static class Block {
        public static final int TYPE_HEADER = 1;
        public static final int TYPE_BULLET = 2;
        public static final int TYPE_NUMBERED = 3;
        public static final int TYPE_QUOTE = 4;
        public static final int TYPE_BODY = 5;

        public final int type;
        public final CharSequence content;

        public Block(int type, CharSequence content) {
            this.type = type;
            this.content = content;
        }
    }

    private static final Pattern BOLD_PATTERN = Pattern.compile("\\*\\*(.*?)\\*\\*");
    private static final Pattern ITALIC_PATTERN = Pattern.compile("(?<!\\*)\\*(?!\\*)(.*?)(?<!\\*)\\*(?!\\*)");
    private static final Pattern CODE_PATTERN = Pattern.compile("`([^`]+)`");
    private static final Pattern NUMBERED_PATTERN = Pattern.compile("^(\\d+)[.)]\\s+(.*)");
    private static final Pattern STATUTE_PATTERN = Pattern.compile("(?i)\\b(Section\\s+\\d+[A-Z]?|Sec\\.?\\s*\\d+[A-Z]?|BNS|BNSS|IPC|CrPC|BSA|IEA|CPC|Article\\s+\\d+)\\b");

    /**
     * Parses raw legal AI response text into structured blocks suitable for line-by-line animated rendering.
     */
    public static List<Block> parseToBlocks(Context context, String rawText) {
        List<Block> blocks = new ArrayList<>();
        if (rawText == null || rawText.trim().isEmpty()) {
            return blocks;
        }

        String[] rawLines = rawText.split("\n");
        boolean prevWasEmpty = false;

        for (String line : rawLines) {
            String trimmed = line.trim();
            if (trimmed.isEmpty()) {
                if (!prevWasEmpty) {
                    prevWasEmpty = true;
                }
                continue;
            }
            prevWasEmpty = false;

            // 1. Headings: ### Header or ## Header or # Header or **Header:** on isolated line
            if (trimmed.startsWith("#")) {
                String headerText = trimmed.replaceFirst("^#+\\s*", "");
                CharSequence formatted = formatInline(context, headerText, false, null);
                blocks.add(new Block(Block.TYPE_HEADER, formatted));
            } else if (trimmed.startsWith(">") || trimmed.startsWith("⚖️ **Legal Domain") || trimmed.toLowerCase().contains("disclaimer:")) {
                String quoteText = trimmed.replaceFirst("^>\\s*", "");
                CharSequence formatted = formatInline(context, quoteText, false, null);
                blocks.add(new Block(Block.TYPE_QUOTE, formatted));
            } else if (trimmed.startsWith("* ") || trimmed.startsWith("- ") || trimmed.startsWith("• ") || trimmed.startsWith("+ ")) {
                String bulletText = trimmed.substring(2).trim();
                CharSequence formatted = formatInline(context, bulletText, true, "• ");
                blocks.add(new Block(Block.TYPE_BULLET, formatted));
            } else {
                Matcher numMatcher = NUMBERED_PATTERN.matcher(trimmed);
                if (numMatcher.matches()) {
                    String numStr = numMatcher.group(1) + ". ";
                    String rest = numMatcher.group(2);
                    CharSequence formatted = formatInline(context, rest, true, numStr);
                    blocks.add(new Block(Block.TYPE_NUMBERED, formatted));
                } else if (trimmed.startsWith("**") && (trimmed.endsWith("**") || trimmed.endsWith("**:")) && trimmed.length() < 60) {
                    // Short bold line functioning as sub-header
                    String clean = trimmed.replaceAll("^\\*\\*|\\*\\*:?$", "").trim();
                    if (trimmed.endsWith(":")) clean += ":";
                    CharSequence formatted = formatInline(context, clean, false, null);
                    blocks.add(new Block(Block.TYPE_HEADER, formatted));
                } else {
                    // Regular paragraph line
                    CharSequence formatted = formatInline(context, trimmed, false, null);
                    blocks.add(new Block(Block.TYPE_BODY, formatted));
                }
            }
        }

        return blocks;
    }

    /**
     * Formats inline Markdown (bold, italic, code) and highlights statutory references.
     */
    public static CharSequence formatInline(Context context, String text, boolean isList, String prefix) {
        SpannableStringBuilder builder = new SpannableStringBuilder();

        int prefixLen = 0;
        if (isList && prefix != null) {
            builder.append(prefix);
            prefixLen = prefix.length();
            // Accent the bullet/number prefix
            builder.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(context, R.color.ios_blue)),
                    0,
                    prefixLen,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            builder.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    0,
                    prefixLen,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // Process markdown markers
        String clean = text;
        List<SpanRange> boldRanges = new ArrayList<>();
        List<SpanRange> italicRanges = new ArrayList<>();
        List<SpanRange> codeRanges = new ArrayList<>();

        // Replace bold **text**
        StringBuilder cleanBuilder = new StringBuilder();
        int lastIndex = 0;
        Matcher boldMatcher = BOLD_PATTERN.matcher(clean);
        while (boldMatcher.find()) {
            cleanBuilder.append(clean, lastIndex, boldMatcher.start());
            int start = cleanBuilder.length();
            String inner = boldMatcher.group(1);
            cleanBuilder.append(inner);
            int end = cleanBuilder.length();
            boldRanges.add(new SpanRange(start, end));
            lastIndex = boldMatcher.end();
        }
        cleanBuilder.append(clean.substring(lastIndex));
        clean = cleanBuilder.toString();

        // Replace italic *text*
        cleanBuilder = new StringBuilder();
        lastIndex = 0;
        Matcher italicMatcher = ITALIC_PATTERN.matcher(clean);
        while (italicMatcher.find()) {
            cleanBuilder.append(clean, lastIndex, italicMatcher.start());
            int start = cleanBuilder.length();
            String inner = italicMatcher.group(1);
            cleanBuilder.append(inner);
            int end = cleanBuilder.length();
            italicRanges.add(new SpanRange(start, end));
            lastIndex = italicMatcher.end();
        }
        cleanBuilder.append(clean.substring(lastIndex));
        clean = cleanBuilder.toString();

        // Replace code `text`
        cleanBuilder = new StringBuilder();
        lastIndex = 0;
        Matcher codeMatcher = CODE_PATTERN.matcher(clean);
        while (codeMatcher.find()) {
            cleanBuilder.append(clean, lastIndex, codeMatcher.start());
            int start = cleanBuilder.length();
            String inner = codeMatcher.group(1);
            cleanBuilder.append(inner);
            int end = cleanBuilder.length();
            codeRanges.add(new SpanRange(start, end));
            lastIndex = codeMatcher.end();
        }
        cleanBuilder.append(clean.substring(lastIndex));
        clean = cleanBuilder.toString();

        int textStartInBuilder = builder.length();
        builder.append(clean);

        // Apply Bold spans
        for (SpanRange r : boldRanges) {
            builder.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    textStartInBuilder + r.start,
                    textStartInBuilder + r.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // Apply Italic spans
        for (SpanRange r : italicRanges) {
            builder.setSpan(
                    new StyleSpan(Typeface.ITALIC),
                    textStartInBuilder + r.start,
                    textStartInBuilder + r.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // Apply Code / Mono spans
        for (SpanRange r : codeRanges) {
            builder.setSpan(
                    new TypefaceSpan("monospace"),
                    textStartInBuilder + r.start,
                    textStartInBuilder + r.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            builder.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(context, R.color.ios_purple)),
                    textStartInBuilder + r.start,
                    textStartInBuilder + r.end,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        // Highlight Indian legal citations (e.g., Section 103, BNS, IPC, BNSS)
        Matcher statuteMatcher = STATUTE_PATTERN.matcher(clean);
        while (statuteMatcher.find()) {
            int sStart = textStartInBuilder + statuteMatcher.start();
            int sEnd = textStartInBuilder + statuteMatcher.end();
            builder.setSpan(
                    new ForegroundColorSpan(ContextCompat.getColor(context, R.color.ios_blue)),
                    sStart,
                    sEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
            builder.setSpan(
                    new StyleSpan(Typeface.BOLD),
                    sStart,
                    sEnd,
                    Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }

        return builder;
    }

    /**
     * Creates a styled, theme-aware TextView for a specific legal block.
     */
    public static TextView createBlockView(Context context, Block block) {
        TextView tv = new TextView(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
        );

        int dp10 = dp(context, 10);
        int dp8 = dp(context, 8);
        int dp6 = dp(context, 6);
        int dp4 = dp(context, 4);
        int dp3 = dp(context, 3);

        switch (block.type) {
            case Block.TYPE_HEADER:
                params.setMargins(0, dp8, 0, dp4);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f);
                tv.setTextColor(ContextCompat.getColor(context, R.color.ios_blue));
                tv.setTypeface(Typeface.create("sans-serif-medium", Typeface.BOLD));
                tv.setLineSpacing(0, 1.2f);
                break;

            case Block.TYPE_BULLET:
            case Block.TYPE_NUMBERED:
                params.setMargins(dp6, dp3, 0, dp3);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                tv.setTextColor(ContextCompat.getColor(context, R.color.ios_text_primary));
                tv.setLineSpacing(0, 1.25f);
                break;

            case Block.TYPE_QUOTE:
                params.setMargins(0, dp6, 0, dp6);
                tv.setPadding(dp10, dp6, dp10, dp6);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 12.5f);
                tv.setTextColor(ContextCompat.getColor(context, R.color.ios_text_secondary));
                tv.setBackgroundResource(R.drawable.bg_tag_statute);
                tv.setTypeface(Typeface.create("sans-serif", Typeface.ITALIC));
                tv.setLineSpacing(0, 1.2f);
                break;

            case Block.TYPE_BODY:
            default:
                params.setMargins(0, dp3, 0, dp3);
                tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, 13.5f);
                tv.setTextColor(ContextCompat.getColor(context, R.color.ios_text_primary));
                tv.setLineSpacing(0, 1.25f);
                break;
        }

        tv.setText(block.content);
        tv.setLayoutParams(params);
        return tv;
    }

    private static int dp(Context context, int dpValue) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dpValue,
                context.getResources().getDisplayMetrics()
        );
    }

    private static class SpanRange {
        final int start;
        final int end;

        SpanRange(int start, int end) {
            this.start = start;
            this.end = end;
        }
    }
}
