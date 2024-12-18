package no.nav.foreldrepenger.web.app.tjenester.abakus;

import java.util.Optional;
import java.util.function.Function;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.abakus.callback.registerdata.CallbackDto;
import no.nav.abakus.callback.registerdata.ReferanseDto;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.RegisterdataCallback;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/registerdata")
@RequestScoped
@Transactional
public class IAYRegisterdataCallbackRestTjeneste {

    private IAYRegisterdataTjeneste iayTjeneste;
    private BehandlingLåsRepository låsRepository;

    public IAYRegisterdataCallbackRestTjeneste() {
        // Plattform trenger tom Ctor (Hibernate, CDI, etc)
    }

    @Inject
    public IAYRegisterdataCallbackRestTjeneste(IAYRegisterdataTjeneste iayTjeneste,
                                               BehandlingLåsRepository låsRepository) {
        this.iayTjeneste = iayTjeneste;
        this.låsRepository = låsRepository;
    }

    @POST
    @Path("/iay/callback")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Callback når registerinnhenting av IAY har blitt fullført i Abakus", tags = "registerdata")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.APPLIKASJON)
    public Response callback(@Parameter(description = "callbackDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) CallbackDto dto) {
        // Ta lås
        var behandlingLås = låsRepository.taLås(dto.getAvsenderRef().getReferanse());
        // Oppdaterer grunnlag med ny referanse
        var registerdataCallback = new RegisterdataCallback(behandlingLås.getBehandlingId(),
            Optional.ofNullable(dto.getOpprinneligGrunnlagRef()).map(ReferanseDto::getReferanse).orElse(null),
            dto.getOppdatertGrunnlagRef().getReferanse(),
            dto.getOpprettetTidspunkt());

        iayTjeneste.håndterCallback(registerdataCallback);

        return Response.accepted().build();
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
