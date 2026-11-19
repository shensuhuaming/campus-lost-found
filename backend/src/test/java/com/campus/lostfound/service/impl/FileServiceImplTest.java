package com.campus.lostfound.service.impl;

import com.campus.lostfound.common.BusinessException;
import com.campus.lostfound.common.ResultCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** U2.2 图片上传（详细设计 4.2.2）：空文件、超大、类型、魔数、落盘。 */
class FileServiceImplTest {

    private final FileServiceImpl fileService = new FileServiceImpl();

    @TempDir
    Path tempDir;

    private String originalUserDir;

    @BeforeEach
    void setUp() {
        originalUserDir = System.getProperty("user.dir");
        System.setProperty("user.dir", tempDir.toString());
    }

    @AfterEach
    void tearDown() {
        System.setProperty("user.dir", originalUserDir);
    }

    @Test
    @DisplayName("空文件返回 4001")
    void upload_empty_returns4001() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[0]);

        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.FILE_EMPTY);
    }

    @Test
    @DisplayName("超过 2MB 返回 4002")
    void upload_tooLarge_returns4002() {
        byte[] data = new byte[2 * 1024 * 1024 + 1];
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", data);

        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("扩展名不在白名单返回 4003")
    void upload_badExtension_returns4003() {
        MockMultipartFile file = new MockMultipartFile("file", "a.exe", "application/octet-stream",
                new byte[]{(byte) 0x4D, 0x5A});

        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.FILE_TYPE_ERROR);
    }

    @Test
    @DisplayName("改扩展名伪装（png 头却是 jpg 内容）被魔数校验拦下返回 4003")
    void upload_magicMismatch_returns4003() {
        byte[] jpgBytes = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "fake.png", "image/png", jpgBytes);

        assertThatThrownBy(() -> fileService.upload(file))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.FILE_TYPE_ERROR);
    }

    @Test
    @DisplayName("合法 PNG：返回 /uploads/yyyyMM/uuid.png 且文件落盘")
    void upload_success_writesFile() throws Exception {
        byte[] png = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
        MockMultipartFile file = new MockMultipartFile("file", "wallet.png", "image/png", png);

        String url = fileService.upload(file);

        assertThat(url).matches("^/uploads/\\d{6}/[0-9a-f]{32}\\.png$");
        Path saved = tempDir.resolve("uploads").resolve(url.substring("/uploads/".length()));
        assertThat(Files.exists(saved)).isTrue();
        assertThat(Files.size(saved)).isEqualTo(png.length);
    }
}
