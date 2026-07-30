package com.n1249874.slipstack.ocr;

import android.graphics.Rect;
import android.util.Log;

import com.google.mlkit.vision.text.Text;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiptParser {

    private static final String TAG = "ReceiptParser";

    public static class ParsedReceipt {
        public String merchantName = "";
        public String date = "";
        public double total = 0.0;
        public List<LineItem> lineItems = new ArrayList<>();
    }

    public static class LineItem {
        public String name;
        public double price;

        public LineItem(String name, double price) {
            this.name = name;
            this.price = price;
        }
    }

    // Matches any price: optional leading minus, currency symbol
    private static final Pattern ANY_PRICE = Pattern.compile(
            "(-?)[£EfS9sB]?(\\d{1,4}[.,]\\d{2})(?!\\d)");

    // Price alone at end of line (fallback for name-before-price receipts)
    private static final Pattern PRICE_END = Pattern.compile(
            "£?(\\d{1,4}\\.\\d{2})\\s*[A-Z]?\\s*$");

    // Excludes lines containing Cash, Change, Tendered to avoid misidentifying
    // payment info as the total
    private static final Pattern TOTAL_LINE = Pattern.compile(
            "(?:^|.*)" +
                    "(?!.*(?:cash|change|tendered))" +
                    "(?:total|balance\\s+due|amount\\s+due|grand\\s+total|to\\s+pay|" +
                    "net\\s+total|subtotal|net\\s+amount|amount\\s+payable)" +
                    "[\\s:*]*[£EfS9sB]?(\\d{1,4}[.,]\\d{2})",
            Pattern.CASE_INSENSITIVE);

    // Date patterns - improved for UK formats (including textual months and OCR
    // noise)
    // Group 1: Numeric (DD/MM/YYYY)
    // Group 2/3/4: Component-based Textual (DD MMM YYYY)
    private static final Pattern DATE_PATTERN = Pattern.compile(
            "(\\d{1,2}[/\\-.]\\s*?\\d{1,2}[/\\-.](?:\\d{4}|\\d{2}))" +
                    "|(\\d{1,2})\\s*?(Jan(?:uary)?|Feb(?:ruary)?|Mar(?:ch)?|Apr(?:il)?|May|Jun(?:e)?" +
                    "|Jul(?:y)?|Aug(?:ust)?|Sep(?:tember)?|Oct(?:ober)?|Nov(?:ember)?|Dec(?:ember)?)" +
                    "[\\s,\\-]*?(\\d{4}|\\d{2})",
            Pattern.CASE_INSENSITIVE);

    // Lines / name segments to silently skip
    private static final Pattern SKIP_PATTERN = Pattern.compile(
            "^(?:" +
                    "nectar\\s+price\\s+saving|nectar\\s+saving|price\\s+saving|" +
                    "vat\\s+number|vat\\s+reg|vat\\s+no|" +
                    "www\\.|http|@|tel:|phone:|fax:|" +
                    "thank|welcome|please|visit|check\\s+you|check\\s+the|lettuce|" +
                    "contactless|\\[icc?\\]|aid:|pan\\s+seq|merchant:|auth\\s+code:|tid:|" +
                    "pan\\s+sequence|no\\s+cardh|cardholder|" +
                    "cash|change|cashback|debit\\b|visa\\b|mastercard\\b|" +
                    "your\\s+savings|promotions|" +
                    "\\*{4,}|={3,}|-{3,}|#{3,}" +
                    ")",
            Pattern.CASE_INSENSITIVE);

    // Total keywords — used to validate name segments
    private static final Pattern TOTAL_KEYWORDS = Pattern.compile(
            "(?:balance\\s+due|amount\\s+due|grand\\s+total|subtotal|to\\s+pay|" +
                    "debit|mastercard|visa|cash|change|contactless)",
            Pattern.CASE_INSENSITIVE);

    // Hard footer — stop collecting items entirely
    private static final Pattern FOOTER_MARKER = Pattern.compile(
            "(?:my\\s+nectar\\s+summary|nectar\\s+summary|points\\s+earned\\s+on|" +
                    "previous\\s+points|please\\s+keep\\s+for\\s+your|published\\s+terms|" +
                    "thank\\s+you\\s+for\\s+your|100,000\\s+nectar|lettuce-know)",
            Pattern.CASE_INSENSITIVE);

    // Words in store headers/addresses
    private static final Set<String> HEADER_WORDS = new HashSet<>(Arrays.asList(
            "helping", "supermarket", "ltd", "plc", "limited", "holborn", "london",
            "station", "local", "store", "wandsworth", "southfields", "www", "vat",
            "everyone", "better", "eat", "charterhouse", "derby", "good food"));

    private static final int ROW_TOLERANCE_PX = 42;

    public static ParsedReceipt parse(Text ocrText) {
        ParsedReceipt result = new ParsedReceipt();

        List<String> rows = buildRowsByBoundingBox(ocrText);
        Log.d(TAG, "=== BB rows (" + rows.size() + ") ===");
        for (String r : rows)
            Log.d(TAG, "  BB| " + r);

        // Find merchant name
        for (String row : rows) {
            String clean = row.trim().replaceAll("^[-£*.\\s]+", ""); // also strip leading prices
            if (clean.isEmpty())
                continue;
            if (clean.matches("^[\\d\\s£.,/\\-:*()]+$"))
                continue;
            if (isHeaderLine(clean))
                continue;

            // Skip if the clean text starts with a price
            if (clean.matches("^\\d{1,4}\\.\\d{2}.*"))
                continue;

            result.merchantName = toTitleCase(clean.split("\\s{2,}")[0].trim());
            break;
        }
        Log.d(TAG, "Merchant: " + result.merchantName);

        // Parse rows for date, total, items
        for (String row : rows) {
            String trimmed = row.trim();
            if (trimmed.isEmpty())
                continue;
            String lower = trimmed.toLowerCase();

            // Total detection
            Matcher totalMatcher = TOTAL_LINE.matcher(trimmed);
            if (totalMatcher.find()) {
                try {
                    String val = totalMatcher.group(1).replace(",", ".");
                    double c = Double.parseDouble(val);
                    // Lock in the total if found
                    if (result.total == 0.0 || lower.contains("balance due") || lower.contains("to pay")) {
                        result.total = c;
                    }
                } catch (NumberFormatException ignored) {
                }
            }

            // Item extraction — handles BOTH "£PRICE NAME" and "NAME £PRICE" formats
            extractItemsFromRow(trimmed, result, result.merchantName);
        }

        StringBuilder fullTextBuilder = new StringBuilder();
        for (String r : rows)
            fullTextBuilder.append(r).append(" ");
        String fullText = fullTextBuilder.toString();

        if (result.date.isEmpty()) {
            result.date = magicSearchDate(fullText);
        }

        if (result.total == 0.0) {
            result.total = magicSearchTotal(fullText);
        }

        // Fallback: sum items only if we found absolutely no total on the receipt
        if (result.total == 0.0 && !result.lineItems.isEmpty()) {
            for (LineItem item : result.lineItems)
                if (item.price > 0)
                    result.total += item.price;
            result.total = Math.round(result.total * 100.0) / 100.0;
        }

        Log.d(TAG, "Final Date identified: " + result.date);
        Log.d(TAG, "Total: £" + result.total + "  Items: " + result.lineItems.size());
        for (LineItem li : result.lineItems)
            Log.d(TAG, "  → " + li.name + " £" + li.price);

        return result;
    }

    private static void extractItemsFromRow(String row, ParsedReceipt result, String merchantName) {
        // Find every price occurrence in the row
        Matcher m = ANY_PRICE.matcher(row);
        List<int[]> spans = new ArrayList<>();
        List<Double> values = new ArrayList<>();

        while (m.find()) {
            boolean isNeg = !m.group(1).isEmpty();
            spans.add(new int[] { m.start(), m.end(), isNeg ? 1 : 0 });
            try {
                values.add(Double.parseDouble(m.group(2)));
            } catch (NumberFormatException e) {
                values.add(0.0);
            }
        }

        if (spans.isEmpty())
            return;

        for (int i = 0; i < spans.size(); i++) {
            boolean isNegative = spans.get(i)[2] == 1;
            double price = values.get(i);
            if (price <= 0 || price >= 999)
                continue;

            int priceStart = spans.get(i)[0];
            int priceEnd = spans.get(i)[1];

            // Boundaries for name searching
            int prevBoundary = (i == 0) ? 0 : spans.get(i - 1)[1];
            int nextBoundary = (i + 1 < spans.size()) ? spans.get(i + 1)[0] : row.length();

            // Text BEFORE this price (e.g. "PRODUCT £1.30")
            String beforeText = row.substring(prevBoundary, priceStart).trim();
            beforeText = beforeText.replaceAll("^[*\\s]+", "").replaceAll("[.\\s*]+$", "").trim();

            // Text AFTER this price (e.g. "£1.30 PRODUCT")
            String afterText = row.substring(priceEnd, nextBoundary).trim();
            afterText = afterText.replaceAll("^[*\\s]+", "").replaceAll("[.\\s*]+$", "").trim();

            String name = "";
            if (isNegative) {
                // For negative prices (discounts), accept any text that isn't just numbers
                if (isValidNameCandidate(afterText, merchantName)) {
                    name = afterText;
                } else if (isValidNameCandidate(beforeText, merchantName)) {
                    name = beforeText;
                } else {
                    name = "Discount";
                }
                Log.d(TAG, "  Discount: '" + name + "' -£" + price);
                result.lineItems.add(new LineItem(toTitleCase(name), -price));
            } else {
                // Positive price: stricter validation
                String cleanedName = cleanItemName(beforeText.isEmpty() ? afterText : beforeText);
                if (isValidName(cleanedName, merchantName)) {
                    name = cleanedName;
                }

                if (!name.isEmpty()) {
                    Log.d(TAG, "  Item: '" + name + "' £" + price);
                    result.lineItems.add(new LineItem(toTitleCase(name), price));
                }
            }
        }
    }

    private static boolean isValidNameCandidate(String s, String merchantName) {
        return s.length() >= 2 && !s.matches("^[\\d\\s£.,/\\-:*()]+$") && !s.equalsIgnoreCase(merchantName);
    }

    private static boolean isValidName(String name, String merchantName) {
        if (name == null || name.length() < 2)
            return false;
        String lower = name.toLowerCase();
        if (name.matches("^[\\d\\s£.,/\\-:*()]+$"))
            return false; // all symbols/digits
        if (SKIP_PATTERN.matcher(name).find())
            return false;
        if (TOTAL_KEYWORDS.matcher(name).find())
            return false;
        // isHeaderLine check removed here to rescue items like Milk that appear near
        // store headers
        if (!merchantName.isEmpty() && name.equalsIgnoreCase(merchantName))
            return false;

        // Noise Filters: Reject purely financial unit/price noise
        if (lower.equals("each") || lower.contains("each £") || lower.matches("each\\s*\\d+"))
            return false;

        String[] strictBans = { "total", "total:", "balance", "balance due", "amount due", "cash", "change", "savings",
                "points" };
        for (String ban : strictBans)
            if (lower.equals(ban))
                return false;
        return true;
    }

    private static String cleanItemName(String name) {
        if (name == null)
            return "";
        String c = name.replace("Mi1k", "Milk")
                .replace("MiLk", "Milk")
                .replace("P0wder", "Powder")
                .replace("Strawberr1es", "Strawberries")
                .trim();

        // This handles cases where wide grouping merges products with footers/headers
        c = c.replaceAll(
                "(?i)(total|balance|due|to\\s*pay|cash|change|savings|clubcard|reward|points|refund|credit|debit)[\\s:*]*",
                "").trim();
        // Remove trailing currency/symbols after the word purge
        c = c.replaceAll("[£EfS9sB:\\s-]+$", "").trim();
        return c;
    }

    private static List<String> buildRowsByBoundingBox(Text ocrText) {
        List<int[]> positions = new ArrayList<>();
        List<String> texts = new ArrayList<>();

        for (Text.TextBlock block : ocrText.getTextBlocks()) {
            for (Text.Line line : block.getLines()) {
                Rect box = line.getBoundingBox();
                if (box == null)
                    continue;
                String text = line.getText().trim();
                if (text.isEmpty())
                    continue;
                int yCenter = (box.top + box.bottom) / 2;
                positions.add(new int[] { yCenter, box.left, texts.size() });
                texts.add(text);
            }
        }

        if (positions.isEmpty())
            return new ArrayList<>();
        positions.sort((a, b) -> a[0] != b[0] ? a[0] - b[0] : a[1] - b[1]);

        List<String> rows = new ArrayList<>();
        List<int[]> group = new ArrayList<>();
        int currentY = positions.get(0)[0];

        for (int[] pos : positions) {
            if (Math.abs(pos[0] - currentY) <= ROW_TOLERANCE_PX) {
                group.add(pos);
            } else {
                rows.add(joinGroup(group, texts));
                group = new ArrayList<>();
                group.add(pos);
                currentY = pos[0];
            }
        }
        if (!group.isEmpty())
            rows.add(joinGroup(group, texts));
        return rows;
    }

    private static String joinGroup(List<int[]> group, List<String> texts) {
        StringBuilder sb = new StringBuilder();
        for (int[] pos : group) {
            if (sb.length() > 0)
                sb.append("  ");
            sb.append(texts.get(pos[2]));
        }
        return sb.toString();
    }

    private static boolean isHeaderLine(String line) {
        String lower = line.toLowerCase();
        for (String word : HEADER_WORDS) {
            if (lower.contains(word))
                return true;
        }
        // Mostly digits (phone, postcode, barcode)
        String digits = line.replaceAll("[^0-9]", "");
        if (digits.length() > 5 && (double) digits.length() / line.trim().length() > 0.45)
            return true;
        return false;
    }

    public static String magicSearchDate(String fullText) {
        if (fullText == null || fullText.isEmpty())
            return "";
        // normalise spaces and punctuation
        String upper = fullText.toUpperCase().trim();

        String months = "JAN|FEB|MAR|APR|MAY|JUN|JUL|AUG|SEP|OCT|NOV|DEC";

        // 1. "Sainsbury's": Specific regex for mashed strings (DDMMMYYYY)
        // Updated to handle 08APR2026 clearly
        Pattern pSniper = Pattern.compile("([0-9OIS]{1,2})(" + months + ")([0-9OIS]{2,4})");
        Matcher mSniper = pSniper.matcher(upper.replaceAll("\\s", ""));
        if (mSniper.find()) {
            String d = fuzzyFix(mSniper.group(1));
            String m = mSniper.group(2);
            String y = fuzzyFix(mSniper.group(3));
            try {
                int dVal = Integer.parseInt(d);
                if (dVal >= 1 && dVal <= 31) {
                    return formatConfirmedDate(d, m, y);
                }
            } catch (Exception ignored) {
            }
        }

        // 2. Global Proximity Search (50-char neighborhood)
        Pattern monthPattern = Pattern.compile(months);
        Matcher mm = monthPattern.matcher(upper);
        String bestD = "", bestM = "", bestY = "";
        int bestScore = -1;

        while (mm.find()) {
            String monthFound = mm.group();
            int monthPos = mm.start();
            // Expanded neighborhood (50 chars) to catch split rows
            int start = Math.max(0, monthPos - 50);
            int end = Math.min(upper.length(), monthPos + monthFound.length() + 50);

            // correction on neighborhood before digit search
            String neighborhood = fuzzyFix(upper.substring(start, end));

            // Find all potential day/year fragments
            Matcher dm = Pattern.compile("(\\d{1,2}|\\d{4})").matcher(neighborhood);
            List<String> digits = new ArrayList<>();
            List<Integer> offsets = new ArrayList<>();
            while (dm.find()) {
                digits.add(dm.group());
                offsets.add(start + dm.start());
            }

            String d = "", y = "";
            int dDist = 99, yDist = 99;
            for (int i = 0; i < digits.size(); i++) {
                String val = digits.get(i);
                int pos = offsets.get(i);
                int dist = Math.abs(pos - monthPos);

                if (val.length() <= 2) {
                    try {
                        int v = Integer.parseInt(val);
                        if (v >= 1 && v <= 31 && dist < dDist) {
                            dDist = dist;
                            d = val;
                        }
                    } catch (Exception ignored) {
                    }
                } else if (val.length() == 4) {
                    if (dist < yDist) {
                        yDist = dist;
                        y = val;
                    }
                }
            }

            int score = (100 - dDist) + (100 - yDist);
            if (score > bestScore && !d.isEmpty() && !y.isEmpty()) {
                bestScore = score;
                bestD = d;
                bestM = monthFound;
                bestY = y;
            }
        }

        if (!bestD.isEmpty())
            return formatConfirmedDate(bestD, bestM, bestY);

        // 3. Numeric Sniper: Last resort for numeric dates (e.g. 13/03/2026)
        Matcher numDM = Pattern.compile("(\\d{1,2})[/\\-.](\\d{1,2})[/\\-.](20\\d{2}|\\d{2})").matcher(upper);
        String lastFound = "";
        while (numDM.find()) {
            String d = numDM.group(1);
            String mo = numDM.group(2);
            String y = numDM.group(3);
            if (y.length() == 2)
                y = "20" + y;

            int dVal = Integer.parseInt(d);
            int mVal = Integer.parseInt(mo);
            if (dVal >= 1 && dVal <= 31 && mVal >= 1 && mVal <= 12) {
                String[] monthNames = { "", "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December" };
                lastFound = d + " " + monthNames[mVal] + " " + y;
            }
        }
        return lastFound;
    }

    private static double magicSearchTotal(String fullText) {
        if (fullText == null || fullText.isEmpty())
            return 0.0;
        String upper = fullText.toUpperCase();

        // Search for the last occurrence of Total / Balance / To Pay
        // Modern receipts put the final total at the bottom
        String[] keywords = { "TOTAL", "BALANCE DUE", "TO PAY", "AMOUNT PAYABLE", "SUBTOTAL", "AMOUNT DUE" };
        double bestTotal = 0.0;
        int lastKeywordPos = -1;

        for (String kw : keywords) {
            int pos = upper.lastIndexOf(kw);
            if (pos > lastKeywordPos) {
                // Look for a price within 40 characters after this keyword
                String neighborhood = upper.substring(pos, Math.min(pos + 60, upper.length()));

                // Block "CASH" or "CHANGE" fields from being treated as the total
                if (neighborhood.contains("CASH") || neighborhood.contains("CHANGE")) {
                    continue;
                }

                Matcher m = Pattern.compile("[£EfS9sB]?(\\d{1,4}[.,]\\d{2})(?!\\d)").matcher(neighborhood);
                if (m.find()) {
                    try {
                        bestTotal = Double.parseDouble(m.group(1).replace(",", "."));
                        lastKeywordPos = pos;
                    } catch (Exception ignored) {
                    }
                }
            }
        }
        return bestTotal;
    }

    private static String fuzzyFix(String s) {
        if (s == null)
            return s;
        return s.replace("O", "0")
                .replace("Q", "0")
                .replace("I", "1")
                .replace("L", "1")
                .replace("S", "5")
                .replace("Z", "2")
                .replace("B", "8")
                .replace("G", "6");
    }

    private static String formatConfirmedDate(String d, String mPart, String y) {
        if (d.length() == 1)
            d = "0" + d;
        if (y.length() == 2)
            y = "20" + y;
        String fM = "April";
        String[] mF = { "January", "February", "March", "April", "May", "June", "July", "August", "September",
                "October", "November", "December" };
        String[] mS3 = { "JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC" };
        for (int i = 0; i < 12; i++)
            if (mPart.startsWith(mS3[i]))
                fM = mF[i];
        return d + " " + fM + " " + y;
    }

    public static String normalizeDate(String raw) {
        if (raw == null || raw.isEmpty())
            return "";
        String upper = raw.trim().toUpperCase();

        try {
            // First try component-based textual match (DD MMM YYYY)
            Matcher mText = DATE_PATTERN.matcher(upper);
            if (mText.find() && mText.group(2) != null) {
                String d = mText.group(2);
                String monthPart = mText.group(3);
                String y = mText.group(4);

                if (d.length() == 1)
                    d = "0" + d;
                if (y.length() == 2)
                    y = "20" + y;

                String fullMonth = monthPart;
                if (monthPart.startsWith("JAN"))
                    fullMonth = "January";
                else if (monthPart.startsWith("FEB"))
                    fullMonth = "February";
                else if (monthPart.startsWith("MAR"))
                    fullMonth = "March";
                else if (monthPart.startsWith("APR"))
                    fullMonth = "April";
                else if (monthPart.startsWith("MAY"))
                    fullMonth = "May";
                else if (monthPart.startsWith("JUN"))
                    fullMonth = "June";
                else if (monthPart.startsWith("JUL"))
                    fullMonth = "July";
                else if (monthPart.startsWith("AUG"))
                    fullMonth = "August";
                else if (monthPart.startsWith("SEP"))
                    fullMonth = "September";
                else if (monthPart.startsWith("OCT"))
                    fullMonth = "October";
                else if (monthPart.startsWith("NOV"))
                    fullMonth = "November";
                else if (monthPart.startsWith("DEC"))
                    fullMonth = "December";

                return d + " " + fullMonth + " " + y;
            }

            // Numeric fallback: DD/MM/YYYY
            String numeric = raw.replaceAll("[^0-9/\\-.]", "");
            String[] parts = numeric.split("[/\\-.]");
            if (parts.length >= 3) {
                String d = parts[0].trim();
                String mo = parts[1].trim();
                String y = parts[2].trim();

                if (d.length() == 1)
                    d = "0" + d;
                if (mo.length() == 1)
                    mo = "0" + mo;
                if (y.length() == 2)
                    y = "20" + y;

                // Simple month mapping
                String[] monthNames = { "", "January", "February", "March", "April", "May", "June",
                        "July", "August", "September", "October", "November", "December" };
                int moInt = Integer.parseInt(mo);
                if (moInt >= 1 && moInt <= 12) {
                    return d + " " + monthNames[moInt] + " " + y;
                }
            }
        } catch (Exception ignored) {
        }

        return raw; // Return as-is if parsing fails
    }

    private static String toTitleCase(String s) {
        if (s == null || s.isEmpty())
            return s;
        StringBuilder sb = new StringBuilder();
        for (String word : s.toLowerCase().split("\\s+")) {
            if (!word.isEmpty()) {
                sb.append(Character.toUpperCase(word.charAt(0))).append(word.substring(1)).append(" ");
            }
        }
        return sb.toString().trim();
    }
}
