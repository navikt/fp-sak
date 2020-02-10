package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.VURDER_DOKUMENT;
import static no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderDokumentTask.TASKTYPE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveVurderDokumentTask extends FagsakProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveVurderDokument";
    public static final String KEY_BEHANDLENDE_ENHET = "behandlendEnhetsId";
    public static final String KEY_DOKUMENT_TYPE = "dokumentTypeId";
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveVurderDokumentTask.class);

    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveVurderDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveVurderDokumentTask(OppgaveTjeneste oppgaveTjeneste, BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData) {
        String behandlendeEnhet = prosessTaskData.getPropertyValue(KEY_BEHANDLENDE_ENHET);
        DokumentTypeId dokumentTypeId = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_DOKUMENT_TYPE))
            .map(dtid -> DokumentTypeId.fraKode(dtid)).orElse(DokumentTypeId.UDEFINERT);
        String beskrivelse = dokumentTypeId.getNavn();
        if (beskrivelse == null) {
            beskrivelse = dokumentTypeId.getKode();
        }

        String oppgaveId = oppgaveTjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(prosessTaskData.getFagsakId(),
            VURDER_DOKUMENT, behandlendeEnhet, "VL: " + beskrivelse, false);
        log.info("Oppgave opprettet i GSAK for å vurdere dokument på enhet {}. Oppgavenummer: {}", behandlendeEnhet, oppgaveId);
    }
}
