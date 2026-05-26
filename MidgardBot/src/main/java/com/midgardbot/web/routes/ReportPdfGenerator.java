package com.midgardbot.web.routes;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.HeaderFooter;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.Anchor;
import com.lowagie.text.Chunk;
import com.lowagie.text.FontFactory;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Gera um PDF bonito e organizado com todos os relatórios do mês,
 * agrupados por cargo e data, com imagens embutidas.
 */
public class ReportPdfGenerator {

    private static final Logger LOGGER = LoggerFactory.getLogger(ReportPdfGenerator.class);

    // Cores do tema
    private static final Color DARK_BG = new Color(12, 14, 22);
    private static final Color CARD_BG = new Color(23, 26, 40);
    private static final Color GOLD = new Color(240, 180, 41);
    private static final Color GOLD_DARK = new Color(184, 134, 18);
    private static final Color TEXT_PRIMARY = new Color(232, 236, 244);
    private static final Color TEXT_SECONDARY = new Color(160, 170, 190);
    private static final Color TEXT_MUTED = new Color(100, 110, 135);
    private static final Color BORDER = new Color(40, 45, 65);
    private static final Color ACCENT_LINE = new Color(240, 180, 41, 60);

    private ReportPdfGenerator() {}

    public static byte[] generate(java.util.List<Map<String, Object>> reports, YearMonth month, Path uploadsDir) throws DocumentException, IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Document doc = new Document(PageSize.A4, 40, 40, 50, 50);
        PdfWriter writer = PdfWriter.getInstance(doc, baos);
        writer.setPageEvent(new FooterHandler(month));
        doc.open();

        // Fontes
        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 22, GOLD);
        Font subtitleFont = FontFactory.getFont(FontFactory.HELVETICA, 11, TEXT_SECONDARY);
        Font roleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, TEXT_PRIMARY);
        Font dateFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, GOLD_DARK);
        Font reportTitleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, TEXT_PRIMARY);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_SECONDARY);
        Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 8, TEXT_MUTED);
        Font linkFont = FontFactory.getFont(FontFactory.HELVETICA, 8, new Color(100, 160, 255));
        Font counterFont = FontFactory.getFont(FontFactory.HELVETICA, 9, TEXT_MUTED);

        // ═══════════════════════════════════════════════
        //  CAPA / HEADER
        // ═══════════════════════════════════════════════
        doc.add(spacer(30));

        // Linha decorativa superior
        PdfPTable topLine = new PdfPTable(1);
        topLine.setWidthPercentage(100);
        PdfPCell lineCell = new PdfPCell();
        lineCell.setFixedHeight(3);
        lineCell.setBackgroundColor(GOLD);
        lineCell.setBorder(Rectangle.NO_BORDER);
        topLine.addCell(lineCell);
        doc.add(topLine);

        doc.add(spacer(20));

        Paragraph title = new Paragraph("⚔  RELATÓRIO MENSAL DA STAFF", titleFont);
        title.setAlignment(Element.ALIGN_CENTER);
        doc.add(title);

        String monthName = month.getMonth().getDisplayName(TextStyle.FULL, new Locale("pt", "BR"));
        String subtitle = monthName.substring(0, 1).toUpperCase() + monthName.substring(1) + " de " + month.getYear();
        Paragraph sub = new Paragraph(subtitle, subtitleFont);
        sub.setAlignment(Element.ALIGN_CENTER);
        sub.setSpacingBefore(5);
        doc.add(sub);

        doc.add(spacer(8));

        // Linha decorativa inferior
        PdfPTable bottomLine = new PdfPTable(1);
        bottomLine.setWidthPercentage(100);
        PdfPCell lineCell2 = new PdfPCell();
        lineCell2.setFixedHeight(1);
        lineCell2.setBackgroundColor(BORDER);
        lineCell2.setBorder(Rectangle.NO_BORDER);
        bottomLine.addCell(lineCell2);
        doc.add(bottomLine);

        doc.add(spacer(8));

        // Resumo geral
        Paragraph summary = new Paragraph("Total de relatórios: " + reports.size(), counterFont);
        summary.setAlignment(Element.ALIGN_CENTER);
        doc.add(summary);

        doc.add(spacer(20));

        // ═══════════════════════════════════════════════
        //  AGRUPAR POR CARGO
        // ═══════════════════════════════════════════════
        Map<String, java.util.List<Map<String, Object>>> byRole = new LinkedHashMap<>();
        for (var r : reports) {
            String roleName = (String) r.getOrDefault("roleName", "Sem Cargo");
            byRole.computeIfAbsent(roleName, k -> new ArrayList<>()).add(r);
        }

        for (var entry : byRole.entrySet()) {
            String roleName = entry.getKey();
            java.util.List<Map<String, Object>> roleReports = entry.getValue();

            // Header do cargo
            PdfPTable roleHeader = new PdfPTable(1);
            roleHeader.setWidthPercentage(100);
            roleHeader.setSpacingBefore(15);

            PdfPCell roleCell = new PdfPCell();
            roleCell.setBackgroundColor(CARD_BG);
            roleCell.setBorderColor(BORDER);
            roleCell.setBorderWidth(1);
            roleCell.setPadding(10);
            roleCell.setPaddingLeft(15);

            Paragraph roleParagraph = new Paragraph();
            roleParagraph.add(new Chunk("■ ", FontFactory.getFont(FontFactory.HELVETICA, 12, GOLD)));
            roleParagraph.add(new Chunk(roleName.toUpperCase(), roleFont));
            roleParagraph.add(new Chunk("  (" + roleReports.size() + " relatório" + (roleReports.size() != 1 ? "s" : "") + ")", counterFont));
            roleCell.addElement(roleParagraph);
            roleHeader.addCell(roleCell);
            doc.add(roleHeader);

            // Agrupar por data dentro do cargo
            Map<String, java.util.List<Map<String, Object>>> byDate = new LinkedHashMap<>();
            for (var r : roleReports) {
                String actDate = (String) r.getOrDefault("activityDate", "Sem data");
                String dateKey = actDate.length() >= 10 ? actDate.substring(0, 10) : actDate;
                byDate.computeIfAbsent(dateKey, k -> new ArrayList<>()).add(r);
            }

            for (var dateEntry : byDate.entrySet()) {
                String dateStr = dateEntry.getKey();
                java.util.List<Map<String, Object>> dateReports = dateEntry.getValue();

                // Tag de data
                String formattedDate = formatDateBR(dateStr);
                Paragraph datePara = new Paragraph("📅  " + formattedDate, dateFont);
                datePara.setSpacingBefore(12);
                datePara.setSpacingAfter(6);
                datePara.setIndentationLeft(15);
                doc.add(datePara);

                // Cards de relatórios
                for (var report : dateReports) {
                    PdfPTable card = new PdfPTable(1);
                    card.setWidthPercentage(96);
                    card.setHorizontalAlignment(Element.ALIGN_CENTER);
                    card.setSpacingBefore(4);

                    PdfPCell cardCell = new PdfPCell();
                    cardCell.setBackgroundColor(new Color(20, 23, 35));
                    cardCell.setBorderColor(BORDER);
                    cardCell.setBorderWidth(0.5f);
                    cardCell.setPadding(12);
                    cardCell.setPaddingTop(10);

                    // Título do relatório
                    String rTitle = (String) report.getOrDefault("title", "Sem título");
                    Paragraph rTitlePara = new Paragraph(rTitle, reportTitleFont);
                    cardCell.addElement(rTitlePara);

                    // Meta: autor + data
                    String author = (String) report.getOrDefault("authorDisplayName",
                            report.getOrDefault("authorName", "Desconhecido"));
                    String createdAt = (String) report.getOrDefault("activityDate", "");
                    Paragraph metaPara = new Paragraph("por " + author + "  •  " + createdAt, metaFont);
                    metaPara.setSpacingBefore(3);
                    cardCell.addElement(metaPara);

                    // Separador
                    PdfPTable sep = new PdfPTable(1);
                    sep.setWidthPercentage(100);
                    sep.setSpacingBefore(6);
                    sep.setSpacingAfter(6);
                    PdfPCell sepCell = new PdfPCell();
                    sepCell.setFixedHeight(0.5f);
                    sepCell.setBackgroundColor(BORDER);
                    sepCell.setBorder(Rectangle.NO_BORDER);
                    sep.addCell(sepCell);
                    cardCell.addElement(sep);

                    // Descrição
                    String desc = (String) report.getOrDefault("description", "");
                    Paragraph descPara = new Paragraph(desc, bodyFont);
                    descPara.setLeading(14);
                    cardCell.addElement(descPara);

                    // Anexos (imagens embutidas + links)
                    @SuppressWarnings("unchecked")
                    java.util.List<Map<String, Object>> attachments = (java.util.List<Map<String, Object>>) report.getOrDefault("attachments", java.util.List.of());
                    if (!attachments.isEmpty()) {
                        PdfPTable attSep = new PdfPTable(1);
                        attSep.setWidthPercentage(100);
                        attSep.setSpacingBefore(8);
                        PdfPCell attSepCell = new PdfPCell();
                        attSepCell.setFixedHeight(0.5f);
                        attSepCell.setBackgroundColor(BORDER);
                        attSepCell.setBorder(Rectangle.NO_BORDER);
                        attSep.addCell(attSepCell);
                        cardCell.addElement(attSep);

                        Paragraph attLabel = new Paragraph("Anexos:", metaFont);
                        attLabel.setSpacingBefore(4);
                        cardCell.addElement(attLabel);

                        for (var att : attachments) {
                            String type = (String) att.getOrDefault("type", "");
                            if ("image".equals(type)) {
                                String filename = (String) att.get("filename");
                                if (filename != null && uploadsDir != null) {
                                    Path imgPath = uploadsDir.resolve(filename);
                                    if (Files.exists(imgPath)) {
                                        try {
                                            Image img = Image.getInstance(imgPath.toString());
                                            img.scaleToFit(200, 150);
                                            img.setSpacingBefore(6);
                                            img.setAlignment(Element.ALIGN_LEFT);
                                            cardCell.addElement(img);
                                        } catch (Exception e) {
                                            LOGGER.warn("[PDF] Falha ao embutir imagem: {}", filename);
                                            cardCell.addElement(new Paragraph("📷 " + att.getOrDefault("originalName", filename), metaFont));
                                        }
                                    }
                                }
                            } else if ("video".equals(type)) {
                                String origName = (String) att.getOrDefault("originalName", "vídeo");
                                cardCell.addElement(new Paragraph("🎬 " + origName + " (vídeo anexado)", metaFont));
                            } else if ("link".equals(type)) {
                                String url = (String) att.get("url");
                                if (url != null) {
                                    Anchor anchor = new Anchor(url, linkFont);
                                    anchor.setReference(url);
                                    Paragraph linkPara = new Paragraph();
                                    linkPara.add(new Chunk("🔗 ", metaFont));
                                    linkPara.add(anchor);
                                    linkPara.setSpacingBefore(2);
                                    cardCell.addElement(linkPara);
                                }
                            }
                        }
                    }

                    card.addCell(cardCell);
                    doc.add(card);
                }
            }
        }

        // Rodapé final
        doc.add(spacer(30));
        PdfPTable endLine = new PdfPTable(1);
        endLine.setWidthPercentage(100);
        PdfPCell endCell = new PdfPCell();
        endCell.setFixedHeight(2);
        endCell.setBackgroundColor(GOLD);
        endCell.setBorder(Rectangle.NO_BORDER);
        endLine.addCell(endCell);
        doc.add(endLine);

        doc.add(spacer(8));
        Paragraph endText = new Paragraph("Gerado automaticamente pelo Midgard Bot", metaFont);
        endText.setAlignment(Element.ALIGN_CENTER);
        doc.add(endText);

        doc.close();
        return baos.toByteArray();
    }

    private static Paragraph spacer(float height) {
        Paragraph p = new Paragraph(" ");
        p.setSpacingBefore(height);
        return p;
    }

    private static String formatDateBR(String dateStr) {
        if (dateStr == null || dateStr.length() < 10) return dateStr != null ? dateStr : "—";
        try {
            String[] parts = dateStr.substring(0, 10).split("-");
            return parts[2] + "/" + parts[1] + "/" + parts[0];
        } catch (Exception e) {
            return dateStr;
        }
    }

    /**
     * Adiciona número de página no rodapé de cada página.
     */
    private static class FooterHandler extends PdfPageEventHelper {
        private final YearMonth month;

        FooterHandler(YearMonth month) {
            this.month = month;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Font footerFont = FontFactory.getFont(FontFactory.HELVETICA, 7, TEXT_MUTED);
            String left = "Midgard — Relatório " + month;
            String right = "Página " + writer.getPageNumber();

            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(left, footerFont),
                    document.left(), document.bottom() - 20, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase(right, footerFont),
                    document.right(), document.bottom() - 20, 0);
        }
    }
}
