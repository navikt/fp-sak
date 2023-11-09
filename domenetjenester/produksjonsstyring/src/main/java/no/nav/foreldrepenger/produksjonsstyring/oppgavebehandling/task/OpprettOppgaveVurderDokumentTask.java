package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.DokumentTypeId;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask("oppgavebehandling.opprettOppgaveVurderDokument")
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveVurderDokumentTask extends GenerellProsessTask {

    public static final String KEY_JOURNALPOST_ID = "journalpostId";
    public static final String KEY_BEHANDLENDE_ENHET = "behandlendEnhetsId";
    public static final String KEY_DOKUMENT_TYPE = "dokumentTypeId";
    private static final Logger LOG = LoggerFactory.getLogger(OpprettOppgaveVurderDokumentTask.class);

    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveVurderDokumentTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveVurderDokumentTask(OppgaveTjeneste oppgaveTjeneste) {
        super();
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var journalpostId = prosessTaskData.getPropertyValue(KEY_JOURNALPOST_ID);
        var behandlendeEnhet = prosessTaskData.getPropertyValue(KEY_BEHANDLENDE_ENHET);
        var dokumentTypeId = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_DOKUMENT_TYPE))
            .map(DokumentTypeId::fraKode).orElse(DokumentTypeId.UDEFINERT);
        var beskrivelse = dokumentTypeId.getNavn();
        if (beskrivelse == null) {
            beskrivelse = dokumentTypeId.getKode();
        }

        var oppgaveId = oppgaveTjeneste.opprettVurderDokumentMedBeskrivelseBasertPåFagsakId(fagsakId, journalpostId, behandlendeEnhet, "VL: " + beskrivelse);
        LOG.info("Oppgave opprettet i GSAK for å vurdere dokument på enhet {}. Oppgavenummer: {}", behandlendeEnhet, oppgaveId);
    }
}
