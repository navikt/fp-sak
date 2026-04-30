package no.nav.foreldrepenger.web.app.tjenester.mellomlagring;

import java.util.UUID;
import java.util.function.Function;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.DokumentMalType;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.dokument.MellomlagringType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.dokumentbestiller.DokumentBehandlingTjeneste;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.BehandlingAbacSuppliers;
import no.nav.foreldrepenger.web.app.tjenester.behandling.dto.UuidDto;
import no.nav.vedtak.sikkerhet.abac.AbacDataAttributter;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.StandardAbacAttributtType;
import no.nav.vedtak.sikkerhet.abac.TilpassetAbacAttributt;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ActionType;
import no.nav.vedtak.sikkerhet.abac.beskyttet.ResourceType;
import no.nav.vedtak.util.Fritekst;

@Path(MellomlagringRestTjeneste.BASE_PATH)
@ApplicationScoped
@Transactional
public class MellomlagringRestTjeneste {

    static final String BASE_PATH = "/mellomlagring";
    public static final String MELLOMLAGRING_PATH = BASE_PATH;

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

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(description = "Henter mellomlagret innhold for en behandling. Bruk enten 'type' eller 'dokumentMal' for å identifisere hva som hentes.", tags = "mellomlagring")
    @BeskyttetRessurs(actionType = ActionType.READ, resourceType = ResourceType.FAGSAK, sporingslogg = true)
    public Response hentMellomlagring(
            @TilpassetAbacAttributt(supplierClass = BehandlingAbacSuppliers.UuidAbacDataSupplier.class) @QueryParam(UuidDto.NAME) @Parameter(description = UuidDto.DESC) @Valid UuidDto uuidDto,
            @QueryParam("type") @Parameter(description = "Type mellomlagring (f.eks. VARSEL_REVURDERING). Alternativ til dokumentMal.") MellomlagringType type,
            @QueryParam("dokumentMal") @Parameter(description = "Dokumentmal (f.eks. VARREV). Backend utleder mellomlagringstype. Utelates for vedtaksbrev.") String dokumentMal) {
        var behandling = behandlingRepository.hentBehandling(uuidDto.getBehandlingUuid());
        var resolvedType = resolveType(type, dokumentMal);
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
        if (resolvedType == MellomlagringType.VEDTAKSBREV) {
            if (dto.innhold() == null) {
                dokumentBehandlingTjeneste.fjernOverstyringAvBrev(behandling);
            } else {
                dokumentBehandlingTjeneste.lagreOverstyrtBrev(behandling, dto.innhold());
            }
        } else {
            validerIkkeLåst(behandling.getId(), resolvedType);
            if (dto.innhold() == null) {
                mellomlagringRepository.fjernMellomlagring(behandling.getId(), resolvedType);
            } else {
                mellomlagringRepository.lagreEllerOppdater(behandling.getId(), resolvedType, dto.innhold());
            }
        }
        return Response.ok().build();
    }

    private void validerIkkeLåst(Long behandlingId, MellomlagringType type) {
        mellomlagringRepository.hentMellomlagring(behandlingId, type)
            .filter(MellomlagringEntitet::isBestillingLåst)
            .ifPresent(m -> {
                throw new IllegalStateException("Mellomlagring er låst for endring mens bestilling pågår. BehandlingId: " + behandlingId + ", type: " + type);
            });
    }

    private static MellomlagringType resolveType(MellomlagringType type, String dokumentMal) {
        if (type != null) {
            return type;
        }
        if (dokumentMal != null && !dokumentMal.isEmpty()) {
            var dokumentMalType = DokumentMalType.fraKode(dokumentMal);
            var resolved = MellomlagringType.fraDokumentMalType(dokumentMalType);
            if (resolved == null) {
                throw new IllegalArgumentException("Ukjent dokumentMal for mellomlagring: " + dokumentMal);
            }
            return resolved;
        }
        // Ingen dokumentMal = vedtaksbrev (samme konvensjon som hentBrevHtml)
        return MellomlagringType.VEDTAKSBREV;
    }

    public record MellomlagringDto(
        @Valid @NotNull UUID behandlingUuid,
        @Valid MellomlagringType type,
        @Pattern(regexp = "^[A-Z_]*$") @Size(max = 50) String dokumentMal,
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
}
