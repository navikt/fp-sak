package no.nav.foreldrepenger.web.app.soap.sak.v1;

import java.util.List;
import java.util.function.Function;

import jakarta.enterprise.context.Dependent;
import jakarta.inject.Inject;
import jakarta.jws.WebService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingTema;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakStatus;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.xmlutils.DateUtil;
import no.nav.tjeneste.virksomhet.foreldrepengesak.v1.binding.ForeldrepengesakV1;
import no.nav.tjeneste.virksomhet.foreldrepengesak.v1.informasjon.Behandlingstema;
import no.nav.tjeneste.virksomhet.foreldrepengesak.v1.informasjon.Sak;
import no.nav.tjeneste.virksomhet.foreldrepengesak.v1.informasjon.Saksstatus;
import no.nav.tjeneste.virksomhet.foreldrepengesak.v1.meldinger.FinnSakListeRequest;
import no.nav.tjeneste.virksomhet.foreldrepengesak.v1.meldinger.FinnSakListeResponse;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.felles.integrasjon.felles.ws.SoapWebService;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ServiceType;

/**
 * Webservice for å finne relevante fagsaker i VL.
 */

@Dependent
@WebService(wsdlLocation = "wsdl/no/nav/tjeneste/virksomhet/foreldrepengesak/v1/foreldrepengesak.wsdl", serviceName = "foreldrepengesak_v1", portName = "foreldrepengesak_v1Port", endpointInterface = "no.nav.tjeneste.virksomhet.foreldrepengesak.v1.binding.ForeldrepengesakV1")
@SoapWebService(endpoint = "/sak/finnSak/v1", tjenesteBeskrivelseURL = "https://confluence.adeo.no/pages/viewpage.action?pageId=220528950")
public class FinnSakService implements ForeldrepengesakV1 {

    private static final Logger LOG = LoggerFactory.getLogger(FinnSakService.class);

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;

    public FinnSakService() {
        // CDI
    }

    @Inject
    public FinnSakService(BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @Override
    public void ping() {
        LOG.debug("ping");
    }

    @Override
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.SAKLISTE, serviceType = ServiceType.WEBSERVICE)
    public FinnSakListeResponse finnSakListe(@TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) FinnSakListeRequest request) {

        var sakspart = request.getSakspart();
        var aktørid = sakspart.getAktoerId();

        var fagsaker = fagsakRepository.hentForBruker(new AktørId(aktørid));

        return lagResponse(fagsaker);
    }

    // pkg scope for enhetstest
    FinnSakListeResponse lagResponse(List<Fagsak> fagsaker) {
        var response = new FinnSakListeResponse();
        var saksliste = response.getSakListe();
        for (var fagsak : fagsaker) {
            saksliste.add(lagEksternRepresentasjon(fagsak));
        }
        return response;
    }

    private Sak lagEksternRepresentasjon(Fagsak fagsak) {
        var sak = new Sak();
        var status = fagsak.getStatus();
        sak.setStatus(lagEksternRepresentasjon(status));
        sak.setBehandlingstema(lagEksternRepresentasjonBehandlingstema(fagsak));
        sak.setSakId(fagsak.getSaksnummer().getVerdi());
        sak.setEndret(DateUtil.convertToXMLGregorianCalendar(fagsak.getEndretTidspunkt()));
        sak.setOpprettet(DateUtil.convertToXMLGregorianCalendar(fagsak.getOpprettetTidspunkt()));
        return sak;
    }

    private Behandlingstema lagEksternRepresentasjonBehandlingstema(Fagsak fagsak) {
        var behandlingTemaKodeliste = getBehandlingTema(fagsak);

        var behandlingstema = new Behandlingstema();
        behandlingstema.setValue(behandlingTemaKodeliste.getOffisiellKode());
        behandlingstema.setTermnavn(behandlingTemaKodeliste.getNavn());
        return behandlingstema;
    }

    private static Saksstatus lagEksternRepresentasjon(FagsakStatus status) {
        var saksstatus = new Saksstatus();
        saksstatus.setValue(status.getKode());
        saksstatus.setTermnavn(status.getNavn());
        return saksstatus;
    }

    private BehandlingTema getBehandlingTema(Fagsak fagsak) {
        if (!erStøttetYtelseType(fagsak.getYtelseType())) {
            throw new TekniskException("FP-861850", "Ikke-støttet ytelsestype: " + fagsak.getYtelseType());
        }

        var behandlingTema = getBehandlingsTemaForFagsak(fagsak);
        if (BehandlingTema.gjelderEngangsstønad(behandlingTema) ||
                BehandlingTema.gjelderForeldrepenger(behandlingTema) ||
                BehandlingTema.gjelderSvangerskapspenger(behandlingTema)) {
            return behandlingTema;
        }
        // det er riktig å rapportere på årsakstype, selv om koden over bruker
        // BehandlingTema
        throw new TekniskException("FP-132949", "Ikke-støttet årsakstype: " + behandlingTema);
    }

    private BehandlingTema getBehandlingsTemaForFagsak(Fagsak s) {
        var behandling = behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(s.getId());
        if (behandling.isEmpty()) {
            return BehandlingTema.fraFagsak(s, null);
        }

        var sisteBehandling = behandling.get();
        var grunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(sisteBehandling.getId());
        return BehandlingTema.fraFagsak(s, grunnlag.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null));
    }

    private static boolean erStøttetYtelseType(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType) ||
                FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType) ||
                FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType);
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (FinnSakListeRequest) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, req.getSakspart().getAktoerId());
        }
    }

}
