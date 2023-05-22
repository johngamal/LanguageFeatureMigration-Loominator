# Loominator - An automatic migration tool to project Loom

This project is done as a part of our research thesis project on language feature migration under the supervision of Dr. Sarah Nadi, Dr. Karim Ali, Dr. Sherif Ali in collaboration with IBM Canada.

The team: 
- John Attia
- Yassin Abdelkarim
- Joseph Boulis
- Jacquline Azar
- Sherif Elsamra

## Brief

Loominator is an automatic migration tool that migrates already existing Java codebases to use Project Loom. It uses Javaparser library to parse Java code into Abstract Syntax Trees on which all the edits are applied.


## How to run
We recommend using Intilij to handle the automatic building and running of this project.

To build the project manually from a terminal run:

`mvn clean install`

To run the build:

`java -cp target/Loominator-1.0-SNAPSHOT-shaded.jar com.langFeautreMigration.projectCode.main`

The project asks for the directory on which it will be working and the mode. It will recursively traverse all files under the given directory, migrating all files with the extension ".java". The project currently supports two modes:
 - All: it applies edits automatically to the entire directory without asking the user for any input.
 - File By File: it applies edits to every file and then asks the user to confirm the edits and whether the user would like to keep it or not. 
