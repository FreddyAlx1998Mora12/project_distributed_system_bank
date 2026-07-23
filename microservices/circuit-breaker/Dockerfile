FROM python:3.12-slim
WORKDIR /app
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt
COPY . .
RUN useradd -m banking && chown -R banking:banking /app
USER banking
EXPOSE 9100
CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "9100"]
