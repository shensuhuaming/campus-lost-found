package com.campus.lostfound.service;

import org.springframework.web.multipart.MultipartFile;

/** 图片上传（详细设计 4.2.2）。 */
public interface FileService {

    /** 校验并保存图片，返回相对 URL（如 /uploads/202611/uuid.png）。 */
    String upload(MultipartFile file);
}
