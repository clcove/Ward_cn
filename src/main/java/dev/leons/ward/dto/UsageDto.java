package dev.leons.ward.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * UsageDto 是一个值容器，用于显示服务器使用情况
 *
 * @author Rudolf Barbu
 * @version 1.0.1
 */
@Getter
@Setter
public class UsageDto
{
    /**
     * 处理器使用情况字段
     */
    private int processor;

    /**
     * 内存使用字段
     */
    private int ram;

    /**
     * 存储使用情况字段
     */
    private int storage;
}