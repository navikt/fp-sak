package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.CREATE;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.sikkerhet.abac.AppAbacAttributtType;
import no.nav.foreldrepenger.økonomi.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomi.økonomistøtte.ØkonomiKvittering;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.AbacDto;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;

@Path("/forvaltningOppdrag")
@ApplicationScoped
@Transactional
public class ForvaltningOppdragRestTjeneste {

    private static final Logger logger = LoggerFactory.getLogger(ForvaltningOppdragRestTjeneste.class);

    private BehandleØkonomioppdragKvittering økonomioppdragKvitteringTjeneste;

    public ForvaltningOppdragRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningOppdragRestTjeneste(BehandleØkonomioppdragKvittering økonomioppdragKvitteringTjeneste) {
        this.økonomioppdragKvitteringTjeneste = økonomioppdragKvitteringTjeneste;
    }

    @POST
    @Path("/kvitter-oppdrag-ok")
    @Consumes(APPLICATION_JSON)
    @Produces(APPLICATION_JSON)
    @Operation(description = "Kvitterer oppdrag manuelt. Brukes kun når det er avklart at oppdrag har gått OK, og kvittering ikke kommer til å komme fra Oppdragsystemet. Sjekk med Team Ukelønn hvis i tvil",
        tags = "FORVALTNING-oppdrag")
    @BeskyttetRessurs(action = CREATE, ressurs = DRIFT, sporingslogg = false)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response kvitterOK(@Parameter(description = "Identifikasjon av oppdrag som kvitteres OK. Sett oppdaterProsessTask til false kun når prosesstasken allerede er flyttet til FERDIG") @NotNull @Valid KvitteringDto kvitteringDto) {
        boolean oppdaterProsessTask = kvitteringDto.getOppdaterProsessTask();

        ØkonomiKvittering kvittering = new ØkonomiKvittering();
        kvittering.setBehandlingId(kvitteringDto.getBehandlingId());
        kvittering.setFagsystemId(kvitteringDto.getFagsystemId());

        logger.info("Kvitterer oppdrag OK for behandlingId={] fagsystemId={} oppdaterProsessTask=", kvitteringDto.getBehandlingId(), kvitteringDto.getFagsystemId(), oppdaterProsessTask);
        økonomioppdragKvitteringTjeneste.behandleKvittering(kvittering, oppdaterProsessTask);

        return Response.ok().build();
    }

    public static class KvitteringDto implements AbacDto {
        @Min(0)
        @Max(Long.MAX_VALUE)
        @NotNull
        private Long behandlingId;

        @Min(0)
        @Max(Long.MAX_VALUE)
        @NotNull
        private Long fagsystemId;

        @NotNull
        @DefaultValue("true")
        private Boolean oppdaterProsessTask;

        public Boolean getOppdaterProsessTask() {
            return oppdaterProsessTask;
        }

        public void setOppdaterProsessTask(Boolean oppdaterProsessTask) {
            this.oppdaterProsessTask = oppdaterProsessTask;
        }

        public Long getBehandlingId() {
            return behandlingId;
        }

        public void setBehandlingId(Long behandlingId) {
            this.behandlingId = behandlingId;
        }

        public Long getFagsystemId() {
            return fagsystemId;
        }

        public void setFagsystemId(Long fagsystemId) {
            this.fagsystemId = fagsystemId;
        }

        @Override
        public AbacDataAttributter abacAttributter() {
            return AbacDataAttributter.opprett()
                .leggTil(AppAbacAttributtType.BEHANDLING_ID, behandlingId);
        }
    }

}
