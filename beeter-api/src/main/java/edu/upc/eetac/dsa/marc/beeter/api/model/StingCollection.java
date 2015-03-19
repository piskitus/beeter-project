package edu.upc.eetac.dsa.marc.beeter.api.model;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLink.Style;
import org.glassfish.jersey.linking.InjectLinks;

import edu.upc.eetac.dsa.marc.beeter.api.MediaType;
import edu.upc.eetac.dsa.marc.beeter.api.StingResource;


// rel = current (hay que añadir estos dos enlaces)
// rel = self (añadir los @InjectLink...
public class StingCollection {

	@InjectLinks({
		@InjectLink(resource = StingResource.class, style = Style.ABSOLUTE, rel = "create-sting", title = "Create sting", type = MediaType.BEETER_API_STING),
		//"<http://localhost:8000/beeter-api/stings>; title=\"Create sting\"; rel=\"create-sting\"; type=\"application/vnd.beeter.api.sting+json\"",
		
		@InjectLink(value = "/stings?before={before}", style = Style.ABSOLUTE, rel = "previous", title = "Previous stings", type = MediaType.BEETER_API_STING_COLLECTION, bindings = { @Binding(name = "before", value = "${instance.oldestTimestamp}") }),
		//"<http://localhost:8000/beeter-api/stings%3Fbefore=1426674182000>; title=\"Previous stings\"; rel=\"previous\"; type=\"application/vnd.beeter.api.sting.collection+json\"",
		
		@InjectLink(value = "/stings?after={after}", style = Style.ABSOLUTE, rel = "current", title = "Newest stings", type = MediaType.BEETER_API_STING_COLLECTION, bindings = { @Binding(name = "after", value = "${instance.newestTimestamp}") }),
		//"<http://localhost:8000/beeter-api/stings%3Fafter=1426674203000>; title=\"Newest stings\"; rel=\"current\"; type=\"application/vnd.beeter.api.sting.collection+json\""
	
		@InjectLink(value = "/stings?now={now}", style = Style.ABSOLUTE, rel = "QWERTY", title = "QWERTY", type = MediaType.BEETER_API_STING_COLLECTION, bindings = { @Binding(name = "now", value = "${instance.oldestTimestamp}") }) 
		//"<http://localhost:8000/beeter-api/stings%3Fnow=1426674182000>; title=\"QWERTY\"; rel=\"QWERTY\"; type=\"application/vnd.beeter.api.sting.collection+json\""
	
	})
	
		private List<Link> links;
	private List<Sting> stings;
	private long newestTimestamp;
	private long oldestTimestamp;

	public List<Link> getLinks() {
		return links;
	}

	public void setLinks(List<Link> links) {
		this.links = links;
	}

	public StingCollection() {
		super();
		stings = new ArrayList<>();
	}

	public List<Sting> getStings() {
		return stings;
	}

	public void setStings(List<Sting> stings) {
		this.stings = stings;
	}

	public void addSting(Sting sting) {
		stings.add(sting);
	}

	public long getNewestTimestamp() {
		return newestTimestamp;
	}

	public void setNewestTimestamp(long newestTimestamp) {
		this.newestTimestamp = newestTimestamp;
	}

	public long getOldestTimestamp() {
		return oldestTimestamp;
	}

	public void setOldestTimestamp(long oldestTimestamp) {
		this.oldestTimestamp = oldestTimestamp;
	}

}
