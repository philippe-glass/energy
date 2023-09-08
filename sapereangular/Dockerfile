FROM node:alpine AS builder

WORKDIR /app

COPY . .

RUN npm install
# RUN $(npm bin)/ng build --prod
RUN npm install -g @angular/cli@7.3.9


FROM nginx:alpine

COPY default.conf /etc/nginx/conf.d/

COPY --from=builder /app/dist/* /usr/share/nginx/html/

