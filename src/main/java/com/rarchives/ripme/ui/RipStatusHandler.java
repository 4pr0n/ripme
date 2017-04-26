package com.rarchives.ripme.ui;

import com.rarchives.ripme.ripper.AbstractRipper;

public interface RipStatusHandler {

    void update(AbstractRipper ripper, RipStatusMessage message);

}