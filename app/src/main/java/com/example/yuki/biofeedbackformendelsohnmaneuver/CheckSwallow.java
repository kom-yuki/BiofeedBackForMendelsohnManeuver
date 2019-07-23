package com.example.yuki.biofeedbackformendelsohnmaneuver;

import java.util.ArrayList;
import java.util.Collections;

public class CheckSwallow {
    private double sumDiff;
    private double average;
    private double minDiff;
    private double maxDiff;
    private double rangeDiff;
    private int flag, onset, offset;
    private ArrayList<Double> diff;
    private ArrayList<Double> smoothingDiff;
    private int usedtemp;
    private double DTWThreshold;
    private ArrayList<Double> DTWDistance;
    private ArrayList<Double> DTWDistanceDiff;
    private ArrayList<Double> initDTW;
    private int[][] matching;

    public CheckSwallow() {
        DTWThreshold = 0.0;
        smoothingDiff = new ArrayList<>();
        diff = new ArrayList<>();
        DTWDistance = new ArrayList<>();
        DTWDistanceDiff = new ArrayList<>();
        initDTW = new ArrayList<>();
    }

    public void initialized(){
        sumDiff = 0;
        average = 0;
        minDiff = Double.MAX_VALUE;
        maxDiff = Double.MIN_VALUE;
        rangeDiff = 0;
        flag = 0;
        onset = 0;
        offset = 0;
        usedtemp = 0;
        DTWThreshold = 0;
        diff.clear();
        smoothingDiff.clear();
        DTWDistance.clear();
        DTWDistanceDiff.clear();
        initDTW.clear();
    }

    public boolean checkSwallow(ArrayList<Double> data, ArrayList<ArrayList<Double>> template, int restTime, int i){
        DTWDistance.clear();
        DTWDistanceDiff.clear();

        if(i == 0){
            diff.add(0d);
            smoothingDiff.add(0d);
        } else if (i>=4) {
            diff.add(Math.abs(data.get(i) - data.get(i-1)));
            smoothingDiff.add((diff.get(i)+diff.get(i-1)+diff.get(i-2)+diff.get(i-3)+diff.get(i-4))/5);
        } else {
            diff.add(Math.abs(data.get(i) - data.get(i-1)));
            smoothingDiff.add(0d);
        }

        if(i<restTime){
            sumDiff += smoothingDiff.get(i);
        }
        else if(i==restTime){
            average = sumDiff/restTime;
            for (int j=4;j<restTime;j++){
                if(minDiff > smoothingDiff.get(j)){
                    minDiff = smoothingDiff.get(j);
                }
                if(maxDiff < smoothingDiff.get(j)){
                    maxDiff = smoothingDiff.get(j);
                }
            }
            rangeDiff = maxDiff - minDiff;
        }
        else if (i>restTime && flag == 0) {
            if (smoothingDiff.get(i) > average + 3.0 * rangeDiff) {
                flag = 1;
                onset = i;
                return true;
            }
        }
        else if(i >= onset + 20 && flag == 1){
            DTW_list dtw = new DTW_list();
            for (int j=0; j<template.size(); j++) {
                double distance = dtw.calcReverseSlopeConstraint(data, template.get(j));
                DTWDistance.add(distance);
                if (i == onset + 20){
                    initDTW.add(distance);
                }
                else if (distance < DTWThreshold) {
                    flag = 2;
                    offset = i;
                    usedtemp = j + 1;
                    matching = dtw.getMatching();
                }
            }
            if (i == onset + 20){
                DTWThreshold = getMedian(initDTW)*0.5;
                //DTWThreshold = 0;
            }
            if (flag == 2){
                return true;
            }
        }

        return false;
    }

    public int getOnset(){
        return onset;
    }

    public int getOffset(){
        return offset;
    }

    public int getUsedtemp(){
        return usedtemp;
    }

    public int getFlag() {
        return flag;
    }


    public ArrayList<Double> getDTWDistance(){
        return DTWDistance;
    }


    public ArrayList<Double> getDiff() { return diff;}

    public ArrayList<Double> getSmoothingDiff() {
        return smoothingDiff;
    }

    public double getMedian(ArrayList<Double> x){
        double out;
        Collections.sort(x);
        if (x.size() % 2 == 0){
            out = ( x.get(x.size()/2) + x.get(x.size()/2 - 1) ) / 2;
        }
        else {
            out = x.get((x.size()-1)/2);
        }
        return out;
    }

    public double getAverage(ArrayList<Double> x){
        double out,sum=0;
        for (double elem : x){
            sum += elem;
        }
        out = sum/x.size();
        return out;
    }

    public double getMax(ArrayList<Double> x){
        double max = Double.MIN_VALUE;
        for (double elem : x){
            if (elem > max){
                max = elem;
            }
        }
        return max;
    }

    public double getMin(ArrayList<Double> x){
        double min = Double.MAX_VALUE;
        for (double elem : x){
            if (elem < min){
                min = elem;
            }
        }
        return min;
    }
}
