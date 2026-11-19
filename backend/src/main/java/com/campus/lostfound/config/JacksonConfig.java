package com.campus.lostfound.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

/**
 * 时间格式统一（详细设计 2.5）：输出 {@code yyyy-MM-dd HH:mm:ss}；
 * 输入同时接受 {@code yyyy-MM-dd HH:mm:ss} 与 ISO 格式，兼容前端与接口测试的两种写法。
 */
@Configuration
public class JacksonConfig {

    public static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final List<DateTimeFormatter> INPUT_FORMATS = List.of(
            DATE_TIME,
            DateTimeFormatter.ISO_LOCAL_DATE_TIME,
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm"));

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer localDateTimeCustomizer() {
        return builder -> builder
                .serializers(new LocalDateTimeSerializer(DATE_TIME))
                .deserializers(new LocalDateTimeDeserializer(DATE_TIME))
                .deserializerByType(LocalDateTime.class, new FlexibleLocalDateTimeDeserializer());
    }

    /** 宽松解析：依次尝试多种格式，全部失败才报错。 */
    static class FlexibleLocalDateTimeDeserializer extends JsonDeserializer<LocalDateTime> {
        @Override
        public LocalDateTime deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            String text = parser.getText();
            if (text == null || text.isBlank()) {
                return null;
            }
            String value = text.trim();
            for (DateTimeFormatter format : INPUT_FORMATS) {
                try {
                    return LocalDateTime.parse(value, format);
                } catch (DateTimeParseException ignored) {
                    // 尝试下一种格式
                }
            }
            throw new IOException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss：" + value);
        }
    }
}
