#!/usr/bin/env bash

CUR="$(pwd)"

out="$CUR/schemas"
mkdir -p "$out"
rm -f "$CUR/failure.txt"

pushd "$GOPATH/src/github.com/terraform-providers" >/dev/null

mkdir -p "$GOPATH/src/github.com/terraform-providers"

for p in $(cat "$CUR/providers.list.full"); do
  if [ -d "$p" ]; then
    echo "Updating $p"
    pushd "$p" >/dev/null
    git checkout -- vendor/
    git pull
    popd >/dev/null
  else
    echo "Cloning $p"
    git clone "https://github.com/terraform-providers/$p.git"
  fi

  pushd "$p" >/dev/null


  echo "Preparing $p"
  revision="$(git describe)"

  rm -rf generate-schema
  mkdir generate-schema
  cp -r "$CUR/template/generate-schema.go" generate-schema/generate-schema.go
  find generate-schema -type f -exec sed -i "s/__FULL_NAME__/$p/g" {} +
  find generate-schema -type f -exec sed -i "s/__NAME__/${p:19}/g" {} +
  find generate-schema -type f -exec sed -i "s/__REVISION__/$revision/g" {} +
  find generate-schema -type f -exec sed -i "s~__OUT__~$out~g" {} +

  #echo "Building $p"
  #make

  echo "Generating schema for $p"
  go run generate-schema/generate-schema.go
  if [[ $? -ne 0 ]]; then
     echo "$p" >> "$CUR/failure.txt"
  fi

  echo "Finished $p"

  popd >/dev/null
done

popd >/dev/null

