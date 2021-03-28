package com.example.guidemyeyes;

import android.os.Bundle;

import androidx.fragment.app.Fragment;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

public class GuideFragment extends Fragment {

    public GuideFragment() {
        // Required empty public constructor
    }

    public static GuideFragment newInstance(String param1, String param2) {
        GuideFragment fragment = new GuideFragment();
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_guide, container, false);
    }
}