package no.nav.foreldrepenger.domene.vedtak.task;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.økonomistøtte.BehandleØkonomioppdragKvittering;
import no.nav.foreldrepenger.økonomistøtte.OppdragInputTjeneste;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.postcondition.OppdragPostConditionTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;

@ApplicationScoped
@ProsessTask(value = "iverksetteVedtak.oppdragTilØkonomi", maxFailedRuns = 1) // TODO BehandleNegativeKvitteringTjenesteTest deps on name
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOgSendØkonomiOppdragTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(VurderOgSendØkonomiOppdragTask.class);

    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    private ProsessTaskTjeneste taskTjeneste;
    private OppdragPostConditionTjeneste oppdragPostConditionTjeneste;
    private OppdragInputTjeneste oppdragInputTjeneste;

    VurderOgSendØkonomiOppdragTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOgSendØkonomiOppdragTask(ProsessTaskTjeneste taskTjeneste,
                                          BehandlingRepositoryProvider repositoryProvider,
                                          OppdragskontrollTjeneste oppdragskontrollTjeneste,
                                          OppdragPostConditionTjeneste oppdragPostConditionTjeneste,
                                          OppdragInputTjeneste oppdragInputTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.taskTjeneste = taskTjeneste;
        this.oppdragskontrollTjeneste = oppdragskontrollTjeneste;
        this.oppdragPostConditionTjeneste = oppdragPostConditionTjeneste;
        this.oppdragInputTjeneste = oppdragInputTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        // Har vi mottatt kvittering?
        var hendelse = prosessTaskData.getVentetHendelse();
        if (hendelse.isPresent()) {
            behandleHendelse(hendelse.get(), behandlingId);
            return;
        }
        vurderSendingAvOppdrag(prosessTaskData, behandlingId);
        oppdragPostConditionTjeneste.softPostCondition(behandlingId);
    }

    private void vurderSendingAvOppdrag(ProsessTaskData prosessTaskData, Long behandlingId) {
        LOG.info("Produserer oppdrag for behandlingId: {}", behandlingId);
        var input = oppdragInputTjeneste.lagOppdragInput(behandlingId, prosessTaskData.getId());
        var oppdragskontrollOpt = oppdragskontrollTjeneste.opprettOppdrag(input);

        if (oppdragskontrollOpt.isPresent()) {
            LOG.info("Klargjør økonomioppdrag for behandling: {}", behandlingId); //$NON-NLS-1$
            var oppdragskontroll = oppdragskontrollOpt.get();
            oppdragskontrollTjeneste.lagre(oppdragskontroll);
            oppdaterProsessTask(prosessTaskData);

            sendØkonomioppdragTask(prosessTaskData, behandlingId);

            LOG.info("Økonomioppdrag er klargjort for behandling: {}", behandlingId); //$NON-NLS-1$
        } else {
            LOG.info("Ikke aktuelt for behandling: {}", behandlingId); //$NON-NLS-1$
        }

        sendTilkjentYtelse(prosessTaskData, behandlingId);
    }

    private void oppdaterProsessTask(ProsessTaskData prosessTaskData) {
        prosessTaskData.venterPåHendelse(BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING);
        prosessTaskData.setCallIdFraEksisterende();
        taskTjeneste.lagre(prosessTaskData);
    }

    private void sendØkonomioppdragTask(ProsessTaskData hovedProsessTask, Long behandlingId) {
        var sendØkonomiOppdrag = ProsessTaskData.forProsessTask(SendØkonomiOppdragTask.class);
        sendØkonomiOppdrag.setGruppe(hovedProsessTask.getGruppe());
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setBehandling(hovedProsessTask.getFagsakId(),
            behandlingId,
            hovedProsessTask.getAktørId());
        taskTjeneste.lagre(sendØkonomiOppdrag);
    }

    private void sendTilkjentYtelse(ProsessTaskData hovedProsessTask, Long behandlingId) {
        var sendØkonomiOppdrag = ProsessTaskData.forProsessTask(SendTilkjentYtelseTask.class);
        sendØkonomiOppdrag.setGruppe(hovedProsessTask.getGruppe());
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setBehandling(hovedProsessTask.getFagsakId(),
            behandlingId,
            hovedProsessTask.getAktørId());
        taskTjeneste.lagre(sendØkonomiOppdrag);
    }

    private void behandleHendelse(String prosessTaskHendelse, Long behandlingId) {
        if (BehandleØkonomioppdragKvittering.ØKONOMI_OPPDRAG_KVITTERING.equals(prosessTaskHendelse)) {
            LOG.info("Økonomioppdrag-kvittering mottatt for behandling: {}", behandlingId); //$NON-NLS-1$
        } else {
            throw new IllegalStateException("Uventet hendelse " + prosessTaskHendelse);
        }
    }
}
