package com.andrewnitu.githubmarkdownviewer.fragment;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.andrewnitu.githubmarkdownviewer.R;
import com.andrewnitu.githubmarkdownviewer.activity.RepoActivity;
import com.andrewnitu.githubmarkdownviewer.adapter.ClickListener;
import com.andrewnitu.githubmarkdownviewer.adapter.RepoListAdapter;
import com.andrewnitu.githubmarkdownviewer.model.db.RealmRepo;
import com.andrewnitu.githubmarkdownviewer.model.local.Repo;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.UUID;

import io.realm.Realm;
import io.realm.RealmQuery;
import io.realm.RealmResults;

public class UserSelectFragment extends Fragment implements ClickListener {
    final String baseUrl = "https://api.github.com";

    private EditText usernameBox;
    private RecyclerView recyclerView;
    private ArrayList<Repo> repos;
    private RepoListAdapter adapter;
    private String username;
    private View rootView;

    private Realm realmInstance;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        rootView = inflater.inflate(R.layout.fragment_user_select, container, false);

        // Get our Realm instance
        realmInstance = Realm.getDefaultInstance();

        rootView.findViewById(R.id.loading_panel).setVisibility(View.GONE);

        // Initialize empty Repo array which will be loaded into
        repos = new ArrayList<Repo>();

        // Bind our UI elements
        usernameBox = (EditText) rootView.findViewById(R.id.edit_text);
        recyclerView = (RecyclerView) rootView.findViewById(R.id.recycler_view);

        // Set up our submit button
        View.OnClickListener btnSubmitClickListener = new View.OnClickListener() {
            public void onClick(View v) {
                retrieveRepos(v);
            }
        };
        Button btnSubmit = (Button) rootView.findViewById(R.id.submit);
        btnSubmit.setOnClickListener(btnSubmitClickListener);

        LinearLayoutManager llm = new LinearLayoutManager(this.getContext());
        llm.setOrientation(LinearLayoutManager.VERTICAL);
        recyclerView.setLayoutManager(llm);

        // Set up RecyclerView row dividers
        DividerItemDecoration mDividerItemDecoration = new DividerItemDecoration(recyclerView.getContext(),
                llm.getOrientation());
        recyclerView.addItemDecoration(mDividerItemDecoration);

        // Give our RecyclerView an adapter
        adapter = new RepoListAdapter(repos);
        recyclerView.setAdapter(adapter);

        adapter.setClickListener(this);

        return rootView;
    }

    public void repoRequest(final String reqUsername) {
        // Instantiate the RequestQueue
        RequestQueue queue = Volley.newRequestQueue(getContext());

        // Create the URL to request the repositories for a user
        String requestURL = baseUrl + "/users/" + reqUsername + "/repos";

        // Request a string response from the provided URL
        JsonArrayRequest stringRequest = new JsonArrayRequest(requestURL,
                new Response.Listener<JSONArray>() {
                    // Do on a successful request
                    @Override
                    public void onResponse(JSONArray response) {
                        // If successful, clear the current repo list to make way for the new one
                        repos.clear();
                        username = reqUsername;

                        try {
                            int numExtracted = 0;

                            // For each repo
                            while (numExtracted < response.length()) {
                                // Retrieve the repository name
                                String repoName = response.getJSONObject(numExtracted).getString("name");

                                // TODO: Add the URL property
                                repos.add(new Repo(repoName, reqUsername));
                                numExtracted++;
                            }
                        } catch (JSONException e) {
                        }

                        rootView.findViewById(R.id.loading_panel).setVisibility(View.GONE);

                        // Update the RecyclerView (don't wait for the user to)
                        adapter.notifyDataSetChanged();
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        rootView.findViewById(R.id.loading_panel).setVisibility(View.GONE);

                        // Give an error!
                        Toast toast = Toast.makeText(getActivity().getApplicationContext(), "Couldn't find that user!", Toast.LENGTH_LONG);
                        toast.show();

                        usernameBox.setText("");
                    }
                });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);
    }

    public void retrieveRepos(View view) {
        // Close the keyboard (using magic)
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }

        rootView.findViewById(R.id.loading_panel).setVisibility(View.VISIBLE);

        repoRequest(usernameBox.getText().toString());
    }

    @Override
    public void onRowClicked(View view, int index) {
        Intent intent = new Intent(getActivity(), RepoActivity.class);
        intent.putExtra("Username", username);
        intent.putExtra("Reponame", repos.get(index).getName());

        startActivity(intent);
    }

    @Override
    public void onFavouriteClicked(View view, int index) {
        RealmQuery<RealmRepo> repoQuery = realmInstance.where(RealmRepo.class).equalTo("name", repos.get(index).getName()).equalTo("ownerUserName", repos.get(index).getOwnerUserName());

        RealmResults<RealmRepo> repoResults = repoQuery.findAll();

        int numResults = repoResults.size();

        realmInstance.beginTransaction();
        if (numResults == 0) {
            Log.d("Realm Transaction", "Added Repo object");
            RealmRepo repo = realmInstance.createObject(RealmRepo.class, UUID.randomUUID().toString());
            repo.setName(repos.get(index).getName());
            repo.setOwnerUserName(repos.get(index).getOwnerUserName());
            switchFavouritesIcon(true, view);
        }
        else {
            Log.d("Realm Transaction", "Removed Repo object");
            repoResults.first().deleteFromRealm();
            switchFavouritesIcon(false, view);
        }
        realmInstance.commitTransaction();
    }

    // Switches the favourites icon in the view
    // Takes a state (true for favourited, false for unfavourited) and the View of the row clicked
    public void switchFavouritesIcon(boolean state, View view) {
        ImageView icon = (ImageView) view.findViewById(R.id.favourite_icon);
        if (state) {
            icon.setImageResource(R.drawable.ic_star_filled);
        }
        else {
            icon.setImageResource(R.drawable.ic_star_empty);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        realmInstance.close();
    }
}