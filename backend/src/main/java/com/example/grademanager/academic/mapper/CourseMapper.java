package com.example.grademanager.academic.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.grademanager.academic.entity.Course;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CourseMapper extends BaseMapper<Course> {
}
