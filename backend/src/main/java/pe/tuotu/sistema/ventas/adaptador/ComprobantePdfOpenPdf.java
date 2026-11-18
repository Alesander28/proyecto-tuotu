package pe.tuotu.sistema.ventas.adaptador;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;
import pe.tuotu.sistema.pedidos.dominio.DetallePedido;
import pe.tuotu.sistema.pedidos.dominio.Pedido;
import pe.tuotu.sistema.ventas.dominio.Comprobante;
import pe.tuotu.sistema.ventas.dominio.Venta;
import pe.tuotu.sistema.ventas.servicio.GeneradorComprobante;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Patrón Adapter: adapta la biblioteca OpenPDF (clases {@code Document}, {@code PdfWriter}, etc.) al contrato
 * {@link GeneradorComprobante} que usa el sistema. Si se cambia de biblioteca o de formato (por ejemplo, un ticket
 * para impresora térmica), solo se escribe otro adaptador. La boleta es de control interno, sin validez tributaria.
 */
@Component
public class ComprobantePdfOpenPdf implements GeneradorComprobante {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @Override
    public byte[] generar(Comprobante comprobante) {
        Venta venta = comprobante.getVenta();
        Pedido pedido = venta.getPedido();
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        Document documento = new Document(PageSize.A5, 36, 36, 36, 36);
        try {
            PdfWriter.getInstance(documento, salida);
            documento.open();

            Font titulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16);
            Font subtitulo = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11);
            Font normal = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font negrita = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10);
            Font nota = FontFactory.getFont(FontFactory.HELVETICA_OBLIQUE, 8);

            documento.add(centrado("Restaurante Tú o Tú", titulo));
            documento.add(centrado(comprobante.getTipo(), subtitulo));
            documento.add(centrado("N.° " + comprobante.getNumero(), negrita));
            documento.add(new Paragraph(" ", normal));
            documento.add(new Paragraph("Fecha: " + comprobante.getFechaEmision().format(FECHA), normal));
            documento.add(new Paragraph("Pedido: " + pedido.getCodigo() + "   Mesa: " + pedido.getMesa(), normal));
            documento.add(new Paragraph("Atendió: " + pedido.getMozo().getNombreCompleto(), normal));
            documento.add(new Paragraph("Cajero: " + venta.getCajero().getNombreCompleto(), normal));
            documento.add(new Paragraph("Método de pago: " + venta.getMetodoPago(), normal));
            documento.add(new Paragraph(" ", normal));

            PdfPTable tabla = new PdfPTable(new float[]{4.5f, 1f, 1.6f, 1.6f});
            tabla.setWidthPercentage(100);
            tabla.addCell(celda("Descripción", negrita, Element.ALIGN_LEFT));
            tabla.addCell(celda("Cant.", negrita, Element.ALIGN_CENTER));
            tabla.addCell(celda("P. unit.", negrita, Element.ALIGN_RIGHT));
            tabla.addCell(celda("Importe", negrita, Element.ALIGN_RIGHT));
            for (DetallePedido d : pedido.getDetalles()) {
                StringBuilder descripcion = new StringBuilder(d.getPresentacion().getProducto().getNombre())
                        .append(" (").append(d.getPresentacion().getNombre()).append(")");
                if (d.isExtraQueso()) {
                    descripcion.append(" + extra queso");
                }
                if (d.isParaLlevar()) {
                    descripcion.append(" - para llevar");
                }
                tabla.addCell(celda(descripcion.toString(), normal, Element.ALIGN_LEFT));
                tabla.addCell(celda(String.valueOf(d.getCantidad()), normal, Element.ALIGN_CENTER));
                tabla.addCell(celda(soles(d.getPrecioUnitario()), normal, Element.ALIGN_RIGHT));
                tabla.addCell(celda(soles(d.calcularSubtotal()), normal, Element.ALIGN_RIGHT));
            }
            documento.add(tabla);

            Paragraph total = new Paragraph("TOTAL: " + soles(venta.getTotal()), subtitulo);
            total.setAlignment(Element.ALIGN_RIGHT);
            total.setSpacingBefore(10);
            documento.add(total);

            Paragraph aviso = new Paragraph("Comprobante de consumo para control interno del restaurante. "
                    + "No es un comprobante de pago electrónico ni tiene validez tributaria.", nota);
            aviso.setAlignment(Element.ALIGN_CENTER);
            aviso.setSpacingBefore(20);
            documento.add(aviso);

            documento.close();
        } catch (DocumentException e) {
            throw new IllegalStateException("No se pudo generar el PDF del comprobante", e);
        }
        return salida.toByteArray();
    }

    private static Paragraph centrado(String texto, Font fuente) {
        Paragraph parrafo = new Paragraph(texto, fuente);
        parrafo.setAlignment(Element.ALIGN_CENTER);
        return parrafo;
    }

    private static PdfPCell celda(String texto, Font fuente, int alineacion) {
        PdfPCell celda = new PdfPCell(new Phrase(texto, fuente));
        celda.setHorizontalAlignment(alineacion);
        celda.setBorder(Rectangle.BOTTOM);
        celda.setPadding(3);
        return celda;
    }

    private static String soles(BigDecimal valor) {
        return String.format(Locale.US, "S/ %.2f", valor);
    }
}
