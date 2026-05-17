package com.stockmaster.backend.service;

import com.stockmaster.backend.dto.MovementReportDto;
import com.stockmaster.backend.dto.SalesReportDto;
import com.stockmaster.backend.dto.StockReportDto;
import com.stockmaster.backend.dto.SupplierTraceabilityDto;
import com.stockmaster.backend.entity.InventoryMovement;
import com.stockmaster.backend.repository.InventoryMovementRepository;
import com.stockmaster.backend.repository.ProductRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.stockmaster.backend.entity.Inventory;
import com.stockmaster.backend.repository.InventoryRepository;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.*;

@Service
public class ReportService {

    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;
    @Autowired
    private InventoryRepository inventoryRepository;

    // HU14: Reporte de Stock Bajo
    public List<StockReportDto> getLowStockReport() {
        List<Inventory> lowStockItems = inventoryRepository.findItemsWithLowStock();
        return lowStockItems.stream()
                .map(inventory -> {
                    StockReportDto dto = new StockReportDto();
                    dto.setProductId(inventory.getProduct().getId());
                    dto.setProductName(inventory.getProduct().getName());
                    dto.setWarehouseName(inventory.getWarehouse().getName());
                    dto.setCurrentStock(inventory.getCurrentStock());
                    dto.setMinimumStock(inventory.getMinStock());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU15: Reporte de Movimientos por Fecha
    public List<MovementReportDto> getMovementReportByDate(LocalDate startDate, LocalDate endDate) {
        LocalDateTime finalStartDate = startDate.atStartOfDay();
        LocalDateTime finalEndDate   = endDate.atTime(23, 59, 59);
        List<InventoryMovement> movements =
                inventoryMovementRepository.findByMovementDateBetweenOrderByMovementDateAsc(finalStartDate, finalEndDate);
        return movements.stream()
                .map(m -> {
                    MovementReportDto dto = new MovementReportDto();
                    dto.setMovementDate(m.getMovementDate());
                    dto.setProductName(m.getProduct().getName());
                    dto.setMovementType(m.getMovementType());
                    dto.setQuantity(m.getQuantity());
                    dto.setWarehouseName(m.getWarehouse().getName());
                    dto.setUserName(m.getUser().getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU16: Reporte de Productos Más Vendidos
    public List<SalesReportDto> getMostSoldProductsReport() {
        List<Object[]> results = inventoryMovementRepository.findMostSoldProductsWithRevenueAndAveragePrice();
        return results.stream()
                .map(result -> {
                    SalesReportDto dto = new SalesReportDto();
                    dto.setProductId((Long)   result[0]);
                    dto.setProductName((String) result[1]);
                    dto.setUnitsSold((Long)   result[2]);
                    dto.setTotalRevenue((Double) result[3]);
                    dto.setAveragePrice((Double) result[4]);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU-PI2-09: Reporte de Trazabilidad por Proveedor
    public List<SupplierTraceabilityDto> getSupplierTraceabilityReport(Long supplierId) {
        List<Inventory> items = inventoryRepository.findBySupplierId(supplierId);
        return items.stream()
                .map(inventory -> {
                    SupplierTraceabilityDto dto = new SupplierTraceabilityDto();
                    dto.setProductName(inventory.getProduct().getName());
                    dto.setCategoryName(inventory.getProduct().getCategory().getName());
                    dto.setTotalStock(inventory.getCurrentStock());
                    dto.setWarehouseName(inventory.getWarehouse().getName());
                    return dto;
                })
                .collect(Collectors.toList());
    }

    // HU-PI2-05 | HU14: Stock Bajo → Excel
    public byte[] exportLowStockToExcel(List<StockReportDto> data) {
        String[] headers = {"ID Producto", "Nombre Producto", "Almacén", "Stock Actual", "Stock Mínimo"};
        String   title   = "Reporte de Stock Bajo";
        String   meta    = "Generado: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Stock Bajo");

            // Estilos
            ExcelStyles styles = new ExcelStyles(workbook);

            // Fila 0: título
            buildTitleRow(sheet, workbook, title, headers.length, styles);
            // Fila 1: metadatos
            buildMetaRow(sheet, meta, headers.length, styles);
            // Fila 2: vacía de separación
            sheet.createRow(2);
            // Fila 3: cabeceras
            buildHeaderRow(sheet, 3, headers, styles);

            // Filas de datos (desde fila 4)
            int rowNum = 4;
            for (StockReportDto dto : data) {
                Row row = sheet.createRow(rowNum);
                CellStyle dataStyle = (rowNum % 2 == 0) ? styles.dataEven : styles.dataOdd;

                createStyledCell(row, 0, String.valueOf(dto.getProductId()), dataStyle);
                createStyledCell(row, 1, dto.getProductName(),               dataStyle);
                createStyledCell(row, 2, dto.getWarehouseName(),             dataStyle);
                createStyledCell(row, 3, String.valueOf(dto.getCurrentStock()), styles.dangerCell);
                createStyledCell(row, 4, String.valueOf(dto.getMinimumStock()), styles.dangerCell);
                rowNum++;
            }

            autoSizeColumns(sheet, headers.length);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Error al generar Excel de Stock Bajo", e);
        }
    }

    // HU-PI2-05 | HU15: Movimientos → Excel
    public byte[] exportMovementsToExcel(List<MovementReportDto> data, LocalDate startDate, LocalDate endDate) {
        String[] headers = {"Fecha", "Producto", "Tipo", "Cantidad", "Almacén", "Usuario"};
        String   title   = "Reporte de Movimientos de Inventario";
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
        String   meta    = "Período: " + startDate.format(fmt) + " — " + endDate.format(fmt)
                         + "   |   Generado: " + LocalDate.now().format(fmt);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Movimientos");

            ExcelStyles styles = new ExcelStyles(workbook);

            buildTitleRow(sheet, workbook, title, headers.length, styles);
            buildMetaRow(sheet, meta, headers.length, styles);
            sheet.createRow(2);
            buildHeaderRow(sheet, 3, headers, styles);

            DateTimeFormatter dtFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            int rowNum = 4;
            for (MovementReportDto dto : data) {
                Row row = sheet.createRow(rowNum);
                CellStyle baseStyle  = (rowNum % 2 == 0) ? styles.dataEven : styles.dataOdd;
                boolean    isExit    = "SALIDA".equalsIgnoreCase(dto.getMovementType());
                CellStyle  typeStyle = isExit ? styles.dangerCell : styles.successCell;

                String fechaStr = dto.getMovementDate() != null
                        ? dto.getMovementDate().format(dtFmt) : "";

                createStyledCell(row, 0, fechaStr,                                baseStyle);
                createStyledCell(row, 1, dto.getProductName(),                    baseStyle);
                createStyledCell(row, 2, isExit ? "Salida" : "Entrada",           typeStyle);
                createStyledCell(row, 3, (isExit ? "-" : "+") + dto.getQuantity(), typeStyle);
                createStyledCell(row, 4, dto.getWarehouseName(),                  baseStyle);
                createStyledCell(row, 5, dto.getUserName(),                        baseStyle);
                rowNum++;
            }

            autoSizeColumns(sheet, headers.length);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Error al generar Excel de Movimientos", e);
        }
    }

    // HU-PI2-05 | HU16: Más Vendidos → Excel
    public byte[] exportTopSellingToExcel(List<SalesReportDto> data) {
        String[] headers = {"Posición", "Producto", "Unidades Vendidas", "Ingresos Generados", "Precio Promedio"};
        String   title   = "Reporte de Productos Más Vendidos";
        String   meta    = "Generado: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Más Vendidos");

            ExcelStyles styles = new ExcelStyles(workbook);

            buildTitleRow(sheet, workbook, title, headers.length, styles);
            buildMetaRow(sheet, meta, headers.length, styles);
            sheet.createRow(2);
            buildHeaderRow(sheet, 3, headers, styles);

            int rowNum = 4;
            int pos    = 1;
            for (SalesReportDto dto : data) {
                Row row = sheet.createRow(rowNum);
                CellStyle dataStyle = (rowNum % 2 == 0) ? styles.dataEven : styles.dataOdd;

                createStyledCell(row, 0, "#" + pos,                                                  styles.rankCell);
                createStyledCell(row, 1, dto.getProductName(),                                        dataStyle);
                createStyledCell(row, 2, dto.getUnitsSold() + " unidades",                           styles.successCell);
                createStyledCell(row, 3, "$" + String.format("%.2f", dto.getTotalRevenue()),         styles.successCell);
                createStyledCell(row, 4, "$" + String.format("%.2f", dto.getAveragePrice() != null
                        ? dto.getAveragePrice()
                        : (dto.getUnitsSold() > 0 ? dto.getTotalRevenue() / dto.getUnitsSold() : 0)), dataStyle);
                rowNum++;
                pos++;
            }

            autoSizeColumns(sheet, headers.length);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Error al generar Excel de Más Vendidos", e);
        }
    }

    // HU-PI2-09 (mejorado con estilos) + HU-PI2-05: Trazabilidad Proveedor → Excel
    public byte[] exportSupplierTraceabilityToExcel(Long supplierId) {
        List<SupplierTraceabilityDto> data = getSupplierTraceabilityReport(supplierId);
        return exportSupplierTraceabilityToExcel(data, "Proveedor #" + supplierId);
    }

    // Sobrecarga que acepta nombre del proveedor (llamada desde el controlador)
    public byte[] exportSupplierTraceabilityToExcel(List<SupplierTraceabilityDto> data, String supplierName) {
        String[] headers = {"Producto", "Categoría", "Stock Total", "Almacén"};
        String   title   = "Reporte de Trazabilidad por Proveedor";
        String   meta    = "Proveedor: " + supplierName
                         + "   |   Generado: " + LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Trazabilidad");

            ExcelStyles styles = new ExcelStyles(workbook);

            buildTitleRow(sheet, workbook, title, headers.length, styles);
            buildMetaRow(sheet, meta, headers.length, styles);
            sheet.createRow(2);
            buildHeaderRow(sheet, 3, headers, styles);

            int rowNum = 4;
            for (SupplierTraceabilityDto dto : data) {
                Row row = sheet.createRow(rowNum);
                CellStyle dataStyle = (rowNum % 2 == 0) ? styles.dataEven : styles.dataOdd;

                createStyledCell(row, 0, dto.getProductName(),              dataStyle);
                createStyledCell(row, 1, dto.getCategoryName(),             dataStyle);
                createStyledCell(row, 2, String.valueOf(dto.getTotalStock()), dataStyle);
                createStyledCell(row, 3, dto.getWarehouseName(),            dataStyle);
                rowNum++;
            }

            autoSizeColumns(sheet, headers.length);
            return toBytes(workbook);
        } catch (IOException e) {
            throw new RuntimeException("Error al generar Excel de Trazabilidad", e);
        }
    }

    /** Fila 0: Título grande con fondo naranja de marca */
    private void buildTitleRow(Sheet sheet, XSSFWorkbook wb, String title, int colCount, ExcelStyles styles) {
        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(32);
        Cell cell = titleRow.createCell(0);
        cell.setCellValue(title);
        cell.setCellStyle(styles.titleStyle);
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, colCount - 1));
    }

    /** Fila 1: Metadatos (fecha, filtros) con fondo gris claro */
    private void buildMetaRow(Sheet sheet, String meta, int colCount, ExcelStyles styles) {
        Row metaRow = sheet.createRow(1);
        metaRow.setHeightInPoints(18);
        Cell cell = metaRow.createCell(0);
        cell.setCellValue(meta);
        cell.setCellStyle(styles.metaStyle);
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, colCount - 1));
    }

    /** Fila de cabeceras con fondo naranja oscuro y texto blanco */
    private void buildHeaderRow(Sheet sheet, int rowIndex, String[] headers, ExcelStyles styles) {
        Row headerRow = sheet.createRow(rowIndex);
        headerRow.setHeightInPoints(20);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(styles.headerStyle);
        }
    }

    private void createStyledCell(Row row, int colIndex, String value, CellStyle style) {
        Cell cell = row.createCell(colIndex);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void autoSizeColumns(Sheet sheet, int colCount) {
        for (int i = 0; i < colCount; i++) {
            sheet.autoSizeColumn(i);
            // Añadir padding mínimo de 3 chars
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.max(width + 1024, 3000));
        }
    }

    private byte[] toBytes(XSSFWorkbook workbook) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        return out.toByteArray();
    }
    
    /**
     * Encapsula todos los CellStyle del workbook para no recrearlos en cada celda.
     * Colores de marca: Naranja primario #FF7B00, naranja oscuro #CC6200.
     */
    private static class ExcelStyles {

        final CellStyle titleStyle;
        final CellStyle metaStyle;
        final CellStyle headerStyle;
        final CellStyle dataEven;
        final CellStyle dataOdd;
        final CellStyle dangerCell;
        final CellStyle successCell;
        final CellStyle rankCell;

        ExcelStyles(XSSFWorkbook wb) {
            // Fuentes
            XSSFFont titleFont = wb.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 14);
            titleFont.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null)); // blanco

            XSSFFont metaFont = wb.createFont();
            metaFont.setFontHeightInPoints((short) 10);
            metaFont.setColor(new XSSFColor(new byte[]{(byte)80,(byte)80,(byte)80}, null));
            metaFont.setItalic(true);

            XSSFFont headerFont = wb.createFont();
            headerFont.setBold(true);
            headerFont.setFontHeightInPoints((short) 11);
            headerFont.setColor(new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255}, null));

            XSSFFont dataFont = wb.createFont();
            dataFont.setFontHeightInPoints((short) 10);

            XSSFFont dangerFont = wb.createFont();
            dangerFont.setFontHeightInPoints((short) 10);
            dangerFont.setBold(true);
            dangerFont.setColor(new XSSFColor(new byte[]{(byte)185,(byte)28,(byte)28}, null)); // rojo

            XSSFFont successFont = wb.createFont();
            successFont.setFontHeightInPoints((short) 10);
            successFont.setBold(true);
            successFont.setColor(new XSSFColor(new byte[]{(byte)21,(byte)128,(byte)61}, null)); // verde

            XSSFFont rankFont = wb.createFont();
            rankFont.setFontHeightInPoints((short) 10);
            rankFont.setBold(true);
            rankFont.setColor(new XSSFColor(new byte[]{(byte)255,(byte)123,(byte)0}, null)); // naranja

            // Colores de fondo
            XSSFColor orange       = new XSSFColor(new byte[]{(byte)255,(byte)123,(byte)0},  null); // #FF7B00
            XSSFColor darkOrange   = new XSSFColor(new byte[]{(byte)204,(byte)98,(byte)0},   null); // #CC6200
            XSSFColor lightGray    = new XSSFColor(new byte[]{(byte)245,(byte)245,(byte)245},null); // #F5F5F5
            XSSFColor white        = new XSSFColor(new byte[]{(byte)255,(byte)255,(byte)255},null);
            XSSFColor lightRed     = new XSSFColor(new byte[]{(byte)254,(byte)226,(byte)226},null); // fondo peligro
            XSSFColor lightGreen   = new XSSFColor(new byte[]{(byte)220,(byte)252,(byte)231},null); // fondo éxito
            XSSFColor lightOrange  = new XSSFColor(new byte[]{(byte)255,(byte)243,(byte)229},null); // fondo rank

            // ── titleStyle ──
            titleStyle = wb.createCellStyle();
            ((XSSFCellStyle) titleStyle).setFillForegroundColor(orange);
            titleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            titleStyle.setFont(titleFont);
            titleStyle.setAlignment(HorizontalAlignment.LEFT);
            titleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(titleStyle, BorderStyle.NONE);

            // ── metaStyle ──
            metaStyle = wb.createCellStyle();
            ((XSSFCellStyle) metaStyle).setFillForegroundColor(lightGray);
            metaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            metaStyle.setFont(metaFont);
            metaStyle.setAlignment(HorizontalAlignment.LEFT);
            metaStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(metaStyle, BorderStyle.NONE);

            // ── headerStyle ──
            headerStyle = wb.createCellStyle();
            ((XSSFCellStyle) headerStyle).setFillForegroundColor(darkOrange);
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setFont(headerFont);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(headerStyle, BorderStyle.THIN);

            // ── dataEven (fila par, blanco) ──
            dataEven = wb.createCellStyle();
            ((XSSFCellStyle) dataEven).setFillForegroundColor(white);
            dataEven.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dataEven.setFont(dataFont);
            dataEven.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(dataEven, BorderStyle.THIN);

            // ── dataOdd (fila impar, gris muy claro) ──
            dataOdd = wb.createCellStyle();
            ((XSSFCellStyle) dataOdd).setFillForegroundColor(lightGray);
            dataOdd.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dataOdd.setFont(dataFont);
            dataOdd.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(dataOdd, BorderStyle.THIN);

            // ── dangerCell (texto rojo, fondo rosado) ──
            dangerCell = wb.createCellStyle();
            ((XSSFCellStyle) dangerCell).setFillForegroundColor(lightRed);
            dangerCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            dangerCell.setFont(dangerFont);
            dangerCell.setAlignment(HorizontalAlignment.CENTER);
            dangerCell.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(dangerCell, BorderStyle.THIN);

            // ── successCell (texto verde, fondo verde claro) ──
            successCell = wb.createCellStyle();
            ((XSSFCellStyle) successCell).setFillForegroundColor(lightGreen);
            successCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            successCell.setFont(successFont);
            successCell.setAlignment(HorizontalAlignment.CENTER);
            successCell.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(successCell, BorderStyle.THIN);

            // ── rankCell (texto naranja, fondo naranja claro) ──
            rankCell = wb.createCellStyle();
            ((XSSFCellStyle) rankCell).setFillForegroundColor(lightOrange);
            rankCell.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            rankCell.setFont(rankFont);
            rankCell.setAlignment(HorizontalAlignment.CENTER);
            rankCell.setVerticalAlignment(VerticalAlignment.CENTER);
            setBorder(rankCell, BorderStyle.THIN);
        }

        private void setBorder(CellStyle style, BorderStyle borderStyle) {
            style.setBorderTop(borderStyle);
            style.setBorderBottom(borderStyle);
            style.setBorderLeft(borderStyle);
            style.setBorderRight(borderStyle);
        }
    }
}
