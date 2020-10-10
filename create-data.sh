#!/bin/bash

function name()
{
  case $(($1%7)) in
    0) echo "peter"
       ;;
    1) echo "franz"
       ;;
    2) echo "ute"
       ;;
    3) echo "klaus"
       ;;
    4) echo "paul"
       ;;
    5) echo "petra"
       ;;
    6) echo "siggi"
       ;;
  esac
}

for i in `seq 1 333`;     do echo $(name $i):$i; done >  data.txt
for i in `seq 70 578`;    do echo $(name $i):$i; done >> data.txt
for i in `seq 400 1211`;  do echo $(name $i):$i; done >> data.txt
for i in `seq 1000 1111`; do echo $(name $i):$i; done >> data.txt
for i in `seq 1200 1711`; do echo $(name $i):$i; done >> data.txt
for i in `seq 1688 3333`; do echo $(name $i):$i; done >> data.txt
for i in `seq 2567 3500`; do echo $(name $i):$i; done >> data.txt

for i in `seq 1 3500`;    do echo $(name $i):$i | awk -F: '/^peter/ { print $2 }'; done > expected_peter.txt
for i in `seq 1 3500`;    do echo $(name $i):$i | awk -F: '/^franz/ { print $2 }'; done > expected_franz.txt
for i in `seq 1 3500`;    do echo $(name $i):$i | awk -F: '/^ute/   { print $2 }'; done > expected_ute.txt
for i in `seq 1 3500`;    do echo $(name $i):$i | awk -F: '/^klaus/ { print $2 }'; done > expected_klaus.txt
for i in `seq 1 3500`;    do echo $(name $i):$i | awk -F: '/^paul/  { print $2 }'; done > expected_paul.txt
for i in `seq 1 3500`;    do echo $(name $i):$i | awk -F: '/^petra/ { print $2 }'; done > expected_petra.txt
for i in `seq 1 3500`;    do echo $(name $i):$i | awk -F: '/^siggi/ { print $2 }'; done > expected_siggi.txt

