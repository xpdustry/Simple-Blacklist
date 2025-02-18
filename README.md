# Simple Blacklist
Just a simple blacklist with modifiable features, like working mode, kick message or priority of listener.

To control the blacklist, run the ``blacklist`` command. <br>
And to see command usage, run ``blacklist help``.

### Features
**TODO**

A pretty good feature is the blacklist listener priority: if set to 'first', it's working before all verifications of client, so it can avoid creating empty accounts or interfere with other listeners

**Also this can allow a blacklist of regex. If a nickname matches with one regex of list, it's also kicked.<br>
This is very useful for servers that suffer raids with different nicknames each time.**


### Feedback
Open an issue if you have a suggestion.


### Releases
Prebuild releases can be found [here](https://github.com/Xpdustry/simple-blacklist/releases)


### Building
Just run ``./gradlew build`` and the plugin will compile and export automatically.


### Installing
Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins by running the `mods` command.
