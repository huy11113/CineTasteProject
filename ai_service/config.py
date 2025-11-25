"""
ai_service/config.py
Cấu hình trung tâm cho AI Service
"""
import os
from dotenv import load_dotenv

load_dotenv()

class AIConfig:
    # API Key
    GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')

    # Định nghĩa Model Names theo yêu cầu của bạn
    # Model nhanh cho các tác vụ đơn giản hoặc cần tốc độ
    MODEL_FAST = "gemini-2.5-flash"

    # Model mạnh cho các tác vụ phức tạp (xử lý ảnh, sáng tạo cao)
    MODEL_SMART = "gemini-2.5-pro" # Hoặc gemini-1.5-pro tùy key của bạn

    if not GOOGLE_API_KEY:
        raise ValueError("Thiếu GOOGLE_API_KEY trong file .env")