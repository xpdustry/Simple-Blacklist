# Simple Blacklist
Just a simple blacklist with some added features to customise it, like working mode,  kick message or priority of listener.

A pretty good feature is the listener priority of blacklist: if set to 'first', it's working before all verifications of client, 
so it can avoid creating empty accounts or interfere with other listeners

To control the blacklist, is very simple. Just use the command 'blacklist'. <br>
And to see usage of command, use 'blacklist help'.

### Feedback
Open an issue if you have a suggestion.

### Releases
Prebuild relases can be found [here](https://github.com/Xpdustry/Simple-Blacklist/releases)

### Building a Jar 
You have just run command ``./gradlew :build`` or execute script ``build.bat`` and the plugin will compile automatically.

### Installing
Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins by running the `mods` command.
