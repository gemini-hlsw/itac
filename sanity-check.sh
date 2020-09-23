#! /bin/bash
echo
echo "👉 diff with gs baseline"
echo
bloop run main -- --dir work queue -s 2> /dev/null | diff baseline-gs -
echo
echo "👉 diff with gs baseline"
echo
bloop run main -- --dir work queue -n 2> /dev/null | diff baseline-gn -