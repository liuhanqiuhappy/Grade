package com.example.grademanager.score.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("score")
public class Score {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long courseId;

    private Long studentId;

    /**
     * 成绩，null = 尚未录入。
     * updateStrategy = ALWAYS：MyBatis-Plus 默认 NOT_NULL，null 字段不会进 SET 子句，
     * 导致「清空成绩」只更新了 version、分数原样留在库里（H2）。这里显式允许写入 null。
     */
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private BigDecimal score;

    /** 乐观锁版本：多方同时保存时，后提交者更新失败（§4-1 并发保护）。 */
    @Version
    private Integer version;

    private LocalDateTime updatedAt;
}
