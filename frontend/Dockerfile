# =====================================================================
# 前端镜像：多阶段构建（Node 编译 → Nginx 托管静态资源）
# =====================================================================

# ---------- 构建阶段 ----------
FROM node:22-alpine AS build
WORKDIR /build

# 依赖层缓存
COPY package*.json ./
RUN npm config set registry https://registry.npmmirror.com \
    && npm ci --no-audit --no-fund

COPY . .
RUN npm run build

# ---------- 运行阶段 ----------
FROM nginx:1.27-alpine
RUN rm -rf /usr/share/nginx/html/*
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
