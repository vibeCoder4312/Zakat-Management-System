# Stage 1: Build React application
FROM node:22-alpine AS build
WORKDIR /app

# pnpm version matches the one that wrote pnpm-lock.yaml
RUN npm install -g pnpm@10.28.0

COPY package.json pnpm-lock.yaml pnpm-workspace.yaml ./
RUN pnpm install --frozen-lockfile

COPY . .
# The app calls the relative path /api/v1, which Nginx proxies to the backend container.
RUN pnpm build

# Stage 2: Serve with Nginx
FROM nginx:alpine
COPY --from=build /app/dist /usr/share/nginx/html
COPY nginx.conf /etc/nginx/conf.d/default.conf

EXPOSE 80

CMD ["nginx", "-g", "daemon off;"]
