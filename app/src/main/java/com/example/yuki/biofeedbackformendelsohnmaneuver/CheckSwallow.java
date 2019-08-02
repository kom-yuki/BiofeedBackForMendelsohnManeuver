package com.example.yuki.biofeedbackformendelsohnmaneuver;

import java.util.ArrayList;

class CheckSwallow {

    private ArrayList<Double> template;
    private int sumDiff;
    double average, minDiff, maxDiff, rangeDiff;
    private ArrayList<Double> diff;
    private ArrayList<Double> smoothingDiff;
    private ArrayList<Double> dtwDistance;
    private SPRING_DTW spring_dtw;

    CheckSwallow(ArrayList<Double> temp){
        diff = new ArrayList<>();
        smoothingDiff = new ArrayList<>();
        template = temp;
        spring_dtw = new SPRING_DTW(template);
        dtwDistance = new ArrayList<>();
    }


    boolean checkOnset(ArrayList<Double> data, int time, int ansei){
        if(time == 0){
            diff.add(0d);
            smoothingDiff.add(0d);
        } else if (time>5) {
            diff.add(Math.abs(data.get(time) - data.get(time-1)));
            smoothingDiff.add((diff.get(time)+diff.get(time-1)+diff.get(time-2)+diff.get(time-3)+diff.get(time-4))/5);
        } else {
            diff.add(Math.abs(data.get(time) - data.get(time-1)));
            smoothingDiff.add(0d);
        }

        if(time<ansei){
            sumDiff += smoothingDiff.get(time);
        }
        else if(time==ansei){
            average = sumDiff/ansei;
            for (int j=0;j<ansei;j++){
                if(minDiff > smoothingDiff.get(j)){
                    minDiff = smoothingDiff.get(j);
                }
                if(maxDiff < smoothingDiff.get(j)){
                    maxDiff = smoothingDiff.get(j);
                }
            }
            rangeDiff = maxDiff - minDiff;
        }
        else {
            if (smoothingDiff.get(time) > average + 1.5*rangeDiff) {
                return true;
            }
        }

        return false;
    }

    boolean checkOffset(ArrayList<Double> data, int time, int select){
        boolean detection;

        if (select == 1){
            detection =  spring_dtw.calcOptimal(data.get(time), time + 1, 3.0); //長いV字型
        }
        else {
            detection =  spring_dtw.calcFirst(data.get(time), time+1, 6.0); //バスタブ型
        }

        dtwDistance.add(spring_dtw.getDistance().get(time + 1).get(template.size()));

        return detection;
    }

    void addData(ArrayList<Double> data, int time, int select){

        if (select == 1){
            spring_dtw.addDataNormal(data.get(time), time+1);
        }
        else {
            spring_dtw.addDataPathRestriction(data.get(time), time+1);
        }

        dtwDistance.add(spring_dtw.getDistance().get(time+1).get(template.size()));
    }

    ArrayList<Double> getDTW(){
        return dtwDistance;
    }

    SPRING_DTW getSPRING_DTW(){
        return spring_dtw;
    }

}

