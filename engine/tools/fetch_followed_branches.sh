#!/bin/bash
for i in `git branch -a | grep '/master' | grep -v origin | grep -v sakai  | cut -f1 -d'/'`
do 
  echo "Fetching $i"
  git fetch $i
done

