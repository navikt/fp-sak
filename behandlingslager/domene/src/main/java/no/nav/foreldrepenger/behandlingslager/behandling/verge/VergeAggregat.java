package no.nav.foreldrepenger.behandlingslager.behandling.verge;

import java.util.Optional;

import no.nav.foreldrepenger.domene.typer.AktørId;

public class VergeAggregat {

    private final VergeEntitet verge;

    public VergeAggregat(VergeEntitet verge) {
        this.verge = verge;
    }

    public Optional<VergeEntitet> getVerge() {
        return Optional.ofNullable(verge);
    }

    public Optional<AktørId> getAktørId() {
        return verge.getBruker() != null ? Optional.ofNullable(verge.getBruker().getAktørId()) : Optional.empty();
    }
}
