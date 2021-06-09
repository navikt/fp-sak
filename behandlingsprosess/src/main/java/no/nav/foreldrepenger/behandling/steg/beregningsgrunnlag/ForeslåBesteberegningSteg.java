package no.nav.foreldrepenger.behandling.steg.beregningsgrunnlag;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.input.BeregningsgrunnlagInput;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingskontroll.AksjonspunktResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegModell;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.prosess.BeregningsgrunnlagKopierOgLagreTjeneste;
import no.nav.foreldrepenger.domene.fp.BesteberegningFødendeKvinneTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@FagsakYtelseTypeRef("FP")
@BehandlingStegRef(kode = "FORS_BESTEBEREGNING")
@BehandlingTypeRef
@ApplicationScoped
public class ForeslåBesteberegningSteg implements BeregningsgrunnlagSteg {
    private static final Logger LOG = LoggerFactory.getLogger(ForeslåBesteberegningSteg.class);
    private static final Set<Arbeidskategori> RELEVANTE_KATEGORIER = Set.of(Arbeidskategori.ARBEIDSTAKER, Arbeidskategori.DAGPENGER,
        Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);

    private BehandlingRepository behandlingRepository;
    private BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste;
    private BeregningsgrunnlagInputProvider beregningsgrunnlagInputProvider;
    private BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;

    protected ForeslåBesteberegningSteg() {
        // for CDI proxy
    }

    @Inject
    public ForeslåBesteberegningSteg(BehandlingRepository behandlingRepository,
                                     BeregningsgrunnlagKopierOgLagreTjeneste beregningsgrunnlagKopierOgLagreTjeneste,
                                     BeregningsgrunnlagInputProvider inputTjenesteProvider,
                                     BesteberegningFødendeKvinneTjeneste besteberegningFødendeKvinneTjeneste,
                                     SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                     InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste) {
        this.behandlingRepository = behandlingRepository;
        this.beregningsgrunnlagKopierOgLagreTjeneste = beregningsgrunnlagKopierOgLagreTjeneste;
        this.beregningsgrunnlagInputProvider = Objects.requireNonNull(inputTjenesteProvider, "inputTjenesteProvider");
        this.besteberegningFødendeKvinneTjeneste = besteberegningFødendeKvinneTjeneste;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandling = behandlingRepository.hentBehandling(kontekst.getBehandlingId());
        var skjæringstidspunkt = skjæringstidspunktTjeneste.getSkjæringstidspunkter(kontekst.getBehandlingId());
        var ref = BehandlingReferanse.fra(behandling, skjæringstidspunkt);
        var input = getInputTjeneste(ref.getFagsakYtelseType()).lagInput(ref.getBehandlingId());
        loggSykepengerOmBesteberegning(ref);
        if (skalBeregnesAutomatisk(ref, input)) {
            var resultat = beregningsgrunnlagKopierOgLagreTjeneste.foreslåBesteberegning(input);
            var aksjonspunkter = resultat.getAksjonspunkter().stream().map(BeregningResultatMapper::map).collect(Collectors.toList());
            aksjonspunkter.add(opprettKontrollpunkt());
            return BehandleStegResultat.utførtMedAksjonspunktResultater(aksjonspunkter);
        }

        return BehandleStegResultat.utførtMedAksjonspunktResultater(Collections.emptyList());
    }

    private void loggSykepengerOmBesteberegning(BehandlingReferanse ref) {
        if (besteberegningFødendeKvinneTjeneste.brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(ref)) {
            InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingUuid());
            // Ifm besteberegning av sykepenger basert på dagpenger er det ønskelig å få oversikt over hvor mange
            // saker som har hvilke ytelsegrunnlag, hele metoden kan fjernes når vi har fått litt data
            loggSykepengegrunnlag(iayGrunnlag, ref);
        }
    }

    private void loggSykepengegrunnlag(InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse behandlingReferanse) {
        Collection<Ytelse> alleYtelser = iayGrunnlag.getAktørYtelseFraRegister(behandlingReferanse.getAktørId())
            .map(AktørYtelse::getAlleYtelser)
            .orElse(Collections.emptyList());
        List<Ytelse> ytelserFørSTPSomSkalLogges = alleYtelser.stream()
            .filter(yt -> yt.getRelatertYtelseType().equals(RelatertYtelseType.SYKEPENGER))
            .filter(yt -> !yt.getPeriode().inkluderer(behandlingReferanse.getUtledetSkjæringstidspunkt()))
            .filter(yt -> yt.getPeriode().getTomDato().isAfter((behandlingReferanse.getUtledetSkjæringstidspunkt().minusMonths(11))))
            .filter(this::harArbeidskategoriSomSkalLogges)
            .collect(Collectors.toList());
        List<Ytelse> ytelserPåSTPSomSkalLogges = alleYtelser.stream()
            .filter(yt -> yt.getRelatertYtelseType().equals(RelatertYtelseType.SYKEPENGER))
            .filter(yt -> yt.getPeriode().inkluderer(behandlingReferanse.getUtledetSkjæringstidspunkt()))
            .filter(this::harArbeidskategoriSomSkalLogges)
            .collect(Collectors.toList());
        ytelserFørSTPSomSkalLogges.forEach(ytelse -> {
            Arbeidskategori kategori = ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getArbeidskategori).orElse(Arbeidskategori.UGYLDIG);
            DatoIntervallEntitet periode = ytelse.getPeriode();
            LOG.info("BB-{} på behandling {} for periode {}", kategori, behandlingReferanse.getBehandlingId(), periode);
        });
        ytelserPåSTPSomSkalLogges.forEach(ytelse -> {
            Arbeidskategori kategori = ytelse.getYtelseGrunnlag().flatMap(YtelseGrunnlag::getArbeidskategori).orElse(Arbeidskategori.UGYLDIG);
            DatoIntervallEntitet periode = ytelse.getPeriode();
            LOG.info("BBSTP-{} på behandling {} for periode {}", kategori, behandlingReferanse.getBehandlingId(), periode);
        });
    }

    private Boolean harArbeidskategoriSomSkalLogges(Ytelse yt) {
        return yt.getYtelseGrunnlag()
            .map(gr -> RELEVANTE_KATEGORIER.contains(gr.getArbeidskategori().orElse(Arbeidskategori.UDEFINERT)))
            .orElse(false);
    }

    private AksjonspunktResultat opprettKontrollpunkt() {
        return AksjonspunktResultat.opprettForAksjonspunkt(AksjonspunktDefinisjon.KONTROLLER_AUTOMATISK_BESTEBEREGNING);
    }

    private boolean skalBeregnesAutomatisk(BehandlingReferanse ref, BeregningsgrunnlagInput input) {
        boolean kanBehandlesAutomatisk = besteberegningFødendeKvinneTjeneste.kvalifisererTilAutomatiskBesteberegning(ref);
        return kanBehandlesAutomatisk && input.isEnabled("automatisk-besteberegning", false);
    }

    @Override
    public void vedHoppOverBakover(BehandlingskontrollKontekst kontekst, BehandlingStegModell modell, BehandlingStegType tilSteg, BehandlingStegType fraSteg) {
        if (tilSteg.equals(BehandlingStegType.FORESLÅ_BESTEBEREGNING)) {
            beregningsgrunnlagKopierOgLagreTjeneste.getRyddBeregningsgrunnlag(kontekst).ryddForeslåBesteberegningVedTilbakeføring();
        }
    }

    private BeregningsgrunnlagInputFelles getInputTjeneste(FagsakYtelseType ytelseType) {
        return beregningsgrunnlagInputProvider.getTjeneste(ytelseType);
    }


}
