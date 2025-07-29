package dev.leons.ward.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * SetupDto 是设置数据的值容器
 *
 * @author Rudolf Barbu
 * @version 1.0.3
 */
@Getter
@Setter
public class SetupDto
{
    /**
     * 服务器名称字段
     */
    @NotNull
    @Size(min = 0, max = 10)
    private String serverName;

    /**
     * 主题名称字段
     */
    @NotNull
    @NotEmpty
    @Pattern(regexp = "light|dark")
    private String theme;

    /**
     * 端口端口字段
     */
    @NotNull
    @NotEmpty
    @Min(value = 10)
    @Max(value = 65535)
    private String port;

    /**
     * 启用雾场
     */
    @NotNull
    @NotEmpty
    @Pattern(regexp = "true|false")
    private String enableFog;

    /**
     * 背景颜色字段
     */
    @NotEmpty
    @Pattern(regexp = "^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$|default")
    private String backgroundColor;
}