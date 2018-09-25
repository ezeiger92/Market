package com.chromaclypse.market;

import com.chromaclypse.api.Defaults;
import com.chromaclypse.api.config.ConfigObject;

import java.util.List;
import java.util.Map;

public class Storage extends ConfigObject {

    public Map<String, List<Register>> registers = Defaults.emptyMap();

    public static class Register {
        public int x = 0;
        public int y = 0;
        public int z = 0;
        public int capacity = 100;
        public int stock = capacity / 2;
    }
}
