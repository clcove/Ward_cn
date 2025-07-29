package dev.leons.ward.dto;

import lombok.Getter;

import java.time.LocalDateTime;

/**
 * ErrorDto 是错误响应的容器
 *
 * @author Rudolf Barbu
 * @version 1.0.1
 */
@Getter
public final class ErrorDto
{
    /**
     *  错误时间戳字段
     */
    private final String timestamp = LocalDateTime.now().toString();

    /**
     *  错误消息字段
     */
    private final String errMessage;

    /**
     *  异常名称字段
     */
    private final String exceptionName;

    /**
     * errMessage 和 exceptionName 字段的 Setter
     *
     * @param exception thrown exception
     */
    public ErrorDto(final Exception exception)
    {
        this.errMessage = exception.getMessage();
        this.exceptionName = exception.getClass().getName();
    }
}