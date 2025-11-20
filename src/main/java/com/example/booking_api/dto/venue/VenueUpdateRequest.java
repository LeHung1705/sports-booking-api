package com.example.booking_api.dto.venue;

import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class VenueUpdateRequest {

    @Size(max = 120, message = "Tên sân tối đa 120 ký tự")
    private String name;

    @Size(max = 255, message = "Địa chỉ sân tối đa 120 ký tự")
    private String address;

    @Size(max = 80, message = "Quận tối đa 80 ký tự")
    private String district;

    @Size(max = 80, message = "Thành phố tối đa 80 ký tự")
    private String city;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    @Pattern(
            regexp = "^(?:\\+84|0084|0)[235789][0-9]{1,2}[0-9]{7}$",
            message = "Số điện thoại Việt Nam không hợp lệ"
    )
    private String phone;

    private String description;


    @DecimalMin(value = "-90.0", message = "Vĩ độ (lat) nằm ngoài phạm vi hợp lệ")
    @DecimalMax(value = "90.0", message = "Vĩ độ (lat) nằm ngoài phạm vi hợp lệ")
    private Double lat;

    @DecimalMin(value = "-180.0", message = "Kinh độ (lng) nằm ngoài phạm vi hợp lệ")
    @DecimalMax(value = "180.0", message = "Kinh độ (lng) nằm ngoài phạm vi hợp lệ")
    private Double lng;

    //    @Pattern(
//            regexp = "^(https?://[\\w\\-./%?=&]+\\.(png|jpg|jpeg|gif|webp))$",
//            message = "URL hình ảnh không hợp lệ"
//    )
    private String imageUrl;
}
