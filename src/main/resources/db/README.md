# 数据库迁移（Flyway）

## 配置

- **数据库名**：`food`（JDBC：`.../food`）
- **业务 schema**：`food`（账号对 `public` 无 CREATE 权限，表建在 `food` schema 下）
- **迁移脚本**：`src/main/resources/db/migration/`
- **首次启动**时自动执行 Flyway

## 脚本

| 版本 | 文件 | 说明 |
|------|------|------|
| V1 | `V1__init_decidemeal_schema.sql` | 创建 schema `food` 及全部业务表 |

完整表名与字段含义见前端仓库 `doc/product/db-schema.md`（随 V1–V15 及后续迁移维护）。

## 表清单（`food` schema）

| 表名 | 说明 |
|------|------|
| `sf_user` | 用户 |
| `sf_invite` | 邀请 |
| `sf_friend` | 好友关系 |
| `sf_room` | 组局房间 |
| `sf_room_option` | 投票选项 |
| `sf_vote` | 投票记录 |
| `sf_restaurant` | 餐厅/POI |
| `sf_restaurant_want` | 「想去」标记 |
| `sf_recipe` | 菜谱库 |
| `sf_user_preference` | 口味偏好 |
| `sf_favorite` | 收藏 |
| `sf_activity_record` | 行为记录时间线 |
| `sf_order` | 历史订单 |
| `sf_user_notification` | 站内通知 |
| `sf_friend_conversation` | 好友会话 |
| `sf_friend_message` | 好友消息 |
| `sf_friend_conversation_read` | 好友已读游标 |
| `sf_ai_conversation` | AI 会话 |
| `sf_ai_chat_message` | AI 消息 |
| `sf_ai_user_memory` | AI 用户分层记忆 |
| `sf_rag_chunk` | RAG 知识切片（pgvector） |

## 首次跑迁移

```bash
cd soulfood-backed
./mvnw spring-boot:run
# 或
./mvnw test -Dtest=SoulfoodBackendApplicationTests
```

成功后在库 `food` 的 schema `food` 下可看到 `flyway_schema_history` 及各 `sf_*` 表。

## 后续新增迁移

命名：`V2__描述.sql`（勿改已执行的 V1）。
