package com.gcodes.iplayer.video.videos;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gcodes.iplayer.MainActivity;
import com.gcodes.iplayer.R;
import com.gcodes.iplayer.helpers.GlideApp;
import com.gcodes.iplayer.video.model.Video;
import com.gcodes.iplayer.video.VideoFragment;
import com.gcodes.iplayer.video.player.VideoPlayerActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.CursorLoader;
import androidx.loader.content.Loader;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import static android.app.Activity.RESULT_OK;
import static com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions.withCrossFade;
import static com.bumptech.glide.request.RequestOptions.centerCropTransform;

public class AllFragment extends Fragment implements VideoFragment.SectionsPagerAdapter.PageTitle, LoaderManager.LoaderCallbacks<Cursor>
{
    ArrayList<Video> videos;
    private CustomAdapter adapter;
    private ActivityResultLauncher<Intent> player;

    public AllFragment() {
    }

    // try joining audio column to media column
    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        LoaderManager.getInstance(requireActivity()).initLoader(MainActivity.AppLoader.VIDEO.getId(), null, this);
        player = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK) {
                Log.d("Video_Controller", "Rendering Video Controller");
                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).showVideoController(requireActivity().getSupportFragmentManager());
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.item_list, container, false);
        adapter = new CustomAdapter();
        RecyclerView listView = (RecyclerView) view;
        GridLayoutManager layout = new GridLayoutManager(getContext(), 2);
        listView.setLayoutManager( layout );
        listView.setAdapter(adapter);
        return view;
    }

    @Override
    public String getTitle() {
        return "Videos";
    }

    @NonNull
    @Override
    public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
        return new CursorLoader( requireContext(), MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                Video.projection, null, null, Video.sort );
    }

    @Override
    public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor cursor) {
        if (videos == null)
            videos = new ArrayList<>();
        if  ( cursor.getCount() > 0 )
        {
            cursor.moveToFirst();
            do
            {
                videos.add( Video.getInstance(cursor));
            } while ( cursor.moveToNext() );
        }
        if (adapter != null)
            adapter.notifyDataSetChanged();
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onPause() {
        super.onPause();
        FloatingActionButton floating = requireActivity().findViewById(R.id.action_floating);
        floating.hide();
    }

    @Override
    public void onResume() {
        super.onResume();
        initFloatingAction();
    }

    private void initFloatingAction()
    {
        FloatingActionButton floating = requireActivity().findViewById(R.id.action_floating);
        floating.show();
        floating.setOnClickListener( v -> {
            new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).initSource();
            Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
            String[] gsonVideos = Video.toGson(videos.toArray(new Video[]{}));
            intent.putExtra( "video", gsonVideos );
            player.launch(intent);
        });
    }

    @Override
    public void onLoaderReset(@NonNull Loader<Cursor> loader) {

    }

    public class CustomAdapter extends RecyclerView.Adapter<ItemHolder>
    {
        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.video_card, parent, false);
            return new ItemHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            Video video = videos.get(position);
            holder.setTitle(video.getName());
            holder.setSubtitle(video.getDuration());
            holder.setDate( video.getDate() );
            String path = video.getData();

            GlideApp.with( AllFragment.this ).load( path )
                    .placeholder( R.drawable.u_video2 ).apply( centerCropTransform() ).into( holder.getImage() );

            holder.itemView.setOnClickListener(v -> {
                new ViewModelProvider(requireActivity()).get(MainActivity.PlayerModel.class).initSource();
                Intent intent = new Intent(getContext(), VideoPlayerActivity.class);
                String[] gsonVideos = {video.toGson()};
                intent.putExtra( "video", gsonVideos );
                player.launch(intent);
            });
        }

        @Override
        public int getItemCount() {
            if (videos == null)
                return 0;
            return videos.size();
        }
    }

    public class ItemHolder extends RecyclerView.ViewHolder
    {
        private final TextView title;
        private final TextView subtitle;
        private final TextView date;
        private final ImageView image;

        public ItemHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.item_title);
            subtitle = itemView.findViewById(R.id.item_subtitle);
            date = itemView.findViewById(R.id.item_date);
            image = itemView.findViewById(R.id.item_image);
        }

        public String getTitle() {
            return title.getText().toString();
        }

        public void setTitle(String name) {
            this.title.setText( name );
        }

        public String getSubtitle() {
            return subtitle.getText().toString();
        }

        public void setSubtitle(String subtitle) {
            this.subtitle.setText(subtitle);
        }

        public void setDate(String subtitle) {
            this.date.setText(subtitle);
        }

        public Bitmap getImageBitmap() {
            return image.getDrawingCache();
        }

        public ImageView getImage() {
            return image;
        }
    }
}
