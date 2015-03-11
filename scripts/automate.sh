#!/bin/sh

diffusion_latest_stable_url=$1
diffusion_url=$2

now=$(date +%s)
echo $now

mkdir ./diffusion_server$now
cd ./diffusion_server$now
echo "The present working directory is `pwd`"
wget --output-document=diffusion_version.xml "${diffusion_latest_stable_url}"
version=$(cat diffusion_version.xml)
echo $version
rm Diffusion$version.jar
wget "${diffusion_url}"
unzip ./Diffusion$version.jar
cd ..
echo "The present working directory is `pwd`"
chmod +x ./diffusion_server$now/Diffusion$version/bin/diffusion.sh
$('pwd')/run_benchmark.sh ./diffusion_server$now/Diffusion$version $version $1
