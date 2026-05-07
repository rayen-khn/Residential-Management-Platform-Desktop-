#!/usr/bin/env python3
"""
Face Detection Service using InsightFace
Provides face detection, landmarks, and embeddings via JSON-RPC over stdio
Works out-of-the-box - auto-downloads models on first use

Python: 3.8+ (tested with 3.13+)
Requires: insightface, onnxruntime
"""

import cv2
import numpy as np
import json
import sys
import base64
import os
from typing import Dict, List, Optional
import logging

# Setup logging
logging.basicConfig(
    level=logging.INFO,
    format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
    stream=sys.stderr,
    datefmt="%Y-%m-%d %H:%M:%S"
)
logger = logging.getLogger('InsightFace')

# Import InsightFace
try:
    import insightface
    logger.info("✓ InsightFace library imported successfully")
except ImportError:
    logger.error("ERROR: insightface not installed")
    logger.error("Install with: pip install insightface onnxruntime")
    sys.exit(1)


class FaceDetector:
    """InsightFace-based face detection and recognition"""
    
    def __init__(self):
        """Initialize InsightFace detector"""
        logger.info("Initializing InsightFace Face Analysis...")
        
        try:
            # Initialize InsightFace with default settings
            self.app = insightface.app.FaceAnalysis(
                name='buffalo_sc',  # Small and fast model
                providers=['CPUProvider']  # Use CPU
            )
            
            logger.info("✓ InsightFace Analysis initialized")
            logger.info("  Auto-downloading models on first use...")
            
            # Prepare the app (downloads models if needed)
            self.app.prepare(ctx_id=-1, det_model='retinaface', rec_model='arcface')
            
            logger.info("✓ InsightFace detector ready")
            logger.info("  Detection: RetinaFace (accurate, fast)")
            logger.info("  Recognition: ArcFace (face embeddings)")
            
        except Exception as e:
            logger.error(f"ERROR initializing InsightFace: {e}")
            logger.error("Attempting fallback initialization...")
            
            try:
                # Simpler initialization as fallback
                self.app = insightface.app.FaceAnalysis()
                self.app.prepare(ctx_id=-1)
                logger.info("✓ InsightFace initialized (compatibility mode)")
            except Exception as e2:
                logger.error(f"FATAL: Could not initialize InsightFace: {e2}")
                import traceback
                traceback.print_exc()
                sys.exit(1)
    
    def base64_to_image(self, base64_str: str) -> Optional[np.ndarray]:
        """Convert base64 string to OpenCV image"""
        try:
            img_data = base64.b64decode(base64_str)
            nparr = np.frombuffer(img_data, np.uint8)
            img = cv2.imdecode(nparr, cv2.IMREAD_COLOR)
            return img
        except Exception as e:
            logger.error(f"Error decoding base64: {e}")
            return None
    
    def image_to_base64(self, image: np.ndarray) -> str:
        """Convert OpenCV image to base64"""
        try:
            _, buffer = cv2.imencode('.jpg', image, [cv2.IMWRITE_JPEG_QUALITY, 95])
            return base64.b64encode(buffer).tobytes().decode('utf-8')
        except Exception as e:
            logger.error(f"Error encoding image: {e}")
            return ""
    
    def detect_faces(self, frame: np.ndarray) -> Dict:
        """Detect faces and extract landmarks using InsightFace"""
        results = {
            'detected': False,
            'faces': [],
            'num_faces': 0
        }
        
        try:
            # Detect all faces
            faces = self.app.get(frame)
            
            if len(faces) == 0:
                return results
            
            results['detected'] = True
            results['num_faces'] = len(faces)
            
            # Process each detected face
            for i, face in enumerate(faces):
                # Extract bounding box
                bbox = face.bbox
                x1, y1, x2, y2 = [int(v) for v in bbox]
                w = x2 - x1
                h = y2 - y1
                
                # Extract keypoints (5 landmark points: eyes, nose, mouth corners)
                kps = face.kps if hasattr(face, 'kps') and face.kps is not None else []
                landmarks = []
                if len(kps) > 0:
                    for pt in kps:
                        landmarks.append({
                            'x': float(pt[0]),
                            'y': float(pt[1])
                        })
                
                face_data = {
                    'face_id': i,
                    'bbox': [x1, y1, x2, y2],
                    'x': x1,
                    'y': y1,
                    'width': w,
                    'height': h,
                    'confidence': float(face.det_score) if hasattr(face, 'det_score') else 0.95,
                    'landmarks': landmarks,
                    'age': int(face.age) if hasattr(face, 'age') else -1,
                    'gender': int(face.gender) if hasattr(face, 'gender') else 0,  # 0=male, 1=female
                }
                
                # Add embedding if available
                if hasattr(face, 'embedding') and face.embedding is not None:
                    face_data['embedding'] = face.embedding.tolist()
                
                results['faces'].append(face_data)
            
            return results
        
        except Exception as e:
            logger.error(f"Detection error: {e}")
            import traceback
            traceback.print_exc()
            return results
    
    def check_liveness(self, frames: List[np.ndarray]) -> Dict:
        """
        Check if faces are real using multiple frames
        Real people: show movement and variation
        Photos: static and unchanging
        """
        if len(frames) < 2:
            return {'is_real': True, 'confidence': 0.5, 'method': 'insufficient_frames'}
        
        try:
            detections = []
            
            # Run detection on all frames
            for frame in frames:
                faces = self.app.get(frame)
                if len(faces) > 0:
                    detections.append(faces[0])
            
            if len(detections) < 2:
                return {'is_real': False, 'confidence': 0.3, 'method': 'no_consistent_detection'}
            
            # Check face embeddings similarity (real person should have similar embeddings)
            embeddings = [f.embedding for f in detections if hasattr(f, 'embedding')]
            
            if len(embeddings) >= 2:
                # Calculate embedding distance (same person = low distance)
                from scipy.spatial.distance import cosine
                
                distances = []
                for i in range(len(embeddings) - 1):
                    dist = cosine(embeddings[i], embeddings[i+1])
                    distances.append(dist)
                
                avg_distance = np.mean(distances)
                
                # Real person: embeddings similar (distance < 0.4)
                # Photo: might vary significantly or be too consistent
                is_real = 0.1 < avg_distance < 0.5
                confidence = 1.0 - (avg_distance / 1.0)  # Normalize
                
                return {
                    'is_real': is_real,
                    'confidence': float(np.clip(confidence, 0, 1)),
                    'method': 'embedding_consistency',
                    'avg_distance': float(avg_distance)
                }
            
            # Fallback: check bbox movement
            bboxes = [f.bbox for f in detections]
            centers = [(box[0] + box[2])/2 for box in bboxes]
            
            movement = np.std(centers)
            is_real = movement > 5.0  # Real people move more
            confidence = min(movement / 20.0, 1.0)
            
            return {
                'is_real': is_real,
                'confidence': float(confidence),
                'method': 'movement_detection',
                'movement': float(movement)
            }
        
        except Exception as e:
            logger.error(f"Liveness check error: {e}")
            return {'is_real': True, 'confidence': 0.0, 'method': 'error'}


def process_json_rpc(detector: FaceDetector, request: Dict) -> Dict:
    """Process JSON-RPC 2.0 request"""
    
    method = request.get('method', '')
    params = request.get('params', {})
    request_id = request.get('id', None)
    
    try:
        if method == 'detect':
            frame_b64 = params.get('frame', '')
            frame = detector.base64_to_image(frame_b64)
            
            if frame is None:
                return {
                    'jsonrpc': '2.0',
                    'error': {'code': -32603, 'message': 'Failed to decode frame'},
                    'id': request_id
                }
            
            result = detector.detect_faces(frame)
            
            return {
                'jsonrpc': '2.0',
                'result': result,
                'id': request_id
            }
        
        elif method == 'liveness':
            frames_b64 = params.get('frames', [])
            frames = [detector.base64_to_image(f) for f in frames_b64]
            frames = [f for f in frames if f is not None]
            
            result = detector.check_liveness(frames)
            
            return {
                'jsonrpc': '2.0',
                'result': result,
                'id': request_id
            }
        
        elif method == 'ping':
            return {
                'jsonrpc': '2.0',
                'result': {'status': 'ready'},
                'id': request_id
            }
        
        else:
            return {
                'jsonrpc': '2.0',
                'error': {'code': -32601, 'message': f'Unknown method: {method}'},
                'id': request_id
            }
    
    except Exception as e:
        logger.error(f"Error processing method {method}: {e}")
        import traceback
        traceback.print_exc()
        
        return {
            'jsonrpc': '2.0',
            'error': {'code': -32603, 'message': str(e)},
            'id': request_id
        }


def main():
    """Main JSON-RPC 2.0 server loop"""
    logger.info("=" * 60)
    logger.info("InsightFace Face Detection Service")
    logger.info("=" * 60)
    
    try:
        detector = FaceDetector()
    except Exception as e:
        logger.error(f"FATAL: Could not initialize detector: {e}")
        sys.exit(1)
    
    logger.info("✓ Service ready - waiting for JSON-RPC commands on stdin")
    logger.info("")
    
    try:
        # Read JSON-RPC commands from stdin
        for line in sys.stdin:
            line = line.strip()
            if not line:
                continue
            
            try:
                request = json.loads(line)
                response = process_json_rpc(detector, request)
                
                # Send JSON-RPC 2.0 response
                output = json.dumps(response, separators=(',', ':'))
                print(output, flush=True)
                
            except json.JSONDecodeError as e:
                logger.error(f"JSON parse error: {e}")
                response = {
                    'jsonrpc': '2.0',
                    'error': {'code': -32700, 'message': 'Parse error'},
                    'id': None
                }
                print(json.dumps(response), flush=True)
    
    except KeyboardInterrupt:
        logger.info("Service interrupted")
    except Exception as e:
        logger.error(f"Fatal error in main loop: {e}")
        import traceback
        traceback.print_exc()
        sys.exit(1)


if __name__ == '__main__':
    main()
