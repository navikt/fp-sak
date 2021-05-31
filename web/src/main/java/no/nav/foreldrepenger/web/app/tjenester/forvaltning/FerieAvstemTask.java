package no.nav.foreldrepenger.web.app.tjenester.forvaltning;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.foreldrepenger.behandlingsprosess.dagligejobber.infobrev.InformasjonssakRepository;
import no.nav.foreldrepenger.ytelse.beregning.FeriepengeReberegnTjeneste;
import no.nav.foreldrepenger.økonomistøtte.feriepengeavstemming.Feriepengeavstemmer;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
@ProsessTask(FerieAvstemTask.TASKTYPE)
@FagsakProsesstaskRekkefølge(gruppeSekvens = false)
public class FerieAvstemTask extends GenerellProsessTask {


    public static final String TASKTYPE = "iverksetteVedtak.validerOgRegenererVedtaksXmlTask";

    public static final String YTELSE_KEY = "ytelse";
    public static final String DATO_KEY = "dato";

    private FeriepengeReberegnTjeneste feriepengeRegeregnTjeneste;
    private InformasjonssakRepository repository;
    private Feriepengeavstemmer feriepengeavstemmer;

    public FerieAvstemTask() {
    }

    @Inject
    public FerieAvstemTask(FeriepengeReberegnTjeneste feriepengeRegeregnTjeneste,
         InformasjonssakRepository repository,
         Feriepengeavstemmer feriepengeavstemmer) {
        super();
        this.repository = repository;
        this.feriepengeRegeregnTjeneste = feriepengeRegeregnTjeneste;
        this.feriepengeavstemmer = feriepengeavstemmer;
    }

    @Override
    public void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var ytelse = Optional.ofNullable(prosessTaskData.getPropertyValue(YTELSE_KEY)).map(FagsakYtelseType::fraKode).orElse(FagsakYtelseType.UDEFINERT);
        var dato = LocalDate.parse(prosessTaskData.getPropertyValue(DATO_KEY), DateTimeFormatter.ISO_LOCAL_DATE);
        if (FagsakYtelseType.UDEFINERT.equals(ytelse) || dato == null) return;
        repository.finnSakerForAvstemmingFeriepenger(dato, dato, ytelse).stream()
            .map(Tuple::getElement2)
            .forEach(b -> {
                feriepengeRegeregnTjeneste.harDiffUtenomPeriode(b);
                feriepengeavstemmer.avstem(b, true);
            });

    }

}
