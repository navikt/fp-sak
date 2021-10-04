package no.nav.foreldrepenger.behandlingsprosess.hjelpemetoder;

import java.util.ArrayList;
import java.util.UUID;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.batch.BatchArguments;
import no.nav.foreldrepenger.batch.BatchTjeneste;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingKandidaterRepository;
import no.nav.foreldrepenger.behandlingsprosess.prosessering.BehandlingProsesseringTjeneste;
import no.nav.vedtak.log.mdc.MDCOperations;

/**
 * Midlertidig batch for å rekjøre kompletthetssjekk for endringssøknader som
 * venter på IM
 *
 * Skal kjøre en gang
 *
 */

@ApplicationScoped
class RekjørKompletthetssjekkForEndringssøknadBatch implements BatchTjeneste {

    private static final String BATCHNAME = "BVL033";

    private BehandlingKandidaterRepository behandlingKandidaterRepository;
    private BehandlingProsesseringTjeneste behandlingProsesseringTjeneste;

    @Inject
    public RekjørKompletthetssjekkForEndringssøknadBatch(BehandlingKandidaterRepository behandlingKandidaterRepository,
                                                         BehandlingProsesseringTjeneste behandlingProsesseringTjeneste) {
        this.behandlingKandidaterRepository = behandlingKandidaterRepository;
        this.behandlingProsesseringTjeneste = behandlingProsesseringTjeneste;
    }

    @Override
    public String launch(BatchArguments arguments) {
        var behandlinger = new ArrayList<Behandling>();  // Lag passenede query

        var callId = MDCOperations.getCallId();
        callId = (callId == null ? MDCOperations.generateCallId() : callId) + "_";

        for (var behandling : behandlinger) {
            opprettRekjøringsTask(behandling, callId);
        }

        return BATCHNAME + "-" + UUID.randomUUID();
    }

    private void opprettRekjøringsTask(Behandling behandling, String callId) {
        // Bruk prosesseringtjeneste til passende videre prosess
        return;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }
}
