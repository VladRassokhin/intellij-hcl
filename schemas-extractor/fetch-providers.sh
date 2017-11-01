#!/bin/bash

pushd "$(dirname $0)" > /dev/null

curl -s https://api.github.com/orgs/terraform-providers/repos?per_page=200 | jq '.[].name' -r > providers.list

popd > /dev/null