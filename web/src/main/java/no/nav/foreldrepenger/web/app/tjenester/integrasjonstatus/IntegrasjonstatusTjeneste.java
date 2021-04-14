package no.nav.foreldrepenger.web.app.tjenester.integrasjonstatus;

import java.util.ArrayList;
import java.util.List;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.web.app.healthchecks.SelftestResultat;
import no.nav.foreldrepenger.web.app.healthchecks.Selftests;

@ApplicationScoped
public class IntegrasjonstatusTjeneste {

    private Selftests selftests;

    public IntegrasjonstatusTjeneste() {
        // CDI
    }

    @Inject
    public IntegrasjonstatusTjeneste(Selftests selftests) {
        this.selftests = selftests;
    }

    public List<SystemNedeDto> finnSystemerSomErNede() {
        List<SystemNedeDto> systemerSomErNede = new ArrayList<>();

        var selftestResultat = selftests.run();
        var alleResultater = selftestResultat.getAlleResultater();
        for (var resultat : alleResultater) {
            if (!resultat.isReady()) {
                systemerSomErNede.add(lagDto(selftestResultat.getApplication(), resultat));
            }
        }

        return systemerSomErNede;
    }

    private SystemNedeDto lagDto(String system, SelftestResultat.InternalResult  resultat) {
        var systemNavn = system;
        var endepunkt = resultat.getEndpoint();
        return new SystemNedeDto(systemNavn, endepunkt, null, resultat.getDescription(), null);
    }
}
