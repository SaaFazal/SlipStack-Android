package com.n1249874.slipstack.utils;

import com.n1249874.slipstack.database.ReceiptEntity;

import java.util.List;

public class CsvGenerator {

    /**
     * Converts a list of receipts into a CSV string.
     */
    public static String generateReceiptsCsv(List<ReceiptEntity> receipts) {
        StringBuilder sb = new StringBuilder();
        // Header
        sb.append("Date,Merchant,Category,Amount,Currency\n");

        for (ReceiptEntity r : receipts) {
            sb.append(escape(r.date)).append(",");
            sb.append(escape(r.merchantName)).append(",");
            sb.append(escape(r.category)).append(",");
            sb.append(String.format("%.2f", r.amount)).append(",");
            sb.append("GBP\n");
        }

        return sb.toString();
    }

    private static String escape(String s) {
        if (s == null) return "";
        if (s.contains(",") || s.contains("\n") || s.contains("\"")) {
            return "\"" + s.replace("\"", "\"\"") + "\"";
        }
        return s;
    }
}
