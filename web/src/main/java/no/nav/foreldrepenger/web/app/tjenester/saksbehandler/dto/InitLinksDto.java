package no.nav.foreldrepenger.web.app.tjenester.saksbehandler.dto;

import java.util.ArrayList;
import java.util.List;

import no.nav.foreldrepenger.web.app.rest.ResourceLink;

public class InitLinksDto {
    private List<ResourceLink> links = new ArrayList<>();
    private List<ResourceLink> sakLinks = new ArrayList<>();

    public InitLinksDto() {
        // Injiseres i test
    }

    public InitLinksDto(List<ResourceLink> links,
                        List<ResourceLink> linksSak) {
        this.links = links;
        this.sakLinks = linksSak;
    }

    public List<ResourceLink> getLinks() {
        return links;
    }
    public List<ResourceLink> getSakLinks() {
        return sakLinks;
    }
}
