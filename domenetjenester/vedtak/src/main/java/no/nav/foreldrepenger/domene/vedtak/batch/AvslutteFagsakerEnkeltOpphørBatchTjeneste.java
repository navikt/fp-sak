package no.nav.foreldrepenger.domene.vedtak.batch;

import java.util.Properties;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.batch.BatchTjeneste;

/**
 * Henter ut fagsaker som har behandling med resultat opphør, som har kobling til annen part, som ikke har en gr_neste_sak (opphør pga nytt barn) og ingen åpne behandlinger.
 * Dersom siste behandling er opphør opprettes en task som sjekker om fagsaken kan avsluttes.
 *
 * Skal kjøres en gang i uken
 *
 * Ingen parametere - går gjennom alle saker som oppfyller kriterier
 */
@ApplicationScoped
public class AvslutteFagsakerEnkeltOpphørBatchTjeneste implements BatchTjeneste {
    private static final String BATCHNAME = "BVL012";
    private AvslutteFagsakerEnkeltOpphørTjeneste avslutteFagsakerEnkeltOpphørTjeneste;

    @Inject
    public AvslutteFagsakerEnkeltOpphørBatchTjeneste(AvslutteFagsakerEnkeltOpphørTjeneste avslutteFagsakerEnkeltOpphørTjeneste) {
        this.avslutteFagsakerEnkeltOpphørTjeneste = avslutteFagsakerEnkeltOpphørTjeneste;
    }

    @Override
    public String launch(Properties properties) {
        var antallSakerSomSkalAvluttes = avslutteFagsakerEnkeltOpphørTjeneste.avslutteSakerMedEnkeltOpphør();

        return BATCHNAME + "-" + antallSakerSomSkalAvluttes;
    }

    @Override
    public String getBatchName() {
        return BATCHNAME;
    }

}
