package com.iqra.zakat.service;

import com.iqra.zakat.dto.AllocationDto;
import com.iqra.zakat.dto.AssessmentDto;
import com.iqra.zakat.dto.BeneficiaryDto;
import com.iqra.zakat.dto.PlanningDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExportService {

    public byte[] exportAllocationsToExcel(List<AllocationDto.Response> allocations) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Allocations");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            String[] headers = {
                    "Allocation ID", "Zakat Year", "Beneficiary", "Classification", "Category",
                    "Planned Amount", "Paid Amount", "Committed Amount", "Outstanding Amount",
                    "Payment Method", "Paid On", "Paid Source Account", "Status", "Approval Status",
                    "Currency", "School", "Current Std", "Remarks"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (AllocationDto.Response a : allocations) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getAllocationId() != null ? a.getAllocationId() : 0);
                row.createCell(1).setCellValue(a.getZakatYear() != null ? a.getZakatYear() : 0);
                row.createCell(2).setCellValue(a.getBeneficiary() != null ? a.getBeneficiary() : "");
                row.createCell(3).setCellValue(a.getClassification() != null ? a.getClassification() : "");
                row.createCell(4).setCellValue(a.getCategory() != null ? a.getCategory() : "");

                Cell c5 = row.createCell(5);
                c5.setCellValue(a.getPlannedAmount() != null ? a.getPlannedAmount().doubleValue() : 0.0);
                c5.setCellStyle(moneyStyle);

                Cell c6 = row.createCell(6);
                c6.setCellValue(a.getPaidAmount() != null ? a.getPaidAmount().doubleValue() : 0.0);
                c6.setCellStyle(moneyStyle);

                Cell c7 = row.createCell(7);
                c7.setCellValue(a.getCommittedAmount() != null ? a.getCommittedAmount().doubleValue() : 0.0);
                c7.setCellStyle(moneyStyle);

                Cell c8 = row.createCell(8);
                c8.setCellValue(a.getOutstandingAmount() != null ? a.getOutstandingAmount().doubleValue() : 0.0);
                c8.setCellStyle(moneyStyle);

                row.createCell(9).setCellValue(a.getPaymentMethod() != null ? a.getPaymentMethod() : "");
                row.createCell(10).setCellValue(a.getPaidOn() != null ? a.getPaidOn().toString() : "");
                row.createCell(11).setCellValue(a.getPaidSourceAccount() != null ? a.getPaidSourceAccount() : "");
                row.createCell(12).setCellValue(a.getStatus() != null ? a.getStatus() : "");
                row.createCell(13).setCellValue(a.getApprovalStatus() != null ? a.getApprovalStatus() : "");
                row.createCell(14).setCellValue(a.getCurrencyCode() != null ? a.getCurrencyCode() : "");
                row.createCell(15).setCellValue(a.getSchool() != null ? a.getSchool() : "");
                row.createCell(16).setCellValue(a.getCurrentStd() != null ? a.getCurrentStd() : "");
                row.createCell(17).setCellValue(a.getRemarks() != null ? a.getRemarks() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate allocations Excel", e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    public byte[] exportBeneficiariesToExcel(List<BeneficiaryDto.Response> beneficiaries) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Beneficiaries");
            CellStyle headerStyle = createHeaderStyle(workbook);

            String[] headers = {
                    "Beneficiary ID", "Code", "Name", "Category", "Contact Number", "Verification Status",
                    "Is Active", "Address", "Referred By", "Description"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (BeneficiaryDto.Response b : beneficiaries) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(b.getBeneficiaryId() != null ? b.getBeneficiaryId() : 0);
                row.createCell(1).setCellValue(b.getBeneficiaryCode() != null ? b.getBeneficiaryCode() : "");
                row.createCell(2).setCellValue(b.getName() != null ? b.getName() : "");
                row.createCell(3).setCellValue(b.getBeneficiaryCategory() != null ? b.getBeneficiaryCategory() : "");
                row.createCell(4).setCellValue(b.getContactNumber() != null ? b.getContactNumber() : "");
                row.createCell(5).setCellValue(b.getVerificationStatus() != null ? b.getVerificationStatus() : "");
                row.createCell(6).setCellValue(b.getIsActive() != null ? b.getIsActive() : "Y");
                row.createCell(7).setCellValue(b.getAddress() != null ? b.getAddress() : "");
                row.createCell(8).setCellValue(b.getReferredBy() != null ? b.getReferredBy() : "");
                row.createCell(9).setCellValue(b.getDescription() != null ? b.getDescription() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate beneficiaries Excel", e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    public byte[] exportAssessmentsToExcel(List<AssessmentDto.Response> assessments) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Zakat Assessments");
            CellStyle headerStyle = createHeaderStyle(workbook);
            CellStyle moneyStyle = createMoneyStyle(workbook);

            String[] headers = {
                    "Zakat Year", "Zakat Amount", "Carry Forward", "Net Payable",
                    "Total Allocated Amount", "Remaining Balance", "Status", "Remarks"
            };

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            for (AssessmentDto.Response a : assessments) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(a.getZakatYear() != null ? a.getZakatYear() : 0);

                Cell c1 = row.createCell(1);
                c1.setCellValue(a.getZakatAmount() != null ? a.getZakatAmount().doubleValue() : 0.0);
                c1.setCellStyle(moneyStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(a.getCarryForward() != null ? a.getCarryForward().doubleValue() : 0.0);
                c2.setCellStyle(moneyStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(a.getNetPayable() != null ? a.getNetPayable().doubleValue() : 0.0);
                c3.setCellStyle(moneyStyle);

                Cell c4 = row.createCell(4);
                c4.setCellValue(a.getTotalAllocatedAmount() != null ? a.getTotalAllocatedAmount().doubleValue() : 0.0);
                c4.setCellStyle(moneyStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(a.getRemainingBalance() != null ? a.getRemainingBalance().doubleValue() : 0.0);
                c5.setCellStyle(moneyStyle);

                row.createCell(6).setCellValue(a.getStatus() != null ? a.getStatus() : "");
                row.createCell(7).setCellValue(a.getRemarks() != null ? a.getRemarks() : "");
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("Failed to generate assessments Excel", e);
            throw new RuntimeException("Excel generation failed", e);
        }
    }

    private CellStyle createHeaderStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        return style;
    }

    private CellStyle createMoneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        DataFormat format = workbook.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        style.setAlignment(HorizontalAlignment.RIGHT);
        return style;
    }
}
