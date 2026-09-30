package com.ap.sts.certificates.pdf;

import com.ap.sts.certificates.domain.CertificateOrigin;
import com.ap.sts.certificates.domain.PrintMode;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.List;

/**
 * Server-side certificate PDF (OI-12 resolved as Java PDF; PDFBox 3). One landscape Letter page
 * per certificate — the size of AP's certificate stock.
 *
 * <p>Both modes print exactly the 10 fields ticked on AP's real certificate (2026-09-29), at the
 * same positions ({@link CertificateLayout}):
 * <ul>
 *   <li>{@link PrintMode#PrePrintedForm}: the 10 values only, onto AP's pre-printed stock.</li>
 *   <li>{@link PrintMode#SystemTemplate}: the same values plus the certificate's fixed wording and
 *       frames, reproducing AP's real certificate face on plain paper (TR-017).</li>
 * </ul>
 * Standard-14 fonts only (no font files in the image); characters outside WinAnsi are replaced.
 */
@Component
public class CertificatePdfRenderer {

    static final String SAMPLE_NOTICE = "SAMPLE - synthetic data for the AP STS sprint. Not a valid stock certificate.";
    static final float MIN_FONT_SIZE = 6f;

    private static final PDRectangle PAGE = new PDRectangle(PDRectangle.LETTER.getHeight(), PDRectangle.LETTER.getWidth());

    private final CertificateLayout layout;

    public CertificatePdfRenderer() {
        this(CertificateLayout.MEASURED_FROM_SCAN);
    }

    CertificatePdfRenderer(CertificateLayout layout) {
        this.layout = layout;
    }

    public byte[] render(List<CertificateSheet> sheets, PrintMode mode) {
        if (sheets == null || sheets.isEmpty()) {
            throw new IllegalArgumentException("nothing to render");
        }
        try (PDDocument doc = new PDDocument(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PDDocumentInformation info = doc.getDocumentInformation();
            info.setTitle("Stock certificates " + sheets.get(0).transactionId());
            info.setCreator("AP Stock Transfer System");
            info.setSubject(SAMPLE_NOTICE);
            Fonts fonts = new Fonts();
            for (CertificateSheet sheet : sheets) {
                PDPage page = new PDPage(PAGE);
                doc.addPage(page);
                try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                    if (mode == PrintMode.SystemTemplate) {
                        drawForm(cs, fonts, sheet);
                    }
                    drawFields(cs, fonts, sheet);
                    drawSampleNotice(cs, fonts, mode);
                }
            }
            doc.save(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("certificate PDF generation failed", e);
        }
    }

    // ---- the 10 ticked fields (both modes) ---------------------------------------------------

    private void drawFields(PDPageContentStream cs, Fonts f, CertificateSheet s) throws IOException {
        cs.setNonStrokingColor(0f, 0f, 0f);
        CertificateLayout l = layout;
        put(cs, f.bold, l.stockClass(), CertificateFace.caps(s.stockType()));                    // 1
        put(cs, f.bold, l.number(), s.number());                                                   // 2
        put(cs, f.bold, l.shares(), CertificateFace.sharesFigure(s.shares()));                     // 3
        put(cs, f.bold, l.company(), CertificateFace.caps(s.companyName()));                       // 4
        put(cs, f.bold, l.holder(), CertificateFace.caps(s.holderName()));                         // 5
        put(cs, f.bold, l.sharesInWords(), CertificateFace.sharesInWords(s.shares()));             // 6
        put(cs, f.bold, l.day(), CertificateFace.day(s.issuedOn()));                               // 7 (day)
        put(cs, f.bold, l.month(), CertificateFace.month(s.issuedOn()));                           // 7 (month)
        put(cs, f.bold, l.year(), CertificateFace.year(s.issuedOn()));                             // 7 (year)
        put(cs, f.bold, l.president(), CertificateFace.caps(s.presidentName()));                   // 8
        put(cs, f.bold, l.corporateSecretary(), CertificateFace.caps(s.corporateSecretaryName())); // 9
        put(cs, f.bold, l.parValue(), CertificateFace.parValue(s.parValue()));                     // 10
    }

    // ---- fixed wording + frames (system template only) -----------------------------------------

    private void drawForm(PDPageContentStream cs, Fonts f, CertificateSheet s) throws IOException {
        float w = PAGE.getWidth();
        float h = PAGE.getHeight();
        cs.setStrokingColor(0.18f, 0.19f, 0.57f); // AP Blue #2E3092
        cs.setLineWidth(4f);
        cs.addRect(20, 20, w - 40, h - 40);
        cs.stroke();
        cs.setLineWidth(1f);
        cs.addRect(30, 30, w - 60, h - 60);
        cs.stroke();

        cs.setNonStrokingColor(0.07f, 0.08f, 0.16f); // #131429
        cs.setLineWidth(0.8f);
        // Header: NUMBER / incorporation box / SHARES
        centered(cs, f.serifBold, 14, 168, 503, "NUMBER");
        centered(cs, f.serifBold, 14, 631, 503, "SHARES");
        box(cs, 95, 440, 146, 55);
        box(cs, 558, 440, 146, 55);
        box(cs, 247, 440, 303, 55);
        centered(cs, f.bold, 8.5f, 398, 471, "INCORPORATED UNDER THE LAWS OF THE");
        centered(cs, f.bold, 8.5f, 398, 459, "REPUBLIC OF THE PHILIPPINES");
        if (s.origin() == CertificateOrigin.replacement) {
            centered(cs, f.bold, 9, 398, 427, "REPLACEMENT CERTIFICATE");
        }
        // Company name box
        box(cs, 95, 362, 610, 42);
        // "This Certifies that ___ is the owner of / ___ Shares of the Capital Stock of"
        text(cs, f.serifBoldItalic, 17, 95, 301, "This Certifies that");
        line(cs, 250, 297, 598);
        text(cs, f.serifItalic, 13, 602, 301, "is the owner of");
        line(cs, 100, 281, 500);
        text(cs, f.serifItalic, 13, 505, 285, "Shares of the Capital Stock of");
        // Transfer clause
        text(cs, f.serifItalic, 13, 240, 246, "transferable only on the books of the Corporation by the holder");
        text(cs, f.serifItalic, 13, 240, 229, "hereof in person or by Attorney, upon surrender of this Certificate");
        text(cs, f.serifItalic, 13, 240, 212, "properly endorsed.");
        // In Witness Whereof … this ___ day of ___ A.D. ___
        text(cs, f.serifBoldItalic, 14, 240, 190, "In Witness Whereof,");
        text(cs, f.serifItalic, 11, 368, 190, "the said Corporation has caused this Certificate to be");
        text(cs, f.serifItalic, 11, 240, 175, "signed by its duly authorized officers and to be sealed with the Seal of the Corporation");
        text(cs, f.serifItalic, 12, 283, 134, "this");
        line(cs, 305, 130, 388);
        text(cs, f.serifItalic, 12, 393, 134, "day of");
        line(cs, 435, 130, 560);
        text(cs, f.serifItalic, 12, 590, 134, "A.D.");
        line(cs, 638, 130, 690);
        // Officer titles under the names
        centered(cs, f.regular, 9, 302, 82, "President");
        centered(cs, f.regular, 9, 615, 82, "Corporate Secretary");
        // "Shares [par value] Each."
        text(cs, f.serifBold, 13, 305, 67, "Shares");
        box(cs, 355, 60, 100, 21);
        text(cs, f.serifBold, 13, 462, 67, "Each.");
        // Seal position (applied by hand; not printed)
        cs.setLineDashPattern(new float[]{3, 3}, 0);
        cs.addRect(105, 130, 110, 110);
        cs.stroke();
        cs.setLineDashPattern(new float[]{}, 0);
        centered(cs, f.regular, 8, 160, 183, "Corporate seal");
    }

    private void drawSampleNotice(PDPageContentStream cs, Fonts f, PrintMode mode) throws IOException {
        cs.setNonStrokingColor(0.36f, 0.36f, 0.36f); // grey-700
        if (mode == PrintMode.SystemTemplate) {
            centered(cs, f.regular, 7, PAGE.getWidth() / 2, 38, SAMPLE_NOTICE);
        } else {
            text(cs, f.regular, 6, 36, 12, SAMPLE_NOTICE + " Field positions from the scan; confirm by test print (OI-12).");
        }
    }

    // ---- helpers ------------------------------------------------------------------------------

    /** Writes a value in its slot, shrinking the font (not below 6 pt) so long names still fit. */
    private static void put(PDPageContentStream cs, PDFont font, CertificateLayout.Slot slot, String value)
            throws IOException {
        String v = safe(font, value);
        float size = fitSize(font, v, slot.size(), slot.maxWidth());
        if (slot.centered()) {
            centered(cs, font, size, slot.x(), slot.y(), v);
        } else {
            text(cs, font, size, slot.x(), slot.y(), v);
        }
    }

    static float fitSize(PDFont font, String value, float size, float maxWidth) throws IOException {
        float s = size;
        while (s > MIN_FONT_SIZE && width(font, value, s) > maxWidth) {
            s -= 0.5f;
        }
        return s;
    }

    private static float width(PDFont font, String value, float size) throws IOException {
        return font.getStringWidth(value) / 1000 * size;
    }

    private static void text(PDPageContentStream cs, PDFont font, float size, float x, float y, String value)
            throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(safe(font, value));
        cs.endText();
    }

    private static void centered(PDPageContentStream cs, PDFont font, float size, float centerX, float y,
                                 String value) throws IOException {
        String v = safe(font, value);
        text(cs, font, size, centerX - width(font, v, size) / 2, y, v);
    }

    private static void box(PDPageContentStream cs, float x, float y, float w, float h) throws IOException {
        cs.addRect(x, y, w, h);
        cs.stroke();
    }

    private static void line(PDPageContentStream cs, float x1, float y, float x2) throws IOException {
        cs.moveTo(x1, y);
        cs.lineTo(x2, y);
        cs.stroke();
    }

    /** Replaces characters the standard-14 font cannot encode (WinAnsi) so rendering never fails. */
    static String safe(PDFont font, String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(cp -> {
            String ch = new String(Character.toChars(cp));
            try {
                font.encode(ch);
                out.append(ch);
            } catch (IOException | IllegalArgumentException e) {
                out.append('?');
            }
        });
        return out.toString();
    }

    private static final class Fonts {
        final PDFont regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        final PDFont bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        final PDFont serifBold = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD);
        final PDFont serifItalic = new PDType1Font(Standard14Fonts.FontName.TIMES_ITALIC);
        final PDFont serifBoldItalic = new PDType1Font(Standard14Fonts.FontName.TIMES_BOLD_ITALIC);
    }
}
