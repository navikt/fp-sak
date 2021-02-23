package no.nav.foreldrepenger.domene.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.RepositoryProvider;

public interface TestScenarioTillegg {
    void lagre(Behandling behandling, RepositoryProvider repositoryProvider);
}
