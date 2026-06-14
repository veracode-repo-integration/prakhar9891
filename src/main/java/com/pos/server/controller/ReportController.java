package com.pos.server.controller;

import com.pos.server.model.Order;
import com.pos.server.model.Shift;
import com.pos.server.repository.ShiftRepository;
import com.pos.server.service.AuthService;
import com.pos.server.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/reports")
@CrossOrigin(origins = "*")
public class ReportController {

    @Autowired
    private OrderService orderService;

    @Autowired
    private ShiftRepository shiftRepository;

    @Autowired
    private AuthService authService;

    @GetMapping("/sales/csv")
    public ResponseEntity<byte[]> exportSalesCsv(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            List<Order> orders = orderService.getAllOrders(rid);

            StringWriter sw = new StringWriter();
            sw.append("Order ID,Invoice Number,Date,Order Type,Table ID,Status,Kitchen Status,Total Amount,Payment Mode\n");

            for (Order o : orders) {
                String id = escapeCsv(o.getId());
                String invoice = escapeCsv(o.getInvoiceNumber());
                String dateStr = o.getDate() != null ? o.getDate() : Instant.ofEpochMilli(o.getCreatedAt()).toString();
                String orderType = escapeCsv(o.getOrderType());
                String tableId = escapeCsv(o.getTableId());
                String status = escapeCsv(o.getStatus());
                String kitchenStatus = escapeCsv(o.getKitchenStatus());
                double amount = o.getTotal();
                String payMode = escapeCsv(o.getMethod());

                sw.append(id).append(",")
                  .append(invoice).append(",")
                  .append(escapeCsv(dateStr)).append(",")
                  .append(orderType).append(",")
                  .append(tableId).append(",")
                  .append(status).append(",")
                  .append(kitchenStatus).append(",")
                  .append(String.valueOf(amount)).append(",")
                  .append(payMode).append("\n");
            }

            byte[] csvBytes = sw.toString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"sales_report.csv\"");
            headers.set(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Error generating CSV: " + e.getMessage()).getBytes());
        }
    }

    @GetMapping("/shifts/csv")
    public ResponseEntity<byte[]> exportShiftsCsv(
            @RequestHeader(value = "X-Restaurant-Id", required = false) String restaurantId) {
        try {
            String rid = authService.validateRestaurantAccess(restaurantId);
            List<Shift> shifts = shiftRepository.findAllByRestaurantId(rid);

            StringWriter sw = new StringWriter();
            sw.append("Shift ID,User ID,Open Time,Close Time,Opening Balance,Cash Sales,UPI Sales,Card Sales,Expected Cash,Actual Cash,Status,Notes\n");

            for (Shift s : shifts) {
                String id = escapeCsv(s.getId());
                String userId = escapeCsv(s.getUserId());
                String openTime = Instant.ofEpochMilli(s.getOpenTime()).toString();
                String closeTime = s.getCloseTime() != null ? Instant.ofEpochMilli(s.getCloseTime()).toString() : "";
                double openBal = s.getOpeningBalance();
                double cash = s.getCashSales();
                double upi = s.getUpiSales();
                double card = s.getCardSales();
                double expected = s.getExpectedCash();
                double actual = s.getActualCash();
                String status = escapeCsv(s.getStatus());
                String notes = escapeCsv(s.getNotes());

                sw.append(id).append(",")
                  .append(userId).append(",")
                  .append(openTime).append(",")
                  .append(closeTime).append(",")
                  .append(String.valueOf(openBal)).append(",")
                  .append(String.valueOf(cash)).append(",")
                  .append(String.valueOf(upi)).append(",")
                  .append(String.valueOf(card)).append(",")
                  .append(String.valueOf(expected)).append(",")
                  .append(String.valueOf(actual)).append(",")
                  .append(status).append(",")
                  .append(notes).append("\n");
            }

            byte[] csvBytes = sw.toString().getBytes(StandardCharsets.UTF_8);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"shifts_report.csv\"");
            headers.set(HttpHeaders.CONTENT_TYPE, "text/csv; charset=UTF-8");

            return new ResponseEntity<>(csvBytes, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.status(500).body(("Error generating CSV: " + e.getMessage()).getBytes());
        }
    }

    private String escapeCsv(String val) {
        if (val == null) {
            return "";
        }
        String s = val.replace("\"", "\"\"");
        if (s.contains(",") || s.contains("\n") || s.contains("\r") || s.contains("\"")) {
            return "\"" + s + "\"";
        }
        return s;
    }
}
