{fetchurl, gradleGen, jdk, java ? jdk}:

(gradleGen.override { java = java; }).gradleGen rec {
  name = "gradle-6.3";
  nativeVersion = "0.22-milestone-1";

  src = fetchurl {
    url = "https://services.gradle.org/distributions/${name}-bin.zip";
    sha256 = "0s0ppngixkkaz1h4nqzwycjcilbrc9rbc1vi6k34aiqzxzz991q3";
  };
}
