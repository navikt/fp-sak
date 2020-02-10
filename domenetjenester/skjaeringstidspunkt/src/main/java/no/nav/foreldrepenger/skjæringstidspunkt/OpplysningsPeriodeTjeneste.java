package no.nav.foreldrepenger.skjæringstidspunkt;

import java.time.LocalDate;
import java.time.Period;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.threeten.extra.Interval;

import no.nav.foreldrepenger.behandlingslager.IntervallUtil;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.vedtak.konfig.KonfigVerdi;
import no.nav.vedtak.util.FPDateUtil;

@ApplicationScoped
public class OpplysningsPeriodeTjeneste {

    private Period periodeFørFP;
    private Period periodeEtterFP;
    private Period periodeFørES;
    private Period periodeEtterES;
    private Period periodeFørSVP;
    private Period periodeEtterSVP;

    private SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste;

    OpplysningsPeriodeTjeneste() {
        // CDI
    }

    /**
     * Konfig angir perioden med registerinnhenting før/etter skjæringstidspunktet (for en gitt ytelse)
     */
    @Inject
    public OpplysningsPeriodeTjeneste(SkjæringstidspunktRegisterinnhentingTjeneste skjæringstidspunktTjeneste,
                                      // TODO: Dette bør splittes i ulike konfig klasser
                                      @KonfigVerdi(value = "es.registerinnhenting.opplysningsperiode.før", defaultVerdi = "P12M") Period periodeFørES,
                                      @KonfigVerdi(value = "es.registerinnhenting.opplysningsperiode.etter", defaultVerdi = "P6M") Period periodeEtterES,
                                      @KonfigVerdi(value = "fp.registerinnhenting.opplysningsperiode.før", defaultVerdi = "P17M") Period periodeFørFP,
                                      @KonfigVerdi(value = "fp.registerinnhenting.opplysningsperiode.etter", defaultVerdi = "P4Y") Period periodeEtterFP,
                                      @KonfigVerdi(value = "svp.registerinnhenting.opplysningsperiode.før", defaultVerdi = "P17M") Period periodeFørSVP,
                                      @KonfigVerdi(value = "svp.registerinnhenting.opplysningsperiode.etter", defaultVerdi = "P4Y") Period periodeEtterSVP) {

        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.periodeFørES = periodeFørES;
        this.periodeEtterES = periodeEtterES;
        this.periodeFørFP = periodeFørFP;
        this.periodeEtterFP = periodeEtterFP;
        this.periodeFørSVP = periodeFørSVP;
        this.periodeEtterSVP = periodeEtterSVP;
    }

    /**
     * Beregner opplysningsperioden (Perioden vi ber om informasjon fra registerne) for en gitt behandling.
     *
     * Benytter konfig-verdier for å setter lengden på intervallene på hver side av skjæringstidspunkt for registerinnhenting.
     *
     * @param behandling behandlingen
     * @return intervallet
     */
    public Interval beregn(Long behandlingId, FagsakYtelseType ytelseType) {
        return beregning(behandlingId, ytelseType, false);
    }

    public Interval beregnTilOgMedIdag(Long behandlingId, FagsakYtelseType ytelseType) {
        return beregning(behandlingId, ytelseType, true);
    }

    private Interval beregning(Long behandlingId, FagsakYtelseType ytelseType, boolean tilOgMedIdag) {
        final LocalDate skjæringstidspunkt = skjæringstidspunktTjeneste.utledSkjæringstidspunktForRegisterInnhenting(behandlingId);
        if (FagsakYtelseType.FORELDREPENGER.equals(ytelseType)) {
            return beregnIntervalFP(skjæringstidspunkt, tilOgMedIdag);
        } else if (FagsakYtelseType.ENGANGSTØNAD.equals(ytelseType)) {
            return beregnIntervalES(skjæringstidspunkt, tilOgMedIdag);
        } else if (FagsakYtelseType.SVANGERSKAPSPENGER.equals(ytelseType)) {
            return beregnIntervalSVP(skjæringstidspunkt, tilOgMedIdag);
        }
        throw SkjæringstidspunktFeil.FACTORY.kanIkkeUtledeOpplysningsperiodeForBehandling(behandlingId).toException();
    }

    private Interval beregnIntervalES(LocalDate skjæringstidspunkt, boolean tilOgMedIdag) {
        return beregnInterval(skjæringstidspunkt.minus(periodeFørES), skjæringstidspunkt.plus(periodeEtterES), tilOgMedIdag);
    }

    private Interval beregnIntervalFP(LocalDate skjæringstidspunkt, boolean tilOgMedIdag) {
        return beregnInterval(skjæringstidspunkt.minus(periodeFørFP), skjæringstidspunkt.plus(periodeEtterFP), tilOgMedIdag);
    }

    private Interval beregnIntervalSVP(LocalDate skjæringstidspunkt, boolean tilOgMedIdag) {
        return beregnInterval(skjæringstidspunkt.minus(periodeFørSVP), skjæringstidspunkt.plus(periodeEtterSVP), tilOgMedIdag);
    }

    private Interval beregnInterval(LocalDate fom, LocalDate tom, boolean tilOgMedIdag) {
        return IntervallUtil.byggIntervall(fom, tilOgMedIdag && tom.isBefore(FPDateUtil.iDag()) ? FPDateUtil.iDag() : tom);
    }
}
