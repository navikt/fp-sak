package no.nav.foreldrepenger.web.app.tjenester.integrasjonstatus;

import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.transaction.Transactional;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.commons.lang3.BooleanUtils;

import io.swagger.v3.oas.annotations.Operation;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessurs;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursActionAttributt;
import no.nav.vedtak.sikkerhet.abac.BeskyttetRessursResourceAttributt;

@Path("/integrasjon")
@ApplicationScoped
@Transactional
@Produces(MediaType.APPLICATION_JSON)
public class IntegrasjonstatusRestTjeneste {

    private IntegrasjonstatusTjeneste integrasjonstatusTjeneste;
    private boolean skalViseDetaljerteFeilmeldinger;

    public IntegrasjonstatusRestTjeneste() {
        // CDI
    }

    @Inject
    public IntegrasjonstatusRestTjeneste(IntegrasjonstatusTjeneste integrasjonstatusTjeneste,
                                         @KonfigVerdi(value = "vise.detaljerte.feilmeldinger", defaultVerdi = "true") Boolean viseDetaljerteFeilmeldinger) {
        this.integrasjonstatusTjeneste = integrasjonstatusTjeneste;
        this.skalViseDetaljerteFeilmeldinger = BooleanUtils.toBoolean(viseDetaljerteFeilmeldinger);
    }

    @GET
    @Path("/status")
    @Operation(description = "Gir en oversikt over systemer som er nede",
        summary = ("Inneholder også detaljer og evt kjent tidspunkt for når systemet er oppe igjen."), tags = "integrasjon")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.APPLIKASJON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public List<SystemNedeDto> finnSystemerSomErNede() {
        return integrasjonstatusTjeneste.finnSystemerSomErNede();
    }

    @GET
    @Path("/status/vises")
    @Operation(description = "Returnerer en boolean som angir om detaljerte feilmeldinger skal vises av GUI", tags = "integrasjon")
    @BeskyttetRessurs(action = BeskyttetRessursActionAttributt.READ, ressurs = BeskyttetRessursResourceAttributt.APPLIKASJON)
    @SuppressWarnings("findsecbugs:JAXRS_ENDPOINT")
    public boolean skalViseDetaljerteFeilmeldinger() {
        return skalViseDetaljerteFeilmeldinger;
    }
}
