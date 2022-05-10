package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandling.revurdering.BerørtBehandlingTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingTjeneste;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerAbacSupplier;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("/forvaltningSvangerskapspenger")
@ApplicationScoped
@Transactional
public class ForvaltningSvangerskapspengerRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningSvangerskapspengerRestTjeneste.class);


    private SVPFeriepengekontrollTjeneste svpFeriepengekontrollTjeneste;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;
    private BerørtBehandlingTjeneste berørtBehandlingTjeneste;
    private FagsakRepository fagsakRepository;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;
    private RevurderingTjeneste revurderingTjeneste;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Inject
    public ForvaltningSvangerskapspengerRestTjeneste(SVPFeriepengekontrollTjeneste svpFeriepengekontrollTjeneste,
                                                     BehandlingRevurderingRepository behandlingRevurderingRepository,
                                                     BerørtBehandlingTjeneste berørtBehandlingTjeneste,
                                                     FagsakRepository fagsakRepository,
                                                     BehandlendeEnhetTjeneste behandlendeEnhetTjeneste,
                                                     @FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER) RevurderingTjeneste revurderingTjeneste,
                                                     BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.svpFeriepengekontrollTjeneste = svpFeriepengekontrollTjeneste;
        this.behandlingRevurderingRepository = behandlingRevurderingRepository;
        this.berørtBehandlingTjeneste = berørtBehandlingTjeneste;
        this.fagsakRepository = fagsakRepository;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
        this.revurderingTjeneste = revurderingTjeneste;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    public ForvaltningSvangerskapspengerRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/finnKandidaterForFeriepengefeil")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Finner saker som kan ha fått beregnet feil feriepenger", tags = "FORVALTNING-svangerskapspenger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response finnFeriepengefeil() {
        var aktørIder = behandlingRevurderingRepository.finnAktørerMedFlereSVPSaker();
        LOG.info("Fant " + aktørIder.size() + " aktører med flere SVP saker");
        aktørIder.forEach(akt -> svpFeriepengekontrollTjeneste.utledOmForMyeFeriepenger(akt));
        return Response.ok().build();
    }

    @POST
    @Path("/opprettFeriepengerevurdering")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppretter revurdering med årsak REBEREGN_FERIEPENGER", tags = "FORVALTNING-svangerskapspenger")
    @BeskyttetRessurs(action = READ, resource = FPSakBeskyttetRessursAttributt.DRIFT)
    public Response opprettRevurdering(@TilpassetAbacAttributt(supplierClass = SaksnummerAbacSupplier.Supplier.class)
                                           @NotNull @QueryParam("saksnummer") @Valid SaksnummerDto dto) {
        var fagsak = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getVerdi()), true)
            .orElseThrow(() -> new IllegalStateException("BRUKSFEIL: Fant ingen fagsak med saksnummer " + dto.getVerdi()));
        if (!fagsak.getYtelseType().equals(FagsakYtelseType.SVANGERSKAPSPENGER)) {
            throw new IllegalStateException("BRUKSFEIL: Saksnummer " + dto.getVerdi() + " er ikke en svangerskapssak,"
                + " denne tjenesten skal kun brukes for svangerskapspenger");
        }
        var behandlingÅrsakType = BehandlingÅrsakType.REBEREGN_FERIEPENGER;
        var behandlendeEnhet = behandlendeEnhetTjeneste.finnBehandlendeEnhetFor(fagsak);
        var nyBehandling = revurderingTjeneste.opprettManuellRevurdering(fagsak, behandlingÅrsakType,
            behandlendeEnhet);
        berørtBehandlingTjeneste.opprettHistorikkinnslagOmRevurdering(nyBehandling, behandlingÅrsakType);
        behandlingProsesseringTjeneste.opprettTasksForStartBehandling(nyBehandling);
        return Response.ok().build();
    }
}
