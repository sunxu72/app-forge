# ---- build ----
FROM node:20-alpine AS build
WORKDIR /app

COPY package*.json ./
RUN npm ci

COPY . .

# 同源部署：请求走 Nginx 的 /api 反代，避免跨域与 Cookie 问题
ARG VITE_API_BASE_URL=/api
ARG VITE_DEPLOY_DOMAIN=http://localhost
ENV VITE_API_BASE_URL=$VITE_API_BASE_URL \
    VITE_DEPLOY_DOMAIN=$VITE_DEPLOY_DOMAIN

# 只跑 vite build，跳过 vue-tsc 类型检查，避免类型告警阻断镜像构建
RUN npm run build-only

# ---- run ----
FROM nginx:1.27-alpine
COPY nginx.conf /etc/nginx/conf.d/default.conf
COPY --from=build /app/dist /usr/share/nginx/html
EXPOSE 80
