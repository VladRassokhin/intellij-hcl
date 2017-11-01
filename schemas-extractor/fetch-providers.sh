#!/bin/bash

pushd "$(dirname $0)" > /dev/null

curl -s https://api.github.com/orgs/terraform-providers/repos?per_page=200 | jq '.[].name' -r | sort > providers.list.full
cat providers.list.full | grep -- '-provider-' | awk -F '-' '{$1=$2=""; print substr($0, 3)}' | sed 's/ /-/' > providers.list

popd > /dev/null