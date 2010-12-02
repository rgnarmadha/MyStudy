This directory contains some scripts

`upload.py` uploads diffs to http://codereview.appspot.com for review

`clean_restart.sh` rebuilds a list of bundles specified on the command line, rebuilds the application, cleans the previous start and restarts the app server. if the list of bundles starts with 'all' it will do a full build.	

`rebundle.sh` rebundles a list of bundles, rebuilding the app

`runalltests.sh` will search for all `testall.sh` scripts and run them.


Doing a Release
===============
`do_release.sh` does most of the heavy lifting. We start the release process with a release candidate, and once a release candidate has been circulated, vetted, and approved, we can cut the tag for the full release. When we create either a release candidate or a full release, we first switch over from SNAPSHOT versions of our Apache dependencies to a frozen-in-time version which is just the snapshot with today's date as the version number.

Before we run `do_release.sh` we use `deploy-snapshots.sh` to convert all of our Apache SNAPSHOTs with a version number. The convention we have been following is to use today's date in YYYYmmdd format as the version number to pass to `deploy-snapshots.sh`. We can use the `date` command to generate the number when we run the script:

    ./tools/deploy-snapshots.sh `date +"%Y%m%d"`
    
The convention is to run all the release scripts from nakamura's base directory. When `deploy-snapshots.sh` is finished, you'll have a tarball of the modified dependencies in a file called `repo.tgz`. When you run the `do_release.sh` script, it will clear out the nakamura and sling portions of your local repository and load them up from the tarball. When the release script completes successfully, you will want to deploy these artifacts to Sakai's public maven repository at http://source.sakaiproject.org/maven2/
    
The next step is to change any of the references to SNAPSHOT dependencies in any of our `pom.xml` or `list.xml` files from "SNAPSHOT" to the version number we just generated. There isn't yet a script to do this part.

Once the version numbers are taken care of, use git to remove `last-release/stage1` and `last-release/stage2`. `stage1` tells the release script that the build has completed succssfully, and `stage2` tells the release script that the integration tests have passed successfully. Do one last commit to the repository and make sure you don't have anything running on port 8080. A note about tools: running `do_release.sh` will use a number of standard Unix commands, but it will also run the integration tests, for which you'll need Ruby and several optional libraries (rubygems, json, and curb). You'll need `git` for the automatic tagging and committing, and you'll need [gpg](http://www.gnupg.org/), with a private key installed, for git's tag-signing feature.

When we run `do_release.sh`, we pass it the current nakamura version number, the next version number and (optionally) an RC suffix if this is going to be a release candidate:

    ./tools/do_release.sh 0.7 0.8 RC1
    
If something goes wrong, like a build failure, use `git checkout .` to revert the changes that the release script has just made. Fix whatever broke the build (it's probably going to be something missing from your maven repo), remove `last-release/stage1` or `last-release/stage2` if they're there, and run it again. If everything goes well, you'll have a new, signed tag in git.

Before pushing to any remotes, you will want to go through the `pom.xml` and `list.xml` files that you modified with today's date and restore them to "SNAPSHOT" versions. This will ensure that the mainline will continue to use SNAPSHOTs and will not experience any disruption.

If you want to push a tag to a remote server, you need to add a flag to git's push command:

    git push --tags sakai master
