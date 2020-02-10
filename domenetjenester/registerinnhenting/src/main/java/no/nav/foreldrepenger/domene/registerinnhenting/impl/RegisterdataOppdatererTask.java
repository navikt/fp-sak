package no.nav.foreldrepenger.domene.registerinnhenting.impl;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.domene.registerinnhenting.RegisterdataEndringshåndterer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * Utfører innhenting av registerdata.
 */
@ApplicationScoped
@ProsessTask(RegisterdataOppdatererTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class RegisterdataOppdatererTask extends BehandlingProsessTask {

    private static final Logger log = LoggerFactory.getLogger(RegisterdataOppdatererTask.class);

    public static final String TASKTYPE = "behandlingskontroll.registerdataOppdaterBehandling";

    private BehandlingskontrollTjeneste behandlingskontrollTjeneste;
    private RegisterdataEndringshåndterer registerdataOppdaterer;
    private BehandlingRepository behandlingRepository;

    RegisterdataOppdatererTask() {
        // for CDI proxy
    }

    @Inject
    public RegisterdataOppdatererTask(BehandlingRepositoryProvider behandlingRepositoryProvider,
                                      BehandlingskontrollTjeneste behandlingskontrollTjeneste,
                                      RegisterdataEndringshåndterer registerdataOppdaterer) {
        super(behandlingRepositoryProvider.getBehandlingLåsRepository());
        this.behandlingskontrollTjeneste = behandlingskontrollTjeneste;
        this.registerdataOppdaterer = registerdataOppdaterer;
        this.behandlingRepository = behandlingRepositoryProvider.getBehandlingRepository();
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        Long behandlingsId = prosessTaskData.getBehandlingId();
        // NB lås før hent behandling
        BehandlingskontrollKontekst kontekst = behandlingskontrollTjeneste.initBehandlingskontroll(behandlingsId);
        Behandling behandling = behandlingRepository.hentBehandling(behandlingsId);

        // sjekk forhåndsbetingelser for å innhente registerdata
        if (behandling.erSaksbehandlingAvsluttet() || !behandling.erYtelseBehandling()) {
            log.info("Behandling er avsluttet eller feil type, kan ikke innhente registerdata: behandlingId={} status={}", behandlingsId, behandling.getStatus());
            return;
        }

        if (!behandlingskontrollTjeneste.erStegPassert(behandling, BehandlingStegType.INNHENT_REGISTEROPP)) {
            log.info("Behandling har ikke etablert grunnlag, skal ikke innhente registerdata: behandlingId={}", behandlingsId);
            return;
        }

        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
        registerdataOppdaterer.oppdaterRegisteropplysningerOgReposisjonerBehandlingVedEndringer(behandling);
        // I tilfelle tilbakehopp reåpner aksjonspunkt
        if (behandling.isBehandlingPåVent()) {
            behandlingskontrollTjeneste.taBehandlingAvVentSetAlleAutopunktUtført(behandling, kontekst);
        }
    }
}
