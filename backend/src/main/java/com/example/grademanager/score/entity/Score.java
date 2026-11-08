package com.example.grademanager.score.entity;

import com.baomidou.mybatisplus.annotation.IdType;
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

    private BigDecimal score;

    /** 乐观锁版本：多方同时保存时，后提交者更新失败（§4-1 并发保护）。 */
    @Version
    private Integer version;

    private LocalDateTime updatedAt;
}
