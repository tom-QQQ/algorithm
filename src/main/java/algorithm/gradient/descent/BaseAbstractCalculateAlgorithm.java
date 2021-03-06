package algorithm.gradient.descent;


import org.ujmp.core.Matrix;
import org.ujmp.core.calculation.Calculation;

import java.math.BigDecimal;
import java.util.List;

/**
 * @author Ning
 * @date Create in 2018/6/19
 */
 abstract class BaseAbstractCalculateAlgorithm extends BaseDataConstructForRegression {

    /**
     * 初始范围和结果范围差值倍数提示值
     */
    private int referenceInitAndActualTimes = 5;

    /**
     * 计算代价,Jθ
     * @param paramsMatrix 数据矩阵 m*n
     * @param resultsMatrix 结果矩阵 m*1
     * @param coefficientMatrix 系数矩阵 n*1
     * @return 代价
     */
    abstract Double calculateCostWithMatrix(Matrix paramsMatrix, Matrix resultsMatrix, Matrix coefficientMatrix);

    /**
     * 计算假设值矩阵，hθ(x)
     * @param paramsMatrix 数据矩阵 m*n
     * @param coefficientMatrix 系数矩阵 n*1
     * @return 新的系数矩阵
     */
    abstract Matrix calculateHypothesisMatrix(Matrix paramsMatrix, Matrix coefficientMatrix);

    /**
     * 借助矩阵使用梯度下降法计算系数结果
     * @param dataParamsList 数据list
     * @param dataResults 结果list
     */
    void calculateRegressionResultByMatrixWithGradientDescent(List<List<Double>> dataParamsList, List<Double> dataResults, boolean ifNeedCalculateResult) {

        if (dataParamsList.size() != dataResults.size()) {
            System.out.println("参数list数量和结果数量不同");
            return;
        }

        List<List<Double>> normalizationDataList = calculateNormalizationData(dataParamsList);

        if (normalizationDataList == null) {
            System.out.println("规格化结果为空");
            return;
        }

        if (!ifNeedCalculateResult) {
            System.out.println("正在进行参数验证");
            return;
        }

        Matrix paramsMatrix = constructDataMatrix(normalizationDataList);
        Matrix resultsMatrix = createMatrixWithList(dataResults);

        List<Double> coefficientList = initCoefficientList(dataParamsList.get(0).size() + 1);
        Matrix coefficientMatrix = createMatrixWithList(coefficientList);

        Matrix resultMatrix =  calculateCoefficientWithIterative(paramsMatrix, resultsMatrix, coefficientMatrix);
        System.out.println(resultMatrix);

        referenceInitCoefficientRange(resultMatrix);
    }

    /**
     * 计算代价中的正则部分L2，λ∑θ^2，不包括θ0
     * @param coefficientMatrix 结果矩阵
     * @return 代价中的正则部分
     */
    double calculateRegularPartCostValue(Matrix coefficientMatrix) {

        if (lambda == 0.0) {
            return lambda;
        }

        double theta0 = coefficientMatrix.getAsDouble(0, 0);
        coefficientMatrix.setAsDouble(0.0, 0, 0);
        double sumThetasSquare = coefficientMatrix.power(Calculation.Ret.NEW, 2.0).getValueSum();
        coefficientMatrix.setAsDouble(theta0, 0, 0);
        return sumThetasSquare*lambda;
    }

    /**
     * 迭代计算系数结果
     * @param paramsMatrix 数据矩阵 m*n
     * @param resultsMatrix 结果矩阵 m*1
     * @param coefficientMatrix 系数矩阵 n*1
     * @return 最终迭代结果
     */
    private Matrix calculateCoefficientWithIterative(Matrix paramsMatrix, Matrix resultsMatrix, Matrix coefficientMatrix) {

        int calculateTimes = 0;

        while (true) {

            double previousCostValue = calculateCostWithMatrix(paramsMatrix, resultsMatrix, coefficientMatrix);

            Matrix hypothesisMatrix = calculateHypothesisMatrix(paramsMatrix, coefficientMatrix);

            Matrix newCoefficientMatrix = calculateNewCoefficientMatrix(hypothesisMatrix, paramsMatrix, resultsMatrix, coefficientMatrix);

            double currentCostValue = calculateCostWithMatrix(paramsMatrix, resultsMatrix, newCoefficientMatrix);

            if (currentCostValue > previousCostValue) {
                newCoefficientMatrix = coefficientMatrix;
                studyRate *= declineValue;

            } else {

                if (couldStopStudy(previousCostValue, currentCostValue)) {
                    double realCost = realCost(newCoefficientMatrix, currentCostValue, resultsMatrix.getRowCount());
                    System.out.println("计算了" + calculateTimes + "次，迭代达到目标精度，迭代停止。 最终去除正则部分代价：" + realCost);
                    return newCoefficientMatrix;
                }

                if (calculateTimes == maxLoop) {
                    double realCost = realCost(newCoefficientMatrix, currentCostValue, resultsMatrix.getRowCount());
                    System.out.println("达到最大迭代次数" + maxLoop + "，迭代停止。 最终去除正则部分代价：" + realCost);
                    return newCoefficientMatrix;
                }
            }

            calculateTimes++;
            coefficientMatrix = newCoefficientMatrix;
        }
    }

    /**
     * 判断是否需要停止迭代，该方法无需在子类中调用
     * @param previousCostValue 之前的代价
     * @param currentCostValue 当前代价
     * @return 是否需要停止迭代
     */
    boolean couldStopStudy(double previousCostValue, double currentCostValue) {

        double difference = previousCostValue - currentCostValue;

        return difference < convergence;
    }

    /**
     * 计算新参数和之前参数的差值，公式： θ(1 - ɑ*(λ/m)) - ɑ/m[(hθ(x) - y)^T*x]^T，计算θ第一项时，λ为0
     * @param hypothesisMatrix 假设结果矩阵 m*1
     * @param paramsMatrix 参数矩阵 m*n
     * @param resultsMatrix 结果矩阵 m*1
     * @param coefficientMatrix 系数矩阵 n*1
     * @return 新系数矩阵 n*1
     */
    private Matrix calculateNewCoefficientMatrix(Matrix hypothesisMatrix, Matrix paramsMatrix, Matrix resultsMatrix, Matrix coefficientMatrix) {

        double previousTheta0Value = coefficientMatrix.getAsDouble(0, 0);
        Matrix differenceCoefficientMatrix = calculateDifferenceCoefficientMatrix(hypothesisMatrix, paramsMatrix, resultsMatrix);

        regularCoefficientMatrix(coefficientMatrix);

        Matrix newCoefficientMatrix = coefficientMatrix.minus(differenceCoefficientMatrix);
        changeTheta0ToRightValue(previousTheta0Value, differenceCoefficientMatrix, newCoefficientMatrix);
        return newCoefficientMatrix;

    }

    /**
     * 计算需要正则化系数值需要减去的值矩阵
     * @param hypothesisMatrix 假设结果矩阵 m*1
     * @param paramsMatrix 参数矩阵 m*n
     * @param resultsMatrix 结果矩阵 m*1
     * @return 需要减去的值的矩阵 n*1
     */
    private Matrix calculateDifferenceCoefficientMatrix(Matrix hypothesisMatrix, Matrix paramsMatrix, Matrix resultsMatrix) {

        Matrix differenceCoefficientMatrix = hypothesisMatrix.minus(resultsMatrix);
        differenceCoefficientMatrix = differenceCoefficientMatrix.transpose().mtimes(paramsMatrix).transpose();
        return differenceCoefficientMatrix.times(studyRate).divide(resultsMatrix.getRowCount());
    }

    /**
     * “正则化”θ矩阵
     * @param coefficientMatrix 正则化结果 n*1
     */
    private void regularCoefficientMatrix(Matrix coefficientMatrix) {

        long size = coefficientMatrix.getRowCount();
        for (int rowIndex = 0; rowIndex < size; rowIndex++) {
            double value = coefficientMatrix.getAsDouble(rowIndex, 0);
            double regularResult = calculateRegularResult(value, size);
            coefficientMatrix.setAsDouble(regularResult, rowIndex, 0);
        }
    }

    /**
     * 计算“正则化”值
     * @param value 需要正则化的值
     * @return 正则化结果
     */
    private double calculateRegularResult(double value, long size) {

        return value*(1 - studyRate*lambda/size);
    }

    /**
     * 修正新的theta0的值，用之前保存的theta0减去变化值，并存储到新系数结果矩阵中
     * @param previousTheta0Value 之前theta0的值
     * @param differenceCoefficientMatrix 需要减去的值的矩阵 n*1
     * @param newCoefficientMatrix 需要修改的系数矩阵 n*1
     */
    private void changeTheta0ToRightValue(double previousTheta0Value, Matrix differenceCoefficientMatrix, Matrix newCoefficientMatrix) {

        double rightValue = previousTheta0Value - differenceCoefficientMatrix.getAsDouble(0, 0);
        newCoefficientMatrix.setAsDouble(rightValue, 0, 0);
    }

    /**
     * 去除正则部分求出实际的代价
     * @param coefficientMatrix 结果矩阵
     * @param costValue 正则代价
     * @return 减去正则代价的代价
     */
    private double realCost(Matrix coefficientMatrix, double costValue, long rowCount) {

        double regularPartCostValue = calculateRegularPartCostValue(coefficientMatrix);
        return costValue - regularPartCostValue/2/rowCount;
    }


    /**
     * 打印建议初始值范围，除去最大值和最小值后的的平均值，只会在设置的范围和建议范围的差距大于referenceInitAndActualTimes - 1倍时会提示
     * @param resultMatrix 系数结果矩阵
     */
    private void referenceInitCoefficientRange(Matrix resultMatrix) {

        if (resultMatrix.getRowCount() < 4) {
            return;
        }

        double maxValue = Math.abs(resultMatrix.getMaxValue());
        double minValue = Math.abs(resultMatrix.getMinValue());
        double sumValue = resultMatrix.getAbsoluteValueSum();
        double referenceValue = (sumValue - maxValue - minValue)/(resultMatrix.getRowCount() - 2);
        int referenceIntValue = BigDecimal.valueOf(referenceValue).setScale(-1, BigDecimal.ROUND_HALF_UP).intValue();

        if (Math.max(referenceValue, initialNumberRange)/Math.min(referenceValue, initialNumberRange) > referenceInitAndActualTimes) {

            if (referenceIntValue != initialNumberRange) {
                System.out.println("根据系数结果，推荐初始结果系数最大绝对值为: " + referenceIntValue);
            }
        }
    }
}
