package com.petfoodstore.service;

import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.*;
import com.petfoodstore.entity.Order;
import org.springframework.stereotype.Service;
import com.itextpdf.io.font.PdfEncodings;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import java.io.InputStream;

import java.io.ByteArrayOutputStream;

@Service
public class InvoiceService {
    public byte[] generateInvoicePdf(Order order) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PdfWriter writer = new PdfWriter(baos);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        // Load Unicode font (DejaVuSans.ttf) from resources/fonts
        PdfFont font = null;
        try {
            InputStream fontStream = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf");
            if (fontStream == null) {
                throw new RuntimeException("Font DejaVuSans.ttf not found in resources/fonts. Please add nó vào backend/src/main/resources/fonts/ và thử lại.");
            }
            font = PdfFontFactory.createFont(fontStream.readAllBytes(), PdfEncodings.IDENTITY_H);
        } catch (Exception e) {
            throw new RuntimeException("Không thể load font Unicode cho PDF: " + e.getMessage(), e);
        }
        document.setFont(font);

        document.add(new Paragraph("HÓA ĐƠN BÁN HÀNG").setBold().setFontSize(18));
        document.add(new Paragraph("Mã đơn: " + order.getOrderNumber()));
        document.add(new Paragraph("Khách hàng: " + order.getUser().getFullName()));
        document.add(new Paragraph("Địa chỉ giao hàng: " + order.getShippingAddress()));
        document.add(new Paragraph("Số điện thoại: " + order.getPhone()));
        document.add(new Paragraph("Ngày tạo: " + order.getCreatedAt()));

        document.add(new Paragraph("Danh sách sản phẩm:").setBold());

        Table table = new Table(new float[]{4, 2, 2, 2});
        table.setFont(font);
        table.addHeaderCell("Tên sản phẩm");
        table.addHeaderCell("Số lượng");
        table.addHeaderCell("Đơn giá");
        table.addHeaderCell("Thành tiền");

        order.getOrderItems().forEach(item -> {
            table.addCell(item.getProduct().getName());
            table.addCell(String.valueOf(item.getQuantity()));
            table.addCell(item.getPrice().toString());
            table.addCell(item.getPrice().multiply(
                    new java.math.BigDecimal(item.getQuantity())).toString());
        });

        document.add(table);
        document.add(new Paragraph("Tổng tiền: " + order.getTotalAmount() + " VND").setBold());

        document.close();
        return baos.toByteArray();
    }
}