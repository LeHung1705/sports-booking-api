package com.example.booking_api.controller.admin;

import com.example.booking_api.dto.voucher.VoucherRequest;
import com.example.booking_api.entity.Voucher;
import com.example.booking_api.service.VoucherService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/admin/vouchers")
public class AdminVoucherController {

    private final VoucherService voucherService;

    public AdminVoucherController(VoucherService voucherService) {
        this.voucherService = voucherService;
    }

    @PostMapping
    public ResponseEntity<Voucher> create(@RequestBody VoucherRequest req) {
        return ResponseEntity.ok(voucherService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Voucher> update(@PathVariable UUID id, @RequestBody VoucherRequest req) {
        return ResponseEntity.ok(voucherService.update(id, req));
    }

    @GetMapping
    public ResponseEntity<List<Voucher>> list() {
        return ResponseEntity.ok(voucherService.list());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        voucherService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
