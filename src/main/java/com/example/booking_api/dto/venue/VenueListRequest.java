package com.example.booking_api.dto.venue;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VenueListRequest {

    @Size(max = 120, message = "Từ khóa tìm kiếm tối đa 120 ký tự")
    private String q;

    @Size(max = 80, message = "Tên thành phố tối đa 80 ký tự")
    private String city;

    @Pattern(
            regexp = "FOOTBALL|BADMINTON|TENNIS|BASKETBALL|VOLLEYBALL",
            message = "Loại môn thể thao không hợp lệ"
    )
    private String sport;

    @DecimalMin(value = "-90.0", message = "Vĩ độ (lat) nằm ngoài phạm vi hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ (lat) nằm ngoài phạm vi hợp lệ")
    private Double lat;

    @DecimalMin(value = "-180.0", message = "Kinh độ (lng) nằm ngoài phạm vi hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ (lng) nằm ngoài phạm vi hợp lệ")
    private Double lng;

    @Positive(message = "Bán kính (radius) phải lớn hơn 0 km")
    private Double radius;
}
