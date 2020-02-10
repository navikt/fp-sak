package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.adapter;

import java.time.LocalDate;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.impl.FinnAnsvarligSaksbehandler;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.wrapper.BehandlingVedtakOppdrag;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
public class MapBehandlingVedtak {
    private BehandlingVedtakRepository behandlingVedtakRepository;

    MapBehandlingVedtak() {
        // for CDI proxy
    }

    @Inject
    public MapBehandlingVedtak(BehandlingVedtakRepository behandlingVedtakRepository) {
        this.behandlingVedtakRepository = behandlingVedtakRepository;
    }

    public BehandlingVedtakOppdrag map(Behandling behandling) {
        Optional<BehandlingVedtak> behandlingVedtakOpt = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandling.getId());

        String ansvarligSaksbehandler = behandlingVedtakOpt.map(BehandlingVedtak::getAnsvarligSaksbehandler)
            .orElseGet(() -> FinnAnsvarligSaksbehandler.finn(behandling));
        LocalDate vedtaksdato = behandlingVedtakOpt.map(BehandlingVedtak::getVedtaksdato)
            .orElseGet(FPDateUtil::iDag);
        BehandlingResultatType behandlingResultatType = behandling.getBehandlingsresultat().getBehandlingResultatType();
        return new BehandlingVedtakOppdrag(ansvarligSaksbehandler, behandlingResultatType, vedtaksdato);
    }
}
