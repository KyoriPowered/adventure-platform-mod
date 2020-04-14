{pkgs ? import <nixpkgs> {}, jdk ? pkgs.jdk12 }:
let 
  gradle6 = pkgs.callPackage gradle/gradle.nix { java = jdk; };
in
pkgs.stdenv.mkDerivation {
  name = "configurate";
  buildInputs = with pkgs; [ jdk gradle6 git bash ];
}
