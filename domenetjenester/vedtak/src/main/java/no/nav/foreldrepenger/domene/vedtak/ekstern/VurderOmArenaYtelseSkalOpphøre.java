package no.nav.foreldrepenger.domene.vedtak.ekstern;

import java.time.LocalDate;
import java.time.Period;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryFeil;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.VedtakResultatType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.produksjonsstyring.oppgavebehandling.OppgaveTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;

@ApplicationScoped
public class VurderOmArenaYtelseSkalOpphøre {
    private static final Logger log = LoggerFactory.getLogger(VurderOmArenaYtelseSkalOpphøre.class);

    private static final long HALV_MELDEKORT_PERIODE = 9;
    private static final Period MELDEKORT_PERIODE = Period.ofDays(14);

    private BeregningsresultatRepository beregningsresultatRepository;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OppgaveTjeneste oppgaveTjeneste;

    VurderOmArenaYtelseSkalOpphøre() {
        // for CDI proxy
    }

    @Inject
    public VurderOmArenaYtelseSkalOpphøre(BeregningsresultatRepository beregningsresultatRepository,
                                          InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                          BehandlingVedtakRepository behandlingVedtakRepository,
                                          OppgaveTjeneste oppgaveTjeneste) {
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.iayTjeneste = inntektArbeidYtelseTjeneste;
        this.behandlingVedtakRepository = behandlingVedtakRepository;
        this.oppgaveTjeneste = oppgaveTjeneste;
    }

    void opprettOppgaveHvisArenaytelseSkalOpphøre(Long behandlingId, AktørId aktørId, LocalDate skjæringstidspunkt) {
        BehandlingVedtak vedtak = behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(behandlingId)
            .orElseThrow(() -> {
                return BehandlingRepositoryFeil.FACTORY.fantIkkeBehandlingVedtak(behandlingId).toException();
            });
        if (!VedtakResultatType.INNVILGET.equals(vedtak.getVedtakResultatType())) {
            return;
        }
        LocalDate vedtaksDato = vedtak.getVedtaksdato();
        LocalDate startdatoFP = finnFørsteAnvistDatoFP(behandlingId).orElse(skjæringstidspunkt);

        if (vurderArenaYtelserOpphøres(behandlingId, aktørId, startdatoFP, vedtaksDato)) {
            String oppgaveId = oppgaveTjeneste.opprettOppgaveStopUtbetalingAvARENAYtelse(behandlingId, startdatoFP);
            log.info("Oppgave opprettet i GOSYS slik at NØS kan behandle saken videre. Oppgavenummer: {}", oppgaveId);
        }
    }

    /**
     * Ved iverksetting av vedtak skal FPSAK gjøre en sjekk av om det er overlapp mellom startdato for foreldrepenger
     * og utbetalt ytelse i ARENA. FPSAK skal benytte lagrede registerdata om meldekortperioder for å vurdere om
     * startdatoen for foreldrepenger overlapper med ytelse i ARENA.
     *
     * @param behandling         behandling til saken i FP
     * @param førsteAnvistDatoFP første dato for utbetaling
     * @param vedtaksDato        vedtaksdato
     * @return true hvis det finnes en overlappende ytelse i ARENA, ellers false
     */
    boolean vurderArenaYtelserOpphøres(Long behandlingId, AktørId aktørId, LocalDate førsteAnvistDatoFP, LocalDate vedtaksDato) {
        LocalDate senesteInputDato = vedtaksDato.isAfter(førsteAnvistDatoFP) ? vedtaksDato : førsteAnvistDatoFP;
        var arenaYtelser = hentArenaYtelser(behandlingId, aktørId, senesteInputDato);

        // Ser både på løpende og avsluttede vedtak som overlapper første anvist dato
        if (!finnesYtelseVedtakPåEtterStartdato(arenaYtelser, førsteAnvistDatoFP)) {
            return false;
        }

        LocalDate sisteArenaAnvistDatoFørVedtaksdato = finnSisteArenaAnvistDatoFørVedtaksdato(arenaYtelser, vedtaksDato);
        if (sisteArenaAnvistDatoFørVedtaksdato == null) {
            return false;
        }
        Optional<LocalDate> nesteArenaAnvistDatoEtterVedtaksdato = finnNesteArenaAnvistDatoEtterVedtaksdato(arenaYtelser, vedtaksDato, sisteArenaAnvistDatoFørVedtaksdato);
        if (førsteAnvistDatoFP.isBefore(sisteArenaAnvistDatoFørVedtaksdato)) {
            return true;
        }
        return (nesteArenaAnvistDatoEtterVedtaksdato.isPresent() &&
            DatoIntervallEntitet.fraOgMedTilOgMed(sisteArenaAnvistDatoFørVedtaksdato, nesteArenaAnvistDatoEtterVedtaksdato.get()).inkluderer(førsteAnvistDatoFP) &&
            vedtaksDato.isAfter(nesteArenaAnvistDatoEtterVedtaksdato.get().minusDays(HALV_MELDEKORT_PERIODE)));
    }

    private Collection<Ytelse> hentArenaYtelser(Long behandlingId, AktørId aktørId, LocalDate skjæringstidspunkt) {
        var ytelseFilter = iayTjeneste.finnGrunnlag(behandlingId)
                .map(it -> new YtelseFilter(it.getAktørYtelseFraRegister(aktørId)).før(skjæringstidspunkt)).orElse(YtelseFilter.EMPTY);

        return ytelseFilter
            .filter(y -> Fagsystem.ARENA.equals(y.getKilde()))
            .getFiltrertYtelser();
    }

    private Optional<LocalDate> finnFørsteAnvistDatoFP(Long behandlingId) {
        return beregningsresultatRepository.hentUtbetBeregningsresultat(behandlingId)
            .map(BeregningsresultatEntitet::getBeregningsresultatPerioder).orElse(Collections.emptyList()).stream()
            .filter(brp -> brp.getBeregningsresultatAndelList().stream().anyMatch(a -> a.getDagsats() > 0))
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(Comparator.naturalOrder());
    }

    private LocalDate finnSisteArenaAnvistDatoFørVedtaksdato(Collection<Ytelse> ytelser, LocalDate vedtaksdato) {
        return ytelser.stream()
            .map(Ytelse::getYtelseAnvist)
            .flatMap(Collection::stream)
            .map(YtelseAnvist::getAnvistTOM)
            .filter(vedtaksdato::isAfter)
            .max(Comparator.naturalOrder())
            .orElse(null);
    }

    private Optional<LocalDate> finnNesteArenaAnvistDatoEtterVedtaksdato(Collection<Ytelse> ytelser, LocalDate vedtaksdato, LocalDate sisteAnvisteDatoArena) {
        // Venter ikke egentlig treff her ettersom vedtaksdato som regel er dagens dato
        var nesteAnvistDato = ytelser.stream()
            .map(Ytelse::getYtelseAnvist)
            .flatMap(Collection::stream)
            .map(YtelseAnvist::getAnvistFOM)
            .filter(vedtaksdato::isBefore)
            .min(Comparator.naturalOrder());
        if (nesteAnvistDato.isPresent()) {
            return nesteAnvistDato;
        }
        var ytelserPåVedtaksdato = ytelser.stream()
            .filter(y -> y.getPeriode().inkluderer(vedtaksdato))
            .collect(Collectors.toList());
        if (!ytelserPåVedtaksdato.isEmpty()) {
            return Optional.of(sisteAnvisteDatoArena.plus(MELDEKORT_PERIODE));
        }
        nesteAnvistDato = ytelser.stream()
            .filter(y -> y.getPeriode().getFomDato().isAfter(vedtaksdato))
            .map(Ytelse::getPeriode)
            .map(DatoIntervallEntitet::getFomDato)
            .min(Comparator.naturalOrder());
        return nesteAnvistDato;
    }

    private boolean finnesYtelseVedtakPåEtterStartdato(Collection<Ytelse> ytelser, LocalDate startdato) {
        return ytelser.stream()
            .anyMatch(y -> y.getPeriode().inkluderer(startdato) || y.getPeriode().getFomDato().isAfter(startdato));
    }
}
