package com.ap.sts.transactions;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * Minimal, dependency-free PDF writer for the transaction report (IR-TXN). Produces a valid
 * single-page PDF (Helvetica, uncompressed text) so we can generate PDFs server-side without
 * adding a library to the frozen {@code build.gradle} (see change request CR-S2-02). A richer
 * layout can replace this once a PDF library is added at Wave 0/2.
 */
final class SimplePdf {

    private SimplePdf() {
    }

    /** One A4 page of left-aligned text lines. Long lists are truncated to what fits one page. */
    static byte[] textPage(List<String> lines) {
        final int fontSize = 11;
        final int leading = 15;
        final int top = 800;
        final int maxLines = (top - 40) / leading; // keep within the page

        StringBuilder content = new StringBuilder();
        content.append("BT\n/F1 ").append(fontSize).append(" Tf\n")
                .append(leading).append(" TL\n50 ").append(top).append(" Td\n");
        List<String> shown = lines.size() > maxLines ? lines.subList(0, maxLines) : lines;
        for (String line : shown) {
            content.append('(').append(escape(line)).append(") Tj T*\n");
        }
        content.append("ET");
        byte[] contentBytes = content.toString().getBytes(StandardCharsets.ISO_8859_1);

        List<byte[]> objects = new ArrayList<>();
        objects.add(obj("<< /Type /Catalog /Pages 2 0 R >>"));
        objects.add(obj("<< /Type /Pages /Kids [3 0 R] /Count 1 >>"));
        objects.add(obj("<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] "
                + "/Resources << /Font << /F1 4 0 R >> >> /Contents 5 0 R >>"));
        objects.add(obj("<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>"));
        objects.add(stream(contentBytes));

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        List<Integer> offsets = new ArrayList<>();
        write(out, "%PDF-1.4\n");
        for (int i = 0; i < objects.size(); i++) {
            offsets.add(out.size());
            write(out, (i + 1) + " 0 obj\n");
            out.writeBytes(objects.get(i));
            write(out, "\nendobj\n");
        }
        int xrefStart = out.size();
        int size = objects.size() + 1;
        write(out, "xref\n0 " + size + "\n");
        write(out, "0000000000 65535 f \n");
        for (int offset : offsets) {
            write(out, String.format("%010d 00000 n \n", offset));
        }
        write(out, "trailer\n<< /Size " + size + " /Root 1 0 R >>\nstartxref\n" + xrefStart + "\n%%EOF");
        return out.toByteArray();
    }

    private static byte[] obj(String dict) {
        return dict.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static byte[] stream(byte[] contentBytes) {
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        write(o, "<< /Length " + contentBytes.length + " >>\nstream\n");
        o.writeBytes(contentBytes);
        write(o, "\nendstream");
        return o.toByteArray();
    }

    private static void write(ByteArrayOutputStream out, String s) {
        out.writeBytes(s.getBytes(StandardCharsets.ISO_8859_1));
    }

    /** Escapes PDF string delimiters and drops non-Latin1 characters. */
    private static String escape(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(' || c == ')' || c == '\\') {
                b.append('\\').append(c);
            } else if (c >= 32 && c < 127) {
                b.append(c);
            } else {
                b.append(' ');
            }
        }
        return b.toString();
    }
}
