[![Build Status](https://travis-ci.org/wpilibsuite/java-installer.svg?branch=master)](https://travis-ci.org/wpilibsuite/java-installer)
# Java Installer Project

Welcome to the WPILib project. This repository contains the Java Installer project, which downloads the embedded JRE creator, creates the JRE for the roboRIO, and installs it on the roboRIO.

## WPILib Mission

The WPILib Mission is to enable FIRST teams to focus on writing game-specific software rather than on hardware details - "raise the floor, don't lower the ceiling". We try to enable teams with limited programming knowledge and/or mentor experience to do as much as possible, while not hampering the abilities of teams with more advanced programming capabilities. We support Kit of Parts control system components directly in the library. We also strive to keep parity between major features of each language (Java, C++, and NI's LabVIEW), so that teams aren't at a disadvantage for choosing a specific programming language. WPILib is an open-source project, licensed under the BSD 3-clause license. You can find a copy of the license [here](BSD_License_for_WPILib_code.txt).

# Building the Java Installer

The Java Installer is built with Gradle. There is nothing to install; simply run the appropriate `gradlew` script for your system. Common tasks are:

- check - This is what is run to verify commits. This will build the program.
- run - Runs the Java Installer program.
- publish - Publishes the Java Installer maven artifact to the local WPILib repository.
- clean - Removes the build artifacts. This is does not remove the local WPILib repository.

## Requirements

- JDK 8u40 or greater. You must also have the approprate JFX version for your JDK.

## Publishing
If you want to create a set of Eclipse plugins with your customized Java Installer, you must publish the Java Installer artifact to your local FRC Maven repository. To do this, simply run the `publish` Gradle task. This will publish the Java Installer to `~/releases/maven/development`. The resulting artifact is `edu.wpi.first.wpilib:java-installer:version`.

# Contributing to WPILib
See [CONTRIBUTING.md](CONTRIBUTING.md)