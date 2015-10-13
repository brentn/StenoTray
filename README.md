StenoTray
=========

<img src="http://i.imgur.com/iJhh5on.png" alt="StenoTray showing strokes for 'steno'" height="500px">

StenoTray is an Autolookup tool to work alongside [Plover stenography software](http://github.com/openstenoproject/plover)

StenoTray is intended to help steno users learn how to stroke new words, without having to go to the dictionary to look them up. 

StenoTray displays a little sidebar on your screen, and as you are typing, StenoTray is constantly trying to figure out what you are attempting to spell, and will display a list of possible choices, along with the shortest way of stroking them. Therefore, if you are stuck on a word, just sound-out the first syllable, or fingerspell the first few letters, and a list of words will appear -- averting the need to turn Plover off, switch to the dictionary, do a search, memorize the stroke, switch back to your document, turn Plover back on, and continue typing.

StenoTray runs alongside Plover, and works by monitoring the Plover log file. Therefore, you must enable *Log Translations* in Plover's configuration dialog for StenoTray to work.

## Installation

1. Download [StenoTray off its GitHub page](https://github.com/brentn/StenoTray) with the **Download ZIP** button.

2. Extract the ZIP to wherever you want to store StenoTray.

3. *Optional.* Create a configuration file for StenoTray. You may not need it, but if StenoTray does not work immediately for you, you must create one. See the configuration section for details on the configuration options and where to create the configuration file.
    Tell StenoTray where the Plover config file is with the `PLOVER_CONFIG` configuration.

4. Double check that logging is turned on in Plover: in `plover.cfg` it should have a line for `log_file` and have `enable_stroke_logging` and `enable_translation_logging` set to `True`.

5. Run `StenoTray.jar` by double-clicking it.

## Configuration

### Creating the StenoTray Configuration File

Configuration is stored in a file that you must create manually, `stenotray.cfg` in your Plover configuration directory. Here are the default locations, depending on your platform:

- Linux: `~/.config/plover/stenotray.cfg`
- Mac OS X: `~/Library/Application\ Support/plover/stenotray.cfg`
- Windows: `C:\Users\[User]\AppData\Local\plover\plover\stenotray.cfg`

Options are kept one per line, with the format: `OPTION_NAME = setting`

There are several options that can be configured in StenoTray.

#### PLOVER_CONFIG = `/path/to/plover.cfg`

The path to your plover configuration file. This is vital if StenoTray doesn't find it automatically.

#### LIMIT_RESULTS = `0`

Limit the number of results StenoTray will display at any one time. `0` is unlimited.

#### SIMPLIFY = `false`

When set to `true`, StenoTray Will display simplified strokes by identifying chords. For example, `KHROUPB` (clown) would display as `KLOUN`.

#### FONT_SIZE = `12`

Alters the size of the displayed text.

## Troubleshooting

If you get the error `Could not find the main class: StenoTray. Program will exit`, then you probably don't have the current folder in your Java path. You can instead run the file by making a shortcut or `.bat` file that runs it like this:

`java -Djava.ext.dirs=. -Djava.library.path=. StenoTray`

On Windows: Right-click `StenoTray.jar` >> Create shortcut. Right-click "StenoTray - Shortcut.lnk" >> Properties, and at `Target` you fill in: `java -Djava.ext.dirs=. -Djava.library.path=. StenoTray`
