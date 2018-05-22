package org.carpenter.generator;

import org.apache.commons.text.similarity.JaccardDistance;
import org.carpenter.generator.dto.unit.ClassExtInfo;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class UnitClassifier {
    public static List<ClassExtInfo> getSimilarClassInfoList(Set<ClassExtInfo> infoSet) {
        List<ClassExtInfo> infoList = new ArrayList<>(infoSet);
        return getSimilarClassInfoList(infoList);
    }

    public static List<ClassExtInfo> getSimilarClassInfoList(List<ClassExtInfo> infoList) {
        List<ClassExtInfo> result = new ArrayList<>();

        Double[][] distMap = new Double[infoList.size()][infoList.size()];

        JaccardDistance jaccardDistance = new JaccardDistance();

        for(int i = 0; i < infoList.size(); i++) {
            for(int j = 0; j < infoList.size(); j++) {
                distMap[i][j] = jaccardDistance.apply(infoList.get(i).getUnitName(), infoList.get(j).getUnitName());
            }
        }

        Set<Integer> excludedNumbers = new HashSet<>();
        for(int i = 0; i < infoList.size(); i++) {
            if(excludedNumbers.contains(i)) continue;
//            double border = getClusterBorder(distMap[i]);
            for (int j = 0; j < infoList.size(); j++) {
                if(excludedNumbers.contains(j)) continue;
                if(distMap[i][j] <= 0.25) {
                    result.add(infoList.get(j));
                    excludedNumbers.add(j);
                }
            }
        }
        return result;
    }

//    private static double mean(double[] values) {
//        return mean(values, -1);
//    }
//
//    private static double mean(double[] values, int excludeIndex) {
//        if(values.length == 1 && excludeIndex == 0) return 0.0;
//        double mean = 0.0;
//        for (int i = 0; i < values.length; i++) {
//            if(excludeIndex > 0 && excludeIndex == i) continue;
//            mean += values[i];
//        }
//        return mean / (double) (excludeIndex >= 0 ? values.length - 1 : values.length);
//    }

//    private static double getClusterBorder(Double[] values) {
//        List<Double> valuesList = Arrays.asList(values);
//        Collections.sort(valuesList, new Comparator<Double>() {
//            @Override
//            public int compare(Double o1, Double o2) {
//                if(o1 > o2) return 1;
//                else if(o1 < o2) return -1;
//                else return 0;
//            }
//        });
//        for (int i = 0; i < valuesList.size(); i++) {
//            if(i == 0) continue;
//            double[] tmpArr = new double[i];
//            for(int j = 0; j < i; j++) {
//                if(j == 0) tmpArr[j] = 0.0;
//                else tmpArr[j] = valuesList.get(j) - valuesList.get(j - 1);
//            }
//            double currentMean = mean(tmpArr, 0);
//            if(currentMean == 0.0) continue;
//
//            BigDecimal delta = new BigDecimal(valuesList.get(i) - valuesList.get(i - 1));
//            BigDecimal bigMean = new BigDecimal(currentMean);
//
//            if(delta.compareTo(bigMean) >= 0) {
//                return valuesList.get(i - 1);
//            }
//        }
//        return -1.0;
//    }
}
