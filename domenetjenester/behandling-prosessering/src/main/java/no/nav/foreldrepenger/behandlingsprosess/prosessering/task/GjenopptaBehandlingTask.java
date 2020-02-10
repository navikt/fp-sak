package no.nav.foreldrepenger.behandlingsprosess.prosessering.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHandler;

/**
 * Utfører automatisk gjenopptagelse av en behandling som har
 * et åpent aksjonspunkt som er et autopunkt og har en frist som er passert.
 */
@ApplicationScoped
@ProsessTask(GjenopptaBehandlingTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class GjenopptaBehandlingTask implements ProsessTaskHandler {

    public static final String TASKTYPE = "behandlingskontroll.gjenopptaBehandling";

    private BehandlingRepository behandlingRepository;
    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RegisterdataEndringshåndterer registerdataOppdaterer;
    private BehandlendeEnhetTjeneste behandlendeEnhetTjeneste;

    GjenopptaBehandlingTask() {
        // for CDI proxy
    }

    @Inject
    public GjenopptaBehandlingTask(BehandlingRepository behandlingRepository,
                                   BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                   RegisterdataEndringshåndterer registerdataOppdaterer,
                                   BehandlendeEnhetTjeneste behandlendeEnhetTjeneste) {

        this.behandlingRepository = behandlingRepository;
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.registerdataOppdaterer = registerdataOppdaterer;
        this.behandlendeEnhetTjeneste = behandlendeEnhetTjeneste;
    }

    @Override
    public void doTask(ProsessTaskData prosessTaskData) {

        Long behandlingsId = prosessTaskData.getBehandlingId();
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingsId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingsId);

        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }

        behandlendeEnhetTjeneste.sjekkEnhetVedGjenopptak(behandling).ifPresent(organisasjonsEnhet -> {
            behandlendeEnhetTjeneste.oppdaterBehandlendeEnhet(behandling, organisasjonsEnhet, HistorikkAktør.VEDTAKSLØSNINGEN, "");
        });

        if (behandling.erYtelseBehandling() && behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.INNHENT_REGISTEROPP)) {
            registerdataOppdaterer.oppdaterRegisteropplysningerOgReposisjonerBehandlingVedEndringer(behandling);
        }
        behandlingskontrollTjeneste.prosesserBehandling(kontekst);
    }
}
