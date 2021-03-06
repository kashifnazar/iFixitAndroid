package com.dozuki.ifixit.ui;

import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.user.LoginEvent;
import com.dozuki.ifixit.ui.guide.view.LoadingFragment;
import com.dozuki.ifixit.ui.login.LoginFragment;
import com.dozuki.ifixit.util.APIEvent;
import com.dozuki.ifixit.util.ViewServer;
import com.google.analytics.tracking.android.EasyTracker;
import com.squareup.otto.Subscribe;

/**
 * Base Activity that performs various functions that all Activities in this app
 * should do. Such as:
 * <p/>
 * Registering for the event bus. Setting the current site's theme. Finishing
 * the Activity if the user logs out but the Activity requires authentication.
 */
public abstract class BaseActivity extends SherlockFragmentActivity {
   protected static final String LOADING = "LOADING_FRAGMENT";

   /**
    * This is incredibly hacky. The issue is that Otto does not search for @Subscribed
    * methods in parent classes because the performance hit is far too big for
    * Android because of the deep inheritance with the framework and views.
    * Because of this
    *
    * @Subscribed methods on BaseActivity itself don't get registered. The
    * workaround is to make an anonymous object that is registered
    * on behalf of the parent class.
    * <p/>
    * Workaround courtesy of:
    * https://github.com/square/otto/issues/26
    * <p/>
    * Note: The '@SuppressWarnings("unused")' is to prevent
    * warnings that are incorrect (the methods *are* actually used.
    */
   private Object loginEventListener = new Object() {
      @SuppressWarnings("unused")
      @Subscribe
      public void onLoginEvent(LoginEvent.Login event) {
         onLogin(event);
      }

      @SuppressWarnings("unused")
      @Subscribe
      public void onLogoutEvent(LoginEvent.Logout event) {
         onLogout(event);
      }

      @SuppressWarnings("unused")
      @Subscribe
      public void onCancelEvent(LoginEvent.Cancel event) {
         onCancelLogin(event);
      }

      @SuppressWarnings("unused")
      @Subscribe
      public void onUnauthorized(APIEvent.Unauthorized event) {
         LoginFragment.newInstance().show(getSupportFragmentManager(), "LoginFragment");
      }
   };

   @Override
   public void onCreate(Bundle savedState) {
      /**
       * Set the current site's theme. Must be before onCreate because of
       * inflating views.
       */

      setTheme(MainApplication.get().getSiteTheme());
      setTitle("");

      super.onCreate(savedState);

      EasyTracker.getInstance().setContext(this);

      /**
       * There is another register call in onResume but we also need it here for the onUnauthorized
       * call that is usually triggered in onCreate of derived Activities.
       */
      MainApplication.getBus().register(this);
      MainApplication.getBus().register(loginEventListener);

      getSupportActionBar().setDisplayHomeAsUpEnabled(true);

      if (MainApplication.inDebug()) {
         ViewServer.get(this).addWindow(this);
      }
   }

   /**
    * If the user is coming back to this Activity make sure they still have
    * permission to view it. onRestoreInstanceState is for Activities that are
    * being recreated and onRestart is for Activities who are merely being
    * restarted. Unfortunately both are needed.
    */
   @Override
   public void onRestoreInstanceState(Bundle savedState) {
      super.onRestoreInstanceState(savedState);
      finishActivityIfPermissionDenied();
   }

   @Override
   public void onStart() {
      super.onStart();

      overridePendingTransition(0, 0);

      // Start analytics tracking
      EasyTracker.getInstance().activityStart(this);
   }

   @Override
   public void onStop() {
      super.onStop();

      // Stop analytics tracking
      EasyTracker.getInstance().activityStop(this);
   }


   @Override
   public void onRestart() {
      super.onRestart();
      finishActivityIfPermissionDenied();
   }

   @Override
   public void onResume() {
      super.onResume();

      MainApplication.getBus().register(this);
      MainApplication.getBus().register(loginEventListener);

      if (MainApplication.inDebug())
         ViewServer.get(this).setFocusedWindow(this);
   }

   @Override
   public void onDestroy() {
      super.onDestroy();

      if (MainApplication.inDebug())
         ViewServer.get(this).removeWindow(this);
   }

   @Override
   public void onPause() {
      super.onPause();

      MainApplication.getBus().unregister(this);
      MainApplication.getBus().unregister(loginEventListener);
   }

   /**
    * Left for derived classes to implement.
    */
   public void onLogin(LoginEvent.Login event) {
   }

   public void onLogout(LoginEvent.Logout event) {
      finishActivityIfPermissionDenied();
   }

   public void onCancelLogin(LoginEvent.Cancel event) {
      finishActivityIfPermissionDenied();
   }

   /**
    * Finishes the Activity if the user should be logged in but isn't.
    */
   private void finishActivityIfPermissionDenied() {
      MainApplication app = MainApplication.get();

      /**
       * Never finish if user is logged in or is logging in.
       */
      if (app.isUserLoggedIn() || app.isLoggingIn()) {
         return;
      }

      /**
       * Finish if the site is private or activity requires authentication.
       */
      if (!neverFinishActivityOnLogout()
       && (finishActivityIfLoggedOut() || !app.getSite().mPublic)) {
         finish();
      }
   }

   /**
    * "Settings" methods for derived classes are found below. Decides when to
    * finish the Activity, what icons to display etc.
    */

   /**
    * Returns true if the Activity should be finished if the user logs out or
    * cancels authentication.
    */
   public boolean finishActivityIfLoggedOut() {
      return false;
   }

   /**
    * Returns true if the Activity should never be finished despite meeting
    * other conditions.
    * <p/>
    * This exists because of a race condition of sorts involving logging out of
    * private Dozuki sites. SiteListActivity can't reset the current site to
    * one that is public so it is erroneously finished unless flagged
    * otherwise.
    */
   public boolean neverFinishActivityOnLogout() {
      return false;
   }

   public void showLoading(int container) {
      showLoading(container, getString(R.string.loading));
   }

   public void showLoading(int container, String message) {
      getSupportFragmentManager().beginTransaction()
       .add(container, new LoadingFragment(message), LOADING).addToBackStack(LOADING)
       .commit();
   }

   public void hideLoading() {
      getSupportFragmentManager().popBackStack(LOADING, FragmentManager.POP_BACK_STACK_INCLUSIVE);
   }
}
