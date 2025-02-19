# Simple Blacklist
Allows to filter player nicknames, which contain specific text or matches a regex.

To control the blacklist, run the ``blacklist`` command. <br>
And to see command usage, run ``blacklist help``.

> [!IMPORTANT]
>
> This plugin only works around player nicknames. 
> For IP/Subnet filtering, please use the [Anti-VPN-Service](github.com/xpdustry/Anti-VPN-Service) plugin instead.


### Features
* **Nickname blacklist**: Verify if the player name contains any element of the list.
* **Nickname normalizer**: Removes color and glyphs from player name during checks.
* **Regex blacklist**: Verify if the player name matches with any pattern of the list.
* **Working mode**: Can kick the player, ban the uuid or ban the IP.
* **Ignore admins**: Can ignore admin players.
* **Kick Message**: Custom kick message when rejecting the player.


### Feedback
Open an issue if you have a suggestion.


### Releases
Prebuild releases can be found [here](https://github.com/Xpdustry/simple-blacklist/releases)


### Building
Just run ``./gradlew build`` and the plugin will compile and export automatically.


### Installing
Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins by running the `mods` command.
