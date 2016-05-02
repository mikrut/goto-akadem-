package ru.mail.park.chat.activities;

import android.app.LoaderManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.Loader;
import android.net.Uri;
import android.os.Environment;
import android.support.design.widget.AppBarLayout;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.support.design.widget.CollapsingToolbarLayout;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import java.io.InputStream;

import ru.mail.park.chat.R;
import ru.mail.park.chat.activities.tasks.AddContactTask;
import ru.mail.park.chat.database.ContactHelper;
import ru.mail.park.chat.loaders.ProfileWebLoader;
import ru.mail.park.chat.models.Contact;
import ru.mail.park.chat.models.OwnerProfile;

public class ProfileViewActivity extends AppCompatActivity {
    public static final String UID_EXTRA = ProfileViewActivity.class.getCanonicalName() + ".UID_EXTRA";
    public static final String SERVER_URL = "http://p30480.lab1.stud.tech-mail.ru/";
    private final static int DB_LOADER = 0;
    private final static int WEB_LOADER = 1;
    private final static int WEB_OWN_LOADER = 2;

    private String uid;

    private CollapsingToolbarLayout toolbarLayout;
    private Toolbar toolbar;
    private AppBarLayout appBar;
    private FloatingActionButton userAddToContacts;
    private FloatingActionButton userSendMessage;

    private ImageView userPicture;
    private TextView userLogin;
    private TextView userEmail;
    private TextView userPhone;
    private TextView onlineIndicator;
    private LinearLayout profileDataLayout;

    private ProgressBar progressBar;

    private Contact contact;
    private Contact.Relation relation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        toolbarLayout = (CollapsingToolbarLayout) findViewById(R.id.toolbar_layout);
        toolbarLayout.setTitle("Loading...");

        toolbar = (Toolbar) findViewById(R.id.toolbar);
        appBar = (AppBarLayout) findViewById(R.id.app_bar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        userAddToContacts = (FloatingActionButton) findViewById(R.id.user_add_to_contacts);
        userSendMessage = (FloatingActionButton) findViewById(R.id.user_send_message);

        userPicture = (ImageView) findViewById(R.id.user_picture);
        userLogin = (TextView) findViewById(R.id.user_login);
        userEmail = (TextView) findViewById(R.id.user_email);
        userPhone = (TextView) findViewById(R.id.user_phone);
        onlineIndicator = (TextView) findViewById(R.id.online_indicator);
        profileDataLayout = (LinearLayout) findViewById(R.id.profileDataLayout);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        OwnerProfile owner = new OwnerProfile(this);
        if (getIntent().hasExtra(UID_EXTRA)) {
            uid = getIntent().getStringExtra(UID_EXTRA);
        } else {
            uid = owner.getUid();
        }

        int loaderType;
        if (uid.equals(owner.getUid())) {
            setUserData(owner, Contact.Relation.SELF);
            loaderType = WEB_OWN_LOADER;
        } else {
            ContactHelper contactHelper = new ContactHelper(this);
            Contact profile = contactHelper.getContact(uid);
            if (profile != null) {
                setUserData(profile, Contact.Relation.FRIEND);
            }
            loaderType = WEB_LOADER;
        }

        Bundle args = new Bundle();
        args.putString(ProfileWebLoader.UID_ARG, uid);
        getLoaderManager().initLoader(loaderType, args, contactsLoaderListener);

        userAddToContacts.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AddContactTask(ProfileViewActivity.this).execute(uid);
            }
        });

        userSendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(ProfileViewActivity.this, DialogActivity.class);
                intent.putExtra(DialogActivity.USER_ID, uid);
                startActivity(intent);
                finish();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        // getMenuInflater().inflate(R.menu.menu_profile_view, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        menu.clear();
        if (relation != null) {
            switch (relation) {
                case FRIEND:
                    getMenuInflater().inflate(R.menu.menu_profile_view, menu);
                    break;
                case SELF:
                    getMenuInflater().inflate(R.menu.menu_owner_profile, menu);
                    break;
                case OTHER:
                    break;
            }
        }
        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        switch (id) {
            case R.id.action_edit_contact:
                Intent intent = new Intent(this, ProfileEditActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_edit_onion: {
                View editView = LayoutInflater.from(this)
                        .inflate(R.layout.dialog_edit_text, null);
                final EditText editText = (EditText) editView.findViewById(R.id.edittext);
                if (contact.getOnionAddress() != null)
                    editText.setText(contact.getOnionAddress().toString());
                new AlertDialog.Builder(this)
                        .setTitle("Input onion address")
                        .setView(editView)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String onionAddress = editText.getText().toString();
                                        onionAddress = onionAddress.replaceAll("\\s", "");
                                        contact.setOnionAddress(onionAddress);
                                        ContactHelper contactHelper = new ContactHelper(editText.getContext());
                                        contactHelper.updateContact(contact);
                                    }
                                }).create().show();
                return true;
            }
            case R.id.action_edit_pubkey: {
                View editView = LayoutInflater.from(this)
                        .inflate(R.layout.dialog_edit_text, null);
                final EditText editText = (EditText) editView.findViewById(R.id.edittext);
                editText.setText(contact.getPubkeyDigestString());
                new AlertDialog.Builder(this)
                        .setTitle("Input public key")
                        .setView(editView)
                        .setPositiveButton(android.R.string.ok,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        String publicKeyDigest = editText.getText().toString();
                                        contact.setPubkeyDigest(publicKeyDigest);
                                        ContactHelper contactHelper = new ContactHelper(editText.getContext());
                                        contactHelper.updateContact(contact);
                                    }
                                }).create().show();
                return true;
            }
        }

        return super.onOptionsItemSelected(item);
    }

    private void setUserData(Contact user, Contact.Relation relation) {
        contact = user;

        toolbarLayout.setTitle(user.getContactTitle());
        userLogin.setText(user.getLogin());

        if (user.getEmail() != null) {
            userEmail.setText(user.getEmail());
            userEmail.setVisibility(View.VISIBLE);
        } else {
            userEmail.setVisibility(View.GONE);
        }

        Log.d("[TP-diploma]", "starting get image task");
        new DownloadImageTask(userPicture).execute(SERVER_URL + user.getImg());

        Calendar lastSeen = user.getLastSeen();

        if(relation != Contact.Relation.SELF) {
            if (user.isOnline())
                onlineIndicator.setText("online");
            else if(lastSeen != null)
                onlineIndicator.setText(lastSeen.getTime().toGMTString());
            else
                onlineIndicator.setText("offline");
        }

        if (user.getPhone() != null) {
            userPhone.setText(user.getPhone());
            userPhone.setVisibility(View.VISIBLE);
        } else {
            userPhone.setVisibility(View.GONE);
        }

        this.relation = relation;

        if (relation != null) {
            switch (relation) {
                case FRIEND:
                    userAddToContacts.setVisibility(View.INVISIBLE);
                    userSendMessage.setVisibility(View.VISIBLE);
                    break;
                case SELF:
                    userAddToContacts.setVisibility(View.INVISIBLE);
                    userSendMessage.setVisibility(View.INVISIBLE);
                    break;
                case OTHER:
                    userAddToContacts.setVisibility(View.VISIBLE);
                    userSendMessage.setVisibility(View.VISIBLE);
                    break;
            }
        }

        profileDataLayout.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.GONE);
        invalidateOptionsMenu();
    }

    private final LoaderManager.LoaderCallbacks<Contact> contactsLoaderListener =
            new LoaderManager.LoaderCallbacks<Contact>() {
                @Override
                public Loader<Contact> onCreateLoader(int id, Bundle args) {
                    return new ProfileWebLoader(ProfileViewActivity.this, id, args);
                }

                @Override
                public void onLoadFinished(Loader<Contact> loader, Contact data) {
                    if (data != null) {
                        Contact.Relation relation = Contact.Relation.OTHER;
                        String ownerUid = (new OwnerProfile(ProfileViewActivity.this)).getUid();
                        if (data.getUid().equals(ownerUid))
                            relation = Contact.Relation.SELF;
                        else {
                            ContactHelper contactHelper = new ContactHelper(ProfileViewActivity.this);
                            if (contactHelper.getContact(data.getUid()) != null) {
                                relation = Contact.Relation.FRIEND;
                            }
                        }
                        setUserData(data, relation);
                    }
                }

                @Override
                public void onLoaderReset(Loader<Contact> loader) {
                    // TODO: something...
                }
            };

    private class DownloadImageTask extends AsyncTask<String, Void, Bitmap> {
        ImageView bmImage;

        public DownloadImageTask(ImageView bmImage) {
            this.bmImage = bmImage;
        }

        protected Bitmap doInBackground(String... urls) {
            Log.d("[TP-diploma]", "task is working");
            String urldisplay = urls[0];
            Bitmap mIcon11 = null;
            try {
                InputStream in = new java.net.URL(urldisplay).openStream();
                mIcon11 = BitmapFactory.decodeStream(in);
            } catch (Exception e) {
                Log.e("Error", e.getMessage());
                e.printStackTrace();
            }
            return mIcon11;
        }

        protected void onPostExecute(Bitmap result) {
            String filePath = Environment.getExternalStorageDirectory() + "/torchat/avatars/users/" + uid + ".bmp";
            File file = new File(filePath);

            if (result != null) {
                Log.d("[TP-diploma]", "DownloadImageTask result not null");
                bmImage.setImageBitmap(result);
                try {
                    FileOutputStream fos = new FileOutputStream(file);
                    result.compress(Bitmap.CompressFormat.PNG, 90, fos);
                    fos.close();
                } catch (FileNotFoundException e) {
                    Log.d("[TP-diploma]", "File not found: " + e.getMessage());
                } catch (IOException e) {
                    Log.d("[TP-diploma]", "Error accessing file: " + e.getMessage());
                }
            } else {
                Log.d("[TP-diploma]", "DownloadImageTask result NULL");
                if(file.exists()) {
                    Log.d("[TP-diploma]", "DownloadImageTask file exists: " + file.getAbsolutePath());
                    bmImage.setImageURI(Uri.parse(filePath));
                }
                else {
                    Log.d("[TP-diploma]", "DownloadImageTask file do not exist");
                    bmImage.setImageDrawable(getResources().getDrawable(R.drawable.ic_user_picture));
                }
            }
        }
    }
}