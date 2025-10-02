from fastapi import FastAPI

# Khởi tạo ứng dụng
app = FastAPI()


@app.get("/api/ai/health")
def health_check():
    return {"status": "OK", "service": "AI Service"}