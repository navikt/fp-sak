package no.nav.foreldrepenger.produksjonsstyring.behandlingenhet;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.aktør.OrganisasjonsEnhet;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Tema;
import no.nav.foreldrepenger.behandlingslager.behandling.Temagrupper;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.Diskresjonskode;
import no.nav.foreldrepenger.domene.person.PersoninfoAdapter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.historikk.OppgaveÅrsak;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.Arbeidsfordeling;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingRequest;
import no.nav.vedtak.felles.integrasjon.arbeidsfordeling.rest.ArbeidsfordelingResponse;
import no.nav.vedtak.felles.integrasjon.rest.jersey.Jersey;

@ApplicationScoped
public class EnhetsTjeneste {

    private static final String TEMAGRUPPE = Temagrupper.FAMILIEYTELSER.getOffisiellKode(); // Kodeverk Temagrupper - dekker FOR + OMS
    private static final String TEMA = Tema.FOR.getOffisiellKode(); // Kodeverk Tema
    private static final String OPPGAVETYPE = OppgaveÅrsak.BEHANDLE_SAK.getKode(); // Kodeverk Oppgavetype - NFP , uten spesialenheter
    private static final String ENHET_TYPE_NFP = "FPY"; // NOSONAR Kodeverk EnhetstyperNORG - NFP , uten spesialenheter (alt dropp behtype og filter på denne)
    private static final String BEHANDLINGTYPE = BehandlingType.FØRSTEGANGSSØKNAD.getOffisiellKode(); // Kodeverk Behandlingstype, bruker søknad
    private static final String NK_ENHET_ID = "4292";

    private static final OrganisasjonsEnhet KLAGE_ENHET =  new OrganisasjonsEnhet(NK_ENHET_ID, "NAV Klageinstans Midt-Norge");

    private PersoninfoAdapter personinfoAdapter;
    private Arbeidsfordeling norgRest;

    private LocalDate sisteInnhenting = LocalDate.MIN;
    private OrganisasjonsEnhet enhetKode6;
    private List<OrganisasjonsEnhet> alleBehandlendeEnheter = new ArrayList<>();

    public EnhetsTjeneste() {
        // For CDI proxy
    }

    @Inject
    public EnhetsTjeneste(PersoninfoAdapter personinfoAdapter,
                          @Jersey Arbeidsfordeling arbeidsfordelingRestKlient) {
        this.personinfoAdapter = personinfoAdapter;
        this.norgRest = arbeidsfordelingRestKlient;
    }


    List<OrganisasjonsEnhet> hentEnhetListe() {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter;
    }

    OrganisasjonsEnhet hentEnhetSjekkKunAktør(AktørId aktørId, BehandlingTema behandlingTema) {
        oppdaterEnhetCache();
        var geografiskTilknytning = personinfoAdapter.hentGeografiskTilknytning(aktørId);

        if (geografiskTilknytning.getTilknytning() == null &&
            (geografiskTilknytning.getDiskresjonskode() == null || Diskresjonskode.UDEFINERT.equals(geografiskTilknytning.getDiskresjonskode())))
            return tilfeldigEnhet();

        return hentEnheterFor(geografiskTilknytning.getTilknytning(), geografiskTilknytning.getDiskresjonskode(), behandlingTema).get(0);
    }

    Optional<OrganisasjonsEnhet> oppdaterEnhetSjekkOppgittePersoner(String enhetId, BehandlingTema behandlingTema, AktørId hovedAktør, Set<AktørId> alleAktører) {
        oppdaterEnhetCache();
        if (enhetKode6.enhetId().equals(enhetId) || NK_ENHET_ID.equals(enhetId)) {
            return Optional.empty();
        }
        if (harNoenDiskresjonskode6(alleAktører)) {
            return Optional.of(enhetKode6);
        }
        if (finnOrganisasjonsEnhet(enhetId).isEmpty()) {
            return Optional.of(hentEnhetSjekkKunAktør(hovedAktør, behandlingTema));
        }
        return Optional.empty();
    }

    private boolean harNoenDiskresjonskode6(Set<AktørId> aktører) {
        return aktører.stream()
            .map(personinfoAdapter::hentDiskresjonskode)
            .anyMatch(Diskresjonskode.KODE6::equals);
    }

    private void oppdaterEnhetCache() {
        if (sisteInnhenting.isBefore(LocalDate.now())) {
            enhetKode6 = hentEnheterFor(null, Diskresjonskode.KODE6, BehandlingTema.UDEFINERT).get(0);
            alleBehandlendeEnheter.clear();
            alleBehandlendeEnheter.addAll(hentEnheterFor(null, Diskresjonskode.UDEFINERT, BehandlingTema.UDEFINERT));
            alleBehandlendeEnheter.add(KLAGE_ENHET);
            sisteInnhenting = LocalDate.now();
        }
    }

    private OrganisasjonsEnhet tilfeldigEnhet() {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter.stream().filter(e -> !KLAGE_ENHET.equals(e) && !enhetKode6.equals(e)).findAny().orElseThrow();
    }

    Optional<OrganisasjonsEnhet> finnOrganisasjonsEnhet(String enhetId) {
        oppdaterEnhetCache();
        return alleBehandlendeEnheter.stream().filter(e -> enhetId.equals(e.enhetId())).findFirst();
    }

    OrganisasjonsEnhet enhetsPresedens(OrganisasjonsEnhet enhetSak1, OrganisasjonsEnhet enhetSak2) {
        oppdaterEnhetCache();
        if (enhetKode6.enhetId().equals(enhetSak1.enhetId()) || enhetKode6.enhetId().equals(enhetSak2.enhetId())) {
            return enhetKode6;
        }
        return enhetSak1;
    }

    static OrganisasjonsEnhet getEnhetKlage() {
        return KLAGE_ENHET;
    }

    private List<OrganisasjonsEnhet> hentEnheterFor(String geografi, Diskresjonskode diskresjon, BehandlingTema behandlingTema) {
        List<ArbeidsfordelingResponse> restenhet;
        var request = ArbeidsfordelingRequest.ny()
            .medTemagruppe(TEMAGRUPPE)
            .medTema(TEMA)
            .medOppgavetype(OPPGAVETYPE)
            .medBehandlingstype(BEHANDLINGTYPE)
            .medBehandlingstema(behandlingTema.getOffisiellKode())
            .medDiskresjonskode(diskresjon.getOffisiellKode())
            .medGeografiskOmraade(geografi)
            .build();
        if (geografi == null && Diskresjonskode.UDEFINERT.equals(diskresjon)) {
            restenhet = norgRest.hentAlleAktiveEnheter(request);
        } else {
            restenhet = norgRest.finnEnhet(request);
        }
        return restenhet.stream()
            .map(r -> new OrganisasjonsEnhet(r.enhetNr(), r.enhetNavn()))
            .collect(Collectors.toList());
    }

}
