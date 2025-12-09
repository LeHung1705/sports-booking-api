package com.example.booking_api.service;

import com.example.booking_api.config.VnPayConfig;
import com.example.booking_api.dto.payment.PaymentCreateRequest;
import com.example.booking_api.dto.payment.PaymentCreateResponse;
import com.example.booking_api.dto.payment.VnPayReturnResponse;
import com.example.booking_api.entity.Booking;
import com.example.booking_api.entity.Payment;
import com.example.booking_api.entity.User;
import com.example.booking_api.entity.enums.BookingStatus;
import com.example.booking_api.entity.enums.PaymentStatus;
import com.example.booking_api.repository.BookingRepository;
import com.example.booking_api.repository.PaymentRepository;
import com.example.booking_api.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PaymentService {
    private final BookingRepository bookingRepository;
    private final PaymentRepository paymentRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PaymentCreateResponse createVnPayPayment(String firebaseUid, PaymentCreateRequest req, HttpServletRequest httpRequest) {
        User me = userRepository.findByFirebaseUid(firebaseUid).orElseThrow(() -> new RuntimeException("User not found"));
        Booking booking = bookingRepository.findById(req.getBookingId()).orElseThrow(() -> new RuntimeException("Booking not found"));

        if (!booking.getUser().getId().equals(me.getId())) {
            throw new SecurityException("FORBIDDEN");
        }

        if (booking.getStatus() != BookingStatus.PENDING) {
            throw new RuntimeException("BOOKING_STATUS_NOT_ALLOWED");
        }

        BigDecimal totalAmount = booking.getTotalAmount();
        if (totalAmount == null || totalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("BOOKING_AMOUNT_INVALID");
        }

        Payment payment = new Payment();
        payment.setBooking(booking);
        payment.setProvider("VNPAY");
        payment.setStatus(PaymentStatus.INIT);
        payment.setAmount(totalAmount);
        payment.setCreatedAt(java.time.OffsetDateTime.now());
        payment.setUpdatedAt(java.time.OffsetDateTime.now());

        String vnpTxnRef = VnPayConfig.getRandomNumber(10);
        payment.setVnpTxnRef(vnpTxnRef);

        payment = paymentRepository.save(payment);

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", "2.1.0");
        vnpParams.put("vnp_Command", "pay");
        vnpParams.put("vnp_TmnCode", VnPayConfig.VNP_TMN_CODE);

        long amount = totalAmount.multiply(BigDecimal.valueOf(100)).longValue();
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toán booking: " + booking.getId());
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", VnPayConfig.VNP_RETURN_URL);
        // Không dùng IPN thật ở đồ án
        // vnpParams.put("vnp_IpnUrl", VnPayConfig.VNP_IPN_URL);

        String ipAddr = VnPayConfig.getIpAddress(httpRequest);
        vnpParams.put("vnp_IpAddr", ipAddr);

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Etc/GMT+7"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        StringBuilder hashData = new StringBuilder();

        for (Iterator<String> it = fieldNames.iterator(); it.hasNext(); ) {
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

        String secureHash = VnPayConfig.hmacSHA512(VnPayConfig.VNP_HASH_SECRET, hashData.toString());
        query.append("&vnp_SecureHash=").append(secureHash);

        String paymentUrl = VnPayConfig.VNP_PAY_URL + "?" + query;

        return PaymentCreateResponse.builder()
                .paymentId(payment.getId())
                .paymentUrl(paymentUrl)
                .build();
    }


    @Transactional
    public VnPayReturnResponse handleVnPayIpn(Map<String, String> allParams) {
        String vnpSecureHash = allParams.get("vnp_SecureHash");
        String vnpTxnRef = allParams.get("vnp_TxnRef");
        String vnpResponseCode = allParams.get("vnp_ResponseCode");       // 00, 24, 91, 97, 13, ...
        String vnpTransactionStatus = allParams.get("vnp_TransactionStatus"); // 00: success
        String vnpTmnCode = allParams.get("vnp_TmnCode");
        String vnpAmountStr = allParams.get("vnp_Amount");

        if (!validateChecksum(allParams, vnpSecureHash)) {
            return VnPayReturnResponse.builder()
                    .bookingId(null)
                    .paymentId(null)
                    .paymentStatus(PaymentStatus.FAILED)
                    .vnpResponseCode("97")
                    .vnpTransactionStatus(null)
                    .message("Sai checksum (vnp_SecureHash không hợp lệ)")
                    .build();
        }


        if (vnpTxnRef == null) {
            throw new RuntimeException("Missing vnp_TxnRef");
        }

        Payment payment = paymentRepository.findByVnpTxnRef(vnpTxnRef)
                .orElseThrow(() -> new RuntimeException("Payment not found with vnp_TxnRef=" + vnpTxnRef));

        Booking booking = payment.getBooking();


        if (!Objects.equals(vnpTmnCode, VnPayConfig.VNP_TMN_CODE)) {
            payment.setStatus(PaymentStatus.FAILED);
            saveReturnPayload(payment, allParams);

            return VnPayReturnResponse.builder()
                    .bookingId(booking.getId())
                    .paymentId(payment.getId())
                    .paymentStatus(payment.getStatus())
                    .vnpResponseCode("97")
                    .vnpTransactionStatus(vnpTransactionStatus)
                    .message("Sai TmnCode")
                    .build();
        }


        if (vnpAmountStr == null) {
            payment.setStatus(PaymentStatus.FAILED);
            saveReturnPayload(payment, allParams);

            return VnPayReturnResponse.builder()
                    .bookingId(booking.getId())
                    .paymentId(payment.getId())
                    .paymentStatus(payment.getStatus())
                    .vnpResponseCode("13")
                    .vnpTransactionStatus(vnpTransactionStatus)
                    .message("Thiếu vnp_Amount")
                    .build();
        }

        BigDecimal amountFromVnp = new BigDecimal(vnpAmountStr).divide(BigDecimal.valueOf(100));

        if (payment.getAmount() == null ||
                payment.getAmount().compareTo(amountFromVnp) != 0) {

            payment.setStatus(PaymentStatus.FAILED);
            saveReturnPayload(payment, allParams);

            return VnPayReturnResponse.builder()
                    .bookingId(booking.getId())
                    .paymentId(payment.getId())
                    .paymentStatus(payment.getStatus())
                    .vnpResponseCode("13")
                    .vnpTransactionStatus(vnpTransactionStatus)
                    .message("Sai số tiền (không khớp với Payment trong hệ thống)")
                    .build();
        }


        String message;

        if ("00".equals(vnpResponseCode) && "00".equals(vnpTransactionStatus)) {
            payment.setStatus(PaymentStatus.SUCCESS);

            String vnpTransactionNo = allParams.get("vnp_TransactionNo");
            payment.setProviderTxnRef(vnpTransactionNo);

            String vnpPayDate = allParams.get("vnp_PayDate"); // yyyyMMddHHmmss
            if (vnpPayDate != null) {
                // Payment entity uses OffsetDateTime, so we need to convert to OffsetDateTime
                // NOTE: The previous instruction said "Booking entity to use LocalDateTime",
                // but Payment entity was not mentioned to be changed.
                // However, I see "PaymentService.java is still trying to assign OffsetDateTime values to the Booking entity".
                // So I must fix the assignment to Booking.
                
                // Let's check where Booking is used.
                // booking.setUpdatedAt(OffsetDateTime.now()) -> CHANGE TO LocalDateTime.now()
            
                payment.setPaidAt(parseVnpPayDate(vnpPayDate));
            }
            booking.setStatus(BookingStatus.CONFIRMED);
            booking.setUpdatedAt(LocalDateTime.now()); // FIXED: Use LocalDateTime
            bookingRepository.save(booking);
            message = "Thanh toán thành công";
        } else if ("24".equals(vnpResponseCode)) {
            payment.setStatus(PaymentStatus.FAILED);
            booking.setStatus(BookingStatus.CANCELED);
            booking.setUpdatedAt(LocalDateTime.now()); // FIXED: Use LocalDateTime
            bookingRepository.save(booking);
            message = "Giao dịch bị hủy bởi người dùng";
        } else if ("91".equals(vnpResponseCode)) {
            payment.setStatus(PaymentStatus.FAILED);
            message = "Giao dịch chưa hoàn thành hoặc bị timeout (91)";
        } else if ("97".equals(vnpResponseCode)) {
            payment.setStatus(PaymentStatus.FAILED);
            message = "Sai checksum (97)";
        } else if ("13".equals(vnpResponseCode)) {
            payment.setStatus(PaymentStatus.FAILED);
            message = "Sai số tiền (13)";
        } else {
            payment.setStatus(PaymentStatus.FAILED);
            message = "Giao dịch thất bại (ResponseCode=" + vnpResponseCode + ")";
        }

        saveReturnPayload(payment, allParams);

        return VnPayReturnResponse.builder()
                .bookingId(booking.getId())
                .paymentId(payment.getId())
                .paymentStatus(payment.getStatus())
                .vnpResponseCode(vnpResponseCode)
                .vnpTransactionStatus(vnpTransactionStatus)
                .message(message)
                .build();
    }

    private boolean validateChecksum(Map<String, String> allParams, String vnpSecureHash) {
        if (vnpSecureHash == null) {
            return false;
        }

        // Lấy các key vnp_ trừ vnp_SecureHash, vnp_SecureHashType
        Map<String, String> sorted = new TreeMap<>();
        allParams.forEach((k, v) -> {
            if (k.startsWith("vnp_")
                    && !"vnp_SecureHash".equals(k)
                    && !"vnp_SecureHashType".equals(k)) {
                sorted.put(k, v);
            }
        });

        StringBuilder hashData = new StringBuilder();
        Iterator<Map.Entry<String, String>> it = sorted.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, String> entry = it.next();
            hashData.append(entry.getKey())
                    .append("=")
                    .append(URLEncoder.encode(entry.getValue(), StandardCharsets.US_ASCII));
            if (it.hasNext()) {
                hashData.append("&");
            }
        }

        String signValue = VnPayConfig.hmacSHA512(VnPayConfig.VNP_HASH_SECRET, hashData.toString());
        return signValue.equalsIgnoreCase(vnpSecureHash);
    }

    private void saveReturnPayload(Payment payment, Map<String, String> allParams) {
        try {
            payment.setReturnPayload(objectMapper.writeValueAsString(allParams));
        } catch (JsonProcessingException e) {
            payment.setReturnPayload(null);
        }
        payment.setUpdatedAt(java.time.OffsetDateTime.now());
        paymentRepository.save(payment);
    }

    private java.time.OffsetDateTime parseVnpPayDate(String vnpPayDate) {
        try {
            // VNPay sends date in GMT+7
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            sdf.setTimeZone(TimeZone.getTimeZone("Etc/GMT+7"));
            Date date = sdf.parse(vnpPayDate);
            return date.toInstant().atOffset(java.time.ZoneOffset.ofHours(7));
        } catch (ParseException e) {
            return java.time.OffsetDateTime.now();
        }
    }
}