package no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.testutilities.behandling;

import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.RepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public interface TestScenarioTillegg {
    void lagre(Behandling behandling, RepositoryProvider repositoryProvider);
}
