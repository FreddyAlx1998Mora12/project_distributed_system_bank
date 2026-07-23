FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN useradd -m banking && mkdir -p /data/logs && chown -R banking:banking /app /data
USER banking
EXPOSE 8500
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8500"]
