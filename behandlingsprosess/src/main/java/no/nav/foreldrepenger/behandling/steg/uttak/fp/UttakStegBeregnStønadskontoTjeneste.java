package no.nav.foreldrepenger.behandling.steg.uttak.fp;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.FagsakRelasjonTjeneste;
import no.nav.foreldrepenger.behandlingslager.fagsak.Dekningsgrad;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRelasjon;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskonto;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.StønadskontoType;
import no.nav.foreldrepenger.behandlingslager.uttak.fp.Stønadskontoberegning;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakPeriode;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttakTjeneste;
import no.nav.foreldrepenger.domene.uttak.beregnkontoer.BeregnStønadskontoerTjeneste;
import no.nav.foreldrepenger.domene.uttak.input.ForeldrepengerGrunnlag;
import no.nav.foreldrepenger.domene.uttak.input.UttakInput;
import no.nav.foreldrepenger.stønadskonto.grensesnitt.Stønadsdager;


@ApplicationScoped
public class UttakStegBeregnStønadskontoTjeneste {

    private static final Logger LOG = LoggerFactory.getLogger(UttakStegBeregnStønadskontoTjeneste.class);

    private BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private FagsakRelasjonTjeneste fagsakRelasjonTjeneste;
    private ForeldrepengerUttakTjeneste uttakTjeneste;

    @Inject
    public UttakStegBeregnStønadskontoTjeneste(BeregnStønadskontoerTjeneste beregnStønadskontoerTjeneste,
                                               DekningsgradTjeneste dekningsgradTjeneste,
                                               ForeldrepengerUttakTjeneste uttakTjeneste,
                                               FagsakRelasjonTjeneste fagsakRelasjonTjeneste) {
        this.beregnStønadskontoerTjeneste = beregnStønadskontoerTjeneste;
        this.fagsakRelasjonTjeneste = fagsakRelasjonTjeneste;
        this.uttakTjeneste = uttakTjeneste;
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    UttakStegBeregnStønadskontoTjeneste() {
        // CDI
    }

    /**
     * Beregner og lagrer stønadskontoer hvis preconditions er oppfylt
     */
    BeregningingAvStønadskontoResultat beregnStønadskontoer(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        var fagsakRelasjon = fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer());
        ForeldrepengerGrunnlag fpGrunnlag = input.getYtelsespesifiktGrunnlag();

        // Trenger ikke behandlingslås siden stønadskontoer lagres på fagsakrelasjon.
        if (fagsakRelasjon.getStønadskontoberegning().isEmpty() || !finnesLøpendeInnvilgetFP(fpGrunnlag)) {
            beregnStønadskontoerTjeneste.opprettStønadskontoer(input);
            return BeregningingAvStønadskontoResultat.BEREGNET;
        }
        // Default er beregning relativt til eksisterende kontoberegning.
        // Endring fra 80 til 100% DG krever full omregning ettersom antall dager reduseres
        var endretDekningsgrad = dekningsgradTjeneste.behandlingHarEndretDekningsgrad(ref);
        var fullBeregning = endretDekningsgrad && Dekningsgrad._100.equals(dekningsgradTjeneste.finnGjeldendeDekningsgrad(ref));
        var eksisterendeKontoUtregning = fagsakRelasjon.getGjeldendeStønadskontoberegning().orElseThrow();
        if (endretDekningsgrad || skalBeregneMedPrematurdager(fpGrunnlag, eksisterendeKontoUtregning)) {
            beregnStønadskontoerTjeneste.overstyrStønadskontoberegning(input, !fullBeregning);
            return BeregningingAvStønadskontoResultat.OVERSTYRT;
        }

        // Mulig hver behandling skal regne ut stønadskontoer på nytt. Logger her for å
        // samle data på hvor mye endringer det fører til
        logEvtEndring(input, fagsakRelasjon);

        return BeregningingAvStønadskontoResultat.INGEN_BEREGNING;
    }

    public Stønadskontoberegning fastsettStønadskontoerForBehandling(UttakInput input) {
        var ref = input.getBehandlingReferanse();
        return beregnStønadskontoerTjeneste.beregnForBehandling(input)
            .or(() -> fagsakRelasjonTjeneste.finnRelasjonFor(ref.saksnummer()).getGjeldendeStønadskontoberegning())
            .orElseThrow();
    }

    private void logEvtEndring(UttakInput input, FagsakRelasjon fagsakRelasjon) {
        var nyeKontoer = beregnStønadskontoerTjeneste.beregn(input, true);

        var eksiterendeKontoer = fagsakRelasjon.getStønadskontoberegning().orElseThrow();
        var endringerVedReberegning = utledEndringer(eksiterendeKontoer, nyeKontoer);
        if (!endringerVedReberegning.isEmpty()) {
            var saksnummer1 = fagsakRelasjon.getFagsakNrEn().getSaksnummer().getVerdi();
            var saksnummer2 = fagsakRelasjon.getFagsakNrTo().map(fr -> fr.getSaksnummer().getVerdi()).orElse("");
            LOG.info("Behandling ville ha endret stønadskontoer {} {} {} {} {}", saksnummer1, saksnummer2,
                endringerVedReberegning, eksiterendeKontoer.getRegelInput(), nyeKontoer.getRegelInput());
        }
    }

    private static Set<KontoEndring> utledEndringer(Stønadskontoberegning eksiterendeKonti, Stønadskontoberegning nyeKonti) {
        HashSet<StønadskontoType> alleTyper = new HashSet<>(Arrays.stream(StønadskontoType.values()).filter(StønadskontoType::erStønadsdager).toList());

        return alleTyper.stream().map(type -> {
            var dagerEksisterende = finnMaksdagerForType(eksiterendeKonti, type);
            var dagerNye = finnMaksdagerForType(nyeKonti, type);
            return dagerNye.equals(dagerEksisterende) ? null : new KontoEndring(type, dagerEksisterende, dagerNye);
        }).filter(Objects::nonNull).collect(Collectors.toSet());
    }

    private static Integer finnMaksdagerForType(Stønadskontoberegning eksiterendeKonti, StønadskontoType type) {
        return eksiterendeKonti.getStønadskontoer()
            .stream()
            .filter(s -> s.getStønadskontoType().equals(type))
            .map(Stønadskonto::getMaxDager)
            .findFirst()
            .orElse(0);
    }

    private boolean skalBeregneMedPrematurdager(ForeldrepengerGrunnlag fpGrunnlag, Stønadskontoberegning eksisterende) {
        var gjeldendeFamilieHendelse = fpGrunnlag.getFamilieHendelser().getGjeldendeFamilieHendelse();
        var fødselsdato = gjeldendeFamilieHendelse.getFødselsdato().orElse(null);
        var termindato = gjeldendeFamilieHendelse.getTermindato().orElse(null);
        var eksisterendePrematurdager = eksisterende.getStønadskontoutregning().getOrDefault(StønadskontoType.TILLEGG_PREMATUR, 0);
        var nyePrematurdager = Stønadsdager.instance(null).ekstradagerPrematur(fødselsdato, termindato);
        return nyePrematurdager > eksisterendePrematurdager;
    }

    private boolean finnesLøpendeInnvilgetFP(ForeldrepengerGrunnlag foreldrepengerGrunnlag) {
        var originalBehandling = foreldrepengerGrunnlag.getOriginalBehandling();
        if (originalBehandling.isPresent() && erLøpendeInnvilgetFP(originalBehandling.get().getId())) {
            return true;
        }

        return foreldrepengerGrunnlag.getAnnenpart().filter(ap -> erLøpendeInnvilgetFP(ap.gjeldendeVedtakBehandlingId())).isPresent();
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

    enum BeregningingAvStønadskontoResultat {
        BEREGNET,
        OVERSTYRT,
        INGEN_BEREGNING
    }

    private record KontoEndring(StønadskontoType type, Integer dagerEksisterende, Integer dagerNye) {
    }
}
