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

## 表清单（`food` schema）

| 表名 | 说明 |
|------|------|
| `sf_user` | 用户 |
| `sf_room` | 组局房间 |
| `sf_room_option` | 投票选项 |
| `sf_vote` | 投票记录 |
| `sf_restaurant` | 餐厅/POI |
| `sf_user_preference` | 口味偏好 |
| `sf_favorite` | 收藏 |
| `sf_activity_record` | 行为记录时间线 |
| `sf_order` | 历史订单 |
| `sf_invite` | 邀请 |
| `sf_restaurant_want` | 「想去」标记 |

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
