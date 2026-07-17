FROM node:20-alpine

WORKDIR /app

COPY package.json ./
RUN npm install --omit=dev

COPY src ./src

# داده روی این مسیر ذخیره می‌شود؛ برای ماندگاری، این پوشه را volume کن.
ENV DATA_DIR=/app/data
VOLUME ["/app/data"]

ENV PORT=3000
EXPOSE 3000

CMD ["node", "src/index.js"]
