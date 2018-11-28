# Google Play Games for cordova

This plugin uses the latest API methods as of the 12/02/2018.

This is intended to be used with the Construct 3 runtime, but should work with any cordova project.

All methods can be found as part of `cordova.plugins.ConstructPlayGames`.

Installation

```bash
	cordova plugin add construct-play-games --variable APP_ID=[ game identifier ]
```

### Login related methods

#### `setCallback(Function callback)`

Register a callback method for updates to the login state

```javascript
	const play = cordova.plugins.ConstructPlayGames;
	
	play.setCallback((err, isLoggedIn) => {
		if (!err)
			console.log(isLoggedIn ? "Logged in" : "Logged out");
	});
```

#### `silentSignIn()`

Login without prompting the user ( they must have previously signed in ).

```javascript
	play.silentSignIn();
```

#### `signin()`

Attempt a normal login, this will display a dialog ( briefly ) even if the user has already logged in.

```javascript
	play.signin();
```

#### `signout()`

Logout of the current account.

```javascript
	play.signout();
```

### Native dialog methods

#### `showLeaderboards(optional Function callback)`

Show the native leaderboard dialog with all leaderboards

```javascript
	play.showLeaderboards()
	// OR
	play.showLeaderboards(err => {
		if(!err)
			console.log("Did show leaderboards");
	});
```

#### `showLeaderboard(Object parameters, optional Function callback)`

Show the native leaderboard dialog with a specific leaderboard

```javascript
	const params = {
		leaderboardId: "my_leaderboard_id"
	};
	
	play.showLeaderboard(params)
	// OR
	play.showLeaderboards(params, err => {
		if(!err)
			console.log("Did show leaderboards");
	});
```

#### `showAchievements(optional Function callback)`

Show the native achievement dialog with all achievements

```javascript
	play.showAchievements()
	// OR
	play.showAchievements(err => {
		if(!err)
			console.log("Did show achievements");
	});
```

### Leaderboard manipulation

#### `getHiScores(Object parameters, Function callback)`

Get the high scores for the selected leaderboard as a JS object. Use the optional `reload` option to bypass the local cache.

```javascript
	const params = {
		leaderboardId: "my_leaderboard_id",
		reload: true
	};

	play.getHiScores(params, (err, obj) => {
		if(!err)
			console.log("high scores", obj);
			
		/*
		 *	obj = {
		 *		"items": [
		 *			{
		 *				"player": {
		 *					"displayName": String,
		 *					"iconURI": String
		 *				},
		 *				"scoreValue": Int,
		 *				"formattedScore": String,
		 *				"scoreTag": String,
		 *				"scoreRank": Int,
		 *				"formattedScoreRank": String
		 *			}
		 *		],
		 *		"playerScore": null
		 *	}
		 */
	});
```

#### `submitScore(Object parameters, optional Function callback)`

Submit a highscore to the selected leaderboard.

```javascript
	const params = {
		leaderboardId: "my_leaderboard_id",
		score: 42
	};
	
	play.submitScore(params);
	// OR
	play.submitScore(params, err => {
		if(!err)
			console.log("successfully submitted score");
	});
```

### Achievement manipulation

#### `getAchievements(optional Object parameters, Function callback)`

Get the achievements for the app as a JS object. Use the optional `reload` option to bypass the local cache.

```javascript
	const params = {
		reload: true
	};
	
	play.getAchievements(params, (err, obj) => {
		if(!err)
			console.log("achievements", obj);
			
		/*
		 *	obj = {
		 *		"items": [
		 *			{
		 *				"id": String,
		 *				"state": "Int"
		 *				"name": String,
		 *				"description": String,
		 *				"type": Int,
		 *				"currentSteps": Int,
		 *				"totalSteps": Int,
		 *				"revealedUrl": String,
		 *				"unlockedUrl": String
		 *			}
		 *		]
		 *	}
		 */
	});
```

#### `incrementAchievement(Object parameters, optional Function callback)`

Increment the selected achievement by the given number of steps.

```javascript
	const params = {
		achievementId: "my_achievement_id",
		numSteps: 42
	};
	
	play.incrementAchievement(params);
	// OR
	play.incrementAchievement(params, err => {
		if(!err)
			console.log("successfully incremented achievement");
	});
```

#### `setStepsAchievement(Object parameters, optional Function callback)`

Set the current step value of the selected achievement.

```javascript
	const params = {
		achievementId: "my_achievement_id",
		numSteps: 42
	};
	
	play.setStepsAchievement(params);
	// OR
	play.setStepsAchievement(params, err => {
		if(!err)
			console.log("successfully set achievement steps");
	});
```

#### `revealAchievement(Object parameters, optional Function callback)`

Reveal the selected achievement.

```javascript
	const params = {
		achievementId: "my_achievement_id"
	};
	
	play.revealAchievement(params);
	// OR
	play.revealAchievement(params, err => {
		if(!err)
			console.log("successfully revealed achievement");
	});
```

#### `unlockAchievement(Object parameters, optional Function callback)`

Unlock the selected achievement.

```javascript
	const params = {
		achievementId: "my_achievement_id"
	};
	
	play.unlockAchievement(params);
	// OR
	play.unlockAchievement(params, err => {
		if(!err)
			console.log("successfully unlocked achievement");
	});
```
