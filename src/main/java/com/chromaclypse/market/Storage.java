package com.chromaclypse.market;

import com.chromaclypse.api.Defaults;
import com.chromaclypse.api.config.ConfigObject;
import com.chromaclypse.api.config.Section;

import java.util.List;

@Section(path="market-locations.yml")
public class Storage extends ConfigObject {

	public List<Register> registers = Defaults.emptyList();
	
	public static class Register {
		public String world = "";
		public int capacity = 2;
		public int x = 0;
		public int y = 0;
		public int z = 0;
	}
}
