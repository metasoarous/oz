#!/usr/bin/env bash

# Sane error handling settings (stop running for most errors)
set -euf -o pipefail



WILL_EXIT=""
if ! generate-extern -h > /dev/null
then
  echo "You need to have generate-extern installed in order to run this script"
  echo "If you'd like to install it, please try first running"
  echo "  npm install externs-generator"
  WILL_EXIT="yes"
fi

if ! boot -h > /dev/null
then
  echo "You need to have boot installed in order to run this script"
  echo "If you'd like to install it, please try first running"
  echo '  sudo bash -c "cd /usr/local/bin && curl -fsSLo boot https://github.com/boot-clj/boot-bin/releases/download/latest/boot.sh && chmod 755 boot"'
  WILL_EXIT="yes"
fi


if [ ! -z "$WILL_EXIT" ]
then
  exit
fi


if echo $@ | grep "\-\-help" > /dev/null
then
  echo "Run like"
  echo "   ./bin/update-cljsjs.sh"
  echo ""
  echo "AVAILABLE OPTIONS:"
  echo "  --help"
  echo "  --skip-pull"
  echo ""
  echo "AVAILABLE ENV VARIABLES:"
  echo "  CLJSJS_PACKAGES_PATH: Where to store cljsjs/packages checkout"
  echo "  CLJSJS_PACKAGES_FORK: Set if you have a github fork"
  exit
fi


# Environment variables

# For latest releases, see:
# https://github.com/vega/vega/releases
v_version="5.9.0"
v_build_version="0"

# https://github.com/vega/vega-lite/releases
vl_version="4.0.2"
vl_build_version="0"

# https://github.com/vega/vega-embed/releases
ve_version="6.0.0"
ve_build_version="0"

# https://github.com/vega/vega-tooltip/releases
vt_version="0.20.0"
vt_build_version="0"

# https://github.com/nyurik/vega-spec-injector
vsi_version="0.0.2"
vsi_build_version="0"

## https://github.com/nyurik/leaflet-vega
#lv_version="0.8.6"
#lv_build_version="0"


# store current directory
oz_dir=$(readlink -f .)

# Set packages path
CLJSJS_PACKAGES_PATH="$oz_dir/cljsjs-packages"
echo "Using cljsjs/packages path $CLJSJS_PACKAGES_PATH"


# If the directory doesn't exist, clone
if [ ! -d $CLJSJS_PACKAGES_PATH ]
then
  git clone git@github.com:cljsjs/packages cljsjs-packages
fi


# Change directories to our pacakges path
cd $CLJSJS_PACKAGES_PATH

# Make sure we're up to date, just in case
# Is there a reason we wouldn't always want to do this?
# Yes... sometimes you don't want to because you have local changes that haven't been merged into origin
# mastered yet.
# We should probably have options for specifying what remote/branch (or local) you want to base on.


if echo $@ | grep "\-\-local-changes" > /dev/null
then
  echo "Skipping pull"
  uncommitted_changes=$(git diff)
  if [[ ! -z $uncommitted_changes ]]; then
    echo "Stashing changes"
    git stash
  fi
  echo "Checking out master"
  git checkout master
  echo "Fetching origin/master"
  git fetch
  echo "Merging origin/master"
  git merge origin/master
  if [[ ! -z $uncommitted_changes ]]; then
    echo "Applying stash"
    git stash apply

    # Actually, I'm not sure that we want to do this; We probably want these changes to show up in each of the
    # projects on a 1 by 1 basis.
    #echo "Adding togit"
    #git add *
    #git status
    #echo "There are changes in the working directory of cljsjs-packages which need to be commited."
    #echo "Please enter a commit message:"
    #read commit_message
    #git commit -m "$commit_message"
  fi
else
  echo "Pulling latest master"
  git checkout master
  git pull origin
fi



# Vega dists env variable
VEGA_DISTS="$oz_dir/cljsjs-vega-dists"
mkdir -p $VEGA_DISTS
cd $VEGA_DISTS



# Building externs for the libs and updating bootfiles!
# =====================================================



## Vega
## ----

v_asset=vega-$v_version.js
v_min_asset=vega-$v_version.min.js
wget https://unpkg.com/vega@$v_version/build/vega.js -O $v_asset
# TODO Hmm... not actually using the minified version right now; should look into this!
wget https://unpkg.com/vega@$v_version/build/vega.min.js -O $v_min_asset

# compute new checksums
v_checksum=$(md5sum $v_asset | grep -o "^[a-z0-9]*")
v_min_checksum=$(md5sum $v_min_asset | grep -o "^[a-z0-9]*")
echo v_checksum $v_checksum
echo v_min_checksum $v_min_checksum

# generate extens
extfile=vega-$v_version.ext.js
generate-extern -f $v_asset -n vega -o $extfile
cp $extfile $CLJSJS_PACKAGES_PATH/vega/resources/cljsjs/vega/common/vega.ext.js

# update lib versions in build.boot
cd $CLJSJS_PACKAGES_PATH/vega
sed -i "s/def +lib-version+ \"[0-9a-z\.\-]*\"/def +lib-version+ \"$v_version\"/" build.boot
sed -i "s/str +lib-version+ \"[0-9a-z\.\-]*\"/str +lib-version+ \"-$v_build_version\"/" build.boot

# update checksums in build.boot
old_v_checksum=$(grep -m 1 ":checksum" build.boot | grep -o "\"[a-zA-Z0-9]*\"")
old_v_min_checksum=$(grep -m 2 ":checksum" build.boot | tail -n 1 | grep -o "\"[a-zA-Z0-9]*\"")
# update checksums
sed -i "s/$old_v_checksum/\"$v_checksum\"/" build.boot
sed -i "s/$old_v_min_checksum/\"$v_min_checksum\"/" build.boot

# try installing
boot package install target


# Commit to a dedicated branch
git checkout -B vega-updates
git add .
VEGA_UPDATES=$(git diff --cached)
if [[ ! -z $VEGA_UPDATES ]]
then
  git commit -m "[vega] update vega version to $v_version-$v_build_version"
fi

cd $VEGA_DISTS

echo "DONE building vega"




## Vega-Lite
## ---------

# asset filenames
vl_asset=vega-lite-$vl_version.js
vl_min_asset=vega-lite-$vl_version.min.js

wget https://unpkg.com/vega-lite@$vl_version/build/vega-lite.js -O $vl_asset
wget https://unpkg.com/vega-lite@$vl_version/build/vega-lite.min.js -O $vl_min_asset

vl_checksum=$(md5sum $vl_asset | grep -o "^[a-z0-9]*")
vl_min_checksum=$(md5sum $vl_min_asset | grep -o "^[a-z0-9]*")
echo vega-lite checksum $vl_checksum
echo vega-lite min checksum $vl_min_checksum

echo Generating vega-lite externs
# unzip and generate externs
extfile=vega-lite.$vl_version.ext.js
echo generate-extern -f $vl_asset -n vl -o $extfile
generate-extern -f $vl_asset -n vegaLite -o $extfile
cp $extfile $CLJSJS_PACKAGES_PATH/vega-lite/resources/cljsjs/vega-lite/common/vega-lite.ext.js
echo Done generating vega-lite externs

# update lib versions and checksums in build.boot, and try installing
cd $CLJSJS_PACKAGES_PATH/vega-lite
sed -i "s/def +lib-version+ \"[0-9a-zA-Z\.\-]*\"/def +lib-version+ \"$vl_version\"/" build.boot
sed -i "s/str +lib-version+ \"[0-9a-zA-Z\.\-]*\"/str +lib-version+ \"-$vl_build_version\"/" build.boot

old_vl_checksum=$(grep -m 1 ":checksum" build.boot | grep -o "\"[a-zA-Z0-9]*\"")
old_vl_min_checksum=$(grep -m 2 ":checksum" build.boot | tail -n 1 | grep -o "\"[a-zA-Z0-9]*\"")

echo "old_vl_checksum:" $old_vl_checksum
echo "old_vl_min_checksum:" $old_vl_min_checksum

# update checksums
sed -i "s/$old_vl_checksum/\"$vl_checksum\"/" build.boot
sed -i "s/$old_vl_min_checksum/\"$vl_min_checksum\"/" build.boot

echo "done sedding on checksums"

# update dependencies
echo sed -i "s/\[cljsjs\/vega \"[0-9a-zA-Z\.\-]*\"\]/[cljsjs\/vega \"$v_version-$v_build_version\"]/" build.boot 
sed -i "s/\[cljsjs\/vega \"[0-9a-zA-Z\.\-]*\"\]/[cljsjs\/vega \"$v_version-$v_build_version\"]/" build.boot 

echo "done sedding on dependency versions"

boot package install target

# Commit to a dedicated branch
git checkout -B vega-lite-updates
git add .
VEGA_LITE_UPDATES=$(git diff --cached)
if [[ ! -z $VEGA_LITE_UPDATES ]]
then
  echo "Committing VEGA LITE"
  git commit -m "[vega-lite] update vega-lite version to $vl_version-$vl_build_version" \
             -m "Full version set: vega -> $v_version-$v_build_version, vega-lite -> $vl_version-$vl_build_version."
fi

# go back
cd $VEGA_DISTS

echo "DONE building vega-lite"



## Vega-embed
## ----------

# get assets
asset=vega-embed-$ve_version.js
min_asset=vega-embed-$ve_version.min.js
wget https://unpkg.com/vega-embed@$ve_version/build/vega-embed.js -O $asset
wget https://unpkg.com/vega-embed@$ve_version/build/vega-embed.min.js -O $min_asset

# compute new checksums
ve_checksum=$(md5sum $asset | grep -o "^[a-z0-9]*")
ve_min_checksum=$(md5sum $min_asset | grep -o "^[a-z0-9]*")
echo ve_checksum $ve_checksum
echo ve_min_checksum $ve_min_checksum

# generate and install externs
extfile=vega-embed.$ve_version.ext.js
# note that this call to generate-extern needs to have all three libs loaded to work
generate-extern -f $v_asset,$vl_asset,$asset -n vegaEmbed -o $extfile
cp $extfile $CLJSJS_PACKAGES_PATH/vega-embed/resources/cljsjs/vega-embed/common/vega-embed.ext.js

# update lib versions and checksums in build.boot, and try installing
cd $CLJSJS_PACKAGES_PATH/vega-embed
sed -i "s/def +lib-version+ \"[0-9a-z\.\-]*\"/def +lib-version+ \"$ve_version\"/" build.boot
sed -i "s/str +lib-version+ \"[0-9a-z\.\-]*\"/str +lib-version+ \"-$ve_build_version\"/" build.boot
# note that this assumes the minified version comes second in the build process
old_ve_checksum=$(grep -m 1 ":checksum" build.boot | grep -o "\"[a-zA-Z0-9]*\"")
old_ve_min_checksum=$(grep -m 2 ":checksum" build.boot | tail -n 1 | grep -o "\"[a-zA-Z0-9]*\"")
# update checksums
sed -i "s/$old_ve_checksum/\"$ve_checksum\"/" build.boot
sed -i "s/$old_ve_min_checksum/\"$ve_min_checksum\"/" build.boot
# update dependencies
sed -i "s/\[cljsjs\/vega \"[0-9a-zA-Z\.\-]*\"\]/[cljsjs\/vega \"$v_version-$v_build_version\"]/" build.boot 
sed -i "s/\[cljsjs\/vega-lite \"[0-9a-zA-Z\.\-]*\"\]/[cljsjs\/vega-lite \"$vl_version-$vl_build_version\"]/" build.boot 
boot package install target
cd $VEGA_DISTS




## Vega tooltip
## ------------

# get assets
asset=vega-tooltip-$vt_version.js
min_asset=vega-tooltip-$vt_version.min.js
wget https://unpkg.com/vega-tooltip@$vt_version/build/vega-tooltip.js -O $asset
wget https://unpkg.com/vega-tooltip@$vt_version/build/vega-tooltip.min.js -O $min_asset

# compute checksums
vt_checksum=$(md5sum $asset | grep -o "^[a-z0-9]*")
vt_min_checksum=$(md5sum $min_asset | grep -o "^[a-z0-9]*")
echo vt_checksum $ve_checksum
echo vt_min_checksum $ve_min_checksum

# generate and install externs
extfile=vega-tooltip.$vt_version.ext.js
generate-extern -f $asset -n vegaTooltip -o $extfile
cp $extfile $CLJSJS_PACKAGES_PATH/vega-tooltip/resources/cljsjs/vega-tooltip/common/vega-tooltip.ext.js

# update lib versions and checksums in build.boot, and try installing
cd $CLJSJS_PACKAGES_PATH/vega-tooltip
sed -i "s/def +lib-version+ \"[0-9a-z\.\-]*\"/def +lib-version+ \"$vt_version\"/" build.boot
sed -i "s/str +lib-version+ \"[0-9a-z\.\-]*\"/str +lib-version+ \"-$vt_build_version\"/" build.boot
# note that this assumes the minified version comes second in the build process
old_vt_checksum=$(grep -m 1 ":checksum" build.boot | grep -o "\"[a-zA-Z0-9]*\"")
old_vt_min_checksum=$(grep -m 2 ":checksum" build.boot | tail -n 1 | grep -o "\"[a-zA-Z0-9]*\"")
# update checksums
sed -i "s/$old_vt_checksum/\"$vt_checksum\"/" build.boot
sed -i "s/$old_vt_min_checksum/\"$vt_min_checksum\"/" build.boot
# update dependencies
sed -i "s/\[cljsjs\/vega \"[0-9a-zA-Z\.\-]*\"\]/[cljsjs\/vega \"$v_version-$v_build_version\"]/" build.boot 
sed -i "s/\[cljsjs\/vega-lite \"[0-9a-zA-Z\.\-]*\"\]/[cljsjs\/vega-lite \"$vl_version-$vl_build_version\"]/" build.boot 
boot package install target



# Commit vega-tooltip and vega-embed changes together as vega-extras
git checkout -B vega-extras-updates
git add .
git add ../vega-embed
VEGA_EXTRAS_UPDATES=$(git diff --cached)
if [[ ! -z $VEGA_EXTRAS_UPDATES ]]
then
  echo "Committing VEGA EXTRAS"
  git commit -m "[vega-extras] update vega-embed & vega-tooltip versions" \
             -m "Full version set: vega -> $v_version-$v_build_version, vega-lite -> $vl_version-$vl_build_version, vega-embed -> $ve_version-$ve_build_version, vega-tooltip -> $vt_version-$vt_build_version."
fi

cd $VEGA_DISTS





# Finishing up
# ============

#echo "premature exit"
#exit

cd $CLJSJS_PACKAGES_PATH

# If we have this fork env variable, we push to the fork
if [[ ! -z ${CLJSJS_PACKAGES_FORK+x} ]]
then
  echo "Found CLJSJS_PACKAGES_PATH env variable; force pushing to branches: vega-updates, vega-lite-updates, and vega-extras-updates."
  git push -f $CLJSJS_PACKAGES_FORK vega-updates:vega-updates
  git push -f $CLJSJS_PACKAGES_FORK vega-lite-updates:vega-lite-updates
  git push -f $CLJSJS_PACKAGES_FORK vega-extras-updates:vega-extras-updates
fi


# Let user know what they need to do next

echo ""
echo "  |============================|"
echo "  ||  IMPORTANT!!! README!!!  ||"
echo "  |============================|"
echo ""
echo "  Update almost complete!"
echo "  The cljsjs/packages repo at $CLJSJS_PACKAGES_PATH now has the following update branches: vega-updates, vega-lite-updates and vega-extras-updates."

if [[ ! -z ${CLJSJS_PACKAGES_FORK+x} ]]
then
  echo "  These branches have now been pushed to your fork at: $CLJSJS_PACKAGES_FORK"
  echo ""

  # Adapted from https://serverfault.com/questions/417241/extract-repository-name-from-github-url-in-bash
  github_re="^(https|git)(:\/\/|@)github.com[\/:]([^\/:]+)\/(.+).git$"

  if [[ $CLJSJS_PACKAGES_FORK =~ $github_re ]]; then    
    protocol=${BASH_REMATCH[1]}
    separator=${BASH_REMATCH[2]}
    user=${BASH_REMATCH[3]}
    repo=${BASH_REMATCH[4]}
    echo "  You can create PRs for these at:"
    echo "    https://github.com/$user/$repo/pull/new/vega-updates"
    echo "    https://github.com/$user/$repo/pull/new/vega-lite-updates"
    echo "    https://github.com/$user/$repo/pull/new/vega-extras-updates"
    exit
  fi
else
  echo ""
  echo "  If you rerun with the CLJSJS_PACKAGES_FORK environment variable set to your fork of cljsjs/packages, this script will automatically push these branches there upon completion."
  echo "  If you choose not to do this you will have to manually push these changes like."
  echo ""
  echo "    git push -f yourfork vega-updates:vega-updates"
fi

echo ""
echo "  The final step is to review the changes to make sure they look sane, then to create pull requests from each of these branches to https://github.com/cljsjs/packages."
echo ""


