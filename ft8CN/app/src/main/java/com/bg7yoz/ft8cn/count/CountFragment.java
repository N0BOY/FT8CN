package com.bg7yoz.ft8cn.count;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bg7yoz.ft8cn.MainViewModel;
import com.bg7yoz.ft8cn.databinding.FragmentCountBinding;

import java.util.ArrayList;


public class CountFragment extends Fragment {
    private static final String TAG="CountFragment";
    private MainViewModel mainViewModel;
    private FragmentCountBinding binding;
    private ArrayList<CountDbOpr.CountInfo> countInfoList=new ArrayList<>();
    private MutableLiveData<ArrayList<CountDbOpr.CountInfo>> mutableInfoList=new MutableLiveData<>();

    private RecyclerView countInfoListRecyclerView;
    private CountInfoAdapter countInfoAdapter;

    private CountDbOpr.AfterCount afterCount=new CountDbOpr.AfterCount() {
        @Override
        public void countInformation(CountDbOpr.CountInfo countInfo) {
            countInfoList.add(countInfo);
            mutableInfoList.postValue(countInfoList);
        }
    };


    public CountFragment() {
        // Required empty public constructor
    }


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mainViewModel = MainViewModel.getInstance(this);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding=FragmentCountBinding.inflate(getLayoutInflater());
        mainViewModel = MainViewModel.getInstance(this);
        countInfoList.clear();
        countInfoListRecyclerView=binding.countRecyclerView;
        countInfoAdapter=new CountInfoAdapter(requireContext(),countInfoList);
        countInfoListRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        countInfoListRecyclerView.setAdapter(countInfoAdapter);
        countInfoAdapter.notifyDataSetChanged();



        CountDbOpr.getQSLTotal(mainViewModel.databaseOpr.getDb(),afterCount);


        CountDbOpr.getDxcc(mainViewModel.databaseOpr.getDb(),afterCount);
        CountDbOpr.getCQZoneCount(mainViewModel.databaseOpr.getDb(),afterCount);
        CountDbOpr.getItuCount(mainViewModel.databaseOpr.getDb(),afterCount);

        CountDbOpr.getBandCount(mainViewModel.databaseOpr.getDb(),afterCount);
        CountDbOpr.getDistanceCount(mainViewModel.databaseOpr.getDb(),afterCount);



        mutableInfoList.observe(requireActivity(), new Observer<ArrayList<CountDbOpr.CountInfo>>() {
            @Override
            public void onChanged(ArrayList<CountDbOpr.CountInfo> countInfos) {
                countInfoAdapter.notifyDataSetChanged();
            }
        });

        return binding.getRoot();
    }
}