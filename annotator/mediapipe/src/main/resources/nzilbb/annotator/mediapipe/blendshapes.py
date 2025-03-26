#!/usr/bin/env python3
# code adapted from
# https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker/python#video
import sys
import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
from mediapipe.framework.formats import landmark_pb2
from mediapipe import solutions
import cv2
import csv
import numpy as np
if len(sys.argv) < 9:
  print("Please supply th followin parameters:")
  print(" - video file name")
  print(" - num_faces setting (int)")
  print(" - min_face_detection_confidence setting (float)")
  print(" - min_face_presence_confidence setting (float)")
  print(" - min_tracking_confidence setting (float)")
  print(" - CSV file name for blendshape scores, or NA to not generate them")
  print(" - Annotated video file name (must end in .mp4), or NA to not generate it")
  print(" - Annotated frame image file pattern (e.g. frame-{0}.png), or NA to not generate them")
  exit()

MODEL_PATH = 'face_landmarker.task'
INPUT_VIDEO_FILE = sys.argv[1]
NUM_FACES = int(sys.argv[2])
print("NUM_FACES " + str(NUM_FACES))
MIN_FACE_DETECTION_CONFIDENCE = float(sys.argv[3])
print("MIN_FACE_DETECTION_CONFIDENCE " + str(MIN_FACE_DETECTION_CONFIDENCE))
MIN_FACE_PRESENCE_CONFIDENCE = float(sys.argv[4])
print("MIN_FACE_PRESENCE_CONFIDENCE " + str(MIN_FACE_PRESENCE_CONFIDENCE))
MIN_TRACKING_CONFIDENCE = float(sys.argv[5])
print("MIN_TRACKING_CONFIDENCE " + str(MIN_TRACKING_CONFIDENCE))
OUTPUT_CSV_FILE = sys.argv[6]
OUTPUT_VIDEO_FILE = sys.argv[7]
OUTPUT_FRAME_FORMAT = sys.argv[8]

# Use OpenCV’s VideoCapture to load the input video.

# Open the video file
video = cv2.VideoCapture(INPUT_VIDEO_FILE)

# Load the frame rate of the video using OpenCV’s CV_CAP_PROP_FPS
# You’ll need it to calculate the timestamp for each frame.

fps = video.get(cv2.CAP_PROP_FPS)
width = int(video.get(cv2.CAP_PROP_FRAME_WIDTH))
height = int(video.get(cv2.CAP_PROP_FRAME_HEIGHT))

def draw_landmarks_on_image(rgb_image, detection_result):
  face_landmarks_list = detection_result.face_landmarks
  annotated_image = np.copy(rgb_image)

  # Loop through the detected faces to visualize.
  for idx in range(len(face_landmarks_list)):
    face_landmarks = face_landmarks_list[idx]

    # Draw the face landmarks.
    face_landmarks_proto = landmark_pb2.NormalizedLandmarkList()
    face_landmarks_proto.landmark.extend([
      landmark_pb2.NormalizedLandmark(x=landmark.x, y=landmark.y, z=landmark.z) for landmark in face_landmarks
    ])

    solutions.drawing_utils.draw_landmarks(
        image=annotated_image,
        landmark_list=face_landmarks_proto,
        connections=mp.solutions.face_mesh.FACEMESH_TESSELATION,
        landmark_drawing_spec=None,
        connection_drawing_spec=mp.solutions.drawing_styles
        .get_default_face_mesh_tesselation_style())
    solutions.drawing_utils.draw_landmarks(
        image=annotated_image,
        landmark_list=face_landmarks_proto,
        connections=mp.solutions.face_mesh.FACEMESH_CONTOURS,
        landmark_drawing_spec=None,
        connection_drawing_spec=mp.solutions.drawing_styles
        .get_default_face_mesh_contours_style())
    solutions.drawing_utils.draw_landmarks(
        image=annotated_image,
        landmark_list=face_landmarks_proto,
        connections=mp.solutions.face_mesh.FACEMESH_IRISES,
          landmark_drawing_spec=None,
          connection_drawing_spec=mp.solutions.drawing_styles
          .get_default_face_mesh_iris_connections_style())

  return annotated_image


BaseOptions = mp.tasks.BaseOptions
FaceLandmarker = mp.tasks.vision.FaceLandmarker
FaceLandmarkerOptions = mp.tasks.vision.FaceLandmarkerOptions
VisionRunningMode = mp.tasks.vision.RunningMode

# Create a face landmarker instance with the video mode:
options = FaceLandmarkerOptions(
    base_options=BaseOptions(model_asset_path=MODEL_PATH),
    output_face_blendshapes=True,
    output_facial_transformation_matrixes=True,
    num_faces=NUM_FACES,
    min_face_detection_confidence=MIN_FACE_DETECTION_CONFIDENCE,
    min_face_presence_confidence=MIN_FACE_PRESENCE_CONFIDENCE,
    min_tracking_confidence=MIN_TRACKING_CONFIDENCE,
    running_mode=VisionRunningMode.VIDEO)

codec_id = "mp4v" # ID for a video codec.
fourcc = cv2.VideoWriter_fourcc(*codec_id)
if OUTPUT_VIDEO_FILE != "NA":
  out = cv2.VideoWriter(OUTPUT_VIDEO_FILE, fourcc=fourcc, fps=fps, frameSize=(width, height))
    
with FaceLandmarker.create_from_options(options) as landmarker:
  if OUTPUT_CSV_FILE != "NA":
    csvFile = open(OUTPUT_CSV_FILE, 'w')
    # The landmarker is initialized. Use it here.
    fieldnames = ["frame","offset","_neutral","browDownLeft","browDownRight","browInnerUp","browOuterUpLeft","browOuterUpRight","cheekPuff","cheekSquintLeft","cheekSquintRight","eyeBlinkLeft","eyeBlinkRight","eyeLookDownLeft","eyeLookDownRight","eyeLookInLeft","eyeLookInRight","eyeLookOutLeft","eyeLookOutRight","eyeLookUpLeft","eyeLookUpRight","eyeSquintLeft","eyeSquintRight","eyeWideLeft","eyeWideRight","jawForward","jawLeft","jawOpen","jawRight","mouthClose","mouthDimpleLeft","mouthDimpleRight","mouthFrownLeft","mouthFrownRight","mouthFunnel","mouthLeft","mouthLowerDownLeft","mouthLowerDownRight","mouthPressLeft","mouthPressRight","mouthPucker","mouthRight","mouthRollLower","mouthRollUpper","mouthShrugLower","mouthShrugUpper","mouthSmileLeft","mouthSmileRight","mouthStretchLeft","mouthStretchRight","mouthUpperUpLeft","mouthUpperUpRight","noseSneerLeft","noseSneerRight"]
    blendShapes = csv.DictWriter(csvFile, fieldnames=fieldnames)
    blendShapes.writeheader()
  
  # Loop through each frame in the video using VideoCapture#read()
  f = 0
  while video.isOpened():
    ret, frame = video.read()
    if not ret:
      break

    # timestamp
    frame_timestamp_s = f/fps
    frame_timestamp_ms = int(frame_timestamp_s * 1000)
    
    # Convert the frame received from OpenCV to a MediaPipe’s Image object.
    mp_image = mp.Image(image_format=mp.ImageFormat.SRGB, data=frame)
    detection_result = landmarker.detect_for_video(mp_image, frame_timestamp_ms)
    
    if len(detection_result.face_blendshapes) > 0:
      if OUTPUT_CSV_FILE != "NA":
        row = {}
        row["frame"] = f
        row["offset"] = frame_timestamp_s
        for category in detection_result.face_blendshapes[0]:
          row[category.category_name] = '{:f}'.format(category.score) # no scientific notation
        blendShapes.writerow(row)
      if OUTPUT_FRAME_FORMAT != "NA":
        frame = draw_landmarks_on_image(mp_image.numpy_view(), detection_result)      
        cv2.imwrite(OUTPUT_FRAME_FORMAT.format(str(f)), frame)
        
    if OUTPUT_VIDEO_FILE != "NA":
      out.write(frame)
    f = f+1

video.release()
if OUTPUT_VIDEO_FILE != "NA":
  print("Annotated video: " + OUTPUT_VIDEO_FILE)
if OUTPUT_FRAME_FORMAT != "NA":
  print("Annotated frames: " + OUTPUT_FRAME_FORMAT)
if OUTPUT_CSV_FILE != "NA":
  csvFile.close()
  print("Blendshape data: " + OUTPUT_CSV_FILE)
