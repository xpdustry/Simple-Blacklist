# Simple Blacklist
Allows to filter player nicknames, which contain specific text or matches a regex. <br>
Can also prohibit specific IP addresses or entire address ranges.

> [!NOTE]
>
> For advanced IP/Subnet filtering, please use the [Anti-VPN-Service](github.com/xpdustry/Anti-VPN-Service) plugin instead.

To control the blacklist, run the ``blacklist`` command. <br>
And to see command usage, run ``blacklist help``.

### Features
* **Nickname blacklist**: Verify if the player name contains any element of the list.
* **Nickname normalizer**: Removes color and glyphs from player name during checks.
* **Regex blacklist**: Verify if the player name matches with any pattern of the list.
* **IP/Subnet blacklist**: Verify if the player IP is in a subnet of the list.
* **Working mode**: Can kick the player, ban the uuid or ban the IP. *(only working with nickname and regex blacklist)*
* **Ignore admins**: Can ignore admin players. *(only working with nickname and regex blacklist)*
* **Kick Message**: Custom kick message when rejecting the player for blacklisted nickname or IP.


### Feedback
Open an issue if you have a suggestion.


### Releases
Prebuild releases can be found [here](https://github.com/Xpdustry/simple-blacklist/releases)


### Building
Just run ``./gradlew build`` and the plugin will compile and export automatically.


### Installing
Simply place the output jar from the step above in your server's `config/mods` directory and restart the server.
List your currently installed plugins by running the `mods` command.
