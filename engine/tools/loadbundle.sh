#!/bin/sh
curl -Fbundlestartlevel=20 -Faction=install -Fbundlestart=start -F"bundlefile=@$1"   http://admin:admin@localhost:8080/system/console/bundles

