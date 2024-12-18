package no.nav.foreldrepenger.web.app.tjenester.abakus;

import static no.nav.abakus.callback.registerdata.Grunnlag.IAY;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.function.Function;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger LOG = LoggerFactory.getLogger(IAYRegisterdataCallbackRestTjeneste.class);

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
        if (Objects.equals(IAY, dto.getGrunnlagType())) {
            // Ta lås
            var behandlingLås = låsRepository.taLås(dto.getAvsenderRef().getReferanse());
            // Oppdaterer grunnlag med ny referanse
            var registerdataCallback = new RegisterdataCallback(behandlingLås.getBehandlingId(),
                dto.getOpprinneligGrunnlagRef() != null ? dto.getOpprinneligGrunnlagRef().getReferanse() : null,
                dto.getOppdatertGrunnlagRef().getReferanse(),
                dto.getOpprettetTidspunkt());

            iayTjeneste.håndterCallback(registerdataCallback);
        } else {
            LOG.info("Mottatt registerdata callback på IAY-endepunkt for grunnlag av {}", dto);
        }

        return Response.accepted().build();
    }

    @POST
    @Path("/iay/callback/v2")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Callback når registerinnhenting av IAY har blitt fullført i Abakus", tags = "registerdata")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.APPLIKASJON)
    public Response callback(@Parameter(description = "callbackDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) CallbackV2Dto dto) {
            var behandlingLås = låsRepository.taLås(dto.avsenderRef().getReferanse());
            // Oppdaterer grunnlag med ny referanse
            var registerdataCallback = new RegisterdataCallback(behandlingLås.getBehandlingId(),
                dto.opprinneligGrunnlagRef() != null ? dto.opprinneligGrunnlagRef().getReferanse() : null,
                dto.oppdatertGrunnlagRef().getReferanse(),
                dto.opprettetTidspunkt());

            iayTjeneste.håndterCallback(registerdataCallback);

        return Response.accepted().build();
    }

    public record CallbackV2Dto(@NotNull @Valid LocalDateTime opprettetTidspunkt,
                                @NotNull @Valid ReferanseDto avsenderRef,
                                @Valid ReferanseDto opprinneligGrunnlagRef,
                                @Valid ReferanseDto oppdatertGrunnlagRef) {
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
