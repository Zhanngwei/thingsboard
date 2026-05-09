# DAO 模块学习文档

## 模块概述

`dao` 模块是 ThingsBoard 的数据持久化层，负责所有实体的数据库操作。支持 PostgreSQL(关系数据)和可选的 Cassandra(时序数据)，采用 Spring Data JPA + 自定义 SQL 实现。

## 目录结构

```
dao/src/main/java/org/thingsboard/server/dao/
├── Dao.java                    # DAO 基础接口
├── DaoUtil.java                # DAO 工具类(分页转换等)
├── sql/                        # SQL实现基础类 (43个子包)
│   ├── JpaAbstractDao.java    # JPA DAO 抽象基类
│   ├── JpaAbstractSearchTextDao.java  # 带搜索的DAO基类
│   └── ...
├── device/                    # 设备数据访问 (17个文件)
│   ├── DeviceDao.java        # 设备DAO接口
│   ├── DeviceServiceImpl.java # 设备服务实现
│   └── ...
├── alarm/                     # 告警数据访问
├── asset/                     # 资产数据访问
├── attributes/                # 属性数据访问
├── customer/                  # 客户数据访问
├── dashboard/                 # 仪表盘数据访问
├── edge/                      # 边缘节点数据访问
├── entity/                    # 实体通用查询
├── event/                     # 事件数据访问
├── notification/              # 通知数据访问
├── oauth2/                    # OAuth2配置数据访问
├── ota/                       # OTA升级包数据访问
├── queue/                     # 队列配置数据访问
├── relation/                  # 实体关系数据访问
├── resource/                  # 资源数据访问
├── rule/                      # 规则链数据访问
├── tenant/                    # 租户数据访问
├── timeseries/                # 时序数据访问 (18个文件)
├── user/                      # 用户数据访问
├── widget/                    # Widget数据访问
├── config/                    # 数据源配置
├── model/                     # JPA实体映射
├── util/                      # DAO工具类
├── sqlts/                     # SQL时序数据实现 (14个文件)
└── nosql/                     # NoSQL(Cassandra)实现
```

## 核心设计模式

### 1. 分层架构
```
Service接口 (dao-api模块定义)
    ↓
ServiceImpl (dao模块实现，包含业务逻辑验证)
    ↓
Dao接口 (dao模块定义)
    ↓
JpaDao实现 (使用Spring Data JPA)
    ↓
Repository接口 (Spring Data JPA Repository)
    ↓
数据库 (PostgreSQL / Cassandra)
```

### 2. 多数据库支持

**PostgreSQL (必选)**:
- 存储所有实体数据(设备、用户、规则链等)
- 存储时序数据(可选，通过 SQL TimescaleDB 扩展优化)
- 使用 Spring Data JPA + Hibernate

**Cassandra (可选)**:
- 仅用于时序数据存储
- 适用于超大规模时序数据场景

### 3. 实体映射

每个业务实体都有对应的 JPA Entity:
```java
// 数据模型 (common/data)
public class Device extends BaseData<DeviceId> { ... }

// JPA 实体 (dao/model)
@Entity
@Table(name = "device")
public class DeviceEntity implements ToData<Device> { ... }
```

## 关键服务实现

### DeviceServiceImpl
- 设备CRUD操作
- 设备凭证管理
- 设备分配给客户
- 设备搜索和分页查询

### TelemetryServiceImpl (时序数据)
- 时序数据保存和查询
- 数据聚合(AVG, SUM, MIN, MAX, COUNT)
- 数据删除和TTL
- 支持 PostgreSQL/TimescaleDB/Cassandra

### AlarmServiceImpl
- 告警创建和更新
- 告警确认和清除
- 告警查询(按实体、类型、严重程度)
- 告警传播(从设备到资产到租户)

### RelationServiceImpl
- 实体关系管理(Contains, Manages, 自定义)
- 关系查询(向上/向下遍历)
- 批量关系操作

## 时序数据存储

### SQL实现 (sqlts/)
```
ts_kv 表结构:
- entity_id (UUID)
- key (int, 映射到 key_dictionary)
- ts (bigint, 时间戳)
- bool_v, str_v, long_v, dbl_v, json_v (多类型值)
```

### 数据分区策略
- 按月分区 (默认)
- 支持 TimescaleDB 超表
- 自动清理过期数据

## 缓存策略

DAO层集成了多级缓存:
- **设备缓存**: 设备信息、凭证
- **属性缓存**: 客户端/服务端/共享属性
- **关系缓存**: 实体关系
- **配置缓存**: 租户配置、设备配置

## 配置

数据源配置位于 `thingsboard.yml`:
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/thingsboard
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: none
```
