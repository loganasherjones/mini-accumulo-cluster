package com.loganasherjones.mac;

import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExampleIterator extends Filter {
    private static final Logger LOG = LoggerFactory.getLogger(ExampleIterator.class);

    @Override
    public boolean accept(Key key, Value value) {
        LOG.info("ExampleIterator returning false.");
        return false;
    }
}
