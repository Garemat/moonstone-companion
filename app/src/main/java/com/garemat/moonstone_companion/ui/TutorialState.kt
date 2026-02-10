package com.garemat.moonstone_companion.ui

import androidx.compose.ui.layout.LayoutCoordinates

data class TutorialState(
    val showTutorial: Boolean = false,
    val currentStep: Int = 0,
    val targetCoordinates: Map<String, LayoutCoordinates> = emptyMap()
)

data class TutorialStep(
    val targetName: String,
    val text: String,
    val isArrowless: Boolean = false
)

val homeScreenTutorialSteps = listOf(
    TutorialStep("Characters", "Here's a full reference of all the characters available to use."),
    TutorialStep("My Troupes", "Create and manage your own troupes to use in games."),
    TutorialStep("Rules Reference", "Quickly look up rules and keywords."),
    TutorialStep("Settings", "Customize the app's theme and other preferences."),
    TutorialStep("START GAME", "Set up a new game, either locally or with friends."),
)

val troupesScreenTutorialSteps = listOf(
    TutorialStep("TroupeList", "This is where all your troupes will appear when you create them!"),
    TutorialStep("ImportTroupe", "You can import a troupe from a shared QR code here."),
    TutorialStep("ShareTroupe", "Share your troupe with others by generating a QR code."),
    TutorialStep("DeleteTroupe", "Remove troupes you no longer need."),
    TutorialStep("AddTroupe", "Create a brand new troupe from scratch!")
)

val buildTroupeTutorialSteps = listOf(
    TutorialStep("TroupeName", "Start by giving your troupe a name!"),
    TutorialStep("FactionSymbols", "Select which faction this troupe belongs to."),
    TutorialStep("NameSearch", "Search for specific characters by name."),
    TutorialStep("CharacterTags", "Filter characters by their specific tags."),
    TutorialStep("FilterButton", "After selecting your filters, you can press this to give yourself more room to check out the characters!"),
    TutorialStep("AddCharacters", "Here you can select your characters for the troupe. There's no limit here as you can refine the selected troupe in the play game screen"),
    TutorialStep("SettingsCog", "This will let you save some Troupe specific settings, like auto selecting the members before games"),
    TutorialStep("SaveButton", "Lastly the save button! You'll get a pop up at this stage to verify the troupe legality if you've enabled auto selection")
)

val charactersScreenTutorialSteps = listOf(
    TutorialStep("FilterButtonOpen", "Tap the filter button to search and narrow down the characters."),
    TutorialStep("SearchField", "This will filter through character names or any text on abilities!"),
    TutorialStep("FactionFilter", "You can select multiple to find dual faction units."),
    TutorialStep("TagFilter", "Narrow down characters by their specific tags."),
    TutorialStep("FilterButtonClose", "You press this again to close the filter."),
    TutorialStep("FirstCharacterCard", "This is a character card! You can tap anywhere on the title (Or the down arrow if you prefer a target) to expand the character info!"),
    TutorialStep("", "The layout the characters change with the theme, try them all to decide on your favourite!", isArrowless = true),
    TutorialStep("FlipButton", "Tap this to flip to the characters signature move! (You can also tap the Signature Move text at the bottom of the card)"),
    TutorialStep("", "And that's the character compendium! Make sure to reference the physical character cards while the bugs get flattened!", isArrowless = true)
)

val gameSetupTutorialSteps = listOf(

    TutorialStep("", "There's two modes for game setup, a local offline game (Just this device) or a local online game, to automatically track troupe information!", isArrowless = true),
    TutorialStep("", "First we'll run through the offline mode, which is this default screen", isArrowless = true),
    TutorialStep("PlayerCount", "You can select up to 4 players! This will effect the maximum troupe size and the trackable players. We'll just stick with 2 for the examples"),
    TutorialStep("TroupeSelector", "Select here to choose your troupe! (If you filled our your name under settings this will be reflected here too)"),
    TutorialStep("CreateNewTroupe", "You can press here to create a new troupe if you haven't created one yet!"),
    TutorialStep("ExampleTroupeItem", "But we'll use this one for the example. If you haven't set up auto troupe selection you'll get another prompt here to refine your troupe"),
    TutorialStep("SaveTroupeSetup", "You can use this to save a troupe to your local storage, more useful if you scanned in your opponents troupe..."),
    TutorialStep("QrCodeDisplayButton", "This will display the QR code for your troupe that your opponents can scan on their device if you don't want to use a multiplayer session (Note that they can see your troupe name!)"),
    TutorialStep("ScanQR", "This is the QR scanner itself, this will prompt for permission to use the camera."),
    TutorialStep("EditTroupeButton", "You can make some quick edits to a troupe here too"),
    TutorialStep("JoinButton", "Next let's cover multiplayer! The join button will pop up a searching box that will display all the nearby hosts. Make sure your opponent has set their profile name to make it easier to find them!"),
    TutorialStep("HostButton", "This will begin a new multiplayer session, that screen looks a little different, so we'll jump over there now"),
    TutorialStep("SessionId", "Make sure this value is matching for all players"),
    TutorialStep("LeaveButton", "You'll need to hit leave to fully leave the session, you can hop around to other screens and still remain in contact"),
    TutorialStep("SecondPlayerSlot", "Any other players troupe information will automatically sync to all players once they have selected a troupe."),
    TutorialStep("FirstPlayerSlot", "And only you can change your troupe from this screen"),
    TutorialStep("StartBattleButton", "When all players are ready you can press the start battle button to begin! The next screen will be the same for offline or online sessions!")
)
