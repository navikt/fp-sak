package no.nav.foreldrepenger.domene.fpinntektsmelding;

import static no.nav.foreldrepenger.behandlingslager.virksomhet.OrgNummer.tilMaskertNummer;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.task.GenerellProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTask;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskData;

@ApplicationScoped
@ProsessTask(value = "fpinntektsmelding.lukkForesporsler")
public class LukkForespørslerImTask extends GenerellProsessTask {
    public static final String SAK_NUMMER = "saksnummer";
    public static final String ORG_NUMMER = "organisasjonnummer";

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
        var orgnummer = prosessTaskData.getPropertyValue(ORG_NUMMER);

        LOG.info("Starter task for å lukke eventuelle forespørsler i fpinntektsmelding for saksnummer {} og orgnummer {}",  saksnummer, tilMaskertNummer(orgnummer));
        fpInntektsmeldingTjeneste.lukkForespørsel(saksnummer, orgnummer);
    }

}
