package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.ForvaltningBehandlingIdDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.InputValideringRegexDato;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path("/forvaltningUttak")
@ApplicationScoped
@Transactional
public class ForvaltningUttakRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(ForvaltningUttakRestTjeneste.class);

    private ForvaltningUttakTjeneste forvaltningUttakTjeneste;
    private EntityManager entityManager;
    private BehandlingRepository behandlingRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Inject
    public ForvaltningUttakRestTjeneste(ForvaltningUttakTjeneste forvaltningUttakTjeneste,
                                        EntityManager entityManager,
                                        BehandlingRepository behandlingRepository,
                                        BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.forvaltningUttakTjeneste = forvaltningUttakTjeneste;
        this.entityManager = entityManager;
        this.behandlingRepository = behandlingRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    public ForvaltningUttakRestTjeneste() {
        // CDI
    }

    @POST
    @Path("/beregn-kontoer")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Beregner kontoer basert på data fra behandlingen. Husk å revurdere begge foreldre", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response beregnKontoer(@BeanParam @Valid ForvaltningBehandlingIdDto dto) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");
        forvaltningUttakTjeneste.beregnKontoer(dto.getBehandlingUuid());
        return Response.noContent().build();
    }

    @POST
    @Path("/startdato")
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(description = "Setter overstyrt startdato for saken (ved manglende uttak)", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg =true)
    public Response settStartdato(@BeanParam @Valid StartdatoDto dto) {
        Objects.requireNonNull(dto.getBehandlingUuid(), "Støtter bare UUID");

        forvaltningUttakTjeneste.setStartdato(dto.getBehandlingUuid(), LocalDate.parse(dto.getStartdato()));
        return Response.noContent().build();
    }

    public static class StartdatoDto extends ForvaltningBehandlingIdDto {
        @NotNull
        @Parameter(description = "YYYY-MM-DD")
        @Pattern(regexp = InputValideringRegexDato.DATO_PATTERN)
        @FormParam("startdato")
        private String startdato;

        public String getStartdato() {
            return startdato;
        }
    }

    @POST
    @Path("/revurderAktivitetskravPermisjon")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hopper tilbake behandlinger med åpent AP 5074 (aktivitetskrav) der far søker fellesperiode og mor har permisjon. Ny logikk vil auto-godkjenne disse.", tags = "FORVALTNING-uttak")
    @BeskyttetRessurs(actionType = ActionType.CREATE, resourceType = ResourceType.DRIFT, sporingslogg = true)
    public Response revurderAktivitetskravPermisjon() {
        var behandlingIder = finnBehandlingerMedAktivitetskravPermisjon();
        LOG.info("Fant {} behandlinger med aktivitetskrav-permisjon som skal hoppes tilbake", behandlingIder.size());

        var antallHoppet = 0;
        for (var behandlingId : behandlingIder) {
            try {
                hoppTilbakeTilFaktaUttakDokumentasjon(behandlingId);
                antallHoppet++;
            } catch (Exception e) {
                LOG.warn("Kunne ikke hoppe tilbake behandling {}", behandlingId, e);
            }
        }

        LOG.info("Hoppet tilbake {} av {} behandlinger til FAKTA_UTTAK_DOKUMENTASJON", antallHoppet, behandlingIder.size());
        return Response.ok().build();
    }

    @SuppressWarnings("unchecked")
    private List<Long> finnBehandlingerMedAktivitetskravPermisjon() {
        var sql = """
            SELECT DISTINCT b.id
            FROM fagsak f
            JOIN behandling b ON b.fagsak_id = f.id
            JOIN aksjonspunkt ap ON ap.behandling_id = b.id
            JOIN gr_ytelses_fordeling gyf ON gyf.behandling_id = b.id AND gyf.aktiv = 'J'
            JOIN yf_fordeling_periode yfp ON yfp.fordeling_id = nvl(gyf.overstyrt_fordeling_id, gyf.justert_fordeling_id)
            WHERE f.ytelse_type = 'FP'
              AND b.behandling_status IN ('UTRED')
              AND ap.aksjonspunkt_def = '5074'
              AND ap.aksjonspunkt_status = 'OPPR'
              AND yfp.periode_type = 'FELLESPERIODE'
              AND yfp.mors_aktivitet = 'ARBEID'
              AND NOT EXISTS (
                  SELECT 1 FROM aksjonspunkt vent
                  WHERE vent.behandling_id = b.id
                    AND vent.aksjonspunkt_status = 'OPPR'
                    AND vent.aksjonspunkt_def LIKE '7%'
              )
              AND EXISTS (
                  SELECT 1
                  FROM gr_aktivitetskrav_arbeid gak
                  JOIN aktivitetskrav_arbeid_periode akp ON akp.aktivitetskrav_arbeid_perioder_id = gak.aktivitetskrav_arbeid_perioder_id
                  WHERE gak.behandling_id = b.id
                    AND gak.aktiv = 'J'
                    AND akp.sum_permisjonsprosent > 0
              )
            """;
        return entityManager.createNativeQuery(sql).getResultList();
    }

    private void hoppTilbakeTilFaktaUttakDokumentasjon(Long behandlingId) {
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var lås = behandlingRepository.taSkriveLås(behandlingId);
        behandlingProsesseringTjeneste.reposisjonerBehandlingTilbakeTil(behandling, lås, BehandlingStegType.FAKTA_UTTAK_DOKUMENTASJON);
        behandlingProsesseringTjeneste.opprettTasksForFortsettBehandling(behandling);
    }
}
