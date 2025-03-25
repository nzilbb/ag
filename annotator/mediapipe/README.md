# MediaPipe annotator

The annotator integrates with MediaPipe <https://chuoling.github.io/mediapipe/> which must
be installed locally.

## Installation

### Ubuntu

1. `wget https://github.com/bazelbuild/bazelisk/releases/download/v1.25.0/bazelisk-amd64.deb`
1. `sudo apt install ./bazelisk-amd64.deb`
1. `cd $HOME`
1. `git clone https://github.com/google/mediapipe.git`
1. `cd mediapipe`
1. `sudo apt-get install -y \
    libopencv-core-dev \
    libopencv-highgui-dev \
    libopencv-calib3d-dev \
    libopencv-features2d-dev \
    libopencv-imgproc-dev \
    libopencv-video-dev`
1. Test:\
   `bazel run --define MEDIAPIPE_DISABLE_GPU=1 \
    mediapipe/examples/desktop/hello_world:hello_world`

For GPU integration:

1. `sudo apt-get install mesa-common-dev libegl1-mesa-dev libgles2-mesa-dev`
1. `bazel run --copt -DMESA_EGL_NO_X11_HEADERS --copt -DEGL_NO_X11 \
    mediapipe/examples/desktop/hello_world:hello_world`


...But I couldn't get `mediapipe/examples/desktop/face_mesh:face_mesh_gpu` to work;
dependencies and paths seemed wonky and/or there were compilation errors?!

## Python

1. `apt install python3.10-venv`
1. `python3 -m venv mp_env && source mp_env/bin/activate`
1. `pip install mediapipe`
1. `python3`
1. `import mediapipe as mp`
1. `mp_face_mesh = mp.solutions.face_mesh`

For face landmarker:

1. `wget -O face_landmarker_v2_with_blendshapes.task -q https://storage.googleapis.com/mediapipe-models/face_landmarker/face_landmarker/float16/1/face_landmarker.task`
1. `python3 < landmarks.py`

Docs are here:\
<https://ai.google.dev/edge/mediapipe/solutions/vision/face_landmarker>
