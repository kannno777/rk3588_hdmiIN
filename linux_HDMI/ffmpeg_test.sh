ffmpeg -f v4l2 -thread_queue_size 4096 -channel 0 -ignore_input_error 1 -i /dev/video0 -pix_fmt yuv420p -c:v libx264 -crf 20 -preset ultrafast hoge.ts
