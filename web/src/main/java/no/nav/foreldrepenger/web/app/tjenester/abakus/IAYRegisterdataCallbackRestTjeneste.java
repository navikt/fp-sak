package no.nav.foreldrepenger.web.app.tjenester.abakus;

import static no.nav.abakus.callback.registerdata.Grunnlag.IAY;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.UPDATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.APPLIKASJON;

import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.abakus.callback.registerdata.CallbackDto;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLåsRepository;
import no.nav.foreldrepenger.domene.arbeidsforhold.RegisterdataCallback;
import no.nav.foreldrepenger.domene.arbeidsforhold.impl.IAYRegisterdataTjeneste;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;

@Path("/registerdata")
@ApplicationScoped
@Transaction
public class IAYRegisterdataCallbackRestTjeneste {

    private static final Logger log = LoggerFactory.getLogger(IAYRegisterdataCallbackRestTjeneste.class);

    private IAYRegisterdataTjeneste iayTjeneste;
    private BehandlingLåsRepository låsRepository;

    public IAYRegisterdataCallbackRestTjeneste() {
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
    @Operation(description = "Callback når registerinnhenting av IAY har blitt fullført i Abakus", tags = "registerdata")
    @BeskyttetRessurs(action = UPDATE, ressurs = APPLIKASJON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response callback(@Parameter(description = "callbackDto") @Valid @TilpassetAbacAttributt(supplierClass = AbacDataSupplier.class) CallbackDto dto) {
        if (Objects.equals(IAY, dto.getGrunnlagType())) {
            // Ta lås
            BehandlingLås behandlingLås = låsRepository.taLås(UUID.fromString(dto.getAvsenderRef().getReferanse()));
            // Oppdaterer grunnlag med ny referanse
            RegisterdataCallback registerdataCallback = new RegisterdataCallback(behandlingLås.getBehandlingId(),
                dto.getOpprinneligGrunnlagRef() != null ? UUID.fromString(dto.getOpprinneligGrunnlagRef().getReferanse()) : null,
                UUID.fromString(dto.getOppdatertGrunnlagRef().getReferanse()),
                dto.getOpprettetTidspunkt());

            iayTjeneste.håndterCallback(registerdataCallback);
        } else {
            log.info("Mottatt registerdata callback på IAY-endepunkt for grunnlag av {}", dto);
        }

        return Response.accepted().build();
    }

    public static class AbacDataSupplier implements Function<Object, AbacDataAttributter> {

        @Override
        public AbacDataAttributter apply(Object obj) {
            return AbacDataAttributter.opprett();
        }
    }
}
