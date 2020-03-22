package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt.READ;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.DRIFT;
import static no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt.FAGSAK;

import java.sql.Timestamp;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.OverlappData;
import no.nav.foreldrepenger.domene.vedtak.infotrygd.rest.SjekkOverlappForeldrepengerInfotrygdTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.fagsak.dto.SaksnummerDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.AksjonspunktKodeDto;
import no.nav.foreldrepenger.web.app.tjenester.forvaltning.dto.EnkelPeriodeDto;
import no.nav.vedtak.felles.jpa.Transaction;
import no.nav.vedtak.felles.jpa.VLPersistenceUnit;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/forvaltningUttrekk")
@ApplicationScoped
@Transaction
public class ForvaltningUttrekkRestTjeneste {

    private EntityManager entityManager;
    private FagsakRepository fagsakRepository;
    private InformasjonssakRepository informasjonssakRepository;
    private SjekkOverlappForeldrepengerInfotrygdTjeneste overlapper;

    public ForvaltningUttrekkRestTjeneste() {
        // For CDI
    }

    @Inject
    public ForvaltningUttrekkRestTjeneste(@VLPersistenceUnit EntityManager entityManager, BehandlingRepositoryProvider repositoryProvider,
                                          InformasjonssakRepository informasjonssakRepository, SjekkOverlappForeldrepengerInfotrygdTjeneste overlapper) {
        this.entityManager = entityManager;
        this.fagsakRepository = repositoryProvider.getFagsakRepository();
        this.informasjonssakRepository = informasjonssakRepository;
        this.overlapper = overlapper;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Gir åpne aksjonspunkter med angitt kode", tags = "FORVALTNING-uttrekk")
    @Path("/openAutopunkt")
    @BeskyttetRessurs(action = READ, ressurs = BeskyttetRessursResourceAttributt.DRIFT, sporingslogg = false)
    public Response openAutopunkt(@Parameter(description = "Aksjonspunktkoden") @BeanParam @Valid AksjonspunktKodeDto dto) {
        AksjonspunktDefinisjon apDef = AksjonspunktDefinisjon.fraKode(dto.getAksjonspunktKode());
        if (apDef == null) {
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        Query query = entityManager.createNativeQuery("select saksnummer, ap.opprettet_tid, ap.frist_tid " +
                " from fpsak.fagsak fs join fpsak.behandling bh on bh.fagsak_id=fs.id " +
                " join FPSAK.AKSJONSPUNKT ap on ap.behandling_id=bh.id " +
                " where aksjonspunkt_def=:apdef and aksjonspunkt_status=:status "); //$NON-NLS-1$
        query.setParameter("apdef", apDef.getKode());
        query.setParameter("status", AksjonspunktStatus.OPPRETTET.getKode());
        @SuppressWarnings("unchecked")
        List<Object[]> resultatList = query.getResultList();
        List<OpenAutopunkt> åpneAksjonspunkt = resultatList.stream()
            .map(this::mapFraAksjonspunktTilDto)
            .collect(Collectors.toList());
        return Response.ok(åpneAksjonspunkt).build();
    }

    private OpenAutopunkt mapFraAksjonspunktTilDto(Object[] row) {
        OpenAutopunkt autopunkt = new OpenAutopunkt();
        autopunkt.aksjonspunktOpprettetDato = ((Timestamp)row[1]).toLocalDateTime().toLocalDate(); // NOSONAR
        autopunkt.aksjonspunktFristDato = row[2] != null ? ((Timestamp)row[2]).toLocalDateTime().toLocalDate() : null; // NOSONAR
        autopunkt.saksnummer = (String)row[0]; // NOSONAR
        return autopunkt;
    }

    public static class OpenAutopunkt {
        public String saksnummer;  // NOSONAR
        public LocalDate aksjonspunktOpprettetDato;  // NOSONAR
        public LocalDate aksjonspunktFristDato;  // NOSONAR
    }

    @GET
    @Path("/listFagsakUtenBehandling")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av saknumre for fagsak uten noen behandlinger",
        tags = "FORVALTNING-uttrekk",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Fagsaker uten behandling",
                content = @Content(
                    array = @ArraySchema(
                        arraySchema = @Schema(implementation = List.class),
                        schema = @Schema(implementation = SaksnummerDto.class)),
                    mediaType = MediaType.APPLICATION_JSON
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = FAGSAK)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<SaksnummerDto> listFagsakUtenBehandling() {
        return fagsakRepository.hentÅpneFagsakerUtenBehandling().stream().map(SaksnummerDto::new).collect(Collectors.toList());
    }

    @GET
    @Path("/listFagsakMedoverlappTrex")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Hent liste av saknumre for fagsak som har overlapp med ytelse i Infotrygd",
        tags = "FORVALTNING-uttrekk",
        responses = {
            @ApiResponse(responseCode = "200",
                description = "Fagsaker med overlapp",
                content = @Content(
                    array = @ArraySchema(
                        arraySchema = @Schema(implementation = List.class),
                        schema = @Schema(implementation = SaksnummerDto.class)),
                    mediaType = MediaType.APPLICATION_JSON
                )
            )
        })
    @BeskyttetRessurs(action = READ, ressurs = DRIFT)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public Response listFagsakerMedOverlapp(@Parameter(description = "Periode") @BeanParam @Valid EnkelPeriodeDto dto) {
        var saker = informasjonssakRepository.finnSakerMedSisteVedtakOpprettetInnenIntervall(dto.getFom(), dto.getTom());
        var overlappende = saker.stream().map(this::vurderOverlapp).filter(Objects::nonNull).collect(Collectors.toList());
        return Response.ok(overlappende).build();
    }

    public static class OverlappendeSak {
        public String saksnummer;  // NOSONAR
        public String overlappene ;
    }

    private OverlappendeSak vurderOverlapp(OverlappData data) {
        boolean match = false;
        LocalDate startdato = fomMandag(data.getMinUtbetalingDato());
        var resultat = new OverlappendeSak();
        var builder = new StringBuilder();
        resultat.saksnummer = data.getSaksnummer().getVerdi();
        if (overlapper.harForeldrepengerInfotrygdSomOverlapper(data.getAktørId(), startdato)) {
            match = true;
            builder.append(" Foreldrepenger ");
        }
        if (RelasjonsRolleType.erMor(data.getRolle()) && overlapper.harSvangerskapspengerInfotrygdSomOverlapper(data.getAktørId(), startdato)) {
            match = true;
            builder.append(" Svangerskap ");
        }
        if (RelasjonsRolleType.erMor(data.getRolle()) && data.getAnnenPartAktørId() != null && FagsakYtelseType.FORELDREPENGER.equals(data.getYtelseType()) &&
            overlapper.harForeldrepengerInfotrygdSomOverlapper(data.getAnnenPartAktørId(), startdato)) {
            match = true;
            resultat.overlappene += " AnnenPart ";
        }
        if (!match)
            return null;
        resultat.overlappene = builder.toString();
        return resultat;
    }

    private LocalDate fomMandag(LocalDate fom) {
        DayOfWeek ukedag = DayOfWeek.from(fom);
        if (DayOfWeek.SUNDAY.getValue() == ukedag.getValue())
            return fom.plusDays(1);
        if (DayOfWeek.SATURDAY.getValue() == ukedag.getValue())
            return fom.plusDays(2);
        return fom;
    }
}
