# Kafka EFAK 部署设计

## 目标

在虚拟机 `192.168.61.127` 上部署一个 EFAK 容器，用更友好的 Web 页面查看现有 Kafka `2.8.1` 的 Broker、Topic、Consumer 与 Offset 信息，同时保留现有 Kafka Manager 作为备用。

## 现状

- Docker Compose 项目目录：`/opt/austin/kafka`
- Compose 网络：`kafka_default`
- ZooKeeper 容器：`zookeeper:2181`，Kafka chroot 为 `/kafka`
- Kafka 容器：`kafka`，宿主机映射端口为 `9092`
- Kafka 数据当前位于 Docker 匿名卷，日志目录含旧容器 ID，重建前必须迁移到命名卷和稳定目录
- Kafka 当前错误公布地址：`192.168.61.128:9092`
- 虚拟机实际地址：`192.168.61.127`
- EFAK Web 端口 `8048` 当前未占用
- 现有内存与磁盘空间足够新增一个 EFAK 容器

## 方案比较

### 方案 A：加入现有 Docker Compose（采用）

- 在 `/opt/austin/kafka/docker-compose.yml` 中新增 `efak` 服务。
- 使用现有 `kafka_default` 网络，通过 `zookeeper:2181/kafka` 获取 Kafka 元数据。
- 映射宿主机端口 `8048`，并将 SQLite 数据库持久化到宿主机。
- 优点：配置集中、依赖关系明确、重启后可自动恢复。
- 缺点：需要修改现有 Compose 文件，并短暂重建 Kafka 容器以修正地址。

### 方案 B：单独执行 Docker Run

- 用 `docker run` 将 EFAK 接入 `kafka_default` 网络。
- 优点：部署最快，不修改 Compose 文件。
- 缺点：运行参数容易漂移，后续执行 Compose 操作时不易统一维护。

### 方案 C：用 EFAK 替换 Kafka Manager

- 停止并删除 Kafka Manager，仅保留 EFAK。
- 优点：少运行一个管理服务。
- 缺点：失去旧页面作为回退入口，收益有限。

## 采用设计

### Kafka 修正

- 将 `KAFKA_ADVERTISED_LISTENERS` 从 `PLAINTEXT://192.168.61.128:9092` 改为 `PLAINTEXT://192.168.61.127:9092`。
- 将 Kafka 匿名卷中的数据停机复制到命名卷 `kafka_data`，并把日志目录统一为 `/kafka/kafka-logs`。
- 在 Compose 中显式挂载 `kafka_data:/kafka` 并设置 `KAFKA_LOG_DIRS=/kafka/kafka-logs`。
- 为 EFAK Broker 探测配置 Kafka JMX 端口 `9999`，并映射 `9999:9999`。
- 仅重建 Kafka 容器，不删除 Kafka 数据或 ZooKeeper 数据。
- Kafka 会有几十秒不可用窗口；等待 `9092` 恢复后再启动 EFAK。

### EFAK 服务

- 镜像固定为 `nickzurich/efak:3.0.1`，避免 `latest` 漂移。
- 容器名为 `kafka-efak`。
- Web 映射为 `8048:8048`。
- ZooKeeper 地址为 `zookeeper:2181/kafka`。
- 使用镜像默认集群别名 `cluster`，避免维护自定义入口脚本。
- 使用镜像内置 SQLite，数据库目录挂载到 `/opt/austin/kafka/efak/db`。
- 设置 `restart: always`，随 Docker 自动恢复。
- EFAK 与 Kafka Manager 并存，不修改 Kafka Manager 配置。

### 访问与认证

- 页面地址：`http://192.168.61.127:8048`
- 初始登录账号：`admin`
- 初始登录密码：`123456`

## 验证标准

- Kafka 容器持续处于 `running` 状态，宿主机 `9092` 可连接。
- Kafka 元数据中的 Broker 地址为 `192.168.61.127:9092`。
- ZooKeeper Broker 节点中的 `jmx_port` 为 `9999`，EFAK 日志不再出现 `port out of range:-1`。
- `kafka-efak` 容器持续处于 `running` 状态且无启动异常。
- 从虚拟机内部和当前工作机访问 `8048` 均返回 HTTP 成功响应。
- EFAK 页面能显示 `cluster` 集群及现有 Topic。

## 回滚

- 停止并删除 `kafka-efak` 容器。
- 从备份恢复原 `docker-compose.yml`。
- 如需恢复旧 Kafka 公布地址，再将其改回 `192.168.61.128:9092` 并重建 Kafka 容器。
