package com.example.grademanager.identity.config;

import com.example.grademanager.clazz.entity.SchoolClass;
import com.example.grademanager.clazz.mapper.SchoolClassMapper;
import com.example.grademanager.course.entity.Course;
import com.example.grademanager.course.mapper.CourseMapper;
import com.example.grademanager.identity.entity.User;
import com.example.grademanager.identity.mapper.UserMapper;
import com.example.grademanager.student.entity.Student;
import com.example.grademanager.student.mapper.StudentMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 首次启动补齐演示账号与示例数据（app.demo-data.enabled=true 时生效）。 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.demo-data.enabled", havingValue = "true", matchIfMissing = true)
public class DemoDataInitializer implements ApplicationRunner {

    private final UserMapper userMapper;
    private final SchoolClassMapper classMapper;
    private final StudentMapper studentMapper;
    private final CourseMapper courseMapper;
    private final PasswordEncoder passwordEncoder;

    public DemoDataInitializer(UserMapper userMapper, SchoolClassMapper classMapper,
                               StudentMapper studentMapper, CourseMapper courseMapper,
                               PasswordEncoder passwordEncoder) {
        this.userMapper = userMapper;
        this.classMapper = classMapper;
        this.studentMapper = studentMapper;
        this.courseMapper = courseMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(org.springframework.boot.ApplicationArguments args) {
        if (userMapper.findByUsername("teacher01") != null) {
            return;
        }
        User teacher = newUser("teacher01", "张老师", User.ROLE_TEACHER);
        User student = newUser("student01", "李同学", User.ROLE_STUDENT);
        userMapper.insert(teacher);
        userMapper.insert(student);

        SchoolClass cls = new SchoolClass();
        cls.setName("计算机2301班");
        classMapper.insert(cls);

        Student s1 = new Student();
        s1.setStudentNo("2023010101");
        s1.setName("李同学");
        s1.setGender("男");
        s1.setClassId(cls.getId());
        s1.setUserId(student.getId());
        studentMapper.insert(s1);

        Course course = new Course();
        course.setName("Java 程序设计");
        course.setTerm("2026-2027-1");
        course.setTeacherId(teacher.getId());
        course.setStatus(Course.STATUS_DRAFT);
        courseMapper.insert(course);

        log.info("演示数据初始化完成：teacher01/student01（口令 pass1234）、示例班级与学生、示例课程");
    }

    private User newUser(String username, String displayName, String role) {
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode("pass1234"));
        user.setDisplayName(displayName);
        user.setRole(role);
        return user;
    }
}
