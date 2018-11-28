package com.scirra;

import android.app.Activity;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.View;

import android.GameHelper.GameHelperListener;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.games.AchievementsClient;
import com.google.android.gms.games.AnnotatedData;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.LeaderboardsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.achievement.Achievement;
import com.google.android.gms.games.achievement.AchievementBuffer;
import com.google.android.gms.games.leaderboard.LeaderboardScore;
import com.google.android.gms.games.leaderboard.LeaderboardScoreBuffer;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import static com.google.android.gms.games.leaderboard.LeaderboardVariant.COLLECTION_PUBLIC;
import static com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_ALL_TIME;
import static com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_DAILY;
import static com.google.android.gms.games.leaderboard.LeaderboardVariant.TIME_SPAN_WEEKLY;

public class ConstructPlayGames extends CordovaPlugin implements GameHelperListener
{
	// Client used to sign in with Google APIs
	private GoogleSignInClient mGoogleSignInClient;

	// Client variables
	private AchievementsClient mAchievementsClient;
	private LeaderboardsClient mLeaderboardsClient;
	private PlayersClient mPlayersClient;
	private CallbackContext mSignInCallback;
	private GamesClient mGamesClient;
	private PluginResult mQueuedSignInResult;

	private static final int RC_UNUSED = 5001;
	private static final int RC_SIGN_IN = 9001;

	private static final String TAG = "Construct Play Games";

	@Override
	public void initialize(CordovaInterface cordova, CordovaWebView webView)
	{
		super.initialize(cordova, webView);

		mGoogleSignInClient = GoogleSignIn.getClient(
				cordova.getActivity(),
				new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build()
		);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent intent)
	{
		if (requestCode == RC_SIGN_IN)
		{
			Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(intent);

			try
			{
				GoogleSignInAccount account = task.getResult(ApiException.class);
				onConnected(account);
			}
			catch (ApiException apiException)
			{
				String message = apiException.getMessage();

				onDisconnected(message);
			}
		}
	}

	@Override
	public void onResume(boolean multitasking)
	{
		super.onResume(multitasking);
		// Since the state of the signed in user can change when the activity is not active
		// it is recommended to try and sign in silently from when the app resumes.
		//signInSilently();
	}

	@Override
	public void onStop()
	{
		super.onStop();
	}

	@Override
	public boolean execute(final String action, final JSONArray arguments, final CallbackContext callbackContext) throws JSONException
	{
		final ConstructPlayGames self = this;
		final JSONObject options = arguments.getJSONObject(0);

		cordova.getActivity().runOnUiThread(new Runnable() {
			@Override
			public void run() {
				if (action.equals("signIn"))
				{
					self.signIn(callbackContext);
				}
				else if (action.equals("signOut"))
				{
					self.signOut(callbackContext);
				}
				else if (action.equals("signInSilently"))
				{
					self.signInSilently(callbackContext);
				}
				else if (action.equals("resumeSignIn"))
				{
					self.resumeSignIn(callbackContext);
				}
				else if (action.equals("incrementAchievement"))
				{
					self.incrementAchievement(callbackContext, options);
				}
				else if (action.equals("setStepsAchievement"))
				{
					self.setStepsAchievement(callbackContext, options);
				}
				else if (action.equals("getAchievements"))
				{
					self.getAchievements(callbackContext, options);
				}
				else if (action.equals("revealAchievement"))
				{
					self.revealAchievement(callbackContext, options);
				}
				else if (action.equals("unlockAchievement"))
				{
					self.unlockAchievement(callbackContext, options);
				}
				else if (action.equals("getPlayerData"))
				{
					self.getPlayerData(callbackContext);
				}
				else if (action.equals("getHiScores"))
				{
					self.getHiScores(callbackContext, options);
				}
				else if (action.equals("submitScore"))
				{
					self.submitScore(callbackContext, options);
				}
				else if (action.equals("showAchievements"))
				{
					self.showAchievements(callbackContext);
				}
				else if (action.equals("showLeaderboards"))
				{
					self.showLeaderboards(callbackContext);
				}
				else if (action.equals("showLeaderboard"))
				{
					self.showLeaderboard(callbackContext, options);
				}
				else
				{
					callbackContext.error("invalid method");
				}
			}
		});

		return true;
	}

	// NOTE internal methods only here

	private void updateSignInState(boolean bSignedIn)
	{
		PluginResult result = new PluginResult(PluginResult.Status.OK, bSignedIn);
		result.setKeepCallback(true);

		if (mSignInCallback == null)
			mQueuedSignInResult = result;
		else
			mSignInCallback.sendPluginResult(result);
	}

	private boolean isSignedIn()
	{
		return GoogleSignIn.getLastSignedInAccount(cordova.getActivity()) != null;
	}

	// private void achievementToast(String achievement) {
	//   // Only show toast if not signed in. If signed in, the standard Google Play
	//   // toasts will appear, so we don't need to show our own.
	//   if (!isSignedIn()) {
	// 	Toast.makeText(this, getString(R.string.achievement) + ": " + achievement,
	// 		Toast.LENGTH_LONG).show();
	//   }
	// }

	private void onConnected (GoogleSignInAccount googleSignInAccount)
	{
		Activity activity = cordova.getActivity();
		mGamesClient = Games.getGamesClient(activity, googleSignInAccount);
		mAchievementsClient = Games.getAchievementsClient(activity, googleSignInAccount);
		mLeaderboardsClient = Games.getLeaderboardsClient(activity, googleSignInAccount);
		mPlayersClient = Games.getPlayersClient(activity, googleSignInAccount);

		View rootView = webView.getView();
		mGamesClient.setViewForPopups(rootView);

		updateSignInState(true);
	}

	private void onDisconnected ()
	{
		onDisconnected("Disconnected");
	}

	private void onDisconnected (String message)
	{
		Log.w(TAG, message);

		mGamesClient = null;
		mAchievementsClient = null;
		mLeaderboardsClient = null;
		mPlayersClient = null;

		updateSignInState(false);
	}

	private JSONObject createScoreData (LeaderboardsClient.LeaderboardScores leaderboarcScores, LeaderboardScore playerScore) throws JSONException
	{
		LeaderboardScoreBuffer scoreBuffer = leaderboarcScores.getScores();
		Iterator<LeaderboardScore> scoreIterator = scoreBuffer.iterator();
		LeaderboardScore leaderboardScore;
		JSONObject result = new JSONObject();
		JSONArray entries = new JSONArray();
		JSONObject entry;
		JSONObject playerInfo;
		Player scoreHolder;
		String scoreHolderID;

		/*
			// Resulting JSON structure

			{
				"items": [
					{
						"player": {
							"displayName": String,
							"iconURI": String
						},
						"scoreValue": Int,
						"formattedScore": String,
						"scoreTag": String,
						"scoreRank": Int,
						"formattedScoreRank": String
					}
				],
				"numScores": 0,
				"playerScore": null
			}
		*/

		if (playerScore != null)
		{
			JSONObject currentPlayerScore = new JSONObject();

			currentPlayerScore.put("scoreValue", playerScore.getRawScore());
			currentPlayerScore.put("formattedScore", playerScore.getDisplayScore());
			currentPlayerScore.put("scoreTag", playerScore.getScoreTag());
			currentPlayerScore.put("scoreRank", playerScore.getRank());
			currentPlayerScore.put("formattedScoreRank", playerScore.getDisplayRank());

			result.put("playerScore", currentPlayerScore);
		}
		else
		{
			result.put("playerScore", JSONObject.NULL);
		}

		while (scoreIterator.hasNext())
		{
			leaderboardScore = scoreIterator.next();
			entry = new JSONObject();
			playerInfo = new JSONObject();
			scoreHolder = leaderboardScore.getScoreHolder();

			playerInfo.put("displayName", leaderboardScore.getScoreHolderDisplayName());
			playerInfo.put("iconURI", leaderboardScore.getScoreHolderIconImageUri());

			entry.put("scoreValue", leaderboardScore.getRawScore());
			entry.put("formattedScore", leaderboardScore.getDisplayScore());
			entry.put("scoreTag", leaderboardScore.getScoreTag());
			entry.put("scoreRank", leaderboardScore.getRank());
			entry.put("formattedScoreRank", leaderboardScore.getDisplayRank());

			entry.put("player", playerInfo);
			entries.put(entry);
		}

		result.put("numScores", 0);
		result.put("items", entries);

		return result;
	}

	private JSONObject createAchievementData (AchievementBuffer achievementBuffer) throws JSONException
	{
		Iterator<Achievement> achievementIterator = achievementBuffer.iterator();
		Achievement achievement;
		JSONObject result = new JSONObject();
		JSONArray entries = new JSONArray();
		JSONObject entry;
		int type;
		/*
			// Resulting JSON structure

			{
				"items": [
					{
						"id": String,
						"achievementState": "Int"
						"name": String,
						"description": String,
						"type": String,
						"currentSteps": Int,
						"totalSteps": Int,
						"revealedUrl": String,
						"unlockedUrl": String
					}
				]
			}
		*/

		while (achievementIterator.hasNext())
		{
			achievement = achievementIterator.next();
			entry = new JSONObject();
			type = achievement.getType();

			entry.put("id", achievement.getAchievementId());
			switch (achievement.getState()) {
				case Achievement.STATE_HIDDEN:
					entry.put("achievementState", "HIDDEN");
					break;
				case Achievement.STATE_REVEALED:
					entry.put("achievementState", "REVEALED");
					break;
				case Achievement.STATE_UNLOCKED:
					entry.put("achievementState", "UNLOCKED");
					break;
			}

			entry.put("name", achievement.getName());
			entry.put("description", achievement.getDescription());

			if (type == Achievement.TYPE_INCREMENTAL) {
				entry.put("type", "incremental");
				entry.put("currentSteps", achievement.getCurrentSteps());
				entry.put("totalSteps", achievement.getTotalSteps());
			}
			else {
				entry.put("type", "standard");
				entry.put("currentSteps", JSONObject.NULL);
				entry.put("totalSteps", JSONObject.NULL);
			}
			entry.put("revealedUrl", achievement.getRevealedImageUri());
			entry.put("unlockedUrl", achievement.getUnlockedImageUri());

			entries.put(entry);
		}

		result.put("items", entries);

		return result;
	}

	// NOTE async task utilities

	public interface TaskOperator {
		public void fn(Task a);
	}

	public interface TaskCollectionOperator {
		public void fn(Task[] a);
	}

	private void awaitTask(Task task, TaskOperator success, TaskOperator failure) {
		task.addOnCompleteListener(
				cordova.getActivity(),
				new OnCompleteListener() {
					@Override
					public void onComplete(@NonNull Task task) {
						if (task.isSuccessful())
							success.fn(task);
						else
							failure.fn(task);
					}
				}
		);
	}

	private void awaitTasks(Task[] tasks, TaskCollectionOperator success, TaskCollectionOperator failure) {
		TaskCounter counter = new TaskCounter(tasks.length);

		for (Task task: tasks) {
			awaitTask(
					task,
					t -> {
						if (!counter.hasFailed())
						{
							counter.decrement();

							if (counter.isComplete())
								success.fn(tasks);
						}
					},
					t -> {
						counter.fail();
						failure.fn(tasks);
					}
			);
		}
	}

	// NOTE methods called by execute

	private void signInSilently(final CallbackContext callbackContext)
	{
		mSignInCallback = callbackContext;

		if (isSignedIn())
		{
			// sometimes will be signed in but not connected
			if (mPlayersClient == null)
			{
				onConnected(GoogleSignIn.getLastSignedInAccount(cordova.getActivity()));
			}
			return;
		}

		awaitTask(
				mGoogleSignInClient.silentSignIn(),
				t -> onConnected(((GoogleSignInAccount) t.getResult())),
				t -> onDisconnected()
		);
	}

	private void signIn(final CallbackContext callbackContext)
	{
		mSignInCallback = callbackContext;

		if (isSignedIn())
		{
			// sometimes will be signed in but not connected
			if (mPlayersClient == null)
			{
				onConnected(GoogleSignIn.getLastSignedInAccount(cordova.getActivity()));
			}
			return;
		}

		cordova.setActivityResultCallback(this);
		cordova
				.getActivity()
				.startActivityForResult(
						mGoogleSignInClient.getSignInIntent(),
						RC_SIGN_IN
				);
	}

	private void signOut(final CallbackContext callbackContext)
	{
		mSignInCallback = callbackContext;

		if (!isSignedIn())
		{
			updateSignInState(false);
			return;
		}

		awaitTask(
				mGoogleSignInClient.signOut(),
				t -> onDisconnected(),
				t -> Log.d(TAG, "signOut(): success")
		);
	}

	private void resumeSignIn(final CallbackContext callbackContext)
	{
		mSignInCallback = callbackContext;

		if (mQueuedSignInResult != null)
		{
			mSignInCallback.sendPluginResult(mQueuedSignInResult);
			mQueuedSignInResult = null;
		}
	}

	private void getAchievements(final CallbackContext callbackContext, final JSONObject options)
	{
		if (!isSignedIn()) {
			callbackContext.error("Not signed in");
			return;
		}

		boolean forceReload = options.optBoolean("reload", false);

		awaitTask(
				mAchievementsClient.load(forceReload),
				t -> {
					AnnotatedData<AchievementBuffer> annotatedBuffer = (AnnotatedData<AchievementBuffer>) t.getResult();
					AchievementBuffer buffer = annotatedBuffer.get();

					try
					{
						JSONObject data = createAchievementData(buffer);
						callbackContext.success(data);
					}
					catch (JSONException e)
					{
						callbackContext.error("Failed to get achievements");
					}

					buffer.release();
				},
				t -> callbackContext.error("Failed to get achievements")
		);
	}

	private void incrementAchievement(final CallbackContext callbackContext, final JSONObject options)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		String achievementId = options.optString("achievementId");
		int numSteps = options.optInt("numSteps");

		if (achievementId.equals(""))
		{
			callbackContext.error("\"achievementId\" is not defined");
			return;
		}

		if (numSteps == 0)
		{
			callbackContext.error("\"numSteps\" is invalid");
			return;
		}

		mAchievementsClient.increment(achievementId, numSteps);

		callbackContext.success();
	}

	private void setStepsAchievement(final CallbackContext callbackContext, final JSONObject options)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		String achievementId = options.optString("achievementId");
		int numSteps = options.optInt("numSteps");

		if (achievementId.equals(""))
		{
			callbackContext.error("\"achievementId\" is not defined");
			return;
		}

		if (numSteps == 0)
		{
			callbackContext.error("\"numSteps\" is invalid");
			return;
		}

		mAchievementsClient.setSteps(achievementId, numSteps);

		callbackContext.success();
	}

	private void revealAchievement(final CallbackContext callbackContext, final JSONObject options)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		String achievementId = options.optString("achievementId");

		if (achievementId.equals(""))
		{
			callbackContext.error("\"achievementId\" is not defined");
			return;
		}

		mAchievementsClient.reveal(achievementId);

		callbackContext.success();
	}

	private void unlockAchievement(final CallbackContext callbackContext, final JSONObject options)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		String achievementId = options.optString("achievementId");

		if (achievementId.equals(""))
		{
			callbackContext.error("\"achievementId\" is not defined");
			return;
		}

		mAchievementsClient.unlock(achievementId);

		callbackContext.success();
	}

	private void getPlayerData(final CallbackContext callbackContext)
	{
       //Log.d(LOGTAG, "executeShowPlayer");

        cordova.getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                try {
                    if (gameHelper.isSignedIn()) {

                        Player player = Games.Players.getCurrentPlayer(gameHelper.getApiClient());

                        JSONObject playerJson = new JSONObject();
                        playerJson.put("displayName", player.getDisplayName());
                        playerJson.put("playerId", player.getPlayerId());
                        playerJson.put("title", player.getTitle());
                        playerJson.put("iconImageUrl", player.getIconImageUrl());
                        playerJson.put("hiResIconImageUrl", player.getHiResImageUrl());

                        callbackContext.success(playerJson);

                    } else {
                        //Log.w(LOGTAG, "executeShowPlayer: not yet signed in");
                        callbackContext.error("executeShowPlayer: not yet signed in");
                    }
                }
                catch(Exception e) {
                    //Log.w(LOGTAG, "executeShowPlayer: Error providing player data", e);
                    callbackContext.error("executeShowPlayer: Error providing player data");
                }
            }
        });
    }

	private void getHiScores(final CallbackContext callbackContext, final JSONObject options)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		String leaderboardId = options.optString("leaderboardId");
		String timeSpan = options.optString("timeSpan", "ALL_TIME");
		int timeSpanEnum;
		int maxResults = options.optInt("maxResults", 25);
		String type = options.optString("type", "scores");
		boolean forceReload = options.optBoolean("reload", false);

		if (leaderboardId.equals(""))
		{
			callbackContext.error("invalid leaderboardId");
			return;
		}

		if (timeSpan.equals("ALL_TIME"))
		{
			timeSpanEnum = TIME_SPAN_ALL_TIME;
		}
		else if (timeSpan.equals("DAILY"))
		{
			timeSpanEnum = TIME_SPAN_DAILY;
		}
		else if (timeSpan.equals("WEEKLY"))
		{
			timeSpanEnum = TIME_SPAN_WEEKLY;
		}
		else
		{
			callbackContext.error("Invalid timespan");
			return;
		}

		if (maxResults > 25 || maxResults < 1)
		{
			callbackContext.error("Invalid maxResults value");
			return;
		}

		Task loadAllScores;

		if (type.equals("window"))
		{
			loadAllScores = mLeaderboardsClient.loadPlayerCenteredScores(leaderboardId, timeSpanEnum, COLLECTION_PUBLIC, maxResults, forceReload);
		}
		else if (type.equals("scores"))
		{
			loadAllScores = mLeaderboardsClient.loadTopScores(leaderboardId, timeSpanEnum, COLLECTION_PUBLIC, maxResults, forceReload);
		}
		else
		{
			callbackContext.error("Invalid scores type");
			return;
		}

		Task loadPlayerScores = mLeaderboardsClient.loadCurrentPlayerLeaderboardScore(leaderboardId, timeSpanEnum, COLLECTION_PUBLIC);

		Task[] tasks = {loadPlayerScores,loadAllScores};

		awaitTasks(
				tasks,
				t -> {
					AnnotatedData<LeaderboardScore> annotatedPlayerScore = (AnnotatedData<LeaderboardScore>) t[0].getResult();
					AnnotatedData<LeaderboardsClient.LeaderboardScores> annotatedTopScores = (AnnotatedData<LeaderboardsClient.LeaderboardScores>) t[1].getResult();

					LeaderboardsClient.LeaderboardScores scores = annotatedTopScores.get();
					LeaderboardScore playerScore = annotatedPlayerScore.get();

					try
					{
						JSONObject result = createScoreData(scores, playerScore);

						callbackContext.success(result);
					}
					catch (JSONException e)
					{
						callbackContext.error("Failed to load top scores");
					}

					scores.release();
				},
				t -> callbackContext.error("Failed to load top scores")
		);
	}

	private void submitScore(final CallbackContext callbackContext, final JSONObject options)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		String leaderboardId = options.optString("leaderboardId");
		int score = options.optInt("score");
		String tag = options.optString("tag", "");

		if (score == 0)
		{
			callbackContext.error("Invalid score value");
			return;
		}

		if (leaderboardId.equals(""))
		{
			callbackContext.error("Invalid leaderboardId");
		}

		if (tag.equals(""))
		{
			mLeaderboardsClient.submitScore(leaderboardId, score);
		}
		else
		{
			mLeaderboardsClient.submitScore(leaderboardId, score, tag);
		}


		callbackContext.success();
	}

	private void showAchievements(final CallbackContext callbackContext)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		final CordovaInterface cordovaInterface = cordova;
		final CordovaPlugin self = this;

		mAchievementsClient.getAchievementsIntent()
				.addOnSuccessListener(new OnSuccessListener<Intent>()
				{
					@Override
					public void onSuccess(Intent intent)
					{
						Activity activity = cordovaInterface.getActivity();
						cordovaInterface.setActivityResultCallback(self);
						activity.startActivityForResult(intent, RC_UNUSED);
						callbackContext.success();
					}
				})
				.addOnFailureListener(new OnFailureListener()
				{
					@Override
					public void onFailure(@NonNull Exception e)
					{
						callbackContext.error("Failed to show achievements");
					}
				});
	}

	private void showLeaderboards(final CallbackContext callbackContext)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		final CordovaInterface cordovaInterface = cordova;
		final CordovaPlugin self = this;

		mLeaderboardsClient.getAllLeaderboardsIntent()
				.addOnSuccessListener(new OnSuccessListener<Intent>()
				{
					@Override
					public void onSuccess(Intent intent)
					{
						Activity activity = cordovaInterface.getActivity();
						cordovaInterface.setActivityResultCallback(self);
						activity.startActivityForResult(intent, RC_UNUSED);
						callbackContext.success();
					}
				})
				.addOnFailureListener(new OnFailureListener()
				{
					@Override
					public void onFailure(@NonNull Exception e)
					{
						callbackContext.error("Failed to show leaderboards");
					}
				});
	}

	private void showLeaderboard(final CallbackContext callbackContext, final JSONObject options)
	{
		if (!isSignedIn())
		{
			callbackContext.error("Not signed in");
			return;
		}

		final CordovaInterface cordovaInterface = cordova;
		final CordovaPlugin self = this;
		final String leaderboardId = options.optString("leaderboardId");

		if (leaderboardId.equals(""))
		{
			callbackContext.error("Invalid leaderboardId");
			return;
		}

		mLeaderboardsClient.getLeaderboardIntent(leaderboardId)
				.addOnSuccessListener(new OnSuccessListener<Intent>()
				{
					@Override
					public void onSuccess(Intent intent)
					{
						Activity activity = cordovaInterface.getActivity();
						cordovaInterface.setActivityResultCallback(self);
						activity.startActivityForResult(intent, RC_UNUSED);
						callbackContext.success();
					}
				})
				.addOnFailureListener(new OnFailureListener()
				{
					@Override
					public void onFailure(@NonNull Exception e)
					{
						callbackContext.error("Failed to show leaderboard");
					}
				});
	}
	
	@Override
    public void onSignInFailed() {
        authCallbackContext.error("SIGN IN FAILED");
    }

    @Override
    public void onSignInSucceeded() {
        authCallbackContext.success("SIGN IN SUCCESS");
    }
}