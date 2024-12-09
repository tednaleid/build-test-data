Release Management
===

Currently this project publishes snapshots to the GitHub packages repository and releases to Maven Central.  Change the `snapshotPublishType` on `grailsPublish` extension to switch to a nexus publishing.

Required `Action` Secrets
---
Under the GitHub project's `Settings` -> `Secrets and variables` -> `Actions`, the following `Repository secrets` should exist:
* For snapshots:
  * `GH_TOKEN` - a valid GitHub Personal Access Token that can commit the version # to gradle.properties on releases.
  * `MAVEN_PUBLISH_SNAPSHOT_URL` - the GitHub package location.  See the GitHub [help documentation](https://docs.github.com/en/actions/use-cases-and-examples/publishing-packages/publishing-java-packages-with-gradle#publishing-packages-to-github-packages) for this value.
* For releases:
  * `SONATYPE_USERNAME` - The sonatype username that can access the `SONATYPE_NEXUS_URL`.
  * `SONATYPE_PASSWORD` - The sonatype password that can access the `SONATYPE_NEXUS_URL`.
  * `SONATYPE_NEXUS_URL` - The release url for Maven Central, typically `https://s01.oss.sonatype.org/service/local/`
  * `SONATYPE_STAGING_PROFILE_ID` - The Nexus Staging Profile ID
  * `SIGNING_KEY` - The public key ID.
  * `SIGNING_PASSPHRASE` - The passphrase used while generating GnuPG key.
  * `SECRING_FILE` - The `secring.gpg` file contents for publishing to Maven Central.

See this [Grails Blog Post](https://grails.org/blog/2021-04-07-publish-grails-plugin-to-maven-central.html) for help setting up this information.

Version Numbering
---
Releases are tracked based on a major project version with new branches created on each new major release.

Pull Requests
---
Pull requests will only run tests with no publishing of documentation or builds.

Push
---
Pushes to a major branch (X.X.x) will:
* Perform Tests
* Publish a snapshot build (currently to GitHub)
* Generate documentation
* Publish documentation to the snapshot location if it's on the latest branch.

Releases
---
To perform a release:
* Draft a release announcement on GitHub.  
  * `Choose a tag` - enter `v` + the desired project version & select the `Create new tag: ` option.  i.e. `v6.0.0` 
  * `Target` - choose the major release branch.
  * The `Release title` should be the major version without the `v`
  * Add a description for the release.
  * Select `Publish Release`
* On publish of a new release, the `Release` action will kick off.  It will do the following:
  * (#1) Publish Job:
    * Perform `pre-release` steps:
      * Changes `gradle.properties` projectVersion based on the release.
      * Commits the change using the configured `GH_TOKEN`
    * Builds the project
    * Publishes to the Staging repository and closes the staging repository.
  * (#2) Release Job:
    * Releases the staging repository & closes it so the artifact is available on Maven Central.
    * Perform `post-release` steps:
      * Closes any open milestones associated to the major release
      * Creates the next milestone
      * Modifies `gradle.properties` to go back to the next snapshot version.
      * Commits the `gradle.properties` changes using the configured `GH_TOKEN`
  * (#3) Documentation:
    * Generates the documentation
    * Publishes the documentation
