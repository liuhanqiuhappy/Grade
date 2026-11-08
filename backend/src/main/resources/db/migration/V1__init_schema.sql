-- 全新实现：一次性建立全部业务表
CREATE TABLE sys_user (
    id            BIGINT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    display_name  VARCHAR(50)  NOT NULL,
    role          VARCHAR(20)  NOT NULL COMMENT 'TEACHER / STUDENT',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_username (username)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE school_class (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_class_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE student (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    student_no VARCHAR(50)  NOT NULL,
    name       VARCHAR(50)  NOT NULL,
    gender     VARCHAR(10)  NULL,
    class_id   BIGINT       NOT NULL,
    user_id    BIGINT       NULL COMMENT '绑定的登录账号（学生自助查分）',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_student_no (student_no),
    KEY idx_student_class (class_id),
    CONSTRAINT fk_student_class FOREIGN KEY (class_id) REFERENCES school_class (id),
    CONSTRAINT fk_student_user FOREIGN KEY (user_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE course (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    name       VARCHAR(100) NOT NULL,
    term       VARCHAR(50)  NOT NULL,
    teacher_id BIGINT       NOT NULL,
    status     VARCHAR(20)  NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT / CONFIRMED',
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_name_term (name, term),
    CONSTRAINT fk_course_teacher FOREIGN KEY (teacher_id) REFERENCES sys_user (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE course_member (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id  BIGINT   NOT NULL,
    student_id BIGINT   NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_course_student (course_id, student_id),
    CONSTRAINT fk_member_course FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT fk_member_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;

CREATE TABLE score (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    course_id  BIGINT       NOT NULL,
    student_id BIGINT       NOT NULL,
    score      DECIMAL(5,1) NULL,
    updated_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_score_course_student (course_id, student_id),
    CONSTRAINT fk_score_course FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT fk_score_student FOREIGN KEY (student_id) REFERENCES student (id)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4;
