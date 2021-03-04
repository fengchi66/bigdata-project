* [ClickHouse简介及安装](#clickhouse简介及安装)
  * [ClickHouse简介](#clickhouse简介)
  * [基于Docker安装ClickHouse单机版](#基于docker安装clickhouse单机版)
  
# ClickHouse简介及安装

## ClickHouse简介

ClickHouse是一个用于联机分析(OLAP)的列式数据库管理系统(DBMS)。ClickHouse最初是一款名为Yandex.Metrica的产品，主要用于WEB流量分析。ClickHouse的全称是**Click Stream,Data WareHouse**，简称ClickHouse。



ClickHouse非常适用于商业智能领域，除此之外，它也能够被广泛应用于广告流量、Web、App流量、电信、金融、电子商务、信息安全、网络游戏、物联网等众多其他领域。ClickHouse具有以下特点：

- 支持完备的SQL操作

- 列式存储与数据压缩

- 向量化执行引擎

- 关系型模型(与传统数据库类似)

- 丰富的表引擎

- 并行处理

- 在线查询

- 数据分片

  

  ClickHouse作为一款高性能OLAP数据库，存在以下不足。

- 不支持事务。

- 不擅长根据主键按行粒度进行查询（虽然支持），故不应该把ClickHouse当作Key-Value数据库使用。

- 不擅长按行删除数据（虽然支持）



## 基于Docker安装ClickHouse单机版

1. **直接运行, docker会自动帮你拉取镜像**:

   ```shell
   docker run -d --name ch-server --ulimit nofile=262144:262144 -p 8123:8123 -p 9000:9000 -p 9009:9009 yandex/clickhouse-server
   ```

> -d  代表后台运行 --name 自定义ck的服务名称 -p：容器端口映射到当前主机端口 不指定默认http端口是8123，tcp端口是9000

2. **查看镜像**

   ```shell
   # docker images
   REPOSITORY                           TAG                 IMAGE ID            CREATED             SIZE
   docker.io/yandex/clickhouse-server   latest              c601d506867f        2 weeks ago         809 MB
   docker.io/yandex/clickhouse-client   latest              ba91f385ceea        2 weeks ago         485 MB
   docker.io/wurstmeister/kafka         2.11-0.11.0.3       2b1f874807ac        3 months ago        413 MB
   docker.io/mysql                      5.6.39              079344ce5ebd        2 years ago         256 MB
   docker.io/wurstmeister/zookeeper     3.4.6               6fe5551964f5        4 years ago         451 MB
   docker.io/training/webapp            latest              6fae60ef3446        5 years ago         349 MB
   ```



3. **进入ClickHouse容器**

   ```shell
   docker exec -it d00724297352 /bin/bash
   ```

   - 需要注意的是, 默认的容器是一个依赖包不完整的ubuntu虚拟机

   - 所以我们需要安装vim

     ```shell
     apt-get update
     apt-get install vim -y
     
     ```

   - clickhouse-server目录并查看目录

     ```shell
     cd /etc/clickhouse-server
     
     # 查看目录
     root@d00724297352:/etc/clickhouse-server# ll
     total 52
     drwx------ 1 clickhouse clickhouse  4096 Dec  6 06:06 ./
     drwxr-xr-x 1 root       root        4096 Dec  6 05:24 ../
     dr-x------ 1 clickhouse clickhouse  4096 Nov 20 15:29 config.d/
     -r-------- 1 clickhouse clickhouse 38407 Nov 19 10:34 config.xml
     dr-x------ 2 clickhouse clickhouse  4096 Nov 20 15:29 users.d/
     -r-------- 1 clickhouse clickhouse  5688 Dec  6 06:06 users.xml
     ```

   - - 修改clickhouse的用户密码需要在users.xml中配置

   - - 需要注意的是: 密码必须为加密过的形式, 否则会一直连不上。

   - - 我们这次采用SHA256的方式加密

       ```shell
       PASSWORD=$(base64 < /dev/urandom | head -c8); echo "你的密码"; echo -n "你的密码" | sha256sum | tr -d '-'
       
       ```

     - 执行以上命令后会在命令行打印密码明文和密码密文, 如下

       ```shell
       123456
       8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92
       ```

   - - vim user.xml修改用户密码

       ```shell
       <!-- Users and ACL. -->
           <users>
               <default>
                   <password_sha256_hex>8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92</password_sha256_hex>
                   <networks incl="networks" replace="replace">
                       <ip>::/0</ip>
                   </networks>
                   <profile>default</profile>
                   <quota>default</quota>
               </default>
               <ck>
                   <password_sha256_hex>8d969eef6ecad3c29a3a629280e686cf0c3f5d5a86aff3ca12020c923adc6c92</password_sha256_hex>
                   <networks incl="networks" replace="replace">
                       <ip>::/0</ip>
                   </networks>
                   <profile>readonly</profile>
                   <quota>default</quota>
               </ck>
           </users>
       ```

4. Clickhouse-client连接

   ```shell
   docker run -it --rm --link ch-server:clickhouse-server yandex/clickhouse-client --host clickhouse-server --password 123456
   
   ```

   因为客户端每次使用完不用一直常驻所以这里使用--rm 参数 在使用exit 命令退出liucf-clickhouse-client容器后就会直接删除这个容器，下次启动重新创建就可以了

   注意：这里使用了 docker --link 命令，可以让一个容器和另一个容器很方便的连接起来，其参数部分的ch-server表示要连接的容器真实名称。

- **配置clickhouse-client启动命令**

  把上述命名放在一个文件中，比如start-ch-client.sh, chmod +x ，再放到/usr/local/bin下,就可以直接用了

  ```shell
  vim start-ch-client.sh
  
  docker run -it --rm --link ch-server:clickhouse-server yandex/clickhouse-client --host clickhouse-server --password 123456
  ```

3. 测试

   - 查看数据库

     ```shell
     e38fc4946689 :) show databases
     
     SHOW DATABASES
     
     Query id: d1cb9abc-ef45-4d45-ac48-86db60f45001
     
     ┌─name───────────────────────────┐
     │ _temporary_and_external_tables │
     │ default                        │
     │ system                         │
     └────────────────────────────────┘
     
     3 rows in set. Elapsed: 0.003 sec.
     ```

   - 创建表并插入一条数据

     ```shell
     CREATE TABLE test (FlightDate Date,Year UInt16) ENGINE = MergeTree(FlightDate, (Year, FlightDate), 8192);
      
     insert into test (FlightDate,Year) values('2020-06-05',2001);
     
     ```

   - 查询数据

     ```shell
     SELECT * FROM test
     
     Query id: a729c322-0bf0-41d4-8c2d-b06fc14d1124
     
     ┌─FlightDate─┬─Year─┐
     │ 2020-06-05 │ 2001 │
     └────────────┴──────┘
     
     1 rows in set. Elapsed: 0.004 sec. 
     ```
