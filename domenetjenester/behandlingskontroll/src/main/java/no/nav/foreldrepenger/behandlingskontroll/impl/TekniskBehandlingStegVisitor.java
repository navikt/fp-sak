package no.nav.foreldrepenger.behandlingskontroll.impl;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingModellVisitor;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegTilstandSnapshot;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.StegProsesseringResultat;
import no.nav.foreldrepenger.behandlingskontroll.spi.BehandlingskontrollServiceProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.vedtak.felles.jpa.savepoint.Work;
import no.nav.vedtak.log.mdc.MdcExtendedLogContext;

/**
 * Tekniske oppsett ved kjøring av et steg:<br>
 * <ul>
 * <li>Setter savepoint slik at dersom steg feiler så beholdes tidligere resultater.</li>
 * <li>Setter LOG_CONTEXT slik at ytterligere detaljer blir med i logging.</li>
 * </ul>
 */
public class TekniskBehandlingStegVisitor implements BehandlingModellVisitor {

    private static final MdcExtendedLogContext LOG_CONTEXT = MdcExtendedLogContext.getContext("prosess"); //$NON-NLS-1$

    private final BehandlingskontrollKontekst kontekst;

    private BehandlingskontrollServiceProvider serviceProvider;

    public TekniskBehandlingStegVisitor(BehandlingskontrollServiceProvider serviceProvider,
                                        BehandlingskontrollKontekst kontekst) {
        this.serviceProvider = serviceProvider;
        this.kontekst = kontekst;
    }


    @Override
    public StegProsesseringResultat prosesser(BehandlingStegModell steg) {
        LOG_CONTEXT.add("fagsak", kontekst.getFagsakId()); // NOSONAR //$NON-NLS-1$
        LOG_CONTEXT.add("behandling", kontekst.getBehandlingId()); // NOSONAR //$NON-NLS-1$
        LOG_CONTEXT.add("steg", steg.getBehandlingStegType().getKode()); // NOSONAR //$NON-NLS-1$

        Behandling behandling = serviceProvider.hentBehandling(kontekst.getBehandlingId());
        BehandlingStegTilstandSnapshot forrigeTilstand = BehandlingModellImpl.tilBehandlingsStegSnapshot(behandling.getSisteBehandlingStegTilstand());
        // lag ny for hvert steg som kjøres
        BehandlingStegVisitor stegVisitor = new BehandlingStegVisitor(serviceProvider, behandling, steg, kontekst);

        // kjøres utenfor savepoint. Ellers står vi nakne, med kun utførte steg
        stegVisitor.markerOvergangTilNyttSteg(steg.getBehandlingStegType(), forrigeTilstand);

        StegProsesseringResultat resultat = prosesserStegISavepoint(behandling, stegVisitor);

        /*
         * NB: nullstiller her og ikke i finally block, siden det da fjernes før vi får logget det.
         * Hele settet fjernes så i MDCFilter eller tilsvarende uansett. Steg er del av koden så fanges uansett i
         * stacktrace men trengs her for å kunne ta med i log eks. på DEBUG/INFO/WARN nivå.
         *
         * behandling og fagsak kan være satt utenfor, så nullstiller ikke de i log context her
         */
        LOG_CONTEXT.remove("steg"); // NOSONAR //$NON-NLS-1$

        return resultat;
    }

    protected StegProsesseringResultat prosesserStegISavepoint(Behandling behandling, BehandlingStegVisitor stegVisitor) {
        // legger steg kjøring i et savepiont
        class DoInSavepoint implements Work<StegProsesseringResultat> {
            @Override
            public StegProsesseringResultat doWork() {
                StegProsesseringResultat resultat = prosesserSteg(stegVisitor);
                serviceProvider.lagreOgClear(behandling, kontekst.getSkriveLås());
                return resultat;
            }
        }

        StegProsesseringResultat resultat = serviceProvider.getTekniskRepository().doWorkInSavepoint(new DoInSavepoint());
        return resultat;
    }

    protected StegProsesseringResultat prosesserSteg(BehandlingStegVisitor stegVisitor) {
        return stegVisitor.prosesser();
    }

}
