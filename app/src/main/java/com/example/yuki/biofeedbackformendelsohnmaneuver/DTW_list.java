package com.example.yuki.biofeedbackformendelsohnmaneuver;

import java.util.ArrayList;

public class DTW_list {
    int[][] from;
    double[][] distance;
    int[][] matching;
    int lenI, lenJ;

    public double calc(ArrayList<Double> x, ArrayList<Double> y){
        lenI = x.size();
        lenJ = y.size();
        from = new int[x.size()+1][y.size()+1];
        distance = new double[x.size()+1][y.size()+1];
        matching = new int[x.size()+1][y.size()+1];


        for (int i=0; i<=x.size(); i++){
            for (int j=0; j<=y.size(); j++){
                from[i][j] = -1;
            }
        }
        for (int i=1; i<=x.size(); i++ ){
            distance[i][0] = Double.POSITIVE_INFINITY;
        }
        for (int j=1; j<=y.size(); j++){
            distance[0][j] = Double.POSITIVE_INFINITY;
        }
        distance[0][0] = 0;

        for (int i=1; i<=x.size(); i++){
            for (int j=1; j<=y.size(); j++){
                double cost = Math.abs(x.get(i-1)-y.get(j-1));
                double diag, right, up;
                right = distance[i-1][j] + cost;
                up = distance[i][j-1] + cost;
                diag = distance[i-1][j-1] + cost;

                if(right < up && right < diag){
                    distance[i][j] = right;
                    from[i][j] = 0;
                }
                else if (up < right && up < diag){
                    distance[i][j] = up;
                    from[i][j] = 1;
                }
                else{
                    distance[i][j] = diag;
                    from[i][j] = 2;
                }
            }
        }


        this.backTrace();
        return distance[x.size()][y.size()]/(x.size()+y.size());

    }

    //逆順にやる
    public double calcReverse(ArrayList<Double> x, ArrayList<Double> y){
        lenI = x.size();
        lenJ = y.size();
        from = new int[x.size()+1][y.size()+1];
        distance = new double[x.size()+1][y.size()+1];
        matching = new int[x.size()+1][y.size()+1];

        for (int i=0; i<=x.size(); i++){
            for (int j=0; j<=y.size(); j++){
                from[i][j] = -1;
            }
        }
        for (int i=x.size()-1; i>=0; i-- ){
            distance[i][y.size()] = Double.POSITIVE_INFINITY;
        }
        for (int j=y.size()-1; j>=0; j--){
            distance[x.size()][j] = Double.POSITIVE_INFINITY;
        }
        distance[x.size()][y.size()] = 0;

        for (int i=x.size()-1; i>=0; i--){
            for (int j=y.size()-1; j>=0; j--){
                double cost = Math.abs(x.get(i)-y.get(j));
                double diag, right, up;
                right = distance[i+1][j] + cost;
                up = distance[i][j+1] + cost;
                diag = distance[i+1][j+1] + cost;

                if(right < up && right < diag){
                    distance[i][j] = right;
                    from[i][j] = 0;
                }
                else if (up < right && up < diag){
                    distance[i][j] = up;
                    from[i][j] = 1;
                }
                else{
                    distance[i][j] = diag;
                    from[i][j] = 2;
                }
            }
        }
        this.forwardTrace();
        return  distance[0][0]/(x.size()+y.size());
    }


    //逆順にやる + 傾斜制限
    public double calcReverseSlopeConstraint(ArrayList<Double> x, ArrayList<Double> y){
        lenI = x.size();
        lenJ = y.size();
        from = new int[x.size()+1][y.size()+1];
        distance = new double[x.size()+1][y.size()+1];
        matching = new int[x.size()+1][y.size()+1];

        for (int i=0; i<=x.size(); i++){
            for (int j=0; j<=y.size(); j++){
                from[i][j] = -1;
            }
        }
        for (int i=x.size()-1; i>=0; i-- ){
            distance[i][y.size()] = Double.POSITIVE_INFINITY;
        }
        for (int j=y.size()-1; j>=0; j--){
            distance[x.size()][j] = Double.POSITIVE_INFINITY;
        }
        distance[x.size()][y.size()] = 0;

        for (int i=x.size()-1; i>=0; i--){
            for (int j=y.size()-1; j>=0; j--){
                double cost = Math.abs(x.get(i)-y.get(j));
                double right, up, diag;
                double A, B, C;
                if (i+2 < x.size() && j+2 < y.size() && j>40 && j<80){

                    /*
                    A = distance[i+2][j+1] + 2*Math.abs(x.get(i+1)-y.get(j)) + cost;
                    B = distance[i+1][j+1] + cost;
                    C = distance[i+1][j+2] + 2*Math.abs(x.get(i)-y.get(j+1)) + cost;
                    */

                    A = distance[i+1][j] + cost;
                    B = distance[i+1][j+1] + cost;
                    C = distance[i+1][j+2] + cost;

                    if (A < B && A < C){
                        distance[i][j] = A;
                        from[i][j] = 3;
                    }
                    else if (B < A && B < C){
                        distance[i][j] = B;
                        from[i][j] = 4;
                    }
                    else{
                        distance[i][j] = C;
                        from[i][j] = 5;
                    }


                }
                else {
                    right = distance[i+1][j] + cost;
                    up = distance[i][j+1] + cost;
                    diag = distance[i+1][j+1] + cost;

                    if(right < up && right < diag){
                        distance[i][j] = right;
                        from[i][j] = 0;
                    }
                    else if (up < right && up < diag){
                        distance[i][j] = up;
                        from[i][j] = 1;
                    }
                    else{
                        distance[i][j] = diag;
                        from[i][j] = 2;
                    }
                }

            }
        }
        this.forwardTrace();
        return  distance[0][0]/(x.size()+y.size());
    }


    public void backTrace(){
        int i=lenI, j=lenJ;
        int move;
        while(true){
            move = from[i][j];

            if(move == 0){
                matching[i][j] = 1;
                i--;
            }
            else if(move == 1){
                matching[i][j] = 1;
                j--;
            }
            else if(move == 2){
                matching[i][j] = 1;
                i--;
                j--;
            }
            else {
                matching[i][j] = 1;
            }

            if(i==0 && j>0){
                matching[i][j] = 1;
                j--;
            }
            else if(j==0 && i>0){
                matching[i][j] = 1;
                i--;
            }
            else if(i==0 && j==0){
                break;
            }
        }
    }

    public void forwardTrace(){
        int i=0, j=0;
        int move;
        while(true){
            move = from[i][j];

            if(move == 0){
                matching[i][j] = 1;
                i++;
            }
            else if(move == 1){
                matching[i][j] = 1;
                j++;
            }
            else if(move == 2){
                matching[i][j] = 1;
                i++;
                j++;
            }
            else if (move == 3){
                matching[i][j] = 1;
                i++;
            }
            else if (move == 4){
                matching[i][j] = 1;
                i++;
                j++;
            }
            else if (move == 5){
                matching[i][j] = 1;
                i++;
                j += 2;
            }
            /*
            else if (move == 3){
                matching[i][j] = 1;
                i = i+2;
                j++;
            }
            else if (move == 4){
                matching[i][j] = 1;
                i++;
                j++;
            }
            else if (move == 5){
                matching[i][j] = 1;
                i++;
                j = j+2;
            }
            */
            else {
                matching[i][j] = 1;
            }

            if(i==lenI && j<lenJ){
                matching[i][j] = 1;
                j++;
            }
            else if(j==lenJ && i<lenI){
                matching[i][j] = 1;
                i++;
            }
            else if(i==lenI && j==lenJ){
                break;
            }
        }
    }

    public int getMatching(int i, int j){
        return matching[i][j];
    }

    public int[][] getMatching(){ return matching;};

}