package no.nav.foreldrepenger.web.app.tjenester.mellomlagring;

import java.util.UUID;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.folketrygdloven.kalkulus.annoteringer.Fritekst;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.validering.ValidKodeverk;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;

@Path(MellomlagringRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class MellomlagringRestTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(MellomlagringRestTjeneste.class);

    static final String BASE_PATH = "/mellomlagring";
    private static final String HENT_PART_PATH = "/hent";
    public static final String MELLOMLAGRING_PATH = BASE_PATH;
    public static final String HENT_MELLOMLAGRING_PATH = BASE_PATH + HENT_PART_PATH;

    private MellomlagringRepository mellomlagringRepository;
    private BehandlingRepository behandlingRepository;
    private DokumentBehandlingTjeneste dokumentBehandlingTjeneste;

    public MellomlagringRestTjeneste() {
        // CDI
    }

    @Inject
    public MellomlagringRestTjeneste(MellomlagringRepository mellomlagringRepository,
                                     BehandlingRepository behandlingRepository,
                                     DokumentBehandlingTjeneste dokumentBehandlingTjeneste) {
        this.mellomlagringRepository = mellomlagringRepository;
        this.behandlingRepository = behandlingRepository;
        this.dokumentBehandlingTjeneste = dokumentBehandlingTjeneste;
    }

    @POST
    @Path("/hent")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter mellomlagret innhold for en behandling. Bruk enten 'type' eller 'dokumentMal' for å identifisere hva som hentes.", tags = "mellomlagring")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response hentMellomlagring(@TilpassetAbacAttributt(supplierClass = HentMellomlagringAbacSupplier.class) @Valid @NotNull HentMellomlagringDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.behandlingUuid());
        var resolvedType = resolveType(dto.type(), dto.dokumentMal());
        var innhold = mellomlagringRepository.hentMellomlagring(behandling.getId(), resolvedType)
            .map(MellomlagringEntitet::getInnhold);
        if (innhold.isPresent()) {
            return Response.ok(new MellomlagringResultatDto(innhold.get())).build();
        }
        return Response.noContent().build();
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    @Operation(description = "Lagrer eller sletter mellomlagret innhold for en behandling. Bruk enten 'type' eller 'dokumentMal' for å identifisere hva som lagres.", tags = "mellomlagring")
    @BeskyttetRessurs(actionType = ActionType.UPDATE, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response lagreMellomlagring(@TilpassetAbacAttributt(supplierClass = MellomlagringDtoAbacSupplier.class) @Valid @NotNull MellomlagringDto dto) {
        var behandling = behandlingRepository.hentBehandling(dto.behandlingUuid());
        var resolvedType = resolveType(dto.type(), dto.dokumentMal());
        if (dto.innhold() == null) {
            LOG.info("Mellomlagring slett mottatt, type {}", resolvedType);
        }
        if (resolvedType == MellomlagringType.VEDTAKSBREV) {
            if (dto.innhold() == null) {
                dokumentBehandlingTjeneste.fjernOverstyringAvBrev(behandling);
            } else {
                dokumentBehandlingTjeneste.lagreOverstyrtBrev(behandling, dto.innhold());
            }
        } else {
            if (dto.innhold() == null) {
                if (erLåst(behandling.getId(), resolvedType)) {
                    LOG.info("Ignorerer slett av mellomlagring, type {} - bestilling pågår", resolvedType);
                    return Response.ok().build();
                }
                mellomlagringRepository.fjernMellomlagring(behandling.getId(), resolvedType);
            } else {
                validerIkkeLåst(behandling.getId(), resolvedType);
                mellomlagringRepository.lagreEllerOppdater(behandling.getId(), resolvedType, dto.innhold());
            }
        }
        return Response.ok().build();
    }

    private void validerIkkeLåst(Long behandlingId, MellomlagringType type) {
        if (erLåst(behandlingId, type)) {
            throw new IllegalStateException("Mellomlagring er låst for endring mens bestilling pågår, type: " + type);
        }
    }

    private boolean erLåst(Long behandlingId, MellomlagringType type) {
        return mellomlagringRepository.hentMellomlagring(behandlingId, type)
            .filter(MellomlagringEntitet::isBestillingLåst)
            .isPresent();
    }

    private static MellomlagringType resolveType(MellomlagringType type, DokumentMalType dokumentMalType) {
        if (type != null) {
            return type;
        }
        if (dokumentMalType != null) {
            var resolved = MellomlagringType.fraDokumentMalType(dokumentMalType);
            if (resolved == null) {
                throw new IllegalArgumentException("Ukjent dokumentMal for mellomlagring: " + dokumentMalType);
            }
            return resolved;
        }
        // Ingen dokumentMal = vedtaksbrev (samme konvensjon som hentBrevHtml)
        return MellomlagringType.VEDTAKSBREV;
    }

    public record MellomlagringDto(
        @Valid @NotNull UUID behandlingUuid,
        @Valid MellomlagringType type,
        @ValidKodeverk DokumentMalType dokumentMal,
        @Fritekst @Size(max = 200_000) String innhold
    ) {}

    public record MellomlagringResultatDto(String innhold) {}

    public static class MellomlagringDtoAbacSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (MellomlagringDto) obj;
            return AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid);
        }
    }

    public record HentMellomlagringDto(
        @Valid @NotNull UUID behandlingUuid,
        @Valid MellomlagringType type,
        @ValidKodeverk DokumentMalType dokumentMal
    ) {}

    public static class HentMellomlagringAbacSupplier implements Function<Object, AbacDataAttributter> {
        @Override
        public AbacDataAttributter apply(Object obj) {
            var req = (HentMellomlagringDto) obj;
            return AbacDataAttributter.opprett().leggTil(StandardAbacAttributtType.BEHANDLING_UUID, req.behandlingUuid);
        }
    }
}
