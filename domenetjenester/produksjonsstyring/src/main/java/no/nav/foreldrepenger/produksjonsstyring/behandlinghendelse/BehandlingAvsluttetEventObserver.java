package no.nav.foreldrepenger.produksjonsstyring.behandlinghendelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.events.BehandlingStatusEvent;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;

@ApplicationScoped
public class BehandlingAvsluttetEventObserver {

    private static final Logger LOG = LoggerFactory.getLogger(BehandlingAvsluttetEventObserver.class);

    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingRepository behandlingRepository;

    public BehandlingAvsluttetEventObserver() {
    }

    @Inject
    public BehandlingAvsluttetEventObserver(BeregningTjeneste beregningTjeneste,
                                            InntektArbeidYtelseTjeneste iayTjeneste,
                                            BehandlingRepository behandlingRepository) {
        this.beregningTjeneste = beregningTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public void observerBehandlingAvsluttetEvent(@Observes BehandlingStatusEvent.BehandlingAvsluttetEvent event) {
        var behandling = behandlingRepository.hentBehandling(event.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling);
        if (!ref.fagsakYtelseType().equals(FagsakYtelseType.ENGANGSTØNAD) && ref.behandlingType().erYtelseBehandlingType()) {
            LOG.info("Avslutter behandling {} i abakus og kalkulus", ref.behandlingUuid());
            iayTjeneste.avslutt(ref.behandlingId());
            beregningTjeneste.avslutt(ref);
        }
    }
}
