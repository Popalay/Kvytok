workflow "Build and deploy on push" {
  on = "push"
  resolves = ["Build"]
}

action "Build" {
  uses = "vgaidarji/android-github-actions/build@v1.0.0"
  args = "./gradlew assembleRelease"
}
