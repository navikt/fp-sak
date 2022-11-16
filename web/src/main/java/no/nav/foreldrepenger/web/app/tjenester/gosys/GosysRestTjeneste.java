package no.nav.foreldrepenger.web.app.tjenester.gosys;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.Behandlingstema;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.FinnSakListeRequest;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.FinnSakListeResponse;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.Sak;
import no.nav.foreldrepenger.web.app.tjenester.gosys.finnSak.Saksstatus;
import no.nav.foreldrepenger.web.server.abac.AppAbacAttributtType;
import no.nav.vedtak.exception.TekniskException;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

/**
 * Erstattning for WS for å finne relevante fagsaker i VL.
 * WS dokumentasjon finnes her https://confluence.adeo.no/pages/viewpage.action?pageId=220528950"
 */
@Path("/sak")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@ApplicationScoped
@Transactional
public class GosysRestTjeneste {

    private FagsakRepository fagsakRepository;
    private BehandlingRepository behandlingRepository;
    private FamilieHendelseRepository familieGrunnlagRepository;

    public GosysRestTjeneste() {
        // CDI
    }

    @Inject
    public GosysRestTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    @POST
    @Path("/finnSak/v1")
    @Operation(description = "For å finne relevante fagsaker i FPSAK.", tags = "gosys")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.SAKLISTE)
    public FinnSakListeResponse finnSakListe(
        @Parameter(description = "AktørId til personen som det skal finnes saker i FPSAK.")
        @NotNull @Valid @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) FinnSakListeRequest request) {

        var fagsaker = fagsakRepository.hentForBruker(new AktørId(request.aktørId()));
        return lagResponse(fagsaker);
    }

    FinnSakListeResponse lagResponse(List<Fagsak> fagsaker) {
        var saksliste = new ArrayList<Sak>();
        for (var fagsak : fagsaker) {
            saksliste.add(lagEksternRepresentasjon(fagsak));
        }
        return new FinnSakListeResponse(saksliste);
    }

    private Sak lagEksternRepresentasjon(Fagsak fagsak) {
        return new Sak(fagsak.getSaksnummer().getVerdi(), fagsak.getOpprettetTidspunkt(), lagEksternRepresentasjon(fagsak.getStatus()),
            fagsak.getEndretTidspunkt(), lagEksternRepresentasjonBehandlingstema(fagsak));
    }

    private Behandlingstema lagEksternRepresentasjonBehandlingstema(Fagsak fagsak) {
        var behandlingTemaKodeliste = getBehandlingTema(fagsak);

        return new Behandlingstema(behandlingTemaKodeliste.getNavn(), behandlingTemaKodeliste.getOffisiellKode(), "", null);
    }

    private static Saksstatus lagEksternRepresentasjon(FagsakStatus status) {
        return new Saksstatus(status.getNavn(), status.getKode(), "", null);
    }

    private BehandlingTema getBehandlingTema(Fagsak fagsak) {
        if (!erStøttetYtelseType(fagsak.getYtelseType())) {
            throw new TekniskException("FP-861850", "Ikke-støttet ytelsestype: " + fagsak.getYtelseType());
        }

        var behandlingTema = getBehandlingsTemaForFagsak(fagsak);
        if (BehandlingTema.gjelderEngangsstønad(behandlingTema) || BehandlingTema.gjelderForeldrepenger(behandlingTema)
            || BehandlingTema.gjelderSvangerskapspenger(behandlingTema)) {
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
        final var grunnlag = familieGrunnlagRepository.hentAggregatHvisEksisterer(sisteBehandling.getId());
        return BehandlingTema.fraFagsak(s, grunnlag.map(FamilieHendelseGrunnlagEntitet::getSøknadVersjon).orElse(null));
    }

    private static boolean erStøttetYtelseType(FagsakYtelseType fagsakYtelseType) {
        return FagsakYtelseType.ENGANGSTØNAD.equals(fagsakYtelseType) || FagsakYtelseType.FORELDREPENGER.equals(fagsakYtelseType)
            || FagsakYtelseType.SVANGERSKAPSPENGER.equals(fagsakYtelseType);
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (FinnSakListeRequest) obj;
            return AbacDataAttributter.opprett().leggTil(AppAbacAttributtType.AKTØR_ID, req.aktørId());
        }
    }

}
