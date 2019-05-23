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
  exit
fi


# Environment variables

# For latest releases, see:
# https://github.com/vega/vega/releases
v_version="5.3.4"
v_build_version="0"

# https://github.com/vega/vega-lite/releases
vl_version="3.1.0"
vl_build_version="0"

# https://github.com/vega/vega-embed/releases
ve_version="4.0.0"
ve_build_version="0"

# https://github.com/vega/vega-tooltip/releases
vt_version="0.17.0"
vt_build_version="0"


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
git checkout master
git pull origin master



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
git commit -m "[vega] update vega version to $v_version-$v_build_version"

cd $VEGA_DISTS




## Vega-Lite
## ---------

zipfile=vega-lite-v$vl_version.zip
# first clean up any old zips of the same version, to avoid naming conflicts
rm -f $zipfile

# now actually download the file and compute checksums
wget https://github.com/vega/vega-lite/archive/v$vl_version.zip -O $zipfile
vl_checksum=$(md5sum $zipfile | grep -o "^[a-z0-9]*")
echo vega-lite checksum $vl_checksum

# unzip and generate externs
unzip -uo $zipfile
extfile=vega-lite.$vl_version.ext.js
generate-extern -f vega-lite-$vl_version/build/vega-lite.js -n vl -o $extfile
cp $extfile $CLJSJS_PACKAGES_PATH/vega-lite/resources/cljsjs/vega-lite/common/vega-lite.ext.js

# update lib versions and checksums in build.boot, and try installing
cd $CLJSJS_PACKAGES_PATH/vega-lite
sed -i "s/def +lib-version+ \"[0-9a-zA-Z\.\-]*\"/def +lib-version+ \"$vl_version\"/" build.boot
sed -i "s/str +lib-version+ \"[0-9a-zA-Z\.\-]*\"/str +lib-version+ \"-$vl_build_version\"/" build.boot
sed -i "s/:checksum \"[0-9a-zA-Z]*\"/:checksum \"$vl_checksum\"/" build.boot
# update dependencies
sed -i "s/\[cljsjs\/vega \"[0-9a-zA-Z\.\-]*\"\]/[cljsjs\/vega \"$v_version-$v_build_version\"]/" build.boot 
boot package install target

# Commit to a dedicated branch
git checkout -B vega-lite-updates
git add .
git commit -m "[vega-lite] update vega-lite version to $vl_version-$vl_build_version" \
           -m "Full version set: vega -> $v_version-$v_build_version, vega-lite -> $vl_version-$vl_build_version."

# go back
cd $VEGA_DISTS




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
generate-extern -f $v_asset,vega-lite-$vl_version/build/vega-lite.js,$asset -n vegaEmbed -o $extfile
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
git commit -m "[vega-extras] update vega-embed & vega-tooltip versions" \
           -m "Full version set: vega -> $v_version-$v_build_version, vega-lite -> $vl_version-$vl_build_version, vega-embed -> $ve_version-$ve_build_version, vega-tooltip -> $vt_version-$vt_build_version."



# Finishing up
# ============

#echo "premature exit"
#exit

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



