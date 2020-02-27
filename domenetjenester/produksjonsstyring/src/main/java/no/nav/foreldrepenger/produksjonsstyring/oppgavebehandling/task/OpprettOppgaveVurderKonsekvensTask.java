package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.VURDER_KONS_FOR_YTELSE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.FagsakProsessTask;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import static no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask.TASKTYPE;

/**
 * <p>
 * ProsessTask som oppretter en oppgave i GSAK av typen vurder konsekvens for ytelse
 * <p>
 * </p>
 */
@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveVurderKonsekvensTask extends FagsakProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveVurderKonsekvens";
    public static final String KEY_BEHANDLENDE_ENHET = "behandlendEnhetsId";
    public static final String KEY_BESKRIVELSE = "beskrivelse";
    public static final String KEY_PRIORITET = "prioritet";
    public static final String PRIORITET_HØY = "høy";
    public static final String PRIORITET_NORM = "normal";
    public static final String STANDARD_BESKRIVELSE = "Må behandle sak i VL!";
    public static final String KEY_GJELDENDE_AKTØR_ID = "aktuellAktoerId"; //Settes kun ved opphør av ytelse i Infotrygd ellers null
    private static final Logger log = LoggerFactory.getLogger(OpprettOppgaveVurderKonsekvensTask.class);

    private OppgaveTjeneste oppgaveTjeneste;

    OpprettOppgaveVurderKonsekvensTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveVurderKonsekvensTask(OppgaveTjeneste oppgaveTjeneste,
                                              BehandlingRepositoryProvider repositoryProvider) {
        super(repositoryProvider.getFagsakLåsRepository(), repositoryProvider.getBehandlingLåsRepository());
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData){
        String behandlendeEnhet = prosessTaskData.getPropertyValue(KEY_BEHANDLENDE_ENHET);
        String beskrivelse = prosessTaskData.getPropertyValue(KEY_BESKRIVELSE);
        String prioritet = prosessTaskData.getPropertyValue(KEY_PRIORITET);
        Optional<String> gjeldendeAktørId = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_GJELDENDE_AKTØR_ID));

        boolean høyPrioritet = PRIORITET_HØY.equals(prioritet);

        //vurder opphør av ytelse i Infotrygd pga overlapp på far - vet ikke saksnummer
        String oppgaveId;
        if (gjeldendeAktørId.isPresent()) {
            oppgaveId = oppgaveTjeneste.opprettMedPrioritetOgBeskrivelseBasertPåAktørId(gjeldendeAktørId.get(), prosessTaskData.getFagsakId(), VURDER_KONS_FOR_YTELSE, behandlendeEnhet, beskrivelse, høyPrioritet);
        }else {
            oppgaveId = oppgaveTjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(prosessTaskData.getFagsakId(), VURDER_KONS_FOR_YTELSE, behandlendeEnhet, beskrivelse, høyPrioritet);
        }
        log.info("Oppgave opprettet i GSAK for å vurdere konsekvens for ytelse på enhet {}. Oppgavenummer: {}. Prioritet: {}", behandlendeEnhet, oppgaveId, prioritet); // NOSONAR
    }
}
