package com.keysafev2;

import android.content.Intent;
import android.nfc.NdefMessage;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.os.AsyncTask;
import android.os.Bundle;
import android.app.Activity;
import android.os.Parcelable;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.appspot.innocreatekey.helloworld.Helloworld;
import com.appspot.innocreatekey.helloworld.Helloworld.Greetings.Storedata;
import com.appspot.innocreatekey.helloworld.Helloworld.Greetings.Deletedata;
import com.appspot.innocreatekey.helloworld.Helloworld.Greetings.Getdata;
import com.appspot.innocreatekey.helloworld.model.DataSetCollection;
import com.appspot.innocreatekey.helloworld.model.DataSet;

import com.google.android.gms.plus.PlusClient;
import com.google.common.base.Strings;
import com.google.devrel.samples.helloendpoints.R;
import com.google.devrel.samples.helloendpoints.R.id;
import static com.google.devrel.samples.helloendpoints.BuildConfig.DEBUG;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import android.accounts.Account;
import android.accounts.AccountManager;

import com.google.android.gms.auth.GoogleAuthException;
import com.google.android.gms.auth.GoogleAuthUtil;
import com.google.android.gms.common.AccountPicker;
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;


public class MainActivity extends Activity {

    private static final String LOG_TAG = "MainActivity";
    private GreetingsDataAdapter listAdapter;

    private static final int ACTIVITY_RESULT_FROM_ACCOUNT_SELECTION = 2222;

    private AuthorizationCheckTask mAuthTask;
    private PlusClient mPlusClient;
    private String mEmailAccount = "";
    private NfcAdapter nfc;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        /*Get user credentials
        settings = getSharedPreferences("LoginActivity", 0);
        credentials = GoogleAccountCredential.usingAudience(this,AppConstants.AUDIENCE);


        // Create a Google credential since this is an authenticated request to the API.
        GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(MainActivity.this, AppConstants.AUDIENCE);
        credential.getGoogleAccountManager();
        // Retrieve service handle using credential since this is an authenticated call.
        Helloworld apiServiceHandle = AppConstants.getApiServiceHandle(credential);
        try {
            apiServiceHandle.greetings().authed();
        } catch (IOException e) {
            e.printStackTrace();
        }
        */

        // Prevent the keyboard from being visible upon startup.
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);

        ListView listView = (ListView)this.findViewById(R.id.greetings_list_view);
        listAdapter = new GreetingsDataAdapter((Application)this.getApplication());
        listView.setAdapter(listAdapter);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mAuthTask!=null) {
            mAuthTask.cancel(true);
            mAuthTask = null;
        }
    }

    public boolean hasNFC() {
        return nfc.isEnabled();
    }

    /* NFC section

    // Construct the data to write to the tag
    // Should be of the form [relay/group]-[rid/gid]-[cmd]
    String nfcMessage = relay_type + "-" + id + "-" + cmd;

    // When an NFC tag comes into range, call the main activity which handles writing the data to the tag
    NfcAdapter nfcAdapter = NfcAdapter.getDefaultAdapter(context);

    Intent nfcIntent = new Intent(context, Main.class).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    nfcIntent.putExtra("nfcMessage", nfcMessage);
    PendingIntent pi = PendingIntent.getActivity(context, 0, nfcIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    IntentFilter tagDetected = new IntentFilter(NfcAdapter.ACTION_TAG_DISCOVERED);

    nfcAdapter.enableForegroundDispatch((Activity)context, pi, new IntentFilter[] {tagDetected}, null);
    Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
*/

    /* Kan behöva flyttas till metod "onNewIntent" istället för onResume*/
    public void onNewIntent() {
        super.onNewIntent(null);
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(getIntent().getAction())) {
            Parcelable[] rawMsgs = getIntent().getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);
            if (rawMsgs != null) {
                NdefMessage[] msgs = new NdefMessage[rawMsgs.length];
                for (int i = 0; i < rawMsgs.length; i++) {
                    msgs[i] = (NdefMessage) rawMsgs[i];
                }
            }
        }
    }

    /* End of NFC section */

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == ACTIVITY_RESULT_FROM_ACCOUNT_SELECTION && resultCode == RESULT_OK) {
            // This path indicates the account selection activity resulted in the user selecting a
            // Google account and clicking OK.

            // Set the selected account.
            String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
            TextView emailAccountTextView = (TextView)this.findViewById(id.email_address_tv);
            emailAccountTextView.setText(accountName);

            // Fire off the authorization check for this account and OAuth2 scopes.
            performAuthCheck(accountName);
        }
    }


    /*
        void replaceGreeting(HelloGreeting[] greetings) {
            clear();
            for (HelloGreeting greeting : greetings) {
                add(greeting);
            }
        }
        */
    /**
     * Simple use of an ArrayAdapter but we're using a static class to ensure no references to the
     * Activity exists.
     */
    static class GreetingsDataAdapter extends ArrayAdapter {
        GreetingsDataAdapter(Application application) {
            super(application.getApplicationContext(), android.R.layout.simple_list_item_1, application.greetings);
        }

        void replaceData(DataSet[] dataSets) {
            clear();
            for (DataSet dataSet : dataSets) {
                add(dataSet);
            }
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            TextView view = (TextView)super.getView(position, convertView, parent);

            DataSet dataSet = (DataSet)this.getItem(position);

            StringBuilder sb = new StringBuilder();

            Set<String> fields = dataSet.keySet();
            boolean firstLoop = true;
            for (String fieldName : fields) {
                // Append next line chars to 2.. loop runs.
                if (firstLoop) {
                    firstLoop = false;
                } else {
                    sb.append("\n");
                }

                sb.append(fieldName)
                        .append(": ")
                        .append(dataSet.get(fieldName));
            }

            view.setText(sb.toString());
            return view;
        }
    }


    private void displayData(DataSet... dataSets) {
        String msg;
        if (dataSets==null || dataSets.length < 1) {
            msg = "No data found!";
            Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
        } else {
            if (DEBUG) {
                Log.d(LOG_TAG, "Displaying " + dataSets.length + " data.");
            }

            List<DataSet> dataSetList = Arrays.asList(dataSets);
            listAdapter.replaceData(dataSets);
        }
    }

    /**
     * This method is invoked when the "Get Data" button is clicked. See activity_main.xml for
     * the dynamic reference to this method.
     */
    public void onClickGetData(View unused) {

        if (!isSignedIn()) {
            Toast.makeText(this,"You must sign in for this action.", Toast.LENGTH_LONG).show();
            return;
        };

        // Use of an anonymous class is done for sample code simplicity. {@code AsyncTasks} should be
        // static-inner or top-level classes to prevent memory leak issues.
        // @see http://goo.gl/fN1fuE @26:00 for an great explanation.
        AsyncTask<Void, Void, DataSetCollection> getAndDisplayData =
                new AsyncTask<Void, Void, DataSetCollection> () {
                    @Override
                    protected DataSetCollection doInBackground(Void... unused) {
                        if (!AppConstants.checkGooglePlayServicesAvailable(MainActivity.this)) {
                            return null;
                        }

                        // Create a Google credential since this is an authenticated request to the API.
                        GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(MainActivity.this, AppConstants.AUDIENCE);
                        credential.setSelectedAccountName(mEmailAccount);
                        // Retrieve service handle using credential since this is an authenticated call.
                        Helloworld apiServiceHandle = AppConstants.getApiServiceHandle(credential);

                        try {
                            Getdata getDataCommand = apiServiceHandle.greetings().getdata();
                            return getDataCommand.execute();
                        } catch (IOException e) {
                            Log.e(LOG_TAG, "Exception during API call", e);
                        }
                        return null;
                    }

                    @Override
                    protected void onPostExecute(DataSetCollection dataset) {
                        if (dataset!=null && dataset.getItems()!=null) {
                            displayData(dataset.getItems().toArray(new DataSet[]{}));
                        } else {
                            Log.e(LOG_TAG, "No data was returned by the API.");
                        }
                    }
                };

        getAndDisplayData.execute((Void) null);
    }

    public void onClickSaveData(View view) {

        if (!isSignedIn()) {
            Toast.makeText(this,"You must sign in for this action.", Toast.LENGTH_LONG).show();
            return;
        };

        View rootView = view.getRootView();

        TextView dataTextInputTV = (TextView)rootView.findViewById(id.data_text_edit_text);
        if (dataTextInputTV.getText()==null ||
                Strings.isNullOrEmpty(dataTextInputTV.getText().toString())) {
            Toast.makeText(this, "Input text to save", Toast.LENGTH_SHORT).show();
            return;
        };

        final String dataMessageString = dataTextInputTV.getText().toString();


        AsyncTask<Void, Void, DataSet> sendData = new AsyncTask<Void, Void, DataSet> () {
            @Override
            protected DataSet doInBackground(Void... unused) {

                if (!isSignedIn()) {
                    DataSet dataSet = new DataSet();
                    dataSet.setMessage("You must be logged in to access data storage.");
                    return dataSet;
                };

                if (!AppConstants.checkGooglePlayServicesAvailable(MainActivity.this)) {
                    return null;
                }

                // Create a Google credential since this is an authenticated request to the API.
                GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(MainActivity.this, AppConstants.AUDIENCE);
                credential.setSelectedAccountName(mEmailAccount);
                // Retrieve service handle using credential since this is an authenticated call.
                Helloworld apiServiceHandle = AppConstants.getApiServiceHandle(credential);

                try {
                    DataSet dataSet = new DataSet();
                    dataSet.setMessage("Great put!");

                    Storedata storedataDataSetCommand = apiServiceHandle.greetings().storedata(dataMessageString);
                    storedataDataSetCommand.execute();
                    return dataSet;
                } catch (IOException e) {
                    Log.e(LOG_TAG, "Exception during API call", e);

                }
                return null;
            }

            @Override
            protected void onPostExecute(DataSet dataSet) {
                if (dataSet!=null) {
                    displayData(dataSet);
                } else {
                    Log.e(LOG_TAG, "No greetings were returned by the API.");
                }
            }
        };

        sendData.execute((Void)null);
    }

    public void onClickSignIn(View view) {
        TextView emailAddressTV = (TextView) view.getRootView().findViewById(id.email_address_tv);
        // Check to see how many Google accounts are registered with the device.
        int googleAccounts = AppConstants.countGoogleAccounts(this);
        if (googleAccounts == 0) {
            // No accounts registered, nothing to do.
            Toast.makeText(this, R.string.toast_no_google_accounts_registered,
                    Toast.LENGTH_LONG).show();
        } else if (googleAccounts == 1) {
            // If only one account then select it.
            Toast.makeText(this, R.string.toast_only_one_google_account_registered,
                    Toast.LENGTH_LONG).show();
            AccountManager am = AccountManager.get(this);
            Account[] accounts = am.getAccountsByType(GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE);
            if (accounts != null && accounts.length > 0) {
                // Select account and perform authorization check.
                emailAddressTV.setText(accounts[0].name);
                mEmailAccount = accounts[0].name;
                performAuthCheck(accounts[0].name);
            }
        } else {
            // More than one Google Account is present, a chooser is necessary.

            // Reset selected account.
            emailAddressTV.setText("");

            // Invoke an {@code Intent} to allow the user to select a Google account.
            Intent accountSelector = AccountPicker.newChooseAccountIntent(null, null,
                    new String[]{GoogleAuthUtil.GOOGLE_ACCOUNT_TYPE}, false,
                    "Select the account to access Google Compute Engine API.", null, null, null);
            startActivityForResult(accountSelector,
                    ACTIVITY_RESULT_FROM_ACCOUNT_SELECTION);
        }

    }

    public void onClickSignOut(View view) {
        if(mAuthTask != null) {
            try {
                mAuthTask.cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        Intent intent = AccountPicker.newChooseAccountIntent(null, null, new String[] {"com.google"}, false, null, null, null, null);
        startActivityForResult(intent, 6789);
        Intent myIntent = new Intent(this,LoginActivity.class);
        startActivity(myIntent);
    }

    private boolean isSignedIn() {
        if (!Strings.isNullOrEmpty(mEmailAccount)) {
            return true;
        } else {
            return false;
        }
    }

    public void performAuthCheck(String emailAccount) {
        // Cancel previously running tasks.
        if (mAuthTask != null) {
            try {
                mAuthTask.cancel(true);
            } catch (Exception e) {
                e.printStackTrace();
            }
            return;
        }
    }

        class AuthorizationCheckTask extends AsyncTask<String, Integer, Boolean> {
            @Override
            protected Boolean doInBackground(String... emailAccounts) {
                Log.i(LOG_TAG, "Background task started.");

                if (!AppConstants.checkGooglePlayServicesAvailable(MainActivity.this)) {
                    return false;
                }

                String emailAccount = emailAccounts[0];
                // Ensure only one task is running at a time.
                mAuthTask = this;

                // Ensure an email was selected.
                if (Strings.isNullOrEmpty(emailAccount)) {
                    publishProgress(R.string.toast_no_google_account_selected);
                    // Failure.
                    return false;
                }

                if (DEBUG) {
                    Log.d(LOG_TAG, "Attempting to get AuthToken for account: " + mEmailAccount);
                }

                try {
                    // If the application has the appropriate access then a token will be retrieved, otherwise
                    // an error will be thrown.
                    GoogleAccountCredential credential = GoogleAccountCredential.usingAudience(
                            MainActivity.this, AppConstants.AUDIENCE);
                    credential.setSelectedAccountName(emailAccount);

                    String accessToken = credential.getToken();

                    if (DEBUG) {
                        Log.d(LOG_TAG, "AccessToken retrieved");
                    }

                    // Success.
                    return true;
                } catch (GoogleAuthException unrecoverableException) {
                    Log.e(LOG_TAG, "Exception checking OAuth2 authentication.", unrecoverableException);
                    publishProgress(R.string.toast_exception_checking_authorization);
                    // Failure.
                    return false;
                } catch (IOException ioException) {
                    Log.e(LOG_TAG, "Exception checking OAuth2 authentication.", ioException);
                    publishProgress(R.string.toast_exception_checking_authorization);
                    // Failure or cancel request.
                    return false;
                }
            }

            @Override
            protected void onProgressUpdate(Integer... stringIds) {
                // Toast only the most recent.
                Integer stringId = stringIds[0];
                Toast.makeText(MainActivity.this, stringId, Toast.LENGTH_SHORT).show();
            }

            @Override
            protected void onPreExecute() {
                mAuthTask = this;
            }

            @Override
            protected void onPostExecute(Boolean success) {
                TextView emailAddressTV = (TextView) MainActivity.this.findViewById(id.email_address_tv);
                if (success) {
                    // Authorization check successful, set internal variable.
                    mEmailAccount = emailAddressTV.getText().toString();
                } else {
                    // Authorization check unsuccessful, reset TextView to empty.
                    emailAddressTV.setText("");
                }
                mAuthTask = null;
            }

            @Override
            protected void onCancelled() {
                mAuthTask = null;
            }
        }
}