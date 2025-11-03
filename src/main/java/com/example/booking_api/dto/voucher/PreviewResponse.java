package com.example.booking_api.dto.voucher;

public class PreviewResponse {
    private boolean valid;
    private double discount;
    private String reason;

    public PreviewResponse() {}

    public PreviewResponse(boolean valid, double discount, String reason) {
        this.valid = valid;
        this.discount = discount;
        this.reason = reason;
    }

    public boolean isValid() { return valid; }
    public void setValid(boolean valid) { this.valid = valid; }

    public double getDiscount() { return discount; }
    public void setDiscount(double discount) { this.discount = discount; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
