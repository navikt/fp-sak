package no.nav.foreldrepenger.domene.fp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.folketrygdloven.kalkulator.steg.besteberegning.Ytelsegrunnlag;
import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.InntektArbeidYtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.Ytelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.iay.modell.YtelseGrunnlag;
import no.nav.foreldrepenger.domene.iay.modell.kodeverk.Arbeidskategori;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregatEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlagEntitet;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagRepository;
import no.nav.foreldrepenger.domene.modell.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@ApplicationScoped
public class BesteberegningFødendeKvinneTjeneste {
    private static final Logger LOG = LoggerFactory.getLogger(BesteberegningFødendeKvinneTjeneste.class);

    private static final Set<FamilieHendelseType> fødselHendelser = Set.of(FamilieHendelseType.FØDSEL,
        FamilieHendelseType.TERMIN);
    private static final Set<Arbeidskategori> RELEVANTE_KATEGORIER = Set.of(Arbeidskategori.ARBEIDSTAKER, Arbeidskategori.DAGPENGER,
        Arbeidskategori.KOMBINASJON_ARBEIDSTAKER_OG_DAGPENGER);

    private FamilieHendelseRepository familieHendelseRepository;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningsgrunnlagRepository beregningsgrunnlagRepository;
    private BehandlingRepository behandlingRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FagsakRepository fagsakRepository;

    public BesteberegningFødendeKvinneTjeneste() {
        // CDI
    }

    @Inject
    public BesteberegningFødendeKvinneTjeneste(FamilieHendelseRepository familieHendelseRepository,
                                               OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste,
                                               InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste,
                                               BeregningsgrunnlagRepository beregningsgrunnlagRepository,
                                               BehandlingRepository behandlingRepository,
                                               BeregningsresultatRepository beregningsresultatRepository,
                                               FagsakRepository fagsakRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningsgrunnlagRepository = beregningsgrunnlagRepository;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.fagsakRepository = fagsakRepository;
    }

    public boolean brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(BehandlingReferanse behandlingReferanse) {
        if (!erFødendeKvinneSomSøkerForeldrepenger(behandlingReferanse)) {
            return false;
        }

        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        var opptjeningForBeregning = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(behandlingReferanse,
            iayGrunnlag);
        boolean omfattesAvBB = opptjeningForBeregning.map(
            opptjeningAktiviteter -> brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse,
                opptjeningAktiviteter)).orElse(false);
        if (omfattesAvBB) {
            loggSykepengegrunnlag(iayGrunnlag, behandlingReferanse);
        }
        return omfattesAvBB;
    }

    private void loggSykepengegrunnlag(InntektArbeidYtelseGrunnlag iayGrunnlag, BehandlingReferanse behandlingReferanse) {
        // Ifm besteberegning av sykepenger basert på dagpenger er det ønskelig å få oversikt over hvor mange
        // saker som har hvilke ytelsegrunnlag, hele metoden kan fjernes når vi har fått litt data
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

    boolean erFødendeKvinneSomSøkerForeldrepenger(BehandlingReferanse behandlingReferanse) {
        if (!gjelderForeldrepenger(behandlingReferanse)) {
            return false;
        }
        var familiehendelse = familieHendelseRepository.hentAggregatHvisEksisterer(
            behandlingReferanse.getBehandlingId()).map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        var familiehendelseType = familiehendelse.map(FamilieHendelseEntitet::getType)
            .orElseThrow(() -> new IllegalStateException(
                "Mangler FamilieHendelse#type for behandling: " + behandlingReferanse.getBehandlingId()));
        return erFødendeKvinne(behandlingReferanse.getRelasjonsRolleType(), familiehendelseType);
    }

    public boolean kvalifisererTilAutomatiskBesteberegning(BehandlingReferanse behandlingReferanse) {
        if (!brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse)) {
            return false;
        }
        if (erDagpengerManueltFjernetFraBeregningen(behandlingReferanse)) {
            return false;
        }
        if (erBesteberegningManueltVurdert(behandlingReferanse)) {
            return false;
        }
        if (beregningsgrunnlagErOverstyrt(behandlingReferanse)) {
            return false;
        }

        // Foreløpig besteberegner vi ikke saker med sykepenger, frilans eller næring automatisk.
        return harKunDagpengerEllerArbeidIOpptjening(behandlingReferanse);
    }

    private boolean erDagpengerManueltFjernetFraBeregningen(BehandlingReferanse behandlingReferanse) {
        Optional<BeregningsgrunnlagGrunnlagEntitet> bgGrunnlag = beregningsgrunnlagRepository.hentBeregningsgrunnlagGrunnlagEntitet(behandlingReferanse.getBehandlingId());
        bgGrunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getRegisterAktiviteter).map(BeregningAktivitetAggregatEntitet::getBeregningAktiviteter).orElse(Collections.emptyList());
        boolean harDPFraRegister = dagpengerLiggerIAktivitet(bgGrunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getRegisterAktiviteter));
        boolean harDPIGjeldendeAggregat = dagpengerLiggerIAktivitet(bgGrunnlag.map(BeregningsgrunnlagGrunnlagEntitet::getGjeldendeAktiviteter));
        boolean dagpengerErFjernet = harDPFraRegister && !harDPIGjeldendeAggregat;
        return dagpengerErFjernet;
    }

    private boolean dagpengerLiggerIAktivitet(Optional<BeregningAktivitetAggregatEntitet> aggregat) {
        return aggregat.map(BeregningAktivitetAggregatEntitet::getBeregningAktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(akt -> OpptjeningAktivitetType.DAGPENGER.equals(akt.getOpptjeningAktivitetType()));
    }

    private boolean beregningsgrunnlagErOverstyrt(BehandlingReferanse behandlingReferanse) {
        Optional<BeregningsgrunnlagEntitet> bg = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(behandlingReferanse.getBehandlingId());
        return bg.map(BeregningsgrunnlagEntitet::isOverstyrt).orElse(false);
    }

    public List<Ytelsegrunnlag> lagBesteberegningYtelseinput(BehandlingReferanse behandlingReferanse) {
        InntektArbeidYtelseGrunnlag iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId());
        YtelseFilter ytelseFilter = new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(behandlingReferanse.getAktørId()));
        Optional<LocalDate> førsteMuligeDatoForYtelseIBBGrunnlag = behandlingReferanse.getSkjæringstidspunkt().getSkjæringstidspunktHvisUtledet().map(stp -> stp.minusMonths(12));
        if (førsteMuligeDatoForYtelseIBBGrunnlag.isEmpty()) {
            return Collections.emptyList();
        }
        List<Ytelsegrunnlag> grunnlag = new ArrayList<>();
        BesteberegningYtelsegrunnlagMapper.mapSykepengerTilYtelegrunnlag(førsteMuligeDatoForYtelseIBBGrunnlag.get(), ytelseFilter)
            .ifPresent(grunnlag::add);
        List<Saksnummer> saksnumreSomMåHentesFraFpsak = BesteberegningYtelsegrunnlagMapper.saksnummerSomMåHentesFraFpsak(førsteMuligeDatoForYtelseIBBGrunnlag.get(), ytelseFilter);
        grunnlag.addAll(hentOgMapFpsakYtelser(saksnumreSomMåHentesFraFpsak));
        return grunnlag;
    }

    private List<Ytelsegrunnlag> hentOgMapFpsakYtelser(List<Saksnummer> saksnummer) {
        List<Ytelsegrunnlag> resultater = new ArrayList<>();
        saksnummer.forEach(sak -> {
            Optional<Fagsak> fagsak = fagsakRepository.hentSakGittSaksnummer(sak);
            fagsak.flatMap(fag -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fag.getId()))
                .flatMap(beh -> beregningsresultatRepository.hentUtbetBeregningsresultat(beh.getId()))
                .flatMap(br -> BesteberegningYtelsegrunnlagMapper.mapFpsakYtelseTilYtelsegrunnlag(br, fagsak.get().getYtelseType()))
                .ifPresent(resultater::add);
        });
        return resultater;
    }

    private boolean erBesteberegningManueltVurdert(BehandlingReferanse ref) {
        var beregningsgrunnlagEntitet = beregningsgrunnlagRepository.hentBeregningsgrunnlagForBehandling(ref.getBehandlingId());
        return beregningsgrunnlagEntitet.map(BeregningsgrunnlagEntitet::getFaktaOmBeregningTilfeller)
            .orElse(Collections.emptyList()).stream().anyMatch(tilf ->tilf.equals(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING));
    }

    private boolean harKunDagpengerEllerArbeidIOpptjening(BehandlingReferanse ref) {
        InntektArbeidYtelseGrunnlag iay = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.getBehandlingId());
        var opptjening = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, iay);
        var opptjeningAktiviteter = opptjening.map(OpptjeningAktiviteter::getOpptjeningPerioder).orElse(Collections.emptyList());
        return opptjeningAktiviteter.stream().allMatch(a -> a.opptjeningAktivitetType().equals(OpptjeningAktivitetType.DAGPENGER)
            || a.opptjeningAktivitetType().equals(OpptjeningAktivitetType.ARBEID));
    }

    private static boolean gjelderForeldrepenger(BehandlingReferanse behandlingReferanse) {
        return FagsakYtelseType.FORELDREPENGER.equals(behandlingReferanse.getFagsakYtelseType());
    }

    static boolean erFødendeKvinne(RelasjonsRolleType relasjonsRolleType, FamilieHendelseType type) {
        var erMoren = RelasjonsRolleType.MORA.equals(relasjonsRolleType);
        return erMoren && fødselHendelser.contains(type);
    }

    private boolean brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(BehandlingReferanse behandlingReferanse,
                                                                         OpptjeningAktiviteter opptjeningAktiviteter) {
        var skjæringstidspunkt = behandlingReferanse.getUtledetSkjæringstidspunkt();
        var ytelser = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.getBehandlingId())
            .getAktørYtelseFraRegister(behandlingReferanse.getAktørId())
            .map(AktørYtelse::getAlleYtelser)
            .orElse(Collections.emptyList());
        return DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(opptjeningAktiviteter, ytelser,
            skjæringstidspunkt);
    }

}
