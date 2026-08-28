# ---- Build stage -------------------------------------------------------------
FROM node:22-alpine AS build
WORKDIR /build
COPY package*.json ./
RUN npm ci
COPY . .
RUN npm run build

# ---- Runtime stage -----------------------------------------------------------
# nginx serves the static bundle and proxies /api and /ws to the backend, so the
# browser sees one origin and CORS never enters the picture in production.
FROM nginx:1.27-alpine
COPY --from=build /build/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf
EXPOSE 80
