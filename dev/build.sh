filePath=$(dirname $0)
cd $filePath/..

if [ $# -eq 0 ]; then
 DEPLOY=0
elif [ "$1" == "--deploy" ]; then
 DEPLOY=1 
else
 DEPLOY=0
fi

rm ./out/EC-OpenShift.jar
rm ./EC-OpenShift.zip

jar cvf ./out/EC-OpenShift.jar dsl/ META-INF/ pages/ lib/ htdocs/
zip -r ./EC-OpenShift.zip dsl/ META-INF/ pages/ lib/ htdocs/

if [ $DEPLOY -eq 1 ]; then
  echo "Installing plugin ..."
  ectool --server localhost login admin changeme
  ectool installPlugin ./out/EC-OpenShift.jar --force 1
  ectool promotePlugin EC-OpenShift-1.2.0
fi  