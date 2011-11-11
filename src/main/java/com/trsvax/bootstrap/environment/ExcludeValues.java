package com.trsvax.bootstrap.environment;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

public class ExcludeValues implements ExcludeEnvironment {
	private Map<String,List<String>> excludeMap = new HashMap<String, List<String>>();
	private Map<String,String> scripts = new HashMap<String, String>();

	public List<String> getExcludes(String mode) {
		if ( mode == null ) {
			mode = "ALL";
		}
		List<String> excludes =  excludeMap.get(mode);
		if ( excludes == null ) {
			return Collections.emptyList();
		}
		return excludes;
	}

	public void addExclude(String mode, String pattern) {
		if ( mode == null ) {
			mode = "ALL";
		}
		List<String> excludes = excludeMap.get(mode);
		if ( excludes == null ) {
			excludes = new ArrayList<String>();
			excludeMap.put(mode, excludes);
		}
		excludes.add(pattern);
	}

	public void addScriptOnce(String script) {
		scripts.put(script, null);
		
	}

	public Set<Entry<String, String>> getOnceScripts() {
		return scripts.entrySet();
	}

}
