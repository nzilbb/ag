#!/usr/bin/env python3
# code adapted from
# https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker/python#video
import sys
import json
import mediapipe as mp
from mediapipe.tasks import python
from mediapipe.tasks.python import vision
from mediapipe.framework.formats import landmark_pb2
from mediapipe import solutions
import cv2
import csv
import numpy as np
if len(sys.argv) < 13:
  print("Please supply th followin parameters:")
  print(" - video file name")
  print(" - num_faces setting (int)")
  print(" - min_face_detection_confidence setting (float)")
  print(" - min_face_presence_confidence setting (float)")
  print(" - min_tracking_confidence setting (float)")
  print(" - CSV file name for blendshape scores, or NA to not generate them")
  print(" - JSON file pattern (e.g. frame-{0}.json), or NA to not generate them")
  print(" - Annotated video file name (must end in .mp4), or NA to not generate it")
  print(" - Annotated frame image file pattern (e.g. frame-{0}.png), or NA to not generate them")
  print(" - Paint landmark tesselation: true or false")
  print(" - Paint landmark contours: true or false")
  print(" - Paint irises: true or false")
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
OUTPUT_JSON_FORMAT = sys.argv[7]
OUTPUT_VIDEO_FILE = sys.argv[8]
OUTPUT_FRAME_FORMAT = sys.argv[9]
PAINT_TESSELATION = sys.argv[10].lower() == "true"
PAINT_CONTOURS = sys.argv[11].lower() == "true"
PAINT_IRISES = sys.argv[12].lower() == "true"

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

    if PAINT_TESSELATION:
      solutions.drawing_utils.draw_landmarks(
        image=annotated_image,
        landmark_list=face_landmarks_proto,
        connections=mp.solutions.face_mesh.FACEMESH_TESSELATION,
        landmark_drawing_spec=None,
        connection_drawing_spec=mp.solutions.drawing_styles
        .get_default_face_mesh_tesselation_style())
    if PAINT_CONTOURS:
      solutions.drawing_utils.draw_landmarks(
        image=annotated_image,
        landmark_list=face_landmarks_proto,
        connections=mp.solutions.face_mesh.FACEMESH_CONTOURS,
        landmark_drawing_spec=None,
        connection_drawing_spec=mp.solutions.drawing_styles
        .get_default_face_mesh_contours_style())
    if PAINT_IRISES:
      solutions.drawing_utils.draw_landmarks(
        image=annotated_image,
        landmark_list=face_landmarks_proto,
        connections=mp.solutions.face_mesh.FACEMESH_IRISES,
          landmark_drawing_spec=None,
          connection_drawing_spec=mp.solutions.drawing_styles
          .get_default_face_mesh_iris_connections_style())

  return annotated_image

# convert result to a plain dictionary that can be serialized
def result_to_dict(detection_result):
  face_landmarks = []
  for face in detection_result.face_landmarks:
    landmarks = []
    for landmark in face:
      landmarks.append({
        "x": landmark.x, "y": landmark.y, "z": landmark.z
      })
    face_landmarks.append(landmarks)
  face_blendshapes = []
  for face in detection_result.face_blendshapes:
    blendshapes = {}
    for category in face:
      blendshapes[category.category_name] = category.score
    face_blendshapes.append(blendshapes)
  facial_transformation_matrixes = []
  for face in detection_result.facial_transformation_matrixes:
    matrices = []
    for matrix in face:
      row = []
      for value in matrix:
        row.append(value)
      matrices.append(row)
    facial_transformation_matrixes.append(matrices)
  return ({
    "face_landmarks": face_landmarks,
    "face_blendshapes": face_blendshapes,
    "facial_transformation_matrixes": facial_transformation_matrixes
  })

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
  framesWithBlendshapes = 0
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
      framesWithBlendshapes = framesWithBlendshapes + 1
      if OUTPUT_CSV_FILE != "NA":
        row = {}
        row["frame"] = f
        row["offset"] = frame_timestamp_s
        for category in detection_result.face_blendshapes[0]:
          row[category.category_name] = '{:f}'.format(category.score) # no scientific notation
        blendShapes.writerow(row)
      
      if OUTPUT_JSON_FORMAT != "NA":
        with open(OUTPUT_JSON_FORMAT.format(str(f)), 'w') as jsonFile:
          json.dump(result_to_dict(detection_result), jsonFile)
      
      if OUTPUT_FRAME_FORMAT != "NA" or OUTPUT_VIDEO_FILE != "NA":
        frame = draw_landmarks_on_image(mp_image.numpy_view(), detection_result)
        if OUTPUT_FRAME_FORMAT != "NA":
          cv2.imwrite(OUTPUT_FRAME_FORMAT.format(str(f)), frame)
        
    if OUTPUT_VIDEO_FILE != "NA":
      out.write(frame)
    f = f+1

video.release()
print("Frames with blendshapes: " + str(framesWithBlendshapes))
if OUTPUT_VIDEO_FILE != "NA":
  print("Annotated video: " + OUTPUT_VIDEO_FILE)
if OUTPUT_FRAME_FORMAT != "NA":
  print("Annotated frames: " + OUTPUT_FRAME_FORMAT)
if OUTPUT_CSV_FILE != "NA":
  csvFile.close()
  print("Blendshape data: " + OUTPUT_CSV_FILE)
