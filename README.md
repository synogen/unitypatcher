# [unitypatcher](https://github.com/synogen/unitypatcher)
A quick and dirty command line exporter/importer and patcher for **text assets** in Unity *.assets files.

## Run

unitypatcher requires [Java Runtime Environment 8](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) or higher to run.

## Usage

Command line overview:

`unitypatcher export <assets-file> <path ID/asset name/*>`

`unitypatcher import <assets-file> <path ID/asset name> <file to import>`

`unitypatcher patch <assets-file> <patch file> <more patch files(optional)...>`

Examples: 

`unitypatcher export sharedassets3.assets 530`

`unitypatcher patch sharedassets2.assets ChangeThis.assetmod ChangeThat.assetmod`


Run `unitypatcher` without arguments to get help.

## Patch file format

First line: The text asset name
Second line: The regular expression to search for in the text asset (you can test your regular expressions at [Regex101](https://regex101.com/) )
Third line and after: The text found using the regular expression will be replaced by this, group matchers ($1) are supported

Example:
`Constants
(<td div="Member">s_maxJobs</td>\s+<td div="Value")>\d+<(/td>)
$1>6<$2`


## Download

Check the [releases](https://github.com/synogen/unitypatcher/releases)
 
