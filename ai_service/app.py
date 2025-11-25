"""
ai_service/app.py
Main FastAPI application - Full Version với tất cả features
"""

import os
from functools import lru_cache
from contextlib import asynccontextmanager

from fastapi import FastAPI, UploadFile, File, HTTPException, Form
from fastapi.responses import JSONResponse
from typing import Optional
import google.generativeai as genai

# Import all services
from services.analyze_dish import analyze_dish_from_image, AnalyzeDishResponse
from services.modify_recipe import (
    modify_recipe,
    ModifyRecipeRequest,
    ModifyRecipeResponse
)
from services.create_by_theme import (
    create_by_theme,
    CreateByThemeRequest,
    CreateByThemeResponse
)
from services.critique_dish import (
    critique_dish,
    CritiqueDishRequest,
    CritiqueDishResponse
)

# ============================================================================
# CONFIGURATION
# ============================================================================

GOOGLE_API_KEY = os.getenv('GOOGLE_API_KEY')
if not GOOGLE_API_KEY:
    raise ValueError("Vui lòng thiết lập GOOGLE_API_KEY trong file .env")

genai.configure(api_key=GOOGLE_API_KEY)

# ============================================================================
# MODEL CACHING & WARMUP
# ============================================================================

@lru_cache(maxsize=5)
def get_model(model_name: str):
    """Cache models để tránh khởi tạo lại mỗi request"""
    print(f"[*] Initializing or getting cached Gemini model: {model_name}")
    return genai.GenerativeModel(model_name)

@asynccontextmanager
async def lifespan(app: FastAPI):
    """Warm-up models khi service khởi động"""
    print("[*] Warming up AI models...")
    try:
        get_model('gemini-2.5-flash')
        get_model('gemini-1.5-pro')
        print("✅ AI models pre-loaded successfully.")
    except Exception as e:
        print(f"⚠️ Model pre-load warning: {e}")
    yield
    print("[*] Shutting down AI service...")

# ============================================================================
# FASTAPI APP
# ============================================================================

app = FastAPI(
    title="CineTaste - AI Service",
    description="AI-powered food analysis with film context - Full Version",
    version="6.0.0",
    lifespan=lifespan
)

# ============================================================================
# EXCEPTION HANDLERS
# ============================================================================

@app.exception_handler(ValueError)
async def value_error_handler(request, exc):
    return JSONResponse(
        status_code=400,
        content={"error": "Validation Error", "detail": str(exc)}
    )

@app.exception_handler(RuntimeError)
async def runtime_error_handler(request, exc):
    return JSONResponse(
        status_code=500,
        content={"error": "AI Processing Error", "detail": str(exc)}
    )

# ============================================================================
# HEALTH CHECK
# ============================================================================

@app.get("/", tags=["Health Check"])
def health_check():
    """Kiểm tra trạng thái hoạt động của AI Service"""
    return {
        "status": "healthy",
        "service": "ai-service",
        "version": app.version,
        "features": [
            "analyze-dish",
            "modify-recipe",
            "create-by-theme",
            "critique-dish"
        ]
    }

# ============================================================================
# FEATURE 1: ANALYZE DISH
# ============================================================================

@app.post(
    "/api/ai/analyze-dish",
    response_model=AnalyzeDishResponse,
    tags=["1. Chuyên gia Phân tích Ẩm thực"],
    summary="Phân tích món ăn từ hình ảnh"
)
async def analyze_dish_endpoint(
        image: UploadFile = File(..., description="Ảnh món ăn (JPG/PNG/WEBP, max 10MB)"),
        context: Optional[str] = Form(None, description="Thông tin về phim/cảnh (optional)")
):
    """
    **Phân tích món ăn từ hình ảnh với bối cảnh phim/show**

    - Nhận diện tên món, nguồn gốc, ý nghĩa văn hóa
    - Tìm thông tin phim liên quan (nếu có trong context)
    - Cung cấp công thức nấu chi tiết
    - Ước tính dinh dưỡng
    - Gợi ý đồ uống & món phụ

    **Input:**
    - `image`: File ảnh món ăn
    - `context`: Thông tin bổ sung về phim, cảnh, nhân vật (optional)

    **Output:** Phân tích chi tiết với công thức hoàn chỉnh
    """
    try:
        file_data = await image.read()
        mime_type = image.content_type

        result = await analyze_dish_from_image(
            file_data=file_data,
            mime_type=mime_type,
            context=context
        )

        return result

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        print(f"🚨 Unexpected error in analyze-dish: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống: {str(e)}")

# ============================================================================
# FEATURE 2: MODIFY RECIPE
# ============================================================================

@app.post(
    "/api/ai/modify-recipe",
    response_model=ModifyRecipeResponse,
    tags=["2. Trợ lý Bếp AI"],
    summary="Biến tấu công thức theo yêu cầu"
)
async def modify_recipe_endpoint(request: ModifyRecipeRequest):
    """
    **Biến tấu công thức gốc dựa trên yêu cầu người dùng**

    Các yêu cầu thường gặp:
    - Điều chỉnh khẩu phần (tăng/giảm servings)
    - Thay đổi chế độ ăn (vegan, gluten-free, keto, halal...)
    - Thay thế nguyên liệu (allergies, availability)
    - Thay đổi phương pháp nấu (oven → air fryer)
    - Tối ưu thời gian

    **Input:**
    - `original_recipe`: Công thức gốc
    - `modification_request`: Yêu cầu thay đổi (5-500 ký tự)

    **Output:** Công thức đã biến tấu + giải thích thay đổi
    """
    try:
        result = await modify_recipe(request)
        return result

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        print(f"🚨 Unexpected error in modify-recipe: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống: {str(e)}")

# ============================================================================
# FEATURE 3: CREATE BY THEME
# ============================================================================

@app.post(
    "/api/ai/create-by-theme",
    response_model=CreateByThemeResponse,
    tags=["3. Nhà Sáng tạo Món ăn"],
    summary="Sáng tạo món ăn mới theo chủ đề"
)
async def create_by_theme_endpoint(request: CreateByThemeRequest):
    """
    **Sáng tạo công thức món ăn hoàn toàn mới dựa trên chủ đề**

    Chủ đề có thể là:
    - Tên phim/show (VD: "Blade Runner", "The Grand Budapest Hotel")
    - Thể loại (VD: "Cyberpunk", "Medieval Fantasy", "Tropical Paradise")
    - Văn hóa/Quốc gia (VD: "Japanese fusion", "Modern Vietnamese")
    - Màu sắc/Concept (VD: "Neon Blue", "Rustic Autumn")

    **Input:**
    - `theme`: Chủ đề/nguồn cảm hứng (3-200 ký tự)
    - `dish_type`: Loại món (món chính, tráng miệng, đồ uống...)

    **Output:** Món ăn sáng tạo độc đáo với công thức đầy đủ
    """
    try:
        result = await create_by_theme(request)
        return result

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        print(f"🚨 Unexpected error in create-by-theme: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống: {str(e)}")

# ============================================================================
# FEATURE 4: CRITIQUE DISH
# ============================================================================

@app.post(
    "/api/ai/critique-dish",
    response_model=CritiqueDishResponse,
    tags=["4. Giám khảo Mentor AI"],
    summary="Nhận xét và chấm điểm món ăn"
)
async def critique_dish_endpoint(
        image: UploadFile = File(..., description="Ảnh món ăn của bạn"),
        dish_name: str = Form(..., description="Tên món ăn")
):
    """
    **Nhận xét, chấm điểm và đưa ra gợi ý cải thiện món ăn**

    AI sẽ đánh giá:
    - **Appearance** (Trình bày): Màu sắc, plating, trang trí
    - **Technique** (Kỹ thuật): Độ chín, texture, chuẩn bị
    - **Creativity** (Sáng tạo): Độ độc đáo, artistic expression

    Feedback bao gồm:
    - Điểm tổng thể (0-10)
    - Điểm chi tiết từng tiêu chí
    - Điểm mạnh (strengths)
    - Điểm cần cải thiện (weaknesses)
    - Gợi ý cụ thể (suggestions)

    **Tone:** Thân thiện, khích lệ, mang tính xây dựng

    **Input:**
    - `image`: File ảnh món ăn
    - `dish_name`: Tên món ăn bạn đã nấu

    **Output:** Nhận xét chi tiết với điểm số
    """
    try:
        file_data = await image.read()
        mime_type = image.content_type

        result = await critique_dish(
            file_data=file_data,
            mime_type=mime_type,
            dish_name=dish_name
        )

        return result

    except ValueError as e:
        raise HTTPException(status_code=400, detail=str(e))
    except RuntimeError as e:
        raise HTTPException(status_code=500, detail=str(e))
    except Exception as e:
        print(f"🚨 Unexpected error in critique-dish: {str(e)}")
        raise HTTPException(status_code=500, detail=f"Lỗi hệ thống: {str(e)}")

# ============================================================================
# ADDITIONAL UTILITIES
# ============================================================================

@app.get("/api/ai/features", tags=["Info"])
def list_features():
    """Liệt kê tất cả các tính năng AI có sẵn"""
    return {
        "features": [
            {
                "name": "analyze-dish",
                "description": "Phân tích món ăn từ hình ảnh với bối cảnh phim",
                "method": "POST",
                "endpoint": "/api/ai/analyze-dish"
            },
            {
                "name": "modify-recipe",
                "description": "Biến tấu công thức theo yêu cầu",
                "method": "POST",
                "endpoint": "/api/ai/modify-recipe"
            },
            {
                "name": "create-by-theme",
                "description": "Sáng tạo món ăn mới dựa trên chủ đề",
                "method": "POST",
                "endpoint": "/api/ai/create-by-theme"
            },
            {
                "name": "critique-dish",
                "description": "Nhận xét và chấm điểm món ăn",
                "method": "POST",
                "endpoint": "/api/ai/critique-dish"
            }
        ]
    }