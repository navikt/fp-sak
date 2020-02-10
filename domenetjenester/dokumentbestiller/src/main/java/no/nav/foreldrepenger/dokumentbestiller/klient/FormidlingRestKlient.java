package no.nav.foreldrepenger.dokumentbestiller.klient;

import java.net.URI;
import java.util.List;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.kontrakter.formidling.v1.BehandlingUuidDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BrevmalDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.HentBrevmalerDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.TekstFraSaksbehandlerDto;
import no.nav.vedtak.felles.integrasjon.rest.OidcRestClient;
import no.nav.vedtak.konfig.KonfigVerdi;

@ApplicationScoped
public class FormidlingRestKlient {
    private static final String ENDPOINT_KEY_BESTILL_DOKUMENT = "fpformidling.bestillbrev.url";
    private static final String ENDPOINT_KEY_HENT_BREVMALER = "fpformidling.hentbrevmaler.url";
    private static final String ENDPOINT_KEY_SJEKK_DOKUMENT_PRODUSERT = "fpformidling.erdokumentprodusert.url";
    private static final String ENDPOINT_KEY_HENT_SAKSBEHANDLER_TEKST = "fpformidling.hentsaksbehandlertekst.url";
    private static final String ENDPOINT_KEY_LAGRE_SAKSBEHANDLER_TEKST = "fpformidling.lagresaksbehandlertekst.url";
    private OidcRestClient oidcRestClient;
    private URI endpointBestill;
    private URI endpointHentBrevmaler;
    private URI endpointSjekkDokumentProdusert;
    private URI endpointHentSaksbehandlerTekst;
    private URI endpointLagreSaksbehandlerTekst;

    public FormidlingRestKlient() {
        //CDI
    }

    @Inject
    public FormidlingRestKlient(OidcRestClient oidcRestClient,
                                @KonfigVerdi(ENDPOINT_KEY_BESTILL_DOKUMENT) URI endpointBestill,
                                @KonfigVerdi(ENDPOINT_KEY_HENT_BREVMALER) URI endpointHentBrevmaler,
                                @KonfigVerdi(ENDPOINT_KEY_SJEKK_DOKUMENT_PRODUSERT) URI endpointSjekkDokumentProdusert,
                                @KonfigVerdi(ENDPOINT_KEY_HENT_SAKSBEHANDLER_TEKST) URI endpointHentSaksbehandlerTekst,
                                @KonfigVerdi(ENDPOINT_KEY_LAGRE_SAKSBEHANDLER_TEKST) URI endpointLagreSaksbehandlerTekst) {
        this.oidcRestClient = oidcRestClient;
        this.endpointBestill = endpointBestill;
        this.endpointHentBrevmaler = endpointHentBrevmaler;
        this.endpointSjekkDokumentProdusert = endpointSjekkDokumentProdusert;
        this.endpointHentSaksbehandlerTekst = endpointHentSaksbehandlerTekst;
        this.endpointLagreSaksbehandlerTekst = endpointLagreSaksbehandlerTekst;
    }

    public void bestillDokument(DokumentbestillingDto dokumentbestillingDto) {
        oidcRestClient.post(endpointBestill, dokumentbestillingDto);
    }

    public List<BrevmalDto> hentBrevMaler(BehandlingUuidDto behandlingUuidDto) {
        HentBrevmalerDto brevmalerDto = oidcRestClient.post(endpointHentBrevmaler, behandlingUuidDto, HentBrevmalerDto.class);
        return brevmalerDto.getBrevmalDtoListe();
    }

    public Boolean erDokumentProdusert(DokumentProdusertDto dokumentProdusertDto) {
        return oidcRestClient.post(endpointSjekkDokumentProdusert, dokumentProdusertDto, Boolean.class);
    }

    public void lagreTekstFraSaksbehandler(TekstFraSaksbehandlerDto tekstFraSaksbehandlerDto) {
        oidcRestClient.post(endpointLagreSaksbehandlerTekst, tekstFraSaksbehandlerDto);
    }

    public Optional<TekstFraSaksbehandlerDto> hentTekstFraSaksbehandler(BehandlingUuidDto behandlingUuidDto) {
        return oidcRestClient.postReturnsOptional(endpointHentSaksbehandlerTekst, behandlingUuidDto, TekstFraSaksbehandlerDto.class);
    }
}
