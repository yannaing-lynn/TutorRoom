package inc.osbay.android.tutorroom.ui.fragment;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.ProgressDialog;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.facebook.drawee.view.SimpleDraweeView;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import inc.osbay.android.tutorroom.R;
import inc.osbay.android.tutorroom.adapter.LessonByPackageDetailAdapter;
import inc.osbay.android.tutorroom.sdk.client.ServerError;
import inc.osbay.android.tutorroom.sdk.client.ServerRequestManager;
import inc.osbay.android.tutorroom.sdk.client.ServerResponse;
import inc.osbay.android.tutorroom.sdk.constant.CommonConstant;
import inc.osbay.android.tutorroom.sdk.database.DBAdapter;
import inc.osbay.android.tutorroom.sdk.model.Lesson;
import inc.osbay.android.tutorroom.sdk.model.Packagee;
import inc.osbay.android.tutorroom.utils.SharedPreferenceData;

public class PackageDetailFragment extends BackHandledFragment implements LessonByPackageDetailAdapter.OnItemClicked {

    public static String PackageDetailFragment_EXTRA = "PackageDetailFragment.EXTRA";
    @BindView(R.id.tool_bar)
    Toolbar toolBar;
    @BindView(R.id.lesson_rv)
    RecyclerView lessonRV;
    @BindView(R.id.package_name)
    TextView packageName;
    @BindView(R.id.package_desc)
    TextView packageDesc;
    @BindView(R.id.lesson_count)
    TextView lessonCount;
    @BindView(R.id.total_time)
    TextView totalTime;
    @BindView(R.id.credit)
    TextView credit;
    @BindView(R.id.no_lesson)
    TextView noLesson;
    @BindView(R.id.package_cover)
    SimpleDraweeView packageCover;
    private List<Lesson> lessonList = new ArrayList<>();
    private SharedPreferenceData sharedPreferences;
    private ServerRequestManager mServerRequestManager;
    private String packageeID;
    private Packagee packagee;

    @Override
    public boolean onBackPressed() {
        getFragmentManager().popBackStack();
        return false;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sharedPreferences = new SharedPreferenceData(getActivity());
        //accountId = String.valueOf(sharedPreferences.getInt("account_id"));
        mServerRequestManager = new ServerRequestManager(getActivity().getApplicationContext());
        packageeID = getArguments().getString(PackageDetailFragment_EXTRA);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_package_detail, container, false);
        ButterKnife.bind(this, view);

        toolBar.setBackgroundColor(getActivity().getResources().getColor(R.color.colorPrimary));
        setSupportActionBar(toolBar);
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ProgressDialog progressDialog = new ProgressDialog(getActivity());
        progressDialog.setMessage(getString(R.string.loading));
        progressDialog.show();
        mServerRequestManager.getLessonListByPackageID(packageeID, new ServerRequestManager.OnRequestFinishedListener() {
            @Override
            public void onSuccess(ServerResponse response) {
                progressDialog.dismiss();
                if (response.getCode() == 1) {
                    JSONArray lessonJsonArray;
                    JSONArray packageJsonArray;
                    try {
                        packageJsonArray = new JSONArray(response.getDataSt());
                        for (int i = 0; i < packageJsonArray.length(); i++) {
                            packagee = new Packagee(packageJsonArray.getJSONObject(i));
                            lessonJsonArray = packagee.getLessonJsonArray();
                            for (int j = 0; j < lessonJsonArray.length(); j++) {
                                Lesson packageObj = new Lesson(lessonJsonArray.getJSONObject(j));
                                lessonList.add(packageObj);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showLessonList();
                } else {
                    noLesson.setVisibility(View.VISIBLE);
                    lessonRV.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(ServerError err) {
                progressDialog.dismiss();
            }
        });
    }

    void showLessonList() {
        String cover = packagee.getCoverImg();
        packageCover.setImageURI(Uri.parse(cover));
        packageName.setText(packagee.getPackageName());
        packageDesc.setText(packagee.getPackageDescription());
        lessonCount.setText(String.valueOf(packagee.getLessonCount()));
        totalTime.setText(String.valueOf(packagee.getTotalTime()));
        credit.setText(String.valueOf(packagee.getPackagePrice()));

        if (lessonList.size() > 0) {
            noLesson.setVisibility(View.GONE);
            lessonRV.setVisibility(View.VISIBLE);
            LessonByPackageDetailAdapter lessonAdapter = new LessonByPackageDetailAdapter(lessonList, getActivity(), this);
            RecyclerView.LayoutManager mLayoutManager = new LinearLayoutManager(getActivity());
            lessonRV.setLayoutManager(mLayoutManager);
            lessonRV.setItemAnimator(new DefaultItemAnimator());
            lessonRV.setAdapter(lessonAdapter);
            lessonAdapter.notifyDataSetChanged();
        } else {
            noLesson.setVisibility(View.VISIBLE);
            lessonRV.setVisibility(View.GONE);
        }
    }

    @Override
    public void onStart() {
        super.onStart();
        setHasOptionsMenu(true);
        setTitle(getString(R.string.packagee_detail));
        setDisplayHomeAsUpEnable(true);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
        }
        return true;
    }

    @Override
    public void onItemClick(Lesson lesson) {
        FragmentManager fm = getFragmentManager();
        Fragment frg = fm.findFragmentById(R.id.framelayout);
        Fragment fragment = new LessonDetailFragment();

        Bundle bundle = new Bundle();
        bundle.putSerializable("lesson", lesson);
        bundle.putString("tag_id", "0");
        bundle.putString("package_id", packageeID);
        bundle.putString("lesson_type", String.valueOf(CommonConstant.packageLessonType));
        fragment.setArguments(bundle);
        if (frg == null) {
            fm.beginTransaction()
                    .add(R.id.framelayout, fragment)
                    .commit();
        } else {
            fm.beginTransaction()
                    .addToBackStack(null)
                    .replace(R.id.framelayout, fragment)
                    .commit();
        }
    }
}
