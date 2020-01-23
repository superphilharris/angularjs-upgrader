package org.angularjsupgrader.exception;

/**
 * FUEL-4369 FuelArchitect
 * <p>
 * Created by Philip on 11/01/2020
 */
public class UpgraderException extends Exception {

    public UpgraderException(Exception e) {
        super(e);
    }

    public UpgraderException(String msg) {
        super(msg);
    }
}
