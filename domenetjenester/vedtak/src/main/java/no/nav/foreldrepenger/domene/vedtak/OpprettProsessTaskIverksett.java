package no.nav.foreldrepenger.domene.vedtak;

import java.util.List;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public interface OpprettProsessTaskIverksett {
    String VEDTAK_TIL_DATAVAREHUS_TASK = "iverksetteVedtak.vedtakTilDatavarehus";

    void opprettIverksettingstasker(Behandling behandling, List<String> inititellTaskNavn);
}
