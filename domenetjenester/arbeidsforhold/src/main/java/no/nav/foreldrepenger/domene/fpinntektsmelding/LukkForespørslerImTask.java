package no.nav.foreldrepenger.domene.fpinntektsmelding;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakProsesstaskRekkefølge;
import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "fpinntektsmelding.lukkForesporsler")
@FagsakProsesstaskRekkefølge(gruppeSekvens = true)
public class LukkForespørslerImTask extends GenerellProsessTask {
    public static final String SAK_NUMMER = "saksnummer";
    public static final String ORG_NUMMER = "organisasjonnummer";
    public static final String STATUS = "status";

    private static final Logger LOG = LoggerFactory.getLogger(LukkForespørslerImTask.class);

    private FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste;

    LukkForespørslerImTask() {
        //CDI
    }


    @Inject
    public LukkForespørslerImTask(FpInntektsmeldingTjeneste fpInntektsmeldingTjeneste) {
        this.fpInntektsmeldingTjeneste = fpInntektsmeldingTjeneste;
    }

    @Override
    protected void prosesser(ProsessTaskData prosessTaskData, Long fagsakId, Long behandlingId) {
        var saksnummer = prosessTaskData.getPropertyValue(SAK_NUMMER);
        var status = prosessTaskData.getPropertyValue(STATUS);

        // UTFØRT branchen kan fjernes når alle eksisterende tasks er ferdigkjørt
        if (ForespørselStatus.UTFØRT.name().equals(status)) {
            LOG.warn("Ignorerer utdatert task for å lukke forespørsel i fpinntektsmelding for saksnummer {}. UTFØRT krever ikke lenger et lukkekall.", saksnummer);
        } else if (ForespørselStatus.UTGÅTT.name().equals(status)) {
            LOG.info("Starter task for å sette eventuelle forespørsler i fpinntektsmelding for saksnummer {} til utgått",  saksnummer);
            fpInntektsmeldingTjeneste.settForespørselTilUtgått(saksnummer);
        } else {
            throw new IllegalStateException("Ugyldig status: " + status);
        }
    }

}
