package com.dozuki.ifixit.ui.dozuki;

import android.app.SearchManager;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.FragmentTransaction;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.actionbarsherlock.widget.SearchView;
import com.dozuki.ifixit.MainApplication;
import com.dozuki.ifixit.R;
import com.dozuki.ifixit.model.dozuki.Site;
import com.dozuki.ifixit.ui.BaseActivity;
import com.dozuki.ifixit.util.APIEvent;
import com.dozuki.ifixit.util.APIService;
import com.squareup.otto.Subscribe;

import java.util.ArrayList;

public class SiteListActivity extends BaseActivity
 implements SearchView.OnQueryTextListener {
   private static final String SITE_LIST = "SITE_LIST";
   private static final String SITE_LIST_DIALOG = "SITE_LIST_DIALOG";

   private Button mSiteListButton;
   private SiteListDialogFragment mSiteListDialog;
   private ArrayList<Site> mSiteList;

   @SuppressWarnings("unchecked")
   @Override
   public void onCreate(Bundle savedInstanceState) {
      getSupportActionBar().hide();

      super.onCreate(savedInstanceState);

      if (savedInstanceState != null) {
         mSiteList = (ArrayList<Site>)savedInstanceState.getSerializable(SITE_LIST);
      }

      if (mSiteList == null) {
         APIService.call(this, APIService.getSitesAPICall());
      }

      setTheme(R.style.Theme_Sherlock_Light);

      setContentView(R.layout.site_list);

      mSiteListButton = (Button)findViewById(R.id.list_dialog_btn);
      Typeface btnType = Typeface.createFromAsset(getAssets(), "fonts/ProximaNovaRegular.ttf");
      mSiteListButton.setTypeface(btnType);

      mSiteListButton.setOnClickListener(new OnClickListener() {
         public void onClick(View view) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();

            mSiteListDialog = SiteListDialogFragment.newInstance();
            mSiteListDialog.setSites(mSiteList, false);
            mSiteListDialog.setStyle(DialogFragment.STYLE_NO_TITLE,
             android.R.style.Theme_Holo_Light_DialogWhenLarge);
            mSiteListDialog.show(ft, SITE_LIST_DIALOG);
         }
      });

      mSiteListDialog = (SiteListDialogFragment)getSupportFragmentManager().
       findFragmentByTag(SITE_LIST_DIALOG);
   }

   @Override
   public void onSaveInstanceState(Bundle outState) {
      super.onSaveInstanceState(outState);

      outState.putSerializable(SITE_LIST, mSiteList);
   }

   @Override
   public void onResume() {
      MainApplication.get().setSite(Site.getSite("dozuki"));

      super.onResume();
   }

   @Override
   protected void onNewIntent(Intent intent) {
      setIntent(intent);
      handleIntent(intent);
   }

   @Subscribe
   public void onSites(APIEvent.Sites event) {
      if (!event.hasError()) {
         mSiteList = event.getResult();
         if (mSiteListDialog != null) {
            mSiteListDialog.setSites(mSiteList, true);
         }
      } else {
         APIService.getErrorDialog(this, event).show();
      }
   }

   private void handleIntent(Intent intent) {
      if (Intent.ACTION_SEARCH.equals(intent.getAction())) {
         String query = intent.getStringExtra(SearchManager.QUERY);
         search(query);
      }
   }

   @Override
   public boolean onQueryTextChange(String newText) {
      if (mSiteListDialog != null) {
         if (newText.length() == 0) {
            mSiteListDialog.setSites(mSiteList, true);
         } else {
            // Perform search on every key press.
            search(newText);
         }
      }

      return false;
   }

   @Override
   public boolean onQueryTextSubmit(String query) {
      return false;
   }

   private void search(String query) {
      String lowerQuery = query.toLowerCase();
      ArrayList<Site> matchedSites = new ArrayList<Site>();

      for (Site site : mSiteList) {
         if (site.search(lowerQuery)) {
            matchedSites.add(site);
         }
      }

      mSiteListDialog.setSites(matchedSites, true);
   }

   @Override
   public boolean onKeyUp(int keyCode, KeyEvent event) {
      /**
       * We want to ignore the hardware search button if the dialog doesn't handle it.
       */
      return keyCode == KeyEvent.KEYCODE_SEARCH || super.onKeyUp(keyCode, event);
   }

   @Override
   public boolean neverFinishActivityOnLogout() {
      return true;
   }
}
