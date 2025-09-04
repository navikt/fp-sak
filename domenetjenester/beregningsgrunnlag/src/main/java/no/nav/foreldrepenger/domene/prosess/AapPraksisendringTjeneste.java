package no.nav.foreldrepenger.domene.prosess;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.hendelser.StartpunktType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Fagsystem;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørInntekt;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.Inntekt;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseAggregat;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.InntektsmeldingAggregat;
import no.nav.foreldrepenger.domene.iay.modell.Inntektspost;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseAnvist;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.InntektsKilde;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.AktivitetStatus;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Beløp;
import no.nav.foreldrepenger.domene.typer.Stillingsprosent;

@ApplicationScoped
public class AapPraksisendringTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(AapPraksisendringTjeneste.class);

    private BeregningTjeneste beregningTjeneste;
    private InntektArbeidYtelseTjeneste iayTjeneste;
    private BehandlingRepository behandlingRepository;

    public AapPraksisendringTjeneste() {
    }

    @Inject
    public AapPraksisendringTjeneste(BeregningTjeneste beregningTjeneste,
                                     InntektArbeidYtelseTjeneste iayTjeneste,
                                     BehandlingRepository behandlingRepository) {
        this.beregningTjeneste = beregningTjeneste;
        this.iayTjeneste = iayTjeneste;
        this.behandlingRepository = behandlingRepository;
    }

    public boolean erPåvirketAvPraksisendring(Long fagsakId) {
        // Vi må sjekke om siste behandling som kjørte beregningen har fått aksjonspunkt 5052, kan ha oppstått i beregning men ikke siste
        var alleBehandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForFagsak(fagsakId);
        var sisteBehandlingMedBeregning = alleBehandlinger.stream()
            .filter(Behandling::erYtelseBehandling)
            .filter(b -> b.getType().equals(BehandlingType.FØRSTEGANGSSØKNAD) || starterFørBeregning(b))
            .max(Comparator.comparing(Behandling::getOpprettetDato))
            .orElseThrow();

        if (!harHattAksjonspunkt(sisteBehandlingMedBeregning)) {
            LOG.info("FP-63781: Finner ikke aksjonspunkt 5052 på siste behandling som kjørte beregning for saksnummer {}, behandling uuid {}",
                sisteBehandlingMedBeregning.getSaksnummer(), sisteBehandlingMedBeregning.getUuid());
            return false;
        }
        var ref = BehandlingReferanse.fra(sisteBehandlingMedBeregning);
        var grBeregningsgrunnlag = beregningTjeneste.hent(ref);
        if (harBlittBeregnetSomArbeidstakerEllerFrilans(grBeregningsgrunnlag)) {
            LOG.info("FP-63781: Beregningsgrunnlaget er beregnet på bakgrunn av arbeidstakerstatus for saksnummer {}, behandling uuid {}",
                sisteBehandlingMedBeregning.getSaksnummer(), sisteBehandlingMedBeregning.getUuid());
            return false;
        }
        if (!harInntektsIBeregningsperioden(grBeregningsgrunnlag, ref)) {
            LOG.info("FP-63781: Finner ikke inntekt på fjernede arbeidsforhold i saksnummer {}, behandling uuid {}",
                sisteBehandlingMedBeregning.getSaksnummer(), sisteBehandlingMedBeregning.getUuid());
            return false;
        }
        LOG.info("FP-63782: Påvirket av praksisendring: saksnummer {}, behandling uuid {}", sisteBehandlingMedBeregning.getSaksnummer(),
            sisteBehandlingMedBeregning.getUuid());
        return true;
    }

    private boolean harInntektsIBeregningsperioden(Optional<BeregningsgrunnlagGrunnlag> grBeregningsgrunnlag, BehandlingReferanse ref) {
        var stp = grBeregningsgrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .map(Beregningsgrunnlag::getSkjæringstidspunkt)
            .orElse(null);
        if (stp == null) {
            return false;
        }
        var fjernedeArbeidsgivere = finnFjernedeArbeidsgivere(grBeregningsgrunnlag);
        var grIay = iayTjeneste.finnGrunnlag(ref.behandlingId());
        var erInnsendtImMedInntektHosArbeidsgivere = sjekkInntektsmeldinger(grIay, fjernedeArbeidsgivere);
        var inntektsposter = hentSøkersInntektsposterHosArbeidsgivere(ref, grIay, fjernedeArbeidsgivere);
        var beregningsperiodeTom = stp.minusMonths(1).with(TemporalAdjusters.lastDayOfMonth());
        var beregningsperiodeFom = beregningsperiodeTom.minusMonths(2).withDayOfMonth(1);
        var beregningsperiode = DatoIntervallEntitet.fraOgMedTilOgMed(beregningsperiodeFom, beregningsperiodeTom);
        var finnesInntektsposterMedInntektHosArbeidsgivere = inntektsposter.stream()
            .anyMatch(post -> post.getPeriode().overlapper(beregningsperiode) && post.getBeløp().compareTo(Beløp.ZERO) > 0);
        return finnesInntektsposterMedInntektHosArbeidsgivere || erInnsendtImMedInntektHosArbeidsgivere;
    }

    private boolean sjekkInntektsmeldinger(Optional<InntektArbeidYtelseGrunnlag> grIay, Set<String> fjernedeArbeidsgivere) {
        var inntektsmeldinger = grIay.flatMap(InntektArbeidYtelseGrunnlag::getInntektsmeldinger)
            .map(InntektsmeldingAggregat::getAlleInntektsmeldinger)
            .orElse(List.of());
        return inntektsmeldinger.stream()
            .anyMatch(
                im -> fjernedeArbeidsgivere.contains(im.getArbeidsgiver().getIdentifikator()) && im.getInntektBeløp().compareTo(Beløp.ZERO) > 0);
    }

    private List<Inntektspost> hentSøkersInntektsposterHosArbeidsgivere(BehandlingReferanse ref,
                                                       Optional<InntektArbeidYtelseGrunnlag> grIay,
                                                       Set<String> fjernedeArbeidsgivere) {
        var aktørInntekter = grIay.flatMap(InntektArbeidYtelseGrunnlag::getRegisterVersjon)
            .map(InntektArbeidYtelseAggregat::getAktørInntekt)
            .orElse(List.of());
        var inntekter = aktørInntekter.stream()
            .filter(a -> a.getAktørId().equals(ref.aktørId()))
            .findFirst()
            .map(AktørInntekt::getInntekt)
            .orElse(List.of());
        return inntekter.stream()
            .filter(i -> i.getInntektsKilde().equals(InntektsKilde.INNTEKT_BEREGNING))
            .filter(i -> i.getArbeidsgiver() != null && fjernedeArbeidsgivere.contains(i.getArbeidsgiver().getIdentifikator()))
            .map(Inntekt::getAlleInntektsposter)
            .flatMap(Collection::stream)
            .toList();
    }

    private Set<String> finnFjernedeArbeidsgivere(Optional<BeregningsgrunnlagGrunnlag> grBeregningsgrunnlag) {
        var registerArbeidsgivere = grBeregningsgrunnlag.map(BeregningsgrunnlagGrunnlag::getRegisterAktiviteter)
            .map(this::finnArbeidsgivere)
            .orElse(Set.of());
        var saksbehandletArbeidsgivere = grBeregningsgrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getSaksbehandletAktiviteter)
            .map(this::finnArbeidsgivere)
            .orElse(Set.of());
        return registerArbeidsgivere.stream().filter(a -> !saksbehandletArbeidsgivere.contains(a)).collect(Collectors.toSet());
    }

    private Set<String> finnArbeidsgivere(BeregningAktivitetAggregat akt) {
        return akt.getBeregningAktiviteter()
            .stream()
            .filter(a -> a.getArbeidsgiver() != null)
            .map(a -> a.getArbeidsgiver().getIdentifikator())
            .collect(Collectors.toSet());
    }

    private boolean harBlittBeregnetSomArbeidstakerEllerFrilans(Optional<BeregningsgrunnlagGrunnlag> grBeregningsgrunnlag) {
        var statuserSomBidrarTilGrunnlaget = grBeregningsgrunnlag.flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .map(Beregningsgrunnlag::getAktivitetStatuser)
            .orElse(List.of());
        return statuserSomBidrarTilGrunnlaget.stream().anyMatch(s -> s.getAktivitetStatus().erArbeidstaker() || s.getAktivitetStatus().erFrilanser());
    }

    private boolean harHattAksjonspunkt(Behandling sisteBehandlingMedBeregning) {
        return sisteBehandlingMedBeregning.getAksjonspunkter()
            .stream()
            .anyMatch(ap -> ap.getAksjonspunktDefinisjon().equals(AksjonspunktDefinisjon.AVKLAR_AKTIVITETER) && ap.getStatus()
                .equals(AksjonspunktStatus.UTFØRT));
    }

    private static boolean starterFørBeregning(Behandling b) {
        return !b.getStartpunkt().equals(StartpunktType.UTTAKSVILKÅR) && !b.getStartpunkt().equals(StartpunktType.TILKJENT_YTELSE)
            && !b.getStartpunkt().equals(StartpunktType.BEREGNING_FORESLÅ);
    }

    public void loggVedFullAapOgAnnenStatus(BehandlingReferanse referanse) {
        var beregningsgrunnlag = beregningTjeneste.hent(referanse).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        beregningsgrunnlag.ifPresent(bg -> {
            if (harAapOgAnnenStatus(bg)) {
                if (harFullAap(referanse, bg.getSkjæringstidspunkt())) {
                    LOG.info("FULL_AAP_LOGG: Saksnummer {} mottar full AAP i kombinasjon med annen status i beregningsgrunnlaget",
                        referanse.saksnummer());
                }
            }
        });
    }

    private boolean harFullAap(BehandlingReferanse referanse, LocalDate skjæringstidspunkt) {
        var iayGrunnlag = iayTjeneste.finnGrunnlag(referanse.behandlingId());
        var ytelser = iayGrunnlag.flatMap(gr -> gr.getAktørYtelseFraRegister(referanse.aktørId())).map(AktørYtelse::getAlleYtelser).orElse(List.of());
        var filter = new YtelseFilter(ytelser, skjæringstidspunkt, true);
        var aapVedtak = filter.filter(
                yt -> yt.getKilde().equals(Fagsystem.ARENA) && yt.getRelatertYtelseType().equals(RelatertYtelseType.ARBEIDSAVKLARINGSPENGER))
            .getFiltrertYtelser();
        var utbetalingsprosentSisteMK = aapVedtak.stream()
            .map(Ytelse::getYtelseAnvist)
            .flatMap(Collection::stream)
            .filter(mk -> !mk.getAnvistTOM().isAfter(skjæringstidspunkt))
            .max(Comparator.comparing(YtelseAnvist::getAnvistTOM))
            .flatMap(YtelseAnvist::getUtbetalingsgradProsent);
        return utbetalingsprosentSisteMK.filter(up -> up.compareTo(new Stillingsprosent(200)) == 0).isPresent();
    }

    private boolean harAapOgAnnenStatus(Beregningsgrunnlag bg) {
        var erAap = bg.getAktivitetStatuser().stream().anyMatch(s -> s.getAktivitetStatus().equals(AktivitetStatus.ARBEIDSAVKLARINGSPENGER));
        var harAnnenStatus = bg.getAktivitetStatuser().size() > 1;
        return erAap && harAnnenStatus;
    }
}
