rtt=`sudo ping -q -c 10 -s 65507 ${dst} | grep avg | cut -d"/" -f 5` ; echo "${rtt} ms"
speed=`echo "scale=2; (65535 * 8 * 2) / $rtt / 1000" | bc` ; echo "${speed} Mbps"
