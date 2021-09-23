package no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task;

import static no.nav.foreldrepenger.historikk.OppgaveÅrsak.VURDER_KONS_FOR_YTELSE;
import static no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.task.OpprettOppgaveVurderKonsekvensTask.TASKTYPE;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.behandlingenhet.BehandlendeEnhetTjeneste;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

/**
 * <p>
 * ProsessTask som oppretter en oppgave i GSAK av typen vurder konsekvens for ytelse
 * <p>
 * </p>
 */
@ApplicationScoped
@ProsessTask(TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class OpprettOppgaveVurderKonsekvensTask extends GenerellProsessTask {
    public static final String TASKTYPE = "oppgavebehandling.opprettOppgaveVurderKonsekvens";
    public static final String KEY_BEHANDLENDE_ENHET = "behandlendEnhetsId";
    public static final String KEY_BESKRIVELSE = "beskrivelse";
    public static final String KEY_PRIORITET = "prioritet";
    public static final String PRIORITET_HØY = "høy";
    public static final String PRIORITET_NORM = "normal";
    public static final String STANDARD_BESKRIVELSE = "Må behandle sak i VL!";
    public static final String KEY_GJELDENDE_AKTØR_ID = "aktuellAktoerId"; //Settes kun ved opphør av ytelse i Infotrygd ellers null
    private static final Logger LOG = LoggerFactory.getLogger(OpprettOppgaveVurderKonsekvensTask.class);

    private OppgaveTjeneste oppgaveTjeneste;
    private BehandlendeEnhetTjeneste enhetTjeneste;

    OpprettOppgaveVurderKonsekvensTask() {
        // for CDI proxy
    }

    @Inject
    public OpprettOppgaveVurderKonsekvensTask(OppgaveTjeneste oppgaveTjeneste,
                                              BehandlendeEnhetTjeneste enhetTjeneste) {
        super();
        this.oppgaveTjeneste = oppgaveTjeneste;
        this.enhetTjeneste = enhetTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId){
        var behandlendeEnhet = prosessTaskData.getPropertyValue(KEY_BEHANDLENDE_ENHET);
        var beskrivelse = prosessTaskData.getPropertyValue(KEY_BESKRIVELSE);
        var prioritet = prosessTaskData.getPropertyValue(KEY_PRIORITET);
        var gjeldendeAktørId = Optional.ofNullable(prosessTaskData.getPropertyValue(KEY_GJELDENDE_AKTØR_ID));

        var enhet = enhetTjeneste.gyldigEnhetNfpNk(behandlendeEnhet) ? behandlendeEnhet :
            gjeldendeAktørId.map(a -> enhetTjeneste.finnBehandlendeEnhetForAktørId(new AktørId(a)))
                .orElseGet(() -> enhetTjeneste.finnBehandlendeEnhetForFagsakId(fagsakId)).enhetId();

        var høyPrioritet = PRIORITET_HØY.equals(prioritet);

        //vurder opphør av ytelse i Infotrygd pga overlapp på far - vet ikke saksnummer
        String oppgaveId;
        if (gjeldendeAktørId.isPresent()) {
            oppgaveId = oppgaveTjeneste.opprettMedPrioritetOgBeskrivelseBasertPåAktørId(gjeldendeAktørId.get(), prosessTaskData.getFagsakId(), VURDER_KONS_FOR_YTELSE, enhet, beskrivelse, høyPrioritet);
        }else {
            oppgaveId = oppgaveTjeneste.opprettMedPrioritetOgBeskrivelseBasertPåFagsakId(prosessTaskData.getFagsakId(), VURDER_KONS_FOR_YTELSE, enhet, beskrivelse, høyPrioritet);
        }
        LOG.info("Oppgave opprettet i GSAK for å vurdere konsekvens for ytelse på enhet {}. Oppgavenummer: {}. Prioritet: {}", enhet, oppgaveId, prioritet); // NOSONAR
    }
}
