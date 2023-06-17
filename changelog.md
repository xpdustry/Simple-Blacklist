1.3:
  - Fixed bug when kick player, is not realy kicked (verifications continue after kick)
  - Added a prevention of multiple connection of a client

1.2:
  - Fixed bug with count of regex not saving
  - Fixed bug with regex don't removing to list (without restart)
  - Fixed bug when kicking the player, an empty account is created
  - Added more client verifications (before nickname check) to avoid empty accounts
  - Added setting to ignore admins
  - Changed help argument
  - Changed settings fields **(please check your actual configuration before update, because this will be reset some blacklist settings)**
  - Command with no argument now print all informations of plugin
  - Changed usage of command
  - Fixed bug with compilation

1.1:
  - Added regex blacklist
  - Improved help argument
  - Added a priority setting between nicknames and regex blacklist
  - Added possibility to disable a list
  - Improved blacklist display

1.0:
 - Added nicknames blacklist
 - Added count of kicking
 - Added listener priority setting
 - Added working mode setting between ban or kick player
 - Added command to control the blacklist
 - Added a custom message when kick player