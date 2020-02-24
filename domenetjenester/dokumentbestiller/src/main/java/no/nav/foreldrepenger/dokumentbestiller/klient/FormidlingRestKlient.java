package no.nav.foreldrepenger.dokumentbestiller.klient;

import java.net.URI;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.kontrakter.formidling.v1.BehandlingUuidDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.HentBrevmalerDto;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class FormidlingRestKlient {
    private static final String ENDPOINT_KEY_HENT_BREVMALER = "fpformidling.hentbrevmaler.url";
    private static final String ENDPOINT_KEY_SJEKK_DOKUMENT_PRODUSERT = "fpformidling.erdokumentprodusert.url";
    private OidcRestClient oidcRestClient;
    private URI endpointHentBrevmaler;
    private URI endpointSjekkDokumentProdusert;

    public FormidlingRestKlient() {
        //CDI
    }

    @Inject
    public FormidlingRestKlient(OidcRestClient oidcRestClient,
                                @KonfigVerdi(ENDPOINT_KEY_HENT_BREVMALER) URI endpointHentBrevmaler,
                                @KonfigVerdi(ENDPOINT_KEY_SJEKK_DOKUMENT_PRODUSERT) URI endpointSjekkDokumentProdusert) {
        this.oidcRestClient = oidcRestClient;
        this.endpointHentBrevmaler = endpointHentBrevmaler;
        this.endpointSjekkDokumentProdusert = endpointSjekkDokumentProdusert;
    }

    public List<BrevmalDto> hentBrevMaler(BehandlingUuidDto behandlingUuidDto) {
        HentBrevmalerDto brevmalerDto = oidcRestClient.post(endpointHentBrevmaler, behandlingUuidDto, HentBrevmalerDto.class);
        return brevmalerDto.getBrevmalDtoListe();
    }

    public Boolean erDokumentProdusert(DokumentProdusertDto dokumentProdusertDto) {
        return oidcRestClient.post(endpointSjekkDokumentProdusert, dokumentProdusertDto, Boolean.class);
    }
}
