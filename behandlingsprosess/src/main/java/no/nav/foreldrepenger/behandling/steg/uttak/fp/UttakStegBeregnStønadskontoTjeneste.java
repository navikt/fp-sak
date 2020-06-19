package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.time.LocalDate;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjonRepository;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.UttakRepositoryProvider;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.regler.uttak.felles.PrematurukerUtil;
import no.nav.foreldrepenger.regler.uttak.konfig.StandardKonfigurasjon;

@ApplicationScoped
public class UttakStegBeregnStønadskontoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(UttakStegBeregnStønadskontoTjeneste.class);

    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private FagsakRelasjonRepository fagsakRelasjonRepository;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    @Inject
    public UttakStegBeregnStønadskontoTjeneste(UttakRepositoryProvider repositoryProvider,
                                               BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                               DekningsgradTjeneste dekningsgradTjeneste,
                                               ForeldrepengerUttakTjeneste uttakTjeneste) {
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.fagsakRelasjonRepository = repositoryProvider.getFagsakRelasjonRepository();
        this.uttakTjeneste = uttakTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    UttakStegBeregnStønadskontoTjeneste() {
        //CDI
    }

    /**
     * Beregner og lagrer stønadskontoer hvis preconditions er oppfylt
     */
    BeregningingAvStønadskontoResultat beregnStønadskontoer(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var fagsakRelasjon = fagsakRelasjonRepository.finnRelasjonFor(ref.getSaksnummer());
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();

        //Trenger ikke behandlingslås siden stønadskontoer lagres på fagsakrelasjon.
        if (fagsakRelasjon.getStønadskontoberegning().isEmpty() || !finnesLøpendeInnvilgetFP(fpGrunnlag)) {
            beregnStønadskontoerTjeneste.opprettStønadskontoer(input);
            return BeregningingAvStønadskontoResultat.BEREGNET;
        } else if (dekningsgradTjeneste.behandlingHarEndretDekningsgrad(ref) || oppfyllerPrematurUker(fpGrunnlag)){
            beregnStønadskontoerTjeneste.overstyrStønadskontoberegning(input);
            return BeregningingAvStønadskontoResultat.OVERSTYRT;
        }

        //Mulig hver behandling skal regne ut stønadskontoer på nytt. Logger her for å samle data på hvor mye endringer det fører til
        logEvtEndring(input, fagsakRelasjon);

        return BeregningingAvStønadskontoResultat.INGEN_BEREGNING;
    }

    private void logEvtEndring(UttakInput input, FagsakRelasjon fagsakRelasjon) {
        var eksiterendeKontoer = fagsakRelasjon.getStønadskontoberegning().orElseThrow();
        var nyeKontoer = beregnStønadskontoerTjeneste.beregn(input, fagsakRelasjon);

        if (beregnStønadskontoerTjeneste.inneholderEndringer(eksiterendeKontoer, nyeKontoer)) {
            LOG.info("Behandling ville ha endret stønadskontoer " + nyeKontoer);
        }
    }

    private boolean oppfyllerPrematurUker(ForeldrepengerGrunnlag fpGrunnlag) {
        var gjeldendeFamilieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        LocalDate fødselsdato = gjeldendeFamilieHendelse.getFødselsdato().orElse(null);
        LocalDate termindato = gjeldendeFamilieHendelse.getTermindato().orElse(null);
        return PrematurukerUtil.oppfyllerKravTilPrematuruker(fødselsdato, termindato, StandardKonfigurasjon.KONFIGURASJON);
    }

    private boolean finnesLøpendeInnvilgetFP(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var originalBehandling = foreldrepengerGrunnlag.getOriginalBehandling();
        if (originalBehandling.isPresent() && erLøpendeInnvilgetFP(originalBehandling.get().getId())) {
            return true;
        }

        if (foreldrepengerGrunnlag.getAnnenpart().isPresent()) {
            var annenpartGjeldendeVedtakBehandlingId = foreldrepengerGrunnlag.getAnnenpart().get().getGjeldendeVedtakBehandlingId();
            return erLøpendeInnvilgetFP(annenpartGjeldendeVedtakBehandlingId);
        }
        return false;
    }

    private boolean erLøpendeInnvilgetFP(Long behandlingId) {
        var uttak = uttakTjeneste.hentUttakHvisEksisterer(behandlingId);
        if (uttak.isEmpty()) {
            return false;
        }
        return uttak.get().getGjeldendePerioder()
            .stream()
            .anyMatch(this::harTrekkdagerEllerUtbetaling);
    }

    private boolean harTrekkdagerEllerUtbetaling(ForeldrepengerUttakPeriode periode) {
        return periode.getAktiviteter()
            .stream()
            .anyMatch(aktivitet -> aktivitet.getTrekkdager().merEnn0() || aktivitet.getUtbetalingsgrad().harUtbetaling());
    }

    enum  BeregningingAvStønadskontoResultat {
        BEREGNET, OVERSTYRT, INGEN_BEREGNING
    }
}
