package no.nav.foreldrepenger.domene.vedtak;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;

public interface OpprettProsessTaskIverksett {
    String VEDTAK_TIL_DATAVAREHUS_TASK = "iverksetteVedtak.vedtakTilDatavarehus";

    void opprettIverksettingstasker(Behandling behandling);
}
