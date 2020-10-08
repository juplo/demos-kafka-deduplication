#!/bin/bash

for i in `seq 1 333`;     do echo $(($i%7)):$i; done >  data.txt
for i in `seq 70 578`;    do echo $(($i%7)):$i; done >> data.txt
for i in `seq 400 1211`;  do echo $(($i%7)):$i; done >> data.txt
for i in `seq 1000 1111`; do echo $(($i%7)):$i; done >> data.txt
for i in `seq 1200 1711`; do echo $(($i%7)):$i; done >> data.txt
for i in `seq 1688 3333`; do echo $(($i%7)):$i; done >> data.txt
for i in `seq 2567 3500`; do echo $(($i%7)):$i; done >> data.txt

for i in `seq 1 3500`; do echo $(($i%7)):$i; done | grep ^0 > expected_0.txt
for i in `seq 1 3500`; do echo $(($i%7)):$i; done | grep ^1 > expected_1.txt
for i in `seq 1 3500`; do echo $(($i%7)):$i; done | grep ^2 > expected_2.txt
for i in `seq 1 3500`; do echo $(($i%7)):$i; done | grep ^3 > expected_3.txt
for i in `seq 1 3500`; do echo $(($i%7)):$i; done | grep ^4 > expected_4.txt
for i in `seq 1 3500`; do echo $(($i%7)):$i; done | grep ^5 > expected_5.txt
for i in `seq 1 3500`; do echo $(($i%7)):$i; done | grep ^6 > expected_6.txt
