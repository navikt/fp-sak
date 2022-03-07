package no.nav.foreldrepenger.mottak.kabal;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.control.ActivateRequestContext;
import javax.inject.Inject;
import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.domene.json.StandardJsonConfig;
import no.nav.vedtak.exception.VLException;
import no.nav.vedtak.felles.prosesstask.api.ProsessTaskTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;
import no.nav.vedtak.log.util.LoggerUtils;


@Transactional
@ActivateRequestContext
@ApplicationScoped
public class KabalHendelseHåndterer {

    private static final Logger LOG = LoggerFactory.getLogger(KabalHendelseHåndterer.class);

    private ProsessTaskTjeneste taskTjeneste;

    KabalHendelseHåndterer() {
        // CDI
    }

    @Inject
    public KabalHendelseHåndterer(ProsessTaskTjeneste taskTjeneste) {
        this.taskTjeneste = taskTjeneste;
    }

    void handleMessage(String key, String payload) {
        // enhver exception ut fra denne metoden medfører at tråden som leser fra kafka gir opp og stopper.
        try {
            var mottattHendelse = StandardJsonConfig.fromJson(payload, KabalHendelse.class);
            setCallIdForHendelse(mottattHendelse);
            LOG.info("KABAL mottatt hendelse key={} hendelse={}", key, mottattHendelse);
        } catch (VLException e) {
            LOG.info("FP-328773 KABAL Feil under parsing av vedtak. key={} payload={}", key, payload, e);
        } catch (Exception e) {
            LOG.info("Vedtatt-Ytelse exception ved håndtering av vedtaksmelding, ignorerer key={}", LoggerUtils.removeLineBreaks(payload), e);
        }
    }

    private static void setCallIdForHendelse(KabalHendelse hendelse) {
        if (hendelse.eventId() == null) {
            MDCOperations.putCallId();
        } else {
            MDCOperations.putCallId(hendelse.eventId().toString());
        }
    }

}
