package sonofcim;

import java.util.HashMap;
import java.util.Iterator;

import javax.xml.namespace.NamespaceContext;

public class SimpleNamespaceContext implements NamespaceContext {

	HashMap<String, String> prefixMap = new HashMap<String, String>();
	
	public SimpleNamespaceContext(String prefix, String namespace) {
		prefixMap.put(prefix, namespace);
	}
	
	public String getNamespaceURI(String prefix) {
		return prefixMap.get(prefix);
	}

	public String getPrefix(String namespaceURI) {
		for (String pre : prefixMap.keySet()) {
			if (prefixMap.get(pre).equals(namespaceURI)) return pre;
		}
		return null;
	}

	public Iterator getPrefixes(String namespaceURI) {
		return prefixMap.keySet().iterator();
	}

}
