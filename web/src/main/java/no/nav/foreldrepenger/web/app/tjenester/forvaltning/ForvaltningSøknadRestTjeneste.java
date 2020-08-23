package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.SaksnummerTermindatoDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/forvaltningSoknad")
@ApplicationScoped
@Transactional
public class ForvaltningSøknadRestTjeneste {

    private FamilieHendelseRepository familieHendelseRepository;
    private BehandlingRepository behandlingRepository;
    private FagsakRepository fagsakRepository;


    @Inject
    public ForvaltningSøknadRestTjeneste(BehandlingRepositoryProvider repositoryProvider) {
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
    }

    public ForvaltningSøknadRestTjeneste() {
        //CDI
    }

    @POST
    @Path("/endreTermindato")
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Oppdater termindato åpen/siste behandling ifm prematur situasjons", tags = "FORVALTNING-søknad")
    @BeskyttetRessurs(action = CREATE, ressurs = BeskyttetRessursResourceAttributt.FAGSAK, sporingslogg = false)
    public Response endreTermindato(@BeanParam @Valid SaksnummerTermindatoDto dto) {
        var fagsakId = fagsakRepository.hentSakGittSaksnummer(new Saksnummer(dto.getSaksnummer())).map(Fagsak::getId).orElseThrow();
        var behandling = behandlingRepository.hentÅpneYtelseBehandlingerForFagsakId(fagsakId).stream()
            .filter(b -> !b.harBehandlingÅrsak(BehandlingÅrsakType.BERØRT_BEHANDLING))
            .findFirst().orElseGet(() -> behandlingRepository.hentSisteYtelsesBehandlingForFagsakId(fagsakId).orElseThrow());
        int antall = familieHendelseRepository.oppdaterGjeldendeTermindatoForBehandling(behandling.getId(), dto.getTermindato(), dto.getBegrunnelse());

        return Response.ok(antall).build();
    }

}
