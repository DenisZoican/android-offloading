package com.example.bluetoothconnection.communication.PayloadDataEntities;

import org.opencv.core.Mat;
import static com.example.bluetoothconnection.communication.Utils.Common.MessageContentType;

import com.example.bluetoothconnection.communication.Entities.DeviceNode;

public class PayloadRequestMatData extends PayloadData{
    private final Mat image;
    private final int linePosition;
    private DeviceNode treeNode;

    public PayloadRequestMatData(Mat image, DeviceNode treeNode, int linePosition) {
        super(MessageContentType.RequestImage);
        this.image = image;
        this.treeNode = treeNode;
        this.linePosition = linePosition;
    }
    public Mat getImage() {
        return image;
    }

    public DeviceNode getTreeNode() {
        return treeNode;
    }

    public int getLinePosition() {
        return linePosition;
    }
}