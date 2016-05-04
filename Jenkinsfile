node {

  stage 'Build'
  sh '''
    if [ ! -d vizard/ ]; then
      git clone --depth 1 https://github.com/yieldbot/vizard
    fi
    cd vizard/
    git pull
    lein test
    lein jar
  '''

  stage 'Publish'
  slsSetPackageVars('clojure', 'vizard/project.clj')
  sh '''
    # not implemented yet
    echo "jfrog rt u $PACKAGE_NAME/target/$PACKAGE_NAME-$PACKAGE_VERSION.jar yieldbot-clojure/$PACKAGE_NAME/$PACKAGE_VERSION/ --url=https://artifactory.yb0t.cc/artifactory --dry-run"
  '''

  stage 'Deploy'
  sh '''
    # not implemented yet
  '''

}