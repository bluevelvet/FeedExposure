package com.kwai.feedexposure;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

  private RecyclerView mRecycerView;
  private PhotoExposureCalculator mPhotoExposureCalculator = new PhotoExposureCalculator();

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);
    mRecycerView = findViewById(R.id.recycler_view);
    StaggeredGridLayoutManager layoutManager =
        new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
    mRecycerView.setLayoutManager(layoutManager);
    MyAdapter adapter = new MyAdapter(this, getListItemData());
    mRecycerView.setAdapter(adapter);
    mRecycerView.addOnScrollListener(mPhotoExposureCalculator.getOnScrollListener());
    getLifecycle().addObserver(mPhotoExposureCalculator);
  }

  private List<ItemObject> getListItemData() {
    List<ItemObject> listViewItems = new ArrayList<>();
    for (int i = 0; i < 10000; i++) {
      int l = String.valueOf(i).length();
      StringBuilder randomName = new StringBuilder();
      StringBuilder randomAuthor = new StringBuilder();
      int k = ThreadLocalRandom.current().nextInt(200);
      for (int j = 0; j < k; j += l) {
        randomName.append(String.valueOf(i));
      }
      k = ThreadLocalRandom.current().nextInt(500);
      for (int j = 0; j < k; j += l) {
        randomAuthor.append(String.valueOf(i));
      }
      listViewItems.add(new ItemObject(randomName.toString(), randomAuthor.toString()));
    }
    return listViewItems;
  }

  public static final class MyAdapter extends RecyclerView.Adapter<SampleViewHolders> {
    private List<ItemObject> mItems;
    private Context mContext;

    public MyAdapter(Context context, List<ItemObject> data) {
      mItems = data;
      mContext = context;
    }

    @NonNull
    @Override
    public SampleViewHolders onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = LayoutInflater.from(mContext).inflate(R.layout.book_list_item, null);
      return new SampleViewHolders(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SampleViewHolders holder, int position) {
      holder.bookName.setText(mItems.get(position).getName());
      holder.authorName.setText(mItems.get(position).getAuthor());
    }

    @Override
    public int getItemCount() {
      return mItems.size();
    }

    public ItemObject getItem(int pos) {
      return mItems.get(pos);
    }
  }

  public static final class SampleViewHolders extends RecyclerView.ViewHolder implements
      View.OnClickListener {
    public TextView bookName;
    public TextView authorName;

    public SampleViewHolders(View itemView) {
      super(itemView);
      itemView.setOnClickListener(this);
      bookName = itemView.findViewById(R.id.BookName);
      authorName = itemView.findViewById(R.id.AuthorName);
    }

    @Override
    public void onClick(View view) {
      Toast.makeText(view.getContext(),
          "Clicked Position = " + getPosition(), Toast.LENGTH_SHORT).show();
    }
  }
  public static final class ItemObject {
    private static int sInstanceCount = 0;
    private String _photo_id;
    private String _name;
    private String _author;

    public ItemObject(String name, String auth) {
      this._photo_id = String.valueOf(sInstanceCount++);
      this._name = name;
      this._author = auth;
    }

    public String getPhotoId() {
      return _photo_id;
    }

    public String getName() {
      return _name;
    }

    public void setName(String name) {
      this._name = name;
    }

    public String getAuthor() {
      return _author;
    }

    public void setAuthor(String auth) {
      this._author = auth;
    }
  }
}
