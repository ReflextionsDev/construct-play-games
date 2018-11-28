var exec = require('cordova/exec');
var NAME = "ConstructPlayGames";

var ACTIONS = [
	"incrementAchievement",
	"getAchievements",
	"revealAchievement",
	"unlockAchievement",
	"getPlayerData",
	"getHiScores",
	"submitScore",
	"showAchievements",
	"showLeaderboards",
	"showLeaderboard",
	"setStepsAchievement"
];

function bindAction (action)
{
	function defaultHandler (err, result) {
		if (err)
			console.log("[" + NAME + "] " + action + " failed.");
		else
			console.log("[" + NAME + "] " + action + " succeeded.");
	}
	return function (data, fn)
	{
		var type = typeof data;
		
		if (type === "undefined")
		{
			data = {};
		}
		else if (type === "function")
		{
			fn = data;
			data = {};
		}
		else if (type !== "object")
		{
			throw new TypeError("Invalid data parameter");
		}
		
		if (!fn)
			fn = defaultHandler;
			
		if (typeof fn !== "function")
		{
			throw new TypeError("Invalid callback parameter");
		}
		
		function success (ret) {
			fn(null, ret);
		}
		function failure (ret) {
			fn(ret, null);
		}
		
		exec(success, failure, NAME, action, [ data ]);
	}
}

function defaultStateCallback (err, state)
{
	if (err)
		console.error(err);
	else
		console.log("[" + NAME + "] is logged " + (state ? "in" : "out"));
}

var mod = {
	_login: bindAction("signIn"),
	_logout: bindAction("signOut"),
	_silentLogin: bindAction("signInSilently"),
	_stateCallback: defaultStateCallback,
	"resumeSignIn": bindAction("resumeSignIn"),
	"silentSignIn": function ()
	{
		this._silentLogin(this._stateCallback);
	},
	"signin": function ()
	{
		this._login(this._stateCallback);
	},
	"signout": function ()
	{
		this._logout(this._stateCallback);
	},
	"setCallback": function (fn)
	{
		if (!fn)
			fn = defaultStateCallback;
		this._stateCallback = fn;
	}
};

for (var i = 0, l = ACTIONS.length, action; i < l; i++)
{
	action = ACTIONS[i];
	mod[action] = bindAction(action);
}

module.exports = mod;