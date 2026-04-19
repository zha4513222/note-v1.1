# 笔记管理系统

基于 Spring Boot + Vue 3 的笔记管理系统，支持笔记管理、关键词提取、标签推荐、搜索和统计可视化。

## 环境要求

- **JDK**: 1.8+
- **Maven**: 3.6+
- **Node.js**: 16+
- **MySQL**: 5.7+ / 8.0+

## 一、数据库配置

### 1.1 创建数据库

```sql
-- 登录 MySQL
mysql -u root -p

-- 创建数据库
CREATE DATABASE note_db DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

### 1.2 初始化表结构

执行数据库初始化脚本：

```bash
cd note-service
mysql -u root -p note_db < src/main/resources/db/schema.sql
```

或直接复制以下 SQL 到 MySQL 客户端执行：

```sql
-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(128) NOT NULL,
    email VARCHAR(100),
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 分类表
CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT DEFAULT 0,
    sort_order INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 笔记表（包含置顶和封面图字段）
CREATE TABLE IF NOT EXISTS note (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    content_text MEDIUMTEXT,
    category_id BIGINT,
    view_count INT DEFAULT 0,
    like_count INT DEFAULT 0,
    status TINYINT DEFAULT 1 COMMENT '1-草稿 2-已发布 3-已删除',
    pinned_at DATETIME DEFAULT NULL COMMENT '置顶时间',
    pin_duration INT DEFAULT -1 COMMENT '置顶时长(天),0=永久,-1=未置顶',
    cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图URL',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_category_id (category_id),
    INDEX idx_status (status),
    INDEX idx_pinned_at (pinned_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 标签表
CREATE TABLE IF NOT EXISTS tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE,
    use_count INT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 笔记-标签关联表
CREATE TABLE IF NOT EXISTS note_tag (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    note_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_note_tag (note_id, tag_id),
    INDEX idx_tag_id (tag_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 笔记图片关联表（用于存储笔记中的图片列表）
CREATE TABLE IF NOT EXISTS note_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    note_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    image_order INT DEFAULT 0 COMMENT '图片顺序',
    is_cover TINYINT DEFAULT 0 COMMENT '是否为封面图',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_note_id (note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记图片关联表';

-- 关键词表
CREATE TABLE IF NOT EXISTS keyword (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    note_id BIGINT NOT NULL,
    word VARCHAR(100) NOT NULL,
    weight FLOAT DEFAULT 0.0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_note_id (note_id),
    INDEX idx_word (word)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户标签使用历史
CREATE TABLE IF NOT EXISTS user_tag_history (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    tag_id BIGINT NOT NULL,
    use_count INT DEFAULT 1,
    last_used_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_tag (user_id, tag_id),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 用户每日统计表
CREATE TABLE IF NOT EXISTS user_daily_stats (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    stat_date DATE NOT NULL,
    note_count INT DEFAULT 0,
    total_note_count INT DEFAULT 0,
    tag_count INT DEFAULT 0,
    UNIQUE KEY uk_user_date (user_id, stat_date),
    INDEX idx_user_id (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 1.3 现有数据库升级（重要）

如果已有旧版本数据库，需要执行以下升级 SQL：

```sql
-- 添加置顶和封面图字段
ALTER TABLE note
  ADD COLUMN pinned_at DATETIME DEFAULT NULL COMMENT '置顶时间',
  ADD COLUMN pin_duration INT DEFAULT -1 COMMENT '置顶时长(天),0=永久,-1=未置顶',
  ADD COLUMN cover_image VARCHAR(255) DEFAULT NULL COMMENT '封面图URL';

-- 添加置顶时间索引
ALTER TABLE note ADD INDEX idx_pinned_at (pinned_at);

-- 创建笔记图片关联表
CREATE TABLE IF NOT EXISTS note_image (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    note_id BIGINT NOT NULL,
    image_url VARCHAR(255) NOT NULL,
    image_order INT DEFAULT 0,
    is_cover TINYINT DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_note_id (note_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='笔记图片关联表';
```

### 1.4 修改数据库连接配置

编辑 `note-service/src/main/resources/application.yml`：

```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/note_db?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true
    username: root        # 修改为你的用户名
    password: your_password  # 修改为你的密码
```

## 二、后端启动

### 2.1 进入后端目录

```bash
cd note-service
```

### 2.2 构建项目

```bash
mvn clean package
```

### 2.3 启动服务

```bash
# 开发模式启动（自动加载配置）
mvn spring-boot:run

# 或运行打包后的 JAR
java -jar target/note-service-1.0.0.jar
```

服务启动后运行在 `http://localhost:8082`

## 三、前端启动

### 3.1 进入前端目录

```bash
cd note-web
```

### 3.2 安装依赖

```bash
npm install
```

### 3.3 启动开发服务器

```bash
npm run dev
```

前端启动后访问 `http://localhost:5173`

## 四、功能使用

### 4.1 笔记管理

- **创建笔记**：点击「新建笔记」按钮，输入标题和内容
- **编辑笔记**：点击笔记卡片进入详情，点击编辑按钮
- **删除笔记**：在笔记列表勾选后点击删除，或在详情页删除
- **分类浏览**：使用左侧分类栏筛选笔记

### 4.2 富文本编辑

编辑器支持：
- 标题（1-6级）
- 加粗、斜体、下划线
- 列表（有序、无序）
- 链接、图片插入
- 引用块

### 4.3 图片上传（新功能）

编辑笔记时可上传图片：
- **上传限制**：单个文件最大 5MB，支持 jpg/png/gif/webp 格式
- **数量限制**：每篇笔记最多 10 张图片
- **封面图**：自动提取第一张图作为封面，可手动选择其他图片
- **存储位置**：图片存储在 `uploads/` 目录下

配置文件上传（`application.yml`）：
```yaml
file:
  upload:
    path: ./uploads            # 图片存储路径（可修改为绝对路径）
    max-size: 5242880          # 单文件最大 5MB（字节）
    allowed-types: jpg,jpeg,png,gif,webp
    max-images-per-note: 10    # 每篇笔记最多图片数
```

### 4.4 笔记置顶（新功能）

创建或编辑笔记时可设置置顶：
- **永久置顶**：一直保持在列表顶部
- **7天置顶**：7天后自动取消置顶
- **3天置顶**：3天后自动取消置顶
- **1天置顶**：1天后自动取消置顶
- **取消置顶**：在笔记卡片上点击「取消置顶」按钮

排序规则：置顶笔记 > 置顶时间 > 更新时间 > 创建时间

配置置顶设置（`application.yml`）：
```yaml
note:
  pin:
    durations: 0,7,3,1        # 可选置顶时长（天），0=永久
    default-duration: 0       # 默认永久置顶
```

### 4.5 标签系统

- **添加标签**：编辑笔记时在标签区域输入标签名
- **推荐标签**：系统根据笔记内容和用户历史自动推荐
- **热门标签**：显示全站使用最多的标签

推荐算法权重（可在 `application.yml` 调整）：
- 关键词匹配：30分
- 用户历史使用：25分
- 热门标签：10分
- 时间新近性：5分（7天内使用+5分，30天内+2.5分）

### 4.6 关键词提取

系统使用 Jieba 中文分词自动提取笔记关键词：

1. 创建或编辑笔记时，系统自动提取内容关键词
2. 关键词显示在笔记详情页
3. 支持手动提取：发送内容到 `/api/notes/extract-keywords`

### 4.7 搜索功能

- **关键词搜索**：在搜索框输入标题或内容关键词
- **标签筛选**：选择标签进行精确筛选
- **搜索建议**：输入时显示标题自动补全

### 4.8 统计可视化

访问 `/stats` 页面查看：

- **笔记总数/已发布/草稿**数量统计
- **近7天笔记创建趋势**折线图
- **标签使用分布**饼图
- **分类分布**统计

## 五、API 接口

### 5.1 笔记接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/notes | 获取笔记列表 |
| GET | /api/notes/{id} | 获取笔记详情 |
| POST | /api/notes | 创建笔记 |
| PUT | /api/notes/{id} | 更新笔记 |
| DELETE | /api/notes/{id} | 删除笔记 |
| POST | /api/notes/{id}/publish | 发布笔记 |
| PUT | /api/notes/{id}/pin | 更新置顶状态（新） |
| DELETE | /api/notes/{id}/pin | 取消置顶（新） |
| POST | /api/notes/extract-keywords | 提取关键词 |

### 5.2 文件上传接口（新）

| 方法 | 路径 | 说明 |
|------|------|------|
| POST | /api/files/upload/image | 上传图片 |
| DELETE | /api/files/image | 删除图片 |

### 5.3 标签接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/tags | 获取标签列表 |
| GET | /api/tags/popular | 获取热门标签 |
| POST | /api/tags | 创建标签 |
| DELETE | /api/tags/{id} | 删除标签 |
| GET | /api/tags/recommend | 获取推荐标签 |

### 5.4 分类接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/categories | 获取分类列表 |
| POST | /api/categories | 创建分类 |
| DELETE | /api/categories/{id} | 删除分类 |

### 5.5 搜索接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/search/notes | 搜索笔记 |
| GET | /api/search/suggestions | 获取搜索建议 |

### 5.6 统计接口

| 方法 | 路径 | 说明 |
|------|------|------|
| GET | /api/stats/overview | 统计概览 |
| GET | /api/stats/notes/trend | 笔记创建趋势 |
| GET | /api/stats/tags/usage | 标签使用排行 |
| GET | /api/stats/notes/category-dist | 分类分布统计 |

## 六、项目结构

```
note v1.1/
├── uploads/                   # 图片上传目录（新）
│   └── {userId}/             # 用户图片目录
│
├── note-service/              # Spring Boot 后端
│   ├── src/main/java/com/example/note/
│   │   ├── controller/       # REST API 控制器
│   │   │   ├── NoteController.java
│   │   │   ├── FileUploadController.java  # 文件上传（新）
│   │   │   └── ...
│   │   ├── service/          # 业务逻辑
│   │   │   ├── NoteServiceImpl.java      # 置顶处理（新）
│   │   │   ├── FileUploadService.java    # 文件上传（新）
│   │   │   └── ...
│   │   ├── repository/        # MyBatis Mapper
│   │   │   ├── NoteImageMapper.java      # 图片关联（新）
│   │   │   └── ...
│   │   ├── entity/           # 数据实体
│   │   │   ├── Note.java                  # 添加置顶字段（新）
│   │   │   ├── NoteImage.java             # 图片实体（新）
│   │   │   └── ...
│   │   ├── dto/              # 数据传输对象
│   │   │   ├── NoteCreateDTO.java         # 添加图片/置顶字段（新）
│   │   │   ├── NoteVO.java                # 添加封面/置顶字段（新）
│   │   │   ├── PinUpdateDTO.java          # 置顶DTO（新）
│   │   │   └── ...
│   │   ├── config/           # 配置类
│   │   │   ├── WebConfig.java            # 静态资源映射（新）
│   │   │   └── ...
│   │   └── handler/          # 处理器
│   └── src/main/resources/
│       ├── application.yml   # 应用配置（含上传/置顶配置）
│       └── db/schema.sql     # 数据库脚本（含新字段/表）
│
└── note-web/                  # Vue 3 前端
    ├── src/
    │   ├── api/              # API 调用模块
    │   │   ├── note.js                    # 添加上传/置顶API（新）
    │   │   └── ...
    │   ├── views/            # 页面组件
    │   │   ├── note/
    │   │   │   ├── NoteListView.vue       # 置顶标识/封面图（新）
    │   │   │   ├── NoteEditView.vue       # 图片上传/置顶设置（新）
    │   │   │   └── ...
    │   │   └── ...
    │   ├── components/       # 通用组件
    │   ├── router/           # 路由配置
    │   ├── stores/           # Pinia 状态
    │   │   └── category.js                 # 分类store
    │   └── assets/           # 静态资源
    ├── package.json
    └── vite.config.js       # Vite 配置
```

## 七、配置说明

### 7.1 标签推荐算法权重

在 `application.yml` 中可调整标签推荐算法参数：

```yaml
tag:
  recommend:
    keyword-match-score: 30   # 关键词匹配得分权重
    user-history-score: 25    # 用户历史使用得分权重
    hot-tag-score: 10         # 热门标签得分权重
    recency-score: 5          # 新近度得分权重
    max-results: 5            # 最大推荐数量
```

### 7.2 JWT 配置

```yaml
jwt:
  secret: your-secret-key     # JWT 密钥
  expiration: 86400000        # 过期时间（毫秒）
```

## 八、常见问题

### 8.1 前端无法连接后端

检查 Vite 代理配置是否正确指向 `http://localhost:8082`

### 8.2 关键词提取失败

确保 MySQL 连接正常，且 `keyword` 表已创建

### 8.3 标签推荐为空

检查 `user_tag_history` 表是否有数据，新用户可能需要先使用一些标签
