#!/bin/sh

## @test
##
## @summary test jni MonitorExit
## @run shell testJniMonitorExit.sh
##


export LD_LIBRARY_PATH=.:${COMPILEJAVA}/lib/server:/usr/lib:$LD_LIBRARY_PATH
echo ${COMPILEJAVA}
echo $LD_LIBRARY_PATH
gcc -DLINUX -o testJniMonitorExit \
    -I${COMPILEJAVA}/include -I${COMPILEJAVA}/include/linux \
    -L${COMPILEJAVA}/lib/server \
    ${TESTSRC}/testJniMonitorExit.c -ljvm -lpthread

./testJniMonitorExit
exit $?
