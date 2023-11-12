# RC Lap Tracker
Counts how many runecrafting laps remain until your pouches degrade.

This plugin requires NPC Contact, and a Bank. Sorry UIMs and people who don't like quests.

Resets on NPC Contact spell cast.
Counter decrements the first time you craft runes after banking.

Default reset value is 8, for the Colossal pouch.
10, 29, and 45 are also possible for Giant, Large, and Medium pouches, respectively.
The highest pouch you are using is automatically detected upon closing the Bank interface, and your reset value is set accordingly.

This plugin was created as other essence tracking plugins tend to be unreliable in my experience.
So my idea was to circumvent the pouch entirely and instead infer a lap was taking place, which CAN be definitively tracked.