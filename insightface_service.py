#!/usr/bin/env python3
"""
Face Detection Service using InsightFace
Provides face detection, landmarks, and embeddings via JSON-RPC over stdio
Auto-downloads models on first use

Python: 3.8+
Requires: insightface, onnxruntime
"""

import cv2
import numpy as np
import json
import sys
import base64
from typing import Dict, List, Optional
import logging

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    stream=sys.stderr
)
logger = logging.getLogger('InsightFace')

# Import InsightFace
INSIGHTFACE_AVAILABLE = True
try:
    import insightface
    logger.info("✓ InsightFace library loaded")
except ImportError:
    INSIGHTFACE_AVAILABLE = False
    logger.warning("InsightFace not installed. Falling back to OpenCV-only backend.")


class FaceDetector:
    """InsightFace-based face detection and recognition"""
    
    def __init__(self):
        """Initialize InsightFace detector"""
        logger.info("Initializing InsightFace...")
        self.app = None
        self.face_cascade = None
        
        if INSIGHTFACE_AVAILABLE:
            try:
                # Initialize with default model (buffalo_sc = small + fast)
                self.app = insightface.app.FaceAnalysis(
                    name='buffalo_sc',
                    providers=['CPUProvider']
                )

                # Prepare: downloads models if needed
                self.app.prepare(ctx_id=-1, det_model='retinaface', rec_model='arcface')

                logger.info("✓ InsightFace initialized successfully")
                logger.info("  Detection model: RetinaFace (fast & accurate)")
                logger.info("  Recognition: ArcFace (embeddings)")
                return

            except Exception as e:
                logger.error(f"ERROR: {e}")
                try:
                    logger.info("Trying compatibility mode...")
                    self.app = insightface.app.FaceAnalysis()
                    self.app.prepare(ctx_id=-1)
                    logger.info("✓ InsightFace ready (compatibility mode)")
                    return
                except Exception as e2:
                    logger.warning(f"InsightFace init failed, enabling fallback backend: {e2}")

        self.face_cascade = cv2.CascadeClassifier(cv2.data.haarcascades + 'haarcascade_frontalface_alt.xml')
        if self.face_cascade.empty():
            logger.error("FATAL: OpenCV face cascade is unavailable")
            sys.exit(1)
        logger.info("✓ OpenCV fallback backend initialized")
    
    def base64_to_image(self, b64: str) -> Optional[np.ndarray]:
        """Decode base64 to image"""
        try:
            data = base64.b64decode(b64)
            arr = np.frombuffer(data, np.uint8)
            return cv2.imdecode(arr, cv2.IMREAD_COLOR)
        except Exception as e:
            logger.error(f"Decode error: {e}")
            return None
    
    def image_to_base64(self, img: np.ndarray) -> str:
        """Encode image to base64"""
        try:
            _, buf = cv2.imencode('.jpg', img, [cv2.IMWRITE_JPEG_QUALITY, 95])
            return base64.b64encode(buf).tobytes().decode()
        except Exception as e:
            logger.error(f"Encode error: {e}")
            return ""
    
    def detect_faces(self, frame: np.ndarray) -> Dict:
        """Detect all faces in frame"""
        result = {'detected': False, 'faces': [], 'count': 0}
        
        try:
            if self.app is None:
                gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                gray = cv2.equalizeHist(gray)
                faces = self.face_cascade.detectMultiScale(
                    gray,
                    scaleFactor=1.1,
                    minNeighbors=5,
                    minSize=(30, 30)
                )

                if len(faces) == 0:
                    return result

                result['detected'] = True
                result['count'] = len(faces)

                for i, (x, y, w, h) in enumerate(faces):
                    face_data = {
                        'id': i,
                        'bbox': [int(x), int(y), int(x + w), int(y + h)],
                        'x': int(x),
                        'y': int(y),
                        'width': int(w),
                        'height': int(h),
                        'confidence': 0.85,
                        'landmarks': [
                            {'x': float(x + w * 0.3), 'y': float(y + h * 0.35)},
                            {'x': float(x + w * 0.7), 'y': float(y + h * 0.35)},
                            {'x': float(x + w * 0.5), 'y': float(y + h * 0.55)},
                            {'x': float(x + w * 0.35), 'y': float(y + h * 0.78)},
                            {'x': float(x + w * 0.65), 'y': float(y + h * 0.78)}
                        ]
                    }
                    result['faces'].append(face_data)

                return result

            faces = self.app.get(frame)
            
            if len(faces) == 0:
                return result
            
            result['detected'] = True
            result['count'] = len(faces)
            
            for i, face in enumerate(faces):
                bbox = face.bbox
                x1, y1, x2, y2 = [int(v) for v in bbox]
                
                face_data = {
                    'id': i,
                    'bbox': [x1, y1, x2, y2],
                    'x': x1,
                    'y': y1,
                    'width': x2 - x1,
                    'height': y2 - y1,
                    'confidence': float(face.det_score) if hasattr(face, 'det_score') else 0.95,
                }
                
                # Landmarks
                if hasattr(face, 'kps') and face.kps is not None:
                    face_data['landmarks'] = [
                        {'x': float(p[0]), 'y': float(p[1])}
                        for p in face.kps
                    ]
                
                # Demographics
                if hasattr(face, 'age'):
                    face_data['age'] = int(face.age)
                if hasattr(face, 'gender'):
                    face_data['gender'] = int(face.gender)
                
                # Embedding for recognition
                if hasattr(face, 'embedding'):
                    face_data['embedding'] = face.embedding.tolist()
                
                result['faces'].append(face_data)
            
            return result
        
        except Exception as e:
            logger.error(f"Detection error: {e}")
            return result
    
    def check_liveness(self, frames: List[np.ndarray]) -> Dict:
        """Check if face is real (liveness detection)"""
        if len(frames) < 2:
            return {'is_real': True, 'confidence': 0.5}
        
        try:
            if self.app is None:
                # Fallback mode: movement-based check only.
                boxes = []
                for frame in frames:
                    gray = cv2.cvtColor(frame, cv2.COLOR_BGR2GRAY)
                    detected = self.face_cascade.detectMultiScale(gray, scaleFactor=1.1, minNeighbors=5, minSize=(30, 30))
                    if len(detected) > 0:
                        boxes.append(detected[0])

                if len(boxes) < 2:
                    return {'is_real': False, 'confidence': 0.2}

                movement = float(np.std([b[0] for b in boxes]))
                return {
                    'is_real': movement > 2.0,
                    'confidence': float(min(movement / 12.0, 1.0)),
                    'movement': movement
                }

            detections = []
            for frame in frames:
                faces = self.app.get(frame)
                if len(faces) > 0:
                    detections.append(faces[0])
            
            if len(detections) < 2:
                return {'is_real': False, 'confidence': 0.2}
            
            # Check embedding consistency
            embeddings = [f.embedding for f in detections if hasattr(f, 'embedding')]
            
            if len(embeddings) >= 2:
                try:
                    from scipy.spatial.distance import cosine
                    
                    distances = []
                    for i in range(len(embeddings) - 1):
                        dist = cosine(embeddings[i], embeddings[i+1])
                        distances.append(dist)
                    
                    avg_dist = np.mean(distances)
                    is_real = 0.1 < avg_dist < 0.5
                    conf = max(0, min(1.0, 1.0 - (avg_dist / 1.0)))
                    
                    return {
                        'is_real': is_real,
                        'confidence': float(conf),
                        'distance': float(avg_dist)
                    }
                except ImportError:
                    pass
            
            # Fallback: check movement
            boxes = [f.bbox for f in detections]
            movement = np.std([box[0] for box in boxes])
            
            return {
                'is_real': movement > 3.0,
                'confidence': min(movement / 15.0, 1.0),
                'movement': float(movement)
            }
        
        except Exception as e:
            logger.error(f"Liveness error: {e}")
            return {'is_real': True, 'confidence': 0.0}


def rpc_handler(detector: FaceDetector, req: Dict) -> Dict:
    """Handle JSON-RPC 2.0 requests"""
    
    method = req.get('method', '')
    params = req.get('params', {})
    mid = req.get('id', None)
    
    try:
        if method == 'detect':
            frame = detector.base64_to_image(params.get('frame', ''))
            if frame is None:
                return {
                    'jsonrpc': '2.0',
                    'error': {'code': -32603, 'message': 'Invalid frame'},
                    'id': mid
                }
            
            result = detector.detect_faces(frame)
            return {'jsonrpc': '2.0', 'result': result, 'id': mid}
        
        elif method == 'liveness':
            frames = [
                detector.base64_to_image(f) 
                for f in params.get('frames', [])
            ]
            frames = [f for f in frames if f is not None]
            
            result = detector.check_liveness(frames)
            return {'jsonrpc': '2.0', 'result': result, 'id': mid}
        
        elif method == 'ping':
            return {
                'jsonrpc': '2.0',
                'result': {'status': 'ready'},
                'id': mid
            }
        
        else:
            return {
                'jsonrpc': '2.0',
                'error': {'code': -32601, 'message': f'Unknown method: {method}'},
                'id': mid
            }
    
    except Exception as e:
        logger.error(f"Error: {e}")
        return {
            'jsonrpc': '2.0',
            'error': {'code': -32603, 'message': str(e)},
            'id': mid
        }


def main():
    """Main service loop"""
    logger.info("Starting InsightFace Service")
    
    try:
        detector = FaceDetector()
    except Exception as e:
        logger.error(f"FATAL: {e}")
        sys.exit(1)
    
    logger.info("✓ Ready for commands")
    
    try:
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            
            try:
                req = json.loads(line)
                resp = rpc_handler(detector, req)
                print(json.dumps(resp, separators=(',', ':')), flush=True)
            
            except json.JSONDecodeError as e:
                logger.error(f"JSON error: {e}")
                print(json.dumps({
                    'jsonrpc': '2.0',
                    'error': {'code': -32700, 'message': 'Parse error'},
                    'id': None
                }), flush=True)
    
    except KeyboardInterrupt:
        logger.info("Stopped")
    except Exception as e:
        logger.error(f"Fatal: {e}")
        sys.exit(1)


if __name__ == '__main__':
    main()
