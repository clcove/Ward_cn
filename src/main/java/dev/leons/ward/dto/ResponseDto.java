package dev.leons.ward.dto;

import lombok.Getter;

/**
 * ResponseDto 是一个用于呈现响应信息的值容器
 *
 * @author Rudolf Barbu
 * @version 1.0.0
 */
@Getter
public final class ResponseDto
{
    /**
     * 响应消息字段
     */
    private final String message;

    /**
     * 消息字段的 Setter
     *
     * @param message message to display
     */
    public ResponseDto(final String message)
    {
        this.message = message;
    }
}