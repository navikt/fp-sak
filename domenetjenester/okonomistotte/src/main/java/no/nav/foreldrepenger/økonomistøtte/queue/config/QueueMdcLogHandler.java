package no.nav.foreldrepenger.økonomistøtte.queue.config;

import javax.enterprise.context.ApplicationScoped;

import no.nav.vedtak.felles.integrasjon.jms.MdcHandler;
import no.nav.vedtak.log.mdc.MDCOperations;

@ApplicationScoped
public class QueueMdcLogHandler implements MdcHandler {
    @Override
    public void setCallId(String callId) {
        MDCOperations.putCallId(callId);
    }

    @Override
    public void settNyCallId() {
        MDCOperations.putCallId();
    }

    @Override
    public void removeCallId() {
        MDCOperations.removeCallId();
    }
}
