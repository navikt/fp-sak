package no.nav.foreldrepenger.web.app.tjenester.behandling.dto;

//TODO (TOR) Denne skal returnere rettigheter for behandlingsmeny i klient.
public class BehandlingRettigheterDto {

    private Boolean harSoknad;

    public BehandlingRettigheterDto(Boolean harSoknad) {
        this.harSoknad = harSoknad;
    }

    public Boolean getHarSoknad() {
        return harSoknad;
    }
}
