package no.nav.foreldrepenger.domene.vedtak.task;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.finn.unleash.Unleash;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.BehandlingProsessTask;
import no.nav.foreldrepenger.behandlingslager.økonomioppdrag.Oppdragskontroll;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskHendelse;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskRepository;

@ApplicationScoped
@ProsessTask(VurderOgSendØkonomiOppdragTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class VurderOgSendØkonomiOppdragTask extends BehandlingProsessTask {

    private static final Logger log = LoggerFactory.getLogger(VurderOgSendØkonomiOppdragTask.class);

    public static final String TASKTYPE = "iverksetteVedtak.oppdragTilØkonomi";

    private OppdragskontrollTjeneste oppdragskontrollTjeneste;
    private ProsessTaskRepository prosessTaskRepository;

    private Unleash unleash;

    VurderOgSendØkonomiOppdragTask() {
        // for CDI proxy
    }

    @Inject
    public VurderOgSendØkonomiOppdragTask(OppdragskontrollTjeneste oppdragskontrollTjeneste,
                                          ProsessTaskRepository prosessTaskRepository,
                                          BehandlingRepositoryProvider repositoryProvider, Unleash unleash) {
        super(repositoryProvider.getBehandlingLåsRepository());
        this.oppdragskontrollTjeneste = oppdragskontrollTjeneste;
        this.prosessTaskRepository = prosessTaskRepository;
        this.unleash = unleash;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        // Har vi mottatt kvittering?
        Optional<ProsessTaskHendelse> hendelse = prosessTaskData.getHendelse();
        Long behandlingId = prosessTaskData.getBehandlingId();
        if (hendelse.isPresent()) {
            behandleHendelse(hendelse.get(), behandlingId);
            return;
        }
        vurderSendingAvOppdrag(prosessTaskData, behandlingId);
    }

    private void vurderSendingAvOppdrag(ProsessTaskData prosessTaskData, Long behandlingId) {
        Optional<Oppdragskontroll> oppdragskontrollOpt = oppdragskontrollTjeneste.opprettOppdrag(behandlingId, prosessTaskData.getId());
        if (oppdragskontrollOpt.isPresent()) {
            log.info("Klargjør økonomioppdrag for behandling: {}", behandlingId); //$NON-NLS-1$
            Oppdragskontroll oppdragskontroll = oppdragskontrollOpt.get();
            oppdragskontrollTjeneste.lagre(oppdragskontroll);
            oppdaterProsessTask(prosessTaskData);

            sendØkonomioppdragTask(prosessTaskData);

            log.info("Økonomioppdrag er klargjort for behandling: {}", behandlingId); //$NON-NLS-1$
        } else {
            log.info("Ikke aktuelt for behandling: {}", behandlingId); //$NON-NLS-1$
        }

        sendTilkjentYtelse(prosessTaskData);
    }

    private void oppdaterProsessTask(ProsessTaskData prosessTaskData) {
        prosessTaskData.venterPåHendelse(ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING);
        prosessTaskData.setCallIdFraEksisterende();
        prosessTaskRepository.lagre(prosessTaskData);
    }

    private void sendØkonomioppdragTask(ProsessTaskData hovedProsessTask) {
        ProsessTaskData sendØkonomiOppdrag = new ProsessTaskData(SendØkonomiOppdragTask.TASKTYPE);
        sendØkonomiOppdrag.setGruppe(hovedProsessTask.getGruppe());
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setBehandling(hovedProsessTask.getFagsakId(),
            hovedProsessTask.getBehandlingId(),
            hovedProsessTask.getAktørId());
        prosessTaskRepository.lagre(sendØkonomiOppdrag);
    }

    private void sendTilkjentYtelse(ProsessTaskData hovedProsessTask) {
        ProsessTaskData sendØkonomiOppdrag = new ProsessTaskData(SendTilkjentYtelseTask.TASKTYPE);
        sendØkonomiOppdrag.setGruppe(hovedProsessTask.getGruppe());
        sendØkonomiOppdrag.setCallIdFraEksisterende();
        sendØkonomiOppdrag.setBehandling(hovedProsessTask.getFagsakId(),
            hovedProsessTask.getBehandlingId(),
            hovedProsessTask.getAktørId());
        prosessTaskRepository.lagre(sendØkonomiOppdrag);
    }

    private void behandleHendelse(ProsessTaskHendelse prosessTaskHendelse, Long behandlingId) {
        if (prosessTaskHendelse == ProsessTaskHendelse.ØKONOMI_OPPDRAG_KVITTERING) {
            log.info("Økonomioppdrag-kvittering mottatt for behandling: {}", behandlingId); //$NON-NLS-1$
        } else {
            throw new IllegalStateException("Uventet hendelse " + prosessTaskHendelse);
        }
    }
}
