package com.campus.lostfound.service.impl;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.ResultCode;
import com.campus.lostfound.service.FileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

/**
 * 图片上传实现（详细设计 4.2.2）：类型、大小、文件头三重校验 + UUID 重命名 + 按年月分目录。
 */
@Service
public class FileServiceImpl implements FileService {

    private static final Logger log = LoggerFactory.getLogger(FileServiceImpl.class);

    private static final long MAX_SIZE = 2 * 1024 * 1024L;
    private static final Set<String> ALLOWED_EXT = Set.of("jpg", "jpeg", "png");
    private static final byte[] JPG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 'P', 'N', 'G'};

    @Override
    public String upload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.FILE_EMPTY);           // 4001
        }
        if (file.getSize() > MAX_SIZE) {
            throw new BusinessException(ResultCode.FILE_TOO_LARGE);       // 4002
        }
        String ext = extension(file.getOriginalFilename());
        if (!ALLOWED_EXT.contains(ext) || !magicMatches(file, ext)) {
            throw new BusinessException(ResultCode.FILE_TYPE_ERROR);      // 4003
        }

        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        String name = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        Path dir = Paths.get(System.getProperty("user.dir"), "uploads", month);
        try {
            Files.createDirectories(dir);
            file.transferTo(dir.resolve(name).toFile());
        } catch (IOException e) {
            log.error("保存图片失败", e);
            throw new BusinessException(ResultCode.SYSTEM_ERROR, "图片保存失败，请稍后重试");
        }
        return "/uploads/" + month + "/" + name;
    }

    private String extension(String filename) {
        if (filename == null) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase();
    }

    /** 文件头魔数校验：把 exe 改名成 .png 也会被拦下。 */
    private boolean magicMatches(MultipartFile file, String ext) {
        byte[] head = new byte[4];
        try (InputStream in = file.getInputStream()) {
            if (in.read(head) < head.length) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }
        if ("png".equals(ext)) {
            return startsWith(head, PNG_MAGIC);
        }
        return startsWith(head, JPG_MAGIC);
    }

    private boolean startsWith(byte[] data, byte[] prefix) {
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }
}
