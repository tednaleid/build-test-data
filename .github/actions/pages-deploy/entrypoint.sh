#!/bin/bash

set -e

if [ -z "$GH_TOKEN" ]
then
  echo "You must provide the action with a GitHub Personal Access Token secret in order to deploy."
  exit 1
fi

if [ -z "$BRANCH" ]
then
  echo "You must provide the action with a branch name it should deploy to, for example gh-pages or docs."
  exit 1
fi

if [ -z "$DOC_FOLDER" ];
then
  DOC_FOLDER=$BRANCH
fi

if [ -z "$SKIP_SNAPSHOT" ]; then
  SKIP_SNAPSHOT="$BETA"
fi

if [ -z "$SKIP_LATEST" ]; then
  SKIP_LATEST="$SKIP_SNAPSHOT"
fi

if [ -z "$FOLDER" ]
then
  echo "You must provide the action with the folder name in the repository where your compiled page lives."
  exit 1
fi

case "$FOLDER" in /*|./*)
  echo "The deployment folder cannot be prefixed with '/' or './'. Instead reference the folder name directly."
  exit 1
esac

if [ -z "$COMMIT_EMAIL" ]
then
  COMMIT_EMAIL="${GITHUB_ACTOR}@users.noreply.github.com"
fi

if [ -z "$COMMIT_NAME" ]
then
  COMMIT_NAME="${GITHUB_ACTOR}"
fi
if [ -z "$TARGET_REPOSITORY" ]
then
  TARGET_REPOSITORY="${GITHUB_REPOSITORY}"
fi

# Directs the action to the the Github workspace.
cd $GITHUB_WORKSPACE && \

# Configures Git.
git init && \
git config --global user.email "${COMMIT_EMAIL}" && \
git config --global user.name "${COMMIT_NAME}" && \
git config --global http.version HTTP/1.1 && \

git config --global http.version HTTP/1.1
git config --global http.postBuffer 157286400

## Initializes the repository path using the access token.
REPOSITORY_PATH="https://${GH_TOKEN}@github.com/${TARGET_REPOSITORY}.git" && \

# Checks to see if the remote exists prior to deploying.
# If the branch doesn't exist it gets created here as an orphan.
if [ "$(git ls-remote --heads "$REPOSITORY_PATH" "$BRANCH" | wc -l)" -eq 0 ];
then
  echo "Creating remote branch ${BRANCH} as it doesn't exist..."
  mkdir $DOC_FOLDER && \
  cd $DOC_FOLDER && \
  git init && \
  git checkout -b $BRANCH && \
  git remote add origin $REPOSITORY_PATH && \
  touch README.md && \
  git add README.md && \
  git commit -m "Initial ${BRANCH} commit" && \
  git push $REPOSITORY_PATH $BRANCH
else
  ## Clone the target repository
  git clone "$REPOSITORY_PATH" $DOC_FOLDER --branch $BRANCH --single-branch && \
  cd $DOC_FOLDER
fi

# Builds the project if a build script is provided.
echo "Running build scripts... $BUILD_SCRIPT" && \
eval "$BUILD_SCRIPT" && \

if [ -n "$CNAME" ]; then
  echo "Generating a CNAME file in in the $PWD directory..."
  echo $CNAME > CNAME
  git add CNAME
fi

# Update gh-pages root index page
if [ -f "../$FOLDER/ghpages.html" ]; then
  cp "../$FOLDER/ghpages.html" index.html
  git add index.html
fi

# Commits the data to Github.
if [ -z "$VERSION" ]
then
  if [ -z "$SKIP_SNAPSHOT" ] || [ "$SKIP_SNAPSHOT" == "false" ]; then
    echo "No Version. Publishing Snapshot of Docs"
    if [ -n "${DOC_SUB_FOLDER}" ]; then
      mkdir -p snapshot/$DOC_SUB_FOLDER
      cp -r "../$FOLDER/." ./snapshot/$DOC_SUB_FOLDER/
      git add snapshot/$DOC_SUB_FOLDER/*
    else
      mkdir -p snapshot
      cp -r "../$FOLDER/." ./snapshot/
      git add snapshot/*
    fi
  fi
else 
    if [ -z "$SKIP_LATEST" ] || [ "$SKIP_LATEST" == "false" ]
    then 
      echo "Publishing Latest Docs"
      if [ -n "${DOC_SUB_FOLDER}" ]; then
        mkdir -p latest/$DOC_SUB_FOLDER
        cp -r "../$FOLDER/." ./latest/$DOC_SUB_FOLDER/
        git add latest/$DOC_SUB_FOLDER/*
      else
        mkdir -p latest
        cp -r "../$FOLDER/." ./latest/
        git add latest/*
      fi
    fi   

    echo "Publishing $VERSION of Docs"
    majorVersion=${VERSION:0:4}
    majorVersion="${majorVersion}x"

    if [ -n "${DOC_SUB_FOLDER}" ]; then
      mkdir -p "$VERSION/$DOC_SUB_FOLDER"
      cp -r "../$FOLDER/." "./$VERSION/$DOC_SUB_FOLDER"
      git add "$VERSION/$DOC_SUB_FOLDER/*"
    else
      mkdir -p "$VERSION"
      cp -r "../$FOLDER/." "./$VERSION/"
      git add "$VERSION/*"
    fi

    if [ -n "${DOC_SUB_FOLDER}" ]; then
      mkdir -p "$majorVersion/$DOC_SUB_FOLDER"
      cp -r "../$FOLDER/." "./$majorVersion/$DOC_SUB_FOLDER"
      git add "$majorVersion/$DOC_SUB_FOLDER/*"
    else
      mkdir -p "$majorVersion"
      cp -r "../$FOLDER/." "./$majorVersion/"
      git add "$majorVersion/*"
    fi
fi


git commit -m "Deploying to ${BRANCH} - $(date +"%T")" --quiet && \
git push "https://$GITHUB_ACTOR:$GH_TOKEN@github.com/$TARGET_REPOSITORY.git" gh-pages || true && \
echo "Deployment successful!"
