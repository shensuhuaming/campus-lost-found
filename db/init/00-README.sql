# 校园失物招领系统 · 数据库初始化脚本
# 说明：
#   1. 本目录下的 .sql 会被 docker-compose 挂载到 MySQL 容器的 /docker-entrypoint-initdb.d，
#      **仅在数据卷首次创建时执行**；改了脚本需先 docker compose down -v 再起。
#   2. 01-schema.sql 直接采用《详细设计》的物理设计脚本（项目设计/详细设计/数据库/lost_found.sql），
#      以设计文档为准：7 张表、外键/索引/唯一约束、6 条分类种子数据。
#   3. 管理员账号不写在 SQL 里，由后端 DataInitializer 启动时幂等创建
#      （默认 admin / Admin@123，生产用环境变量 ADMIN_INIT_PASSWORD 覆盖）。
#   4. 修改表结构时请先改设计文档中的 lost_found.sql，再同步本文件，避免两处不一致。
