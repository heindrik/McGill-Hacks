package com.mchacks.tapinto;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.Locale;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentFilter.MalformedMimeTypeException;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.Ndef;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends FragmentActivity implements ActionBar.TabListener {
	
	/*
	 * Heindrik Stuff: NFC declaration
	 */
    public static final String TAG = "TapInto";
    private NfcAdapter mNfcAdapter;
    public static final String MIME_TEXT_PLAIN = "text/plain";
    public String NFCData ="";
    
    /**
     * The {@link android.support.v4.view.PagerAdapter} that will provide
     * fragments for each of the sections. We use a
     * {@link android.support.v4.app.FragmentPagerAdapter} derivative, which
     * will keep every loaded fragment in memory. If this becomes too memory
     * intensive, it may be best to switch to a
     * {@link android.support.v4.app.FragmentStatePagerAdapter}.
     */
    SectionsPagerAdapter mSectionsPagerAdapter;

    /**
     * The {@link ViewPager} that will host the section contents.
     */
    ViewPager mViewPager;
    
    public ActionBar actionBar;
    
    /*
     * UI declarations
     */
    public static TextView contentView;
    public static Button restaurant_button;
    
    /*
     * onCreate function
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
    	Log.i(TAG,"1. CREATED");
    	super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        actionBarUpdate("initial");
        
     //Log.i(TAG,"ActionBar Setup Complete");

        // Create the adapter that will return a fragment for each of the three
        // primary sections of the app.
        mSectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());

        // Set up the ViewPager with the sections adapter.
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mSectionsPagerAdapter);
        
        // Set up NFC Adapter
        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
        
        /*
		 * Check if NFC Exist with the phone
		 */
		if (mNfcAdapter == null){
			Toast.makeText(this, "This device doesn't support NFC.", Toast.LENGTH_LONG).show();
            finish();
            return;
		}
		
		Log.i(TAG,"NFC Exists and is Enabled");

        // When swiping between different sections, select the corresponding
        // tab. We can also use ActionBar.Tab#select() to do this if we have
        // a reference to the Tab.
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
            	Log.i(TAG,"onPageSelected Called: " + position);
                actionBar.setSelectedNavigationItem(position);
            }
            
        });

        // For each of the sections in the app, add a tab to the action bar.
        for (int i = 0; i < mSectionsPagerAdapter.getCount(); i++) {
            // Create a tab with text corresponding to the page title defined by
            // the adapter. Also specify this Activity object, which implements
            // the TabListener interface, as the callback (listener) for when
            // this tab is selected.
            actionBar.addTab(
                             actionBar.newTab()
                            .setText(mSectionsPagerAdapter.getPageTitle(i))
                            .setTabListener(this));
        }        
        
        //handle Intent
        handleIntent(getIntent());
        
    }// End of OnCreate
    
    public void actionBarUpdate(String temp){
    	Log.i(TAG,"actionBarUpdate temp: "+temp);
    	if (temp.equals("initial")){
    	   // Set up the action bar.
           actionBar = getActionBar();
           actionBar.setBackgroundDrawable(new ColorDrawable(Color.parseColor("#86d142")));
           actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
    	}else if (temp.equals("Restaurant")){
    		actionBar.getTabAt(0).setText("Restaurant");
    		actionBar.getTabAt(1).setText("Menu");
    		actionBar.getTabAt(2).setText("Reviews");
    	}else if (temp.equals("STM")){
    		actionBar.getTabAt(0).setText("STM");
    		actionBar.getTabAt(1).setText("Next Arrival");
    		actionBar.getTabAt(2).setText("Route");
    	}
    }
    
    

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        

        return true;
    }//End of onCreateOptionsMenu
    
    /*
     * Activity Methods
     */
    
    @Override
    protected void onStart(){
    	super.onStart();
    	Log.i(TAG,"2. STARTED");
    }
    
    @Override
    protected void onRestart(){
    	super.onRestart();
    	Log.i(TAG,"2. RE-STARTED");
    }

    @Override
    protected void onResume() {
        super.onResume();
         
        /**
         * It's important, that the activity is in the foreground (resumed). Otherwise
         * an IllegalStateException is thrown. 
         */
        Log.i(TAG,"3. RESUMED");
        setupForegroundDispatch(this, mNfcAdapter);
    }
    @Override
    protected void onPause() {
        /**
         * Call this before onPause, otherwise an IllegalArgumentException is thrown as well.
         */
    	Log.i(TAG,"4. PAUSED");
        stopForegroundDispatch(this, mNfcAdapter);
         
        super.onPause();
    }
    @Override
    protected void onStop(){
    	super.onStop();
    	finish();
    	Log.i(TAG,"5. STOPPED");
    }
    @Override
    protected void onDestroy(){
    	super.onDestroy();
    	Log.i(TAG,"6. DESTROYED");
    }
    
    /*
     * 
     */
    
    /**
     * @param activity The corresponding {@link Activity} requesting the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void setupForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        final Intent intent = new Intent(activity.getApplicationContext(), activity.getClass());
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
 
        final PendingIntent pendingIntent = PendingIntent.getActivity(activity.getApplicationContext(), 0, intent, 0);
 
        IntentFilter[] filters = new IntentFilter[1];
        String[][] techList = new String[][]{};
 
        // Notice that this is the same filter as in our manifest.
        filters[0] = new IntentFilter();
        filters[0].addAction(NfcAdapter.ACTION_NDEF_DISCOVERED);
        filters[0].addCategory(Intent.CATEGORY_DEFAULT);
        try {
            filters[0].addDataType(MIME_TEXT_PLAIN);
        } catch (MalformedMimeTypeException e) {
            throw new RuntimeException("Check your mime type.");
        }
         
        adapter.enableForegroundDispatch(activity, pendingIntent, filters, techList);
    }
    /**
     * @param activity The corresponding {@link BaseActivity} requesting to stop the foreground dispatch.
     * @param adapter The {@link NfcAdapter} used for the foreground dispatch.
     */
    public static void stopForegroundDispatch(final Activity activity, NfcAdapter adapter) {
        adapter.disableForegroundDispatch(activity);
    }    

    /*
     * Heindrik: NFC Implementation
     */
    @Override
    protected void onNewIntent(Intent intent) { 
        /**
         * This method gets called, when a new Intent gets associated with the current activity instance.
         * Instead of creating a new activity, onNewIntent will be called. For more information have a look
         * at the documentation.
         * 
         * In our case this method gets called, when the user attaches a Tag to the device.
         */
        handleIntent(intent);
    }
    private void handleIntent(Intent intent){
    	String action = intent.getAction();
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(action)) {
             
            String type = intent.getType();
            if (MIME_TEXT_PLAIN.equals(type)) {
            	Log.i(TAG,"ACTION_NDEF_DISCOVERED");
                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
                new NdefReaderTask().execute(tag);
                
            } else {
                Log.d(TAG, "Wrong mime type: " + type);
            }
        }
    }
    

    
    
    /* 
     * Aldrich:
     * Tab Stuff
     */
    
    @Override
    public void onTabSelected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        // When the given tab is selected, switch to the corresponding page in
   
        // the ViewPager.
        mViewPager.setCurrentItem(tab.getPosition());
    }//onTabSelected

    @Override
    public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
        
        restaurant_button = (Button) mViewPager.findViewById(R.id.restaurant_button);
        
        restaurant_button.setOnClickListener(new Button.OnClickListener(){  
            public void onClick(View v)
            {
                Intent newIntent = new Intent (MainActivity.this, RestaurantActivity.class);
                startActivity(newIntent);
            }
         });
    }//onTabUnselected

    @Override
    public void onTabReselected(ActionBar.Tab tab, FragmentTransaction fragmentTransaction) {
 
        
        restaurant_button.setOnClickListener(new Button.OnClickListener(){  
            public void onClick(View v)
            {
                Intent newIntent = new Intent (MainActivity.this, RestaurantActivity.class);
                startActivity(newIntent);
            }
         });
    }//onTabReselected

  

 

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        public SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a DummySectionFragment (defined as a static inner class
            // below) with the page number as its lone argument.
        	Fragment fragment;
        	if (position==0){
            fragment = new SFragment1();
            Bundle args = new Bundle();
            args.putInt(SFragment1.ARG_SECTION_NUMBER, position + 1);
            fragment.setArguments(args);
            return fragment;
        	}
        	if (position==1){
            fragment = new SFragment2();
            Bundle args = new Bundle();
            args.putInt(SFragment2.ARG_SECTION_NUMBER, position + 1);
            fragment.setArguments(args);
            return fragment;
        	}
            fragment = new SFragment3();
            Bundle args = new Bundle();
            args.putInt(SFragment2.ARG_SECTION_NUMBER, position + 1);
            fragment.setArguments(args);
            return fragment;
        	
        	
        }

        @Override
        public int getCount() {
            // Show 3 total pages.
            return 3;
        }

        //sets the title of each tab
        @Override
        public CharSequence getPageTitle(int position) {
            Locale l = Locale.getDefault();
            switch (position) {
                case 0:
                    return getString(R.string.tap_section).toUpperCase(l);
                case 1:
                    return getString(R.string.history_section).toUpperCase(l);
                case 2:
                    return getString(R.string.explore_section).toUpperCase(l);
            }
            return null;
        }//Char Sequence
    }//SectionsPagerAdapter

  
    /*
     * Fragments  
     */
    public static class SFragment1 extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        public static final String ARG_SECTION_NUMBER = "section_number";

        public SFragment1() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
        	Log.i(TAG,"SFragment1");
        	View rootView = inflater.inflate(R.layout.fragment_main_dummy, container, false);
            TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
            dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            contentView = (TextView) rootView.findViewById(R.id.contentView);	

            return rootView; 
        }
    }
    
    public static class SFragment2 extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        public static final String ARG_SECTION_NUMBER = "section_number";

        public SFragment2() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment2, container, false);
            TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
            dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            	

            return rootView; 
        }
    }
    public static class SFragment3 extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        public static final String ARG_SECTION_NUMBER = "section_number";

        public SFragment3() {
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            View rootView = inflater.inflate(R.layout.fragment3, container, false);
            TextView dummyTextView = (TextView) rootView.findViewById(R.id.section_label);
            dummyTextView.setText(Integer.toString(getArguments().getInt(ARG_SECTION_NUMBER)));
            	

            return rootView; 
        }
    }
    
    /*
     * Inner CLASS NdefReader
     */
 // ================
    /**
     * Background task for reading the data. Do not block the UI thread while reading. 
     * 
     * @author Ralf Wondratschek
     *
     */
    private class NdefReaderTask extends AsyncTask<Tag, Void, String> {
        
        @Override
        protected String doInBackground(Tag... params) {
            Tag tag = params[0];
             
            Ndef ndef = Ndef.get(tag);
            Log.i(TAG,"NdefReaderTask");
            if (ndef == null) {
                // NDEF is not supported by this Tag.
            	Log.i(TAG,"NDEF_NOT SUPPORTED");
                return null;
            }
     
            NdefMessage ndefMessage = ndef.getCachedNdefMessage();
     
            NdefRecord[] records = ndefMessage.getRecords();
            for (NdefRecord ndefRecord : records) {
            	Log.i(TAG,"Checking Records!");
            	if(ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN){
            		Log.i(TAG,"TNF_WELL_KNOWN!");
            		try{
            			return readText(ndefRecord);
            		}catch(UnsupportedEncodingException e) {
                    	
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
            	}else if (ndefRecord.getTnf() == NdefRecord.TNF_MIME_MEDIA){
            		Log.i(TAG,"MIME_MEDIA!");
            		
            	}
            	
                if (ndefRecord.getTnf() == NdefRecord.TNF_WELL_KNOWN && Arrays.equals(ndefRecord.getType(), NdefRecord.RTD_TEXT)) {
                	Log.i(TAG,"PASSED CONDITION!");
                	try {
                        return readText(ndefRecord);
                    } catch (UnsupportedEncodingException e) {
                    	
                        Log.e(TAG, "Unsupported Encoding", e);
                    }
                }
            }
     
            return null;
        }
         
        private String readText(NdefRecord record) throws UnsupportedEncodingException {
            /*
             * See NFC forum specification for "Text Record Type Definition" at 3.2.1 
             * 
             * http://www.nfc-forum.org/specs/
             * 
             * bit_7 defines encoding
             * bit_6 reserved for future use, must be 0
             * bit_5..0 length of IANA language code
             */
        	Log.i(TAG,"** Reading Text **");
            byte[] payload = record.getPayload();
     
            // Get the Text Encoding
            String textEncoding = ((payload[0] & 128) == 0) ? "UTF-8" : "UTF-16";
     
            // Get the Language Code
            int languageCodeLength = payload[0] & 0063;
             
            // String languageCode = new String(payload, 1, languageCodeLength, "US-ASCII");
            // e.g. "en"
             
            // Get the Text
            return new String(payload, languageCodeLength + 1, payload.length - languageCodeLength - 1, textEncoding);
        }
         
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
            	NFCData = result;
                contentView.setText(result);
                actionBarUpdate(NFCData);
                Log.i(TAG,"NFC has content! Data: " + NFCData);
            }else{
            	Log.i(TAG,"** Result is NULL **");
            	contentView.setText("NFC is not compatible");
            }
            
            
        }
    }//end of reader class
    
    
}//MainActivity
