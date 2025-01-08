#!/bin/bash
# $1 == GH_TOKEN

if [ -z "$RELEASE_VERSION" ]; then
    echo -n "Determining release version: "
    release_version=${GITHUB_REF:11}
else
    release_version=${RELEASE_VERSION}
fi
echo $release_version

if [ -z "$GIT_USER_NAME" ]; then
  echo "GIT_USER_NAME not defined."
  exit 1
fi
if [ -z "$GIT_USER_EMAIL" ]; then
  echo "GIT_USER_EMAIL not defined."
  exit 1
fi

echo "Configuring git"
git config --global --add safe.directory /github/workspace
git config --global user.email "$GIT_USER_EMAIL"
git config --global user.name "$GIT_USER_NAME"
git fetch

if [ -z "$TARGET_BRANCH" ]; then
    echo -n "Determining target branch: "
    target_branch=`cat $GITHUB_EVENT_PATH | jq '.release.target_commitish' | sed -e 's/^"\(.*\)"$/\1/g'`
    echo $target_branch
else 
    target_branch=$TARGET_BRANCH
fi

git checkout $target_branch

echo "Setting release version in gradle.properties"
sed -i "s/^projectVersion.*$/projectVersion\=${release_version}/" gradle.properties
cat gradle.properties

echo "Pushing release version and recreating v${release_version} tag"
git add gradle.properties
git commit -m "[skip ci] Release v${release_version}"
git push origin $target_branch
git tag -fa v${release_version} -m "Release v${release_version}"
git push origin $target_branch
# force push the updated tag
git push origin v${release_version} --force

echo "Closing again the release after updating the tag"
if [ -z "$RELEASE_URL" ]; then
    echo -n "Determining Release RELEASE_URL: "
    release_url=`cat $GITHUB_EVENT_PATH | jq '.release.url' | sed -e 's/^"\(.*\)"$/\1/g'`
else
    release_url=$RELEASE_URL
fi
echo $release_url
curl -s --request PATCH -H "Authorization: Bearer $1" -H "Content-Type: application/json" $release_url --data "{\"draft\": false}"
