# 校园失物招领系统 · 数据库初始化脚本
# 说明：
#   1. 本目录下的 .sql 会被 docker-compose 挂载到 MySQL 容器的 /docker-entrypoint-initdb.d，
#      **仅在数据卷首次创建时执行**；改了脚本需先 docker compose down -v 再起。
#   2. 表结构来自《需求规格说明书》V1.0 第 5.2 节数据字典，为可执行基线；
#      字段类型/索引/软删除策略待《详细设计》确认后在此文件上迭代（不要直接改已上线的库，走 migration）。
