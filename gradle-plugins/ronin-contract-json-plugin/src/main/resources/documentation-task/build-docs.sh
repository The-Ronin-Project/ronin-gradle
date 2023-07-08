set -e
pip install json-schema-for-humans
cd /app
if [[ -e ./build/docs ]]; then
  rm -rf ./build/docs
fi
mkdir -p ./build/docs
generate-schema-doc "./src/main/resources/schemas/*.schema.json" ./build/docs
chmod gao+rwx ./build/docs
chmod gao+rw -R ./build/docs
