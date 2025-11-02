package com.example.booking_api.dto;


import jakarta.validation.constraints.*;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;

@Data
public class VenueCreateRequest {
    @NotBlank(message = "Tên sân không được để trống")
    @Size(max = 120, message = "Tên sân tối đa 120 ký tự")
    private String name;

    @NotBlank(message = "Địa chỉ không được để trống")
    @Size(max = 255, message = "Địa chỉ tối đa 255 ký tự")
    private String address;

    @NotBlank(message = "Thành phố không được để trống")
    @Size(max = 80, message = "Thành phố tối đa 80 ký tự")
    private String city;

    @NotBlank(message = "Quận/huyện không được để trống")
    @Size(max = 80, message = "Quận/huyện tối đa 80 ký tự")
    private String district;

    @NotNull(message = "Vĩ độ (lat) không được để trống")
    @DecimalMin(value = "-90.0", message = "Latitude không hợp lệ (min -90)")
    @DecimalMax(value = "90.0", message = "Latitude không hợp lệ (max 90)")
    private Double lat;

    @NotNull(message = "Kinh độ (lng) không được để trống")
    @DecimalMin(value = "-180.0", message = "Longitude không hợp lệ (min -180)")
    @DecimalMax(value = "180.0", message = "Longitude không hợp lệ (max 180)")
    private Double lng;

    @Size(max = 30, message = "Số điện thoại tối đa 30 ký tự")
    @Pattern(
            regexp = "^(?:\\+84|0084|0)[235789][0-9]{1,2}[0-9]{7}$",
            message = "Số điện thoại Việt Nam không hợp lệ"
    )
    private String phone;

    @Size(max = 5000, message = "Mô tả tối đa 5000 ký tự")
    private String description;

    @Size(max = 500, message = "URL hình ảnh tối đa 500 ký tự")
    private String imageUrl;

}