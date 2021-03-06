/*
 * Copyright [2013-2014] eBay Software Foundation
 *  
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ml.shifu.guagua.mapreduce.example.nn.meta;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

import ml.shifu.guagua.io.HaltBytable;
import ml.shifu.guagua.mapreduce.example.nn.NNUtils;

/**
 * NNParams are used to save NN model info which can also be stored into ZooKeeper.
 * 
 * <p>
 * {@link #weights} is used to set model weights which is used to transfer info from master to workers.
 * 
 * <p>
 * {@link #gradients} is used to accumulate all workers' gradients together in master and then use the accumulated
 * gradients to update neural network weights.
 */
public class NNParams extends HaltBytable {

    /**
     * Weights used for NN model
     */
    private double[] weights;

    /**
     * Gradients for NN model
     */
    private double[] gradients;

    /**
     * Current test error which can be sent to master
     */
    private double testError = 0;

    /**
     * Current train error which can be sent to master
     */
    private double trainError = 0;

    /**
     * Training size of each worker and master
     */
    private long trainSize = 0;

    public double[] getWeights() {
        return weights;
    }

    public void setWeights(double[] weights) {
        this.weights = weights;
    }

    public double getTestError() {
        return testError;
    }

    public void setTestError(double testError) {
        this.testError = testError;
    }

    public double getTrainError() {
        return trainError;
    }

    public void setTrainError(double trainError) {
        this.trainError = trainError;
    }

    public void accumulateGradients(double[] gradients) {
        if(this.gradients == null) {
            this.gradients = new double[gradients.length];
            Arrays.fill(this.gradients, 0.0);
        }

        if(this.weights == null) {
            this.weights = new double[gradients.length];
            NNUtils.randomize(gradients.length, this.weights);
        }

        for(int i = 0; i < gradients.length; i++) {
            this.gradients[i] += gradients[i];
        }
    }

    /**
     * @return the gradients
     */
    public double[] getGradients() {
        return gradients;
    }

    /**
     * @param gradients
     *            the gradients to set
     */
    public void setGradients(double[] gradients) {
        this.gradients = gradients;
    }

    public long getTrainSize() {
        return trainSize;
    }

    public void setTrainSize(long trainSize) {
        this.trainSize = trainSize;
    }

    public void accumulateTrainSize(long size) {
        this.trainSize = this.getTrainSize() + size;
    }

    public void reset() {
        this.setTrainSize(0);
        if(this.gradients != null) {
            Arrays.fill(this.gradients, 0.0);
        }
    }

    @Override
    public void doWrite(DataOutput out) throws IOException {
        out.writeDouble(getTrainError());
        out.writeDouble(getTestError());

        out.writeLong(getTrainSize());

        out.writeInt(getWeights().length);
        for(double weight: getWeights()) {
            out.writeDouble(weight);
        }

        out.writeInt(getGradients().length);
        for(double gradient: getGradients()) {
            out.writeDouble(gradient);
        }
    }

    @Override
    public void doReadFields(DataInput in) throws IOException {
        this.trainError = in.readDouble();
        this.testError = in.readDouble();
        this.trainSize = in.readLong();

        int len = in.readInt();
        double[] weights = new double[len];
        for(int i = 0; i < len; i++) {
            weights[i] = in.readDouble();
        }
        this.weights = weights;

        len = in.readInt();
        double[] gradients = new double[len];
        for(int i = 0; i < len; i++) {
            gradients[i] = in.readDouble();
        }
        this.gradients = gradients;
    }

    @Override
    public String toString() {
        return String.format("NNParams [testError=%s, trainError=%s, trainSize=%s, weights=%s, gradients%s]",
                this.testError, this.trainError, this.trainSize, Arrays.toString(this.weights),
                Arrays.toString(this.gradients));
    }

}
