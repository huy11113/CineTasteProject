"""
ai_service/services/image_validator.py
Utility class để validate và optimize images
"""

import io
import logging
from PIL import Image

logger = logging.getLogger(__name__)

# Constants
MAX_FILE_SIZE = 10 * 1024 * 1024  # 10MB
ALLOWED_MIME_TYPES = ['image/jpeg', 'image/png', 'image/webp']
MAX_IMAGE_DIMENSION = 2048


class ImageValidator:
    """Validate và optimize images trước khi gửi đến AI"""

    @staticmethod
    def validate_file(file_data: bytes, mime_type: str) -> None:
        """
        Validate uploaded file

        Args:
            file_data: Raw file bytes
            mime_type: MIME type of file

        Raises:
            ValueError: If validation fails
        """
        if not file_data:
            raise ValueError("Không có dữ liệu file được cung cấp")

        if len(file_data) > MAX_FILE_SIZE:
            raise ValueError(
                f"File quá lớn. Kích thước tối đa là {MAX_FILE_SIZE / 1024 / 1024}MB"
            )

        if mime_type not in ALLOWED_MIME_TYPES:
            raise ValueError(
                f"Định dạng file không được hỗ trợ. "
                f"Chỉ chấp nhận: {', '.join(ALLOWED_MIME_TYPES)}"
            )

        logger.info(
            f"File validation passed - "
            f"Size: {len(file_data) / 1024:.2f}KB, Type: {mime_type}"
        )

    @staticmethod
    def optimize_image(
            file_data: bytes,
            max_dimension: int = MAX_IMAGE_DIMENSION
    ) -> Image.Image:
        """
        Optimize image for AI processing

        Args:
            file_data: Raw image bytes
            max_dimension: Maximum width/height in pixels

        Returns:
            PIL Image object (optimized)

        Raises:
            ValueError: If image cannot be processed
        """
        try:
            pil_image = Image.open(io.BytesIO(file_data))

            # Convert to RGB if needed
            if pil_image.mode not in ('RGB', 'RGBA'):
                pil_image = pil_image.convert('RGB')
            elif pil_image.mode == 'RGBA':
                # Create white background for transparency
                background = Image.new('RGB', pil_image.size, (255, 255, 255))
                background.paste(pil_image, mask=pil_image.split()[3])
                pil_image = background

            # Resize if too large
            original_size = pil_image.size
            if max(original_size) > max_dimension:
                pil_image.thumbnail(
                    (max_dimension, max_dimension),
                    Image.Resampling.LANCZOS
                )
                logger.info(
                    f"Resized image from {original_size} to {pil_image.size}"
                )

            return pil_image

        except Exception as e:
            logger.error(f"Error optimizing image: {str(e)}")
            raise ValueError(f"Không thể xử lý ảnh: {str(e)}")