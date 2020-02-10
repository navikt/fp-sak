package no.nav.foreldrepenger.web.app;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.foreldrepenger.kontrakter.formidling.v1.BehandlingUuidDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentProdusertDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.DokumentbestillingDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.HentBrevmalerDto;
import no.nav.foreldrepenger.kontrakter.formidling.v1.TekstFraSaksbehandlerDto;

@Path("/fpformidling/api")
@ApplicationScoped
public class FormidlingStubbRestTjeneste {

    private final Map<UUID, List<String>> dokumentProduksjon = new HashMap<>();
    private final Map<UUID, TekstFraSaksbehandlerDto> saksbehandlerTekst = new HashMap<>();

    public FormidlingStubbRestTjeneste() {
    }

    @POST
    @Operation(hidden = true)
    @Path("brev/maler")
    @Produces("application/json")
    public HentBrevmalerDto hentBrevmaler(@SuppressWarnings("unused") BehandlingUuidDto uuidDto) {
        return new HentBrevmalerDto(Collections.emptyList());
    }

    @POST
    @Operation(hidden = true)
    @Path("brev/dokument-sendt")
    @Produces("application/json")
    public Boolean erDokumentSendt(DokumentProdusertDto request) {
        return dokumentProduksjon.getOrDefault(request.getBehandlingUuid(), List.of()).contains(request.getDokumentMal());
    }

    @POST
    @Operation(hidden = true)
    @Path("brev/bestill")
    @Produces("application/json")
    public void bestillDokument(DokumentbestillingDto request) {
        dokumentProduksjon.putIfAbsent(request.getBehandlingUuid(), new ArrayList<>());
        dokumentProduksjon.get(request.getBehandlingUuid()).add(request.getDokumentMal());
    }

    @POST
    @Operation(hidden = true)
    @Path("saksbehandlertekst/hent")
    @Produces("application/json")
    public TekstFraSaksbehandlerDto hentSaksbehandlersTekst(BehandlingUuidDto uuidDto) {
        return saksbehandlerTekst.getOrDefault(uuidDto.getBehandlingUuid(), null);
    }

    @POST
    @Operation(hidden = true)
    @Path("saksbehandlertekst/lagre")
    @Produces("application/json")
    public void lagreSaksbehandlersTekst(TekstFraSaksbehandlerDto request) {
        saksbehandlerTekst.put(request.getBehandlingUuid(), request);
    }
}
