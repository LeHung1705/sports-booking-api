// src/main/java/com/example/booking_api/controller/owner/OwnerVoucherController.java
package com.example.booking_api.controller.owner;

import com.example.booking_api.dto.voucher.VoucherRequest;
import com.example.booking_api.entity.Voucher;
import com.example.booking_api.service.VoucherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/v1/owner/vouchers") // 👈 đổi prefix
@RequiredArgsConstructor
public class OwnerVoucherController {

    private final VoucherService voucherService;

    private String currentUid() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        return (String) a.getPrincipal();
    }

    @PreAuthorize("hasRole('OWNER')")
    @PostMapping
    public ResponseEntity<Voucher> create(@RequestBody VoucherRequest req) {
        return ResponseEntity.ok(voucherService.createForOwner(currentUid(), req));
    }

    @PreAuthorize("hasRole('OWNER')")
    @PutMapping("/{id}")
    public ResponseEntity<Voucher> update(@PathVariable UUID id, @RequestBody VoucherRequest req) {
        return ResponseEntity.ok(voucherService.updateOwned(currentUid(), id, req));
    }

    @PreAuthorize("hasRole('OWNER')")
    @GetMapping
    public ResponseEntity<List<Voucher>> listMine() {
        return ResponseEntity.ok(voucherService.listByOwner(currentUid()));
    }

    @PreAuthorize("hasRole('OWNER')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        voucherService.deleteOwned(currentUid(), id);
        return ResponseEntity.noContent().build();
    }
}
