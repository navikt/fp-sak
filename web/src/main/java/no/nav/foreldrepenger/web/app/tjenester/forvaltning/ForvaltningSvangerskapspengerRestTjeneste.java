package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.abac.FPSakBeskyttetRessursAttributt;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRevurderingRepository;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningSvangerskapspenger")
@ApplicationScoped
@Transactional
public class ForvaltningSvangerskapspengerRestTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningSvangerskapspengerRestTjeneste.class);


    private SVPFeriepengekontrollTjeneste svpFeriepengekontrollTjeneste;
    private BehandlingRevurderingRepository behandlingRevurderingRepository;

    @Inject
    public ForvaltningSvangerskapspengerRestTjeneste(SVPFeriepengekontrollTjeneste svpFeriepengekontrollTjeneste,
                                                     BehandlingRevurderingRepository behandlingRevurderingRepository) {
        this.svpFeriepengekontrollTjeneste = svpFeriepengekontrollTjeneste;
        this.behandlingRevurderingRepository = behandlingRevurderingRepository;
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
}
