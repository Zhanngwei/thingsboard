# UI-NGX 前端模块学习文档

## 模块概述

`ui-ngx` 是 ThingsBoard 的前端模块，基于 Angular 框架构建。提供完整的 Web 管理界面，包括设备管理、仪表盘设计、规则链编辑器、告警管理等功能。

## 技术栈

- **框架**: Angular (最新版本)
- **UI 组件库**: Angular Material
- **样式**: SCSS + Tailwind CSS
- **图表**: 多种图表库集成
- **构建**: ESBuild + Angular CLI
- **包管理**: Yarn

## 目录结构

```
ui-ngx/src/
├── app/
│   ├── core/              # 核心服务 (18个子目录)
│   │   ├── api/          # REST API 服务
│   │   ├── auth/         # 认证服务
│   │   ├── guards/       # 路由守卫
│   │   ├── interceptors/ # HTTP拦截器
│   │   ├── services/     # 通用服务
│   │   └── ...
│   ├── modules/           # 功能模块
│   │   ├── home/         # 首页模块 (11个子目录)
│   │   ├── login/        # 登录模块
│   │   ├── dashboard/    # 仪表盘模块
│   │   └── common/       # 公共模块
│   ├── shared/            # 共享组件 (12个子目录)
│   │   ├── components/   # 通用组件
│   │   ├── directives/   # 指令
│   │   ├── models/       # TypeScript模型
│   │   └── pipe/         # 管道
│   ├── app.module.ts      # 根模块
│   └── app-routing.module.ts # 根路由
├── assets/                # 静态资源
├── environments/          # 环境配置
└── styles/               # 全局样式
```

## 核心功能模块

### 1. 登录/认证 (login/)
- JWT Token 认证
- OAuth2 第三方登录
- 双因素认证 (2FA)
- 密码重置

### 2. 设备管理 (home/components/device/)
- 设备列表/搜索/过滤
- 设备详情(属性、遥测、告警、事件)
- 设备凭证管理
- 批量操作

### 3. 仪表盘 (dashboard/)
- 拖拽式仪表盘编辑器
- Widget 库(图表、地图、控制面板等)
- 数据源绑定(实体属性/遥测)
- 仪表盘状态和导航
- 实时数据 WebSocket 更新

### 4. 规则链编辑器 (home/components/rulechain/)
- 可视化规则链设计器
- 拖拽添加规则节点
- 节点连接和配置
- 调试模式(实时消息跟踪)

### 5. 告警管理
- 告警列表和过滤
- 告警详情和评论
- 告警确认/清除操作
- 实时告警通知

## 数据通信

### REST API 调用
通过 Angular HttpClient 调用后端 REST API:
```typescript
// 设备服务示例
@Injectable()
export class DeviceService {
  getDevices(pageLink: PageLink): Observable<PageData<Device>> {
    return this.http.get<PageData<Device>>(`/api/tenant/devices${pageLink.toQuery()}`);
  }
}
```

### WebSocket 实时数据
通过 WebSocket 订阅实时数据更新:
- 遥测数据实时推送
- 告警状态实时更新
- 设备连接状态变更

## 配置文件

- `angular.json` - Angular CLI 配置
- `tsconfig.json` - TypeScript 配置
- `tailwind.config.js` - Tailwind CSS 配置
- `proxy.conf.js` - 开发代理配置
- `eslint.config.mjs` - 代码规范配置

## 开发调试

```bash
cd ui-ngx
yarn install
yarn start  # 启动开发服务器(代理到后端)
```

---

# MSA (微服务架构) 模块

## 模块概述

`msa` 模块提供 ThingsBoard 的微服务架构打包和部署支持。

## 结构
```
msa/
├── tb-node/          # 主节点 Docker 镜像
├── transport/        # 各传输协议独立服务
│   ├── mqtt/
│   ├── coap/
│   ├── http/
│   ├── lwm2m/
│   └── snmp/
├── js-executor/      # JavaScript 规则节点执行器
├── web-ui/           # Web UI 静态资源服务
├── vc-executor/      # 版本控制执行器
├── edqs/             # EDQS 服务
├── monitoring/       # 监控服务
└── black-box-tests/  # 黑盒测试
```

## 微服务部署架构

```
┌──────────┐  ┌──────────┐  ┌──────────┐
│  Web UI  │  │  Web UI  │  │  Web UI  │  (Nginx)
└────┬─────┘  └────┬─────┘  └────┬─────┘
     │              │              │
     └──────────────┼──────────────┘
                    │
            ┌───────┴───────┐
            │  HAProxy/LB   │
            └───────┬───────┘
                    │
     ┌──────────────┼──────────────┐
     │              │              │
┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐
│ TB Node 1│  │ TB Node 2│  │ TB Node 3│  (Core + Rule Engine)
└────┬─────┘  └────┬─────┘  └────┬─────┘
     │              │              │
     └──────────────┼──────────────┘
                    │
            ┌───────┴───────┐
            │    Kafka      │  (消息队列)
            └───────┬───────┘
                    │
     ┌──────────────┼──────────────┐
     │              │              │
┌────┴─────┐  ┌────┴─────┐  ┌────┴─────┐
│MQTT Trans│  │CoAP Trans│  │HTTP Trans│  (独立传输服务)
└──────────┘  └──────────┘  └──────────┘
```

---

# 其他辅助模块

## netty-mqtt
自定义的 MQTT 协议编解码器，基于 Netty 实现:
- MQTT 3.1.1 协议完整支持
- 高性能非阻塞 I/O
- 被 MQTT Transport 模块使用

## rest-client
Java REST 客户端 SDK:
- 封装所有 ThingsBoard REST API
- 支持认证和 Token 刷新
- 方便第三方 Java 应用集成

## monitoring
平台监控服务:
- 健康检查
- 性能指标收集
- 与 Prometheus/Grafana 集成

## tools
开发和运维工具:
- 数据迁移工具
- 性能测试工具
- 设备模拟器
