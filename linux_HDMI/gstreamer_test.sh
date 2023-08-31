gst-launch-1.0 v4l2src device=/dev/video0 ! video/x-raw,format=NV12,width=3840,height=2160,framerate=60/1 ! queue ! xvimagesink

ffmpeg -f v4l2 -thread_queue_size 4096 -channel 0 -ignore_input_error 1 -i /dev/video0 -pix_fmt yuv420p -c:v libx264 -crf 20 -preset ultrafast hdmiIN.ts


