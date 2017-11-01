#!/bin/bash

pushd "$(dirname $0)"

curl -s https://api.github.com/orgs/terraform-providers/repos?per_page=200 | jq '.[].name' -r > ../providers.list
#[ -f /providers.list ] && rm ../providers.list
#
#for p in $(cat providers.list); do
#  echo "https://github.com/terraform-providers/$p" >> ../providers.list
#done

popd