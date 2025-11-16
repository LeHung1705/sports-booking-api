package com.example.booking_api.controller;

import com.example.booking_api.dto.voucher.*;
import com.example.booking_api.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/v1/vouchers")
public class VoucherController {

    private final VoucherService voucherService;

    public VoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    // Preview giảm giá trước khi user thanh toán
    @PostMapping("/preview")
    public ResponseEntity<PreviewResponse> preview(@RequestBody PreviewRequest req) {
        return ResponseEntity.ok(voucherService.preview(req));
    }

    // Redeem khi thanh toán thành công (ghi voucher_redemptions)
    @PostMapping("/redeem")
    public ResponseEntity<RedeemResponse> redeem(@RequestBody RedeemRequest req) {
        return ResponseEntity.ok(voucherService.redeem(req));
    }
}
