package no.nav.foreldrepenger.web.app.tjenester.behandling.dto.behandling;

import com.fasterxml.jackson.annotation.JsonProperty;
import no.nav.foreldrepenger.web.app.rest.ResourceLink;

import java.util.ArrayList;
import java.util.List;

public class BehandlingBrevDto extends BehandlingDto {

    /**
     * REST HATEOAS - pekere på resttjenester som skal brukes for å populere brev.
     * Skal erstatte links feltet på BehandlingDto for brev.
     *
     * @see https://restfulapi.net/hateoas/
     */
    @JsonProperty("brevRessurser")
    private List<ResourceLink> brevRessurser = new ArrayList<>();

    public List<ResourceLink> getBrevRessurser() {
        return brevRessurser;
    }

    public void setBrevRessurser(List<ResourceLink> brevRessurser) {
        this.brevRessurser = brevRessurser;
    }
}
