package com.syndicati.services.pdf;

import com.syndicati.models.syndicat.Reclamation;
import com.syndicati.models.syndicat.Reponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Service for generating PDF reports of reclamations and responses.
 * Extracted from ProfileReclamationSectionEnhanced view for backend/automated use.
 */
public class PdfExportService {

    /**
     * Generates a PDF file for a reclamation including all its responses.
     */
    public static File generateReclamationPdf(Reclamation reclamation, List<Reponse> responses) throws IOException {
        String safeTitle = reclamation.getTitreReclamations() == null || reclamation.getTitreReclamations().isBlank()
                ? "reclamation"
                : reclamation.getTitreReclamations().replaceAll("[^a-zA-Z0-9-_]", "_");
        
        File tempFile = File.createTempFile(safeTitle + "_", ".pdf");

        try (PDDocument document = new PDDocument()) {
            final float margin = 42f;
            final float pageWidth = PDRectangle.LETTER.getWidth();
            final float pageHeight = PDRectangle.LETTER.getHeight();
            final float contentWidth = pageWidth - (margin * 2f);

            final java.awt.Color bgDark = new java.awt.Color(18, 18, 18);
            final java.awt.Color panelDark = new java.awt.Color(30, 30, 30);
            final java.awt.Color border = new java.awt.Color(51, 51, 51);
            final java.awt.Color accent = new java.awt.Color(255, 75, 92);
            final java.awt.Color textLight = new java.awt.Color(245, 245, 245);
            final java.awt.Color textMuted = new java.awt.Color(160, 160, 160);

            PDPage page = new PDPage(PDRectangle.LETTER);
            document.addPage(page);
            PDPageContentStream stream = new PDPageContentStream(document, page);
            paintPdfBackground(stream, page, bgDark);

            float y = pageHeight - margin;

            pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 13, margin, y, "Syndicate Hub", accent);
            y -= 18;

            String pdfTitle = sanitizePdfText(reclamation.getTitreReclamations() == null ? "Reclamation" : reclamation.getTitreReclamations());
            List<String> titleLines = wrapTextByWidth(pdfTitle, PDType1Font.HELVETICA_BOLD, 21, contentWidth);
            for (String line : titleLines) {
                pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 21, margin, y, line, textLight);
                y -= 24;
            }

            stream.setStrokingColor(accent);
            stream.setLineWidth(2f);
            stream.moveTo(margin, y + 8);
            stream.lineTo(pageWidth - margin, y + 8);
            stream.stroke();
            y -= 18;

            float metaBoxHeight = 116f;
            stream.setNonStrokingColor(panelDark);
            stream.addRect(margin, y - metaBoxHeight, contentWidth, metaBoxHeight);
            stream.fill();
            stream.setStrokingColor(border);
            stream.addRect(margin, y - metaBoxHeight, contentWidth, metaBoxHeight);
            stream.stroke();

            float rowHeight = metaBoxHeight / 3f;
            float labelColWidth = 160f;
            for (int i = 1; i <= 2; i++) {
                float yLine = y - (rowHeight * i);
                stream.moveTo(margin, yLine);
                stream.lineTo(margin + contentWidth, yLine);
                stream.stroke();
            }
            stream.moveTo(margin + labelColWidth, y);
            stream.lineTo(margin + labelColWidth, y - metaBoxHeight);
            stream.stroke();

            float row1Y = y - 22;
            float row2Y = y - rowHeight - 22;
            float row3Y = y - (rowHeight * 2f) - 22;
            float valueX = margin + labelColWidth + 12;

            pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 10, margin + 12, row1Y, "DATE SUBMITTED", textMuted);
            pdfWriteText(stream, PDType1Font.HELVETICA, 11, valueX, row1Y,
                    sanitizePdfText(reclamation.getDateReclamation() == null ? "-" : reclamation.getDateReclamation().format(DateTimeFormatter.ofPattern("MMMM d, yyyy, h:mm a"))),
                    textLight);

            pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 10, margin + 12, row2Y, "CURRENT STATUS", textMuted);
            String statusPlain = getStatusDisplayPlain(reclamation.getStatutReclamation());
            float badgeX = valueX;
            float badgeH = 14f;
            float badgeY = row2Y - 3f;
            float badgeW = Math.max(80f, textWidth(PDType1Font.HELVETICA_BOLD, 10, statusPlain) + 18f);

            stream.setNonStrokingColor(new java.awt.Color(50, 24, 28));
            stream.addRect(badgeX, badgeY, badgeW, badgeH);
            stream.fill();
            stream.setStrokingColor(accent);
            stream.addRect(badgeX, badgeY, badgeW, badgeH);
            stream.stroke();
            pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 10, badgeX + 9, row2Y + 1f, statusPlain, accent);

            pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 10, margin + 12, row3Y, "SUBMITTED BY", textMuted);
            String submittedBy = "N/A";
            if (reclamation.getUser() != null) {
                String first = reclamation.getUser().getFirstName() == null ? "" : reclamation.getUser().getFirstName();
                String last = reclamation.getUser().getLastName() == null ? "" : reclamation.getUser().getLastName();
                String full = (first + " " + last).trim();
                String email = reclamation.getUser().getEmailUser() == null ? "" : reclamation.getUser().getEmailUser();
                submittedBy = full.isBlank() ? email : (email.isBlank() ? full : full + " (" + email + ")");
            }
            List<String> byLines = wrapTextByWidth(sanitizePdfText(submittedBy), PDType1Font.HELVETICA, 11, contentWidth - labelColWidth - 24f);
            if (!byLines.isEmpty()) {
                pdfWriteText(stream, PDType1Font.HELVETICA, 11, valueX, row3Y, byLines.get(0), textLight);
            }

            y = y - metaBoxHeight - 24;

            pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 13, margin, y, "CASE DESCRIPTION", accent);
            y -= 14;
            stream.setStrokingColor(border);
            stream.moveTo(margin, y);
            stream.lineTo(pageWidth - margin, y);
            stream.stroke();
            y -= 10;

            List<String> descLines = wrapTextByWidth(sanitizePdfText(reclamation.getDescReclamation() == null ? "-" : reclamation.getDescReclamation()), PDType1Font.HELVETICA, 11, contentWidth - 24f);
            
            String mainImgPath = reclamation.getImageReclamation();
            boolean hasMainImg = mainImgPath != null && !mainImgPath.isBlank() && !"-".equals(mainImgPath);
            float mainImgHeight = hasMainImg ? 155f : 0f;
            float descHeight = Math.max(64f, 25f + (descLines.size() * 14f) + mainImgHeight);

            if (y - descHeight < 70f) {
                stream.close();
                page = new PDPage(PDRectangle.LETTER);
                document.addPage(page);
                stream = new PDPageContentStream(document, page);
                paintPdfBackground(stream, page, bgDark);
                y = pageHeight - margin;
            }

            stream.setNonStrokingColor(panelDark);
            stream.addRect(margin, y - descHeight, contentWidth, descHeight);
            stream.fill();
            stream.setStrokingColor(border);
            stream.addRect(margin, y - descHeight, contentWidth, descHeight);
            stream.stroke();
            stream.setNonStrokingColor(accent);
            stream.addRect(margin, y - descHeight, 4f, descHeight);
            stream.fill();

            float descTextY = y - 20;
            for (String line : descLines) {
                pdfWriteText(stream, PDType1Font.HELVETICA, 11, margin + 12, descTextY, line, textLight);
                descTextY -= 14;
            }

            if (hasMainImg) {
                try {
                    String fullPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + mainImgPath;
                    File imgFile = new File(fullPath);
                    if (imgFile.exists()) {
                        PDImageXObject pdImage = PDImageXObject.createFromFile(fullPath, document);
                        float scale = 145f / pdImage.getHeight();
                        float finalW = Math.min(contentWidth - 24, pdImage.getWidth() * scale);
                        stream.drawImage(pdImage, margin + 12, descTextY - 148, finalW, 145);
                    }
                } catch (Exception ignored) {}
            }
            y = y - descHeight - 24;

            pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 13, margin, y, "RESPONSES", accent);
            y -= 14;
            stream.setStrokingColor(border);
            stream.moveTo(margin, y);
            stream.lineTo(pageWidth - margin, y);
            stream.stroke();
            y -= 12;

            if (responses == null || responses.isEmpty()) {
                pdfWriteText(stream, PDType1Font.HELVETICA_OBLIQUE, 11, margin, y, "No responses.", textMuted);
                y -= 20;
            } else {
                for (Reponse response : responses) {
                    String author = "Unknown";
                    if (response.getUser() != null) {
                        String first = response.getUser().getFirstName() == null ? "" : response.getUser().getFirstName();
                        String last = response.getUser().getLastName() == null ? "" : response.getUser().getLastName();
                        author = (first + " " + last).trim();
                        if (author.isBlank()) author = response.getUser().getEmailUser() == null ? "Unknown" : response.getUser().getEmailUser();
                    }

                    String when = response.getCreatedAt() == null ? "-" : response.getCreatedAt().format(DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm"));
                    String repTitle = response.getTitreReponse() != null ? response.getTitreReponse() : "Response";
                    String header = sanitizePdfText(repTitle + " | By: " + author + " (" + when + ")");

                    List<String> msgLines = wrapTextByWidth(sanitizePdfText(response.getMessageReponse() == null ? "" : response.getMessageReponse()), PDType1Font.HELVETICA, 11, contentWidth - 26f);
                    float textPartHeight = 30f + (msgLines.size() * 14f);
                    String imgPath = response.getImageReponse();
                    boolean hasImage = imgPath != null && !imgPath.isBlank() && !"-".equals(imgPath);
                    float imgHeight = hasImage ? 120f : 0f;
                    float totalCadreHeight = textPartHeight + imgHeight;
                    float spacingBetween = 12f;

                    if (y - totalCadreHeight < 70f) {
                        stream.close();
                        page = new PDPage(PDRectangle.LETTER);
                        document.addPage(page);
                        stream = new PDPageContentStream(document, page);
                        paintPdfBackground(stream, page, bgDark);
                        y = pageHeight - margin;
                        pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 13, margin, y, "RESPONSES (CONTINUED)", accent);
                        y -= 14;
                        stream.setStrokingColor(border);
                        stream.moveTo(margin, y);
                        stream.lineTo(pageWidth - margin, y);
                        stream.stroke();
                        y -= 12;
                    }

                    stream.setNonStrokingColor(new java.awt.Color(24, 24, 24));
                    stream.addRect(margin, y - totalCadreHeight, contentWidth, totalCadreHeight);
                    stream.fill();
                    stream.setStrokingColor(border);
                    stream.addRect(margin, y - totalCadreHeight, contentWidth, totalCadreHeight);
                    stream.stroke();

                    pdfWriteText(stream, PDType1Font.HELVETICA_BOLD, 10, margin + 10, y - 16, header, textMuted);

                    float msgY = y - 32;
                    for (String line : msgLines) {
                        pdfWriteText(stream, PDType1Font.HELVETICA, 11, margin + 10, msgY, line, textLight);
                        msgY -= 14;
                    }

                    if (hasImage) {
                        try {
                            String fullPath = System.getProperty("user.dir") + File.separator + "uploads" + File.separator + imgPath;
                            File imgFile = new File(fullPath);
                            if (imgFile.exists()) {
                                PDImageXObject pdImage = PDImageXObject.createFromFile(fullPath, document);
                                float scale = 110f / pdImage.getHeight();
                                float finalW = Math.min(contentWidth - 20, pdImage.getWidth() * scale);
                                stream.drawImage(pdImage, margin + 10, y - totalCadreHeight + 5, finalW, 110);
                            }
                        } catch (Exception ignored) {}
                    }
                    y -= (totalCadreHeight + spacingBetween);
                }
            }

            pdfWriteText(stream, PDType1Font.HELVETICA, 8, margin, 26, "Generated securely by the Syndicate Central System", textMuted);
            stream.close();
            document.save(tempFile);
        }
        return tempFile;
    }

    private static void paintPdfBackground(PDPageContentStream stream, PDPage page, java.awt.Color color) throws IOException {
        PDRectangle box = page.getMediaBox();
        stream.setNonStrokingColor(color);
        stream.addRect(0, 0, box.getWidth(), box.getHeight());
        stream.fill();
    }

    private static void pdfWriteText(PDPageContentStream stream, PDType1Font font, float size, float x, float y, String text, java.awt.Color color) throws IOException {
        stream.beginText();
        stream.setFont(font, size);
        stream.setNonStrokingColor(color);
        stream.newLineAtOffset(x, y);
        stream.showText(sanitizePdfText(text));
        stream.endText();
    }

    private static List<String> wrapTextByWidth(String text, PDType1Font font, float fontSize, float maxWidth) throws IOException {
        List<String> lines = new ArrayList<>();
        String safe = sanitizePdfText(text);
        if (safe.isBlank()) {
            lines.add("");
            return lines;
        }

        String[] paragraphs = safe.split("\\r?\\n");
        for (String paragraph : paragraphs) {
            String p = paragraph == null ? "" : paragraph.trim();
            if (p.isEmpty()) {
                lines.add("");
                continue;
            }

            StringBuilder current = new StringBuilder();
            for (String word : p.split("\\s+")) {
                String candidate = current.length() == 0 ? word : current + " " + word;
                if (textWidth(font, fontSize, candidate) <= maxWidth) {
                    current.setLength(0);
                    current.append(candidate);
                } else {
                    if (current.length() > 0) {
                        lines.add(current.toString());
                        current.setLength(0);
                        current.append(word);
                    } else {
                        lines.add(word);
                    }
                }
            }
            if (current.length() > 0) lines.add(current.toString());
        }
        return lines;
    }

    private static float textWidth(PDType1Font font, float fontSize, String text) throws IOException {
        return font.getStringWidth(sanitizePdfText(text)) / 1000f * fontSize;
    }

    private static String sanitizePdfText(String text) {
        if (text == null) return "";
        String sanitized = text.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ').trim();
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < sanitized.length(); i++) {
            char c = sanitized.charAt(i);
            if (c >= 32 && c <= 255) out.append(c);
            else out.append('?');
        }
        return out.toString();
    }

    private static String getStatusDisplayPlain(String status) {
        return switch (status) {
            case "Pending", "en_attente" -> "Pending";
            case "active" -> "Active";
            case "Confirmed", "termine" -> "Confirmed";
            case "Refused", "refuse" -> "Refused";
            default -> status == null || status.isBlank() ? "Pending" : status;
        };
    }
}
