package com.campus.lostfound.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.List;

/** 发布/编辑信息请求（API 规格 3.1、3.2）。 */
public class ItemSaveDTO {

    /** 0 寻物，1 拾物 */
    @NotNull(message = "请选择信息类型")
    private Integer type;

    @NotBlank(message = "标题不能为空")
    @Size(max = 30, message = "标题不超过 30 字")
    private String title;

    @NotBlank(message = "描述不能为空")
    @Size(min = 10, max = 500, message = "描述需 10~500 字")
    private String description;

    @NotNull(message = "请选择分类")
    private Long categoryId;

    @NotNull(message = "请选择丢失/拾取时间")
    private LocalDateTime happenTime;

    @NotBlank(message = "地点不能为空")
    @Size(max = 100, message = "地点不超过 100 字")
    private String happenPlace;

    @NotBlank(message = "联系方式不能为空")
    @Size(max = 50, message = "联系方式不超过 50 字")
    private String contact;

    /** 0~3 个 /uploads/ 下的图片地址 */
    @Size(max = 3, message = "最多上传 3 张图片")
    private List<String> imageUrls;

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public LocalDateTime getHappenTime() {
        return happenTime;
    }

    public void setHappenTime(LocalDateTime happenTime) {
        this.happenTime = happenTime;
    }

    public String getHappenPlace() {
        return happenPlace;
    }

    public void setHappenPlace(String happenPlace) {
        this.happenPlace = happenPlace;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public List<String> getImageUrls() {
        return imageUrls;
    }

    public void setImageUrls(List<String> imageUrls) {
        this.imageUrls = imageUrls;
    }
}
