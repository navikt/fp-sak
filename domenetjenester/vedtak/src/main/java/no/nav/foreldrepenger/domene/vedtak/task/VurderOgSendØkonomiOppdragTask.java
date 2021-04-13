package no.nav.foreldrepenger.domene.vedtak.task;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomistøtte.OppdragInputTjeneste;
import no.nav.foreldrepenger.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.foreldrepenger.økonomistøtte.ny.postcondition.OppdragPostConditionTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(VurderOgSendØkonomiOppdragTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOgSendØkonomiOppdragTask extends BehandlingProsessTask {

    private static final Logger LOG = LoggerFactory.getLogger(VurderOgSendØkonomiOppdragTask.class);

    public static final String TASKTYPE = "iverksetteVedtak.oppdragTilØkonomi";

    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    private ProsessTaskRepository prosessTaskRepository;
    private OppdragPostConditionTjeneste oppdragPostConditionTjeneste;
    private OppdragInputTjeneste oppdragInputTjeneste;

    VurderOgSendØkonomiOppdragTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOgSendØkonomiOppdragTask(ProsessTaskRepository prosessTaskRepository,
                                          BehandlingRepositoryProvider repositoryProvider,
                                          OppdragskontrollTjeneste oppdragskontrollTjeneste,
                                          OppdragPostConditionTjeneste oppdragPostConditionTjeneste,
                                          OppdragInputTjeneste oppdragInputTjeneste) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.prosessTaskRepository = prosessTaskRepository;
        this.oppdragskontrollTjeneste = oppdragskontrollTjeneste;
        this.oppdragPostConditionTjeneste = oppdragPostConditionTjeneste;
        this.oppdragInputTjeneste = oppdragInputTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long behandlingId) {
        // Har vi mottatt kvittering?
        Optional<ProsessTaskHendelse> hendelse = prosessTaskData.getHendelse();
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
            Oppdragskontroll oppdragskontroll = oppdragskontrollOpt.get();
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
        prosessTaskData.venterPåHendelse(ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void sendØkonomioppdragTask(ProsessTaskData hovedProsessTask, Long behandlingId) {
        ProsessTaskData sendØkonomiOppdrag = new ProsessTaskData(SendØkonomiOppdragTask.TASKTYPE);
        sendØkonomiOppdrag.setGruppe(hovedProsessTask.getGruppe());
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setBehandling(hovedProsessTask.getFagsakId(),
            behandlingId,
            hovedProsessTask.getAktørId());
        prosessTaskRepository.lagre(sendØkonomiOppdrag);
    }

    private void sendTilkjentYtelse(ProsessTaskData hovedProsessTask, Long behandlingId) {
        ProsessTaskData sendØkonomiOppdrag = new ProsessTaskData(SendTilkjentYtelseTask.TASKTYPE);
        sendØkonomiOppdrag.setGruppe(hovedProsessTask.getGruppe());
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setBehandling(hovedProsessTask.getFagsakId(),
            behandlingId,
            hovedProsessTask.getAktørId());
        prosessTaskRepository.lagre(sendØkonomiOppdrag);
    }

    private void behandleHendelse(ProsessTaskHendelse prosessTaskHendelse, Long behandlingId) {
        if (prosessTaskHendelse == ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING) {
            LOG.info("Økonomioppdrag-kvittering mottatt for behandling: {}", behandlingId); //$NON-NLS-1$
        } else {
            throw new IllegalStateException("Uventet hendelse " + prosessTaskHendelse);
        }
    }
}
