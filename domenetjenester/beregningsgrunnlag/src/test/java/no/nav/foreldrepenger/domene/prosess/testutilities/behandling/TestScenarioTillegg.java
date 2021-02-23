package no.nav.foreldrepenger.domene.prosess.testutilities.behandling;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.domene.prosess.RepositoryProvider;

public interface TestScenarioTillegg {
    void lagre(Behandling behandling, RepositoryProvider repositoryProvider);
}
