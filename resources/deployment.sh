cd ../

gradle clean build

cd ./resources || exit

cp ../build/libs/*all.jar ../resources/

scp ./*all.jar root@106.53.106.142:/root/server/
