package no.nav.foreldrepenger.behandling.steg.beregnytelse;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.*;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatAndel;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatPeriode;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.*;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.ytelse.beregning.BeregnYtelseTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

/**
 * Felles steg for å beregne tilkjent ytelse for foreldrepenger og
 * svangerskapspenger (ikke engangsstønad)
 */

@BehandlingStegRef(BehandlingStegType.BEREGN_YTELSE)
@BehandlingTypeRef
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@FagsakYtelseTypeRef(FagsakYtelseType.SVANGERSKAPSPENGER)
@ApplicationScoped
public class BeregneYtelseStegImpl implements BeregneYtelseSteg {
    private static final Logger LOG = LoggerFactory.getLogger(BeregneYtelseStegImpl.class);

    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private BeregnYtelseTjeneste beregnYtelseTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    protected BeregneYtelseStegImpl() {
        // for proxy
    }

    @Inject
    public BeregneYtelseStegImpl(BehandlingRepository behandlingRepository,
                                 BeregningsresultatRepository beregningsresultatRepository,
                                 BeregnYtelseTjeneste beregnYtelseTjeneste,
                                 InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.beregnYtelseTjeneste = beregnYtelseTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        var behandling = behandlingRepository.hentBehandling(behandlingId);
        var ref = BehandlingReferanse.fra(behandling);

        // Beregn ytelse
        var beregningsresultat = beregnYtelseTjeneste.beregnYtelse(ref);

        // Lagre beregningsresultat
        beregningsresultatRepository.lagre(behandling, beregningsresultat);

        // Logging for å utrede hvor vanlig det er med tilkommet permisjon med og uten refusjon
        try {
            loggPermisjonssaker(beregningsresultat, ref);
        } catch(Exception e) {
            LOG.info("FP-923541: Klarte ikke undersøke permisjonsperioder ", e);
        }

        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private void loggPermisjonssaker(BeregningsresultatEntitet beregningsresultat, BehandlingReferanse ref) {
        var førsteUttaksdagOpt = beregningsresultat.getBeregningsresultatPerioder()
            .stream()
            .map(BeregningsresultatPeriode::getBeregningsresultatPeriodeFom)
            .min(LocalDate::compareTo);
        if (førsteUttaksdagOpt.isEmpty()) {
            return;
        }
        var førsteUttaksdag = førsteUttaksdagOpt.get();
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId());
        var inntektsmeldinger = iayGrunnlag.getInntektsmeldinger()
            .map(InntektsmeldingAggregat::getInntektsmeldingerSomSkalBrukes)
            .orElse(Collections.emptyList());
        var yafilter = new YrkesaktivitetFilter(iayGrunnlag.getArbeidsforholdInformasjon(),
            iayGrunnlag.getAktørArbeidFraRegister(ref.aktørId()));
        yafilter.getYrkesaktiviteter().forEach(ya -> {
            var kreverRefusjon = harIMSomKreverRefusjon(inntektsmeldinger, ya);
            var harPermisajonSomOverlapperUtbetaling = harPermisjonSomOverlapperUtbetaling(beregningsresultat, ya, førsteUttaksdag);
            if (kreverRefusjon && harPermisajonSomOverlapperUtbetaling) {
                var msg = String.format("FP-564876: Saksnummer %s har tilkommet permisjon og krever refusjon fra start", ref.saksnummer().getVerdi());
                LOG.info(msg);
            }

        });
    }

    private boolean harPermisjonSomOverlapperUtbetaling(BeregningsresultatEntitet beregningsresultatEntitet,
                                                        Yrkesaktivitet yrkesaktivitet,
                                                        LocalDate førsteUttaksdag) {
        var tilkomnePermisjoner = yrkesaktivitet.getPermisjon().stream()
            .filter(perm -> erRelevant(perm, førsteUttaksdag))
            .toList();
        return beregningsresultatEntitet.getBeregningsresultatPerioder()
            .stream()
            .filter(p -> harUtbetalingForYA(p.getBeregningsresultatAndelList(), yrkesaktivitet))
            .anyMatch(p -> overlapperPermisjonsperiode(p, tilkomnePermisjoner));
    }

    private boolean overlapperPermisjonsperiode(BeregningsresultatPeriode p, List<Permisjon> tilkomnePermisjoner) {
        var tyPeriode = DatoIntervallEntitet.fraOgMedTilOgMed(p.getBeregningsresultatPeriodeFom(),
            p.getBeregningsresultatPeriodeTom());
        return tilkomnePermisjoner.stream().anyMatch(perm -> perm.getPeriode().overlapper(tyPeriode));
    }

    private boolean harUtbetalingForYA(List<BeregningsresultatAndel> beregningsresultatAndelList,
                                       Yrkesaktivitet yrkesaktivitet) {
        return beregningsresultatAndelList.stream()
            .anyMatch(andel -> andel.getArbeidsgiver().map(ag -> ag.equals(yrkesaktivitet.getArbeidsgiver())).orElse(false)
                && andel.getDagsats() > 0);
    }

    private boolean harIMSomKreverRefusjon(List<Inntektsmelding> inntektsmeldinger, Yrkesaktivitet ya) {
        return inntektsmeldinger.stream().filter(im -> ya.gjelderFor(im.getArbeidsgiver(), im.getArbeidsforholdRef()))
            .anyMatch(im -> im.getRefusjonBeløpPerMnd() != null && im.getRefusjonBeløpPerMnd().getVerdi().compareTo(BigDecimal.ZERO) > 0);
    }

    private boolean erRelevant(Permisjon perm, LocalDate førsteUttaksdag) {
        return !perm.getFraOgMed().isBefore(førsteUttaksdag)
            && perm.getPermisjonsbeskrivelseType().erRelevantForBeregningEllerArbeidsforhold()
            && perm.getProsentsats() != null && perm.getProsentsats().getVerdi().compareTo(BigDecimal.valueOf(100)) >= 0
            && varerMerEnn14Dager(perm);
    }

    private boolean varerMerEnn14Dager(Permisjon permisjon) {
        return permisjon.getPeriode().antallDager() > 14;
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg,
            BehandlingStegType fraSteg) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        beregningsresultatRepository.deaktiverBeregningsresultat(behandling.getId(), kontekst.getSkriveLås());
    }
}
