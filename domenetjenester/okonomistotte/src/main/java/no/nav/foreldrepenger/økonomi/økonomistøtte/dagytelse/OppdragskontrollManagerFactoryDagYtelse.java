package no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse;

import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManager;
import no.nav.foreldrepenger.økonomi.økonomistøtte.OppdragskontrollManagerFactory;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.endring.OppdragskontrollEndring;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.førstegangsoppdrag.OppdragskontrollFørstegang;
import no.nav.foreldrepenger.økonomi.økonomistøtte.dagytelse.opphør.OppdragskontrollOpphør;

@ApplicationScoped
@FagsakYtelseTypeRef("FP")
@FagsakYtelseTypeRef("SVP")
public class OppdragskontrollManagerFactoryDagYtelse implements OppdragskontrollManagerFactory {

    private OppdragskontrollFørstegang oppdragskontrollFørstegang;
    private OppdragskontrollEndring oppdragskontrollEndring;
    private OppdragskontrollOpphør oppdragskontrollOpphør;
    private SjekkOmDetFinnesTilkjentYtelse sjekkOmDetFinnesTilkjentYtelse;

    OppdragskontrollManagerFactoryDagYtelse() {
        // for CDI proxy
    }

    @Inject
    public OppdragskontrollManagerFactoryDagYtelse(OppdragskontrollFørstegang oppdragskontrollFørstegang,
                                                   OppdragskontrollEndring oppdragskontrollEndring,
                                                   OppdragskontrollOpphør oppdragskontrollOpphør,
                                                   SjekkOmDetFinnesTilkjentYtelse sjekkOmDetFinnesTilkjentYtelse) {
        this.oppdragskontrollFørstegang = oppdragskontrollFørstegang;
        this.oppdragskontrollEndring = oppdragskontrollEndring;
        this.oppdragskontrollOpphør = oppdragskontrollOpphør;
        this.sjekkOmDetFinnesTilkjentYtelse = sjekkOmDetFinnesTilkjentYtelse;
    }

    @Override
    public Optional<OppdragskontrollManager> getManager(Behandling behandling, boolean finnesOppdragFraFør) {
        var diff = sjekkOmDetFinnesTilkjentYtelse.tilkjentYtelseDiffMotForrige(behandling);
        switch (diff) {
            case INGEN_ENDRING:
                return Optional.empty();
            case ENDRET_TIL_TOM:
                return Optional.of(oppdragskontrollOpphør);
            case ENDRET_FRA_TOM:
                return Optional.of(finnesOppdragFraFør ? oppdragskontrollEndring : oppdragskontrollFørstegang);
            case ANNEN_ENDRING:
                return Optional.of(oppdragskontrollEndring);
            default:
                throw new IllegalArgumentException("Ikke-støttet YtelseDiff: " + diff);
        }
    }

}
