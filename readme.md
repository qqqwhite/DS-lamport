# 基于Lamport协议的天气信息系统

这是一个使用Lamport协议实现的分布式天气信息系统，用于维护多个服务器之间事件的因果顺序。系统由三个主要组件组成：内容服务器（Content Server）、聚合服务器（Aggregation Server）和GET客户端。

## 系统架构

### 主要组件

1. **内容服务器 (`ContentServer.java`)**
   - 向聚合服务器发布天气信息
   - 实现Lamport时间戳以保证事件顺序
   - 支持PUT操作进行数据更新

2. **聚合服务器 (`AggregationServer.java`)**
   - 作为天气信息的中央处理枢纽
   - 维护来自多个内容服务器的数据
   - 处理数据持久化和恢复
   - 使用锁机制管理并发访问
   - 实现GET和PUT请求处理

3. **GET客户端 (`GETClient.java`)**
   - 从特定内容服务器获取天气信息
   - 实现Lamport时钟同步
   - 支持交互式和测试模式

### 核心特性

- **Lamport协议实现 (`LamportServer.java`)**
  - 分布式组件间的逻辑时钟同步
  - 确保事件的因果顺序
  - 使用CAS（比较并交换）实现原子操作

- **容错机制**
  - 数据持久化和恢复机制
  - 优雅处理服务器断开连接
  - 自动数据备份

- **并发操作**
  - 使用Java NIO实现非阻塞I/O
  - 线程安全的数据结构
  - 使用上传锁机制保证数据一致性

## 技术细节

### 依赖项
- Java NIO：用于非阻塞I/O
- Google Gson：用于JSON处理
- Maven：项目管理工具

### 核心类
- `LamportServer`：实现Lamport协议的基类
- `HttpBuilder`：HTTP请求/响应构建器
- `Storage`：数据持久化管理
- `JsonHandler`：JSON数据处理
- `RequestHandler` 和 `ResponseHandler`：HTTP消息处理

## 快速开始

### 系统要求
- Java JDK 8 或更高版本
- Maven

### 构建项目
```bash
mvn clean install
```

### 运行各组件

1. 启动聚合服务器：
```bash
java -cp target/classes AggregationServer <端口>
```

2. 启动内容服务器：
```bash
java -cp target/classes ContentServer <端口> <服务器名称> <聚合服务器IP> <聚合服务器端口>
```

3. 启动GET客户端：
```bash
java -cp target/classes GETClient <端口> <客户端名称> <聚合服务器IP> <聚合服务器端口>
```

## 协议详情

### Lamport时钟同步
1. 每个服务器维护一个逻辑时钟
2. 时钟值在以下情况下更新：
   - 本地事件：增加1
   - 发送消息：增加1
   - 接收消息：取本地时钟和接收到的时钟的最大值，然后加1

### HTTP通信
- GET请求：用于获取天气信息
- PUT请求：用于更新数据
- 自定义头部：包含Lamport时间戳和服务器标识

## 测试
系统包含一个测试模式，可以通过向各组件传递适当的标志来启用。这允许对分布式系统的功能和时钟同步进行自动化测试。

## 数据格式
天气信息以JSON格式存储和传输，包括以下字段：
- 温度
- 湿度
- 位置
- 时间戳
- 服务器标识