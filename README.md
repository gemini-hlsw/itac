# ITAC Command-Line Interface (2021A)

This is the repository for the Gemini Observatory ITAC application.

## Upgrading from 2020B

If you already have a 2020B workspace set up, you can do `cs update itac` to get a new version, then skip down to **Using ITAC** below.

**Please read** the new documentation because there have been some changes.

## Installing Itac

### 1. Overview

Installing ITAC is straightforward but you may require a bit of assistance if you're not comfortable using the command line. Anyone from ITS or the software group can help you if you get stuck.

Open a command shell (Terminal.app on the Mac) and continue with Step 2.

### 2. Install Coursier

First install the Coursier native launcher. This will let you set up ITAC as well as the Java Virtual Machine, if necessary. Follow the instructions [here](https://get-coursier.io/docs/cli-overview.html#install-native-launcher).

Depending on how you installed it, you may need to move `cs` into a directory that's on your executable path, such as `/usr/local/bin/`.

On success you will be able to do:

```
$ cs --help
Coursier 2.0.0-RC6-10
Usage: cs [options] [command] [command-options]

Available commands: bootstrap, complete, fetch, install, java, java-home, launch, publish, resolve, setup, uninstall, update

Type  cs command --help  for help on an individual command
```

### 3. Install ITAC

Use `cs setup` to set up Java, configure your path, and install ITAC.

```
cs setup --yes --channel edu.gemini:itac-channel --apps itac
```

Close and re-open your terminal window and `itac --help` should now run and print a help message.

### 4. Update ITAC

Once ITAC is installed you can update to the latest version thus:

```
cs update itac
```

And uninstall:

```
cs uninstall itac
```

## Using ITAC

ITAC is a command-line application that reads proposal XML files along with some configuration information stored in YAML files, and produces an observing queue. The main workflow is:

1. Initialize a workspace directory to contain all your input files.
1. Customize configuration files as necessary to specify partner times, shutdown blocks, target changes, and so on.
1. Place proposals in band folders according to desired queue placement.
1. Create queues and examine the output, tweaking configuration and applying proposal edits until you're satisfied.
1. Send emails and ingest the proposals into the ODB.

Keep in mind the following big ideas:

- All the input files are just normal text files. You can rename them, make backups, store them in source control, manage them however you like.
- All proposal "edits" are specified as part of configuration and occur as proposals are loaded from disk. *The XML files are never touched.* This means you can undo or change edits by updating the contents of the `edits/` folder.
- The input files completely specify the Queue. If you want to have several queues that you can compare, you can have several sets of input files.

Now let's examine these steps in more detail.

### Initialize and Configure an ITAC Workspace

> **Note:** this step requires a connection to the internal Gemini network, so if you're working off-site you need to have the VPN turned on.

Create a new directory, move into it, and initialize the ITAC workspace for next semester.

```
tmp$ mkdir itac-example
tmp$ cd itac-example/
itac-example$ itac init 2021A
[INFO ] Creating folder: removed
[INFO ] Creating folder: edits
[INFO ] Creating folder: band-1
[INFO ] Creating folder: band-2
[INFO ] Creating folder: band-3
[INFO ] Creating folder: band-4
[INFO ] Writing: ./common.yaml
[INFO ] Writing: ./gn-queue.yaml
[INFO ] Writing: ./gs-queue.yaml
[INFO ] Fetching current rollover report from Gemini North...
[INFO ] Got rollover information for 2020B
[INFO ] Writing: ./gn-rollovers.yaml
[INFO ] Fetching current rollover report from Gemini South...
[INFO ] Got rollover information for 2020B
[INFO ] Writing: ./gs-rollovers.yaml
[INFO ] init: initialized ITAC workspace in .
itac-example$
```

`itac` has created a bunch of files. Let's look at them.

```
itac-example$ tree -F --dirsfirst
.
├── band-1/
├── band-2/
├── band-3/
├── band-4/
├── edits/
├── removed/
├── common.yaml
├── gn-queue.yaml
├── gn-rollovers.yaml
├── gs-queue.yaml
└── gs-rollovers.yaml

6 directories, 5 files
itac-example$
```

- `band-<n>/` are where your proposal XML files (from Jared's system) need to go. Place each proposal in the folder associated with the desired band placement.
- `edits/` is where you define edits that are applied to the XML files as they are read. See **Editing Proposals** below for more information.
- `removed/` is a folder for proposals that have been removed from consideration for some reason. These will appear in reports and so on but will not be considered by the queue engine.
- `common.yaml` contains configuration that applies to all queues and is unlikely to change much. You will need to edit this file to specify shutdown times, and prior to finalizing the queue you will need to specify information for email generation.
- `<site>-queue.yaml` specify site-specific queue configurations. The most important part here is `hours` which specifies per-partner hours in bands 1, 2, and 3. You may need to add or remove partner lines.
- `<site>-rollovers.yaml` contain rollover reports fetched from the ODBs at GN and GS. You may wish to adjust the times here. To re-fetch a rollover report you can say `itac --force rollover --south` (or `--north`). Note that you must have an internal or VPN connection to do this.

#### Experimenting with Last Semester's Data

If you wish to try out the new software with last semester's data it is recommended that you copy/paste the relevant information from old configuration files into the new ones, because some sections in the old config files are no longer needed and may lead to confusion.

- All proposals must go into the appropriate band folders. The `extras/` and `extras_not_submitted/` folders are no longer used.
- `common.yaml` is compatible with the 2020B, but some entries are no longer used.
- `<site>-queue.yaml` files have a different format for band ovefills.
- `<site>-rollovers.yaml` files can be copied verbatim; the format has not changed.
- `edits/` from 2020B are *not* compatible with this version and cannot be used.
- `bulk_edits.xls` from 2020B can be used, but the ITAC/NTAC comments columns will be ignored. You can delete them to avoid confusion.

### Get Help

You can get detailed help about `itac` commands and options with the `--help` flag.

| Command | Action |
|--|--|
| `itac --help` | Show top-level help. |
| `itac <command> --help` | Show help for a specific command. For example, `itac queue --help` |

You can also ask for help on the `#itac` channel on the **Gemini Software** Slack (ask Arturo if you need access).

### Look at Proposals

The following commands display information about the proposals in your workspace's `proposals/` directory.

| Command | Action |
|--|--|
|`itac ls` | Show a list of all proposals in the workspace. |
|`itac summarize <ref>` | Display a summary of a proposal and its observations. For example, `itac summarize BR-2019B-071` |

### Construct a Queue

Assuming everything is configured reasonably, you should be able to generate a queue.

```
itac queue --south
```

### Editing Proposals

#### Editing Individual Proposals

`itac` allows you to define edits that are applied to proposals as the XML files are read. Here is the workflow.

- Use `itac summarize <ref>` to look at the proposal you wish to edit.
- Use `itac summarize --edit <ref>` to write a copy of the summary into the `edits/` folder, with the name `<ref>.yaml`.
- Edit the file in the `edits/` folder, making changes as instructed in the comment at the top of the file.
- Changes will be applied each time the proposal is loaded from disk. The original XML file is never touched. You can verify changes by running `itac summarize <ref>` again.
- To undo your edits, delete the edit file.

#### Bulk Edits

`itac` provides an Excel spreadsheet for bulk edits that are applied prior to exporting proposals to the ODB. These edits are not used during queue creation. If this file does not exist (or does not contain newly added proposals) you can create/update it explicitly.

```
itac-example$ itac bulk-edits
itac-example$ ls *.xls
bulk_edits.xls
```

### Generating Reports

The following commands are available for generating reports.

| Example Command | Output Type | Description |
|---|---|---|
| `itac blueprints --north` | Text | Blueprints by band. |
| `itac chart-data --north` | Text | Instrument time by RA. |
| `itac director-spreadsheet` | Excel | Director's report. |
| `itac duplicates --north` | Text | Target duplication report. |
| `itac instrument-spreadsheets` | Excel | Instrument report. |
| `itac ngo-spreadsheets` | Excel | NGO queue reports (one per partner). |
| `itac scheduling --north` | Text | Report of prooposals with scheduling notes. |
| `itac splits` | Text | Report of split (joint) proposals. |
| `itac staff-email-spreadsheet` | Excel | Staff email spreadsheet. |

## Exporting Proposals and Sending Email Notifications

This needs to be done by Rob for now :-\
