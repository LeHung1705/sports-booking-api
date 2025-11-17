package com.example.booking_api.service;

import com.example.booking_api.config.VnPayConfig;
import com.example.booking_api.dto.payment.PaymentCreateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
public class VnPayService {
    public PaymentCreateResponse createPayment(UUID bookingId) {
        BigDecimal totalAmount = BigDecimal.valueOf(100000); // demo 100,000 VND
        long vnpAmount = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();

        String vnpTxnRef = VnPayConfig.getRandomNumber(8);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", VnPayConfig.vnp_TmnCode);
        vnpParams.put("vnp_Amount", String.valueOf(vnpAmount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_BankCode", "NCB");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toán booking: " + bookingId);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_IpAddr", "127.0.0.1");
        vnpParams.put("vnp_ReturnUrl", VnPayConfig.vnp_ReturnUrl);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();

        for (Iterator<String> it = fieldNames.iterator(); it.hasNext();) {
            String key = it.next();
            String value = vnpParams.get(key);
            if (value != null && value.length() > 0) {
                hashData.append(key).append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                query.append(URLEncoder.encode(key, StandardCharsets.US_ASCII))
                        .append("=")
                        .append(URLEncoder.encode(value, StandardCharsets.US_ASCII));
                if (it.hasNext()) {
                    hashData.append("&");
                    query.append("&");
                }
            }
        }

        String secureHash = VnPayConfig.hmacSHA512(VnPayConfig.secretKey, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = VnPayConfig.vnp_PayUrl + "?" + query;

        return PaymentCreateResponse.builder()
                .paymentUrl(paymentUrl)
                .paymentId(UUID.randomUUID())
                .build();
    }
}
