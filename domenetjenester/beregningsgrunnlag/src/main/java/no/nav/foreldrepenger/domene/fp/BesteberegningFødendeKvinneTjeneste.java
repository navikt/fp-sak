package no.nav.foreldrepenger.domene.fp;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.Skjæringstidspunkt;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseType;
import no.nav.foreldrepenger.behandlingslager.behandling.opptjening.OpptjeningAktivitetType;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.RelasjonsRolleType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakRepository;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.ytelse.RelatertYtelseType;
import no.nav.foreldrepenger.domene.arbeidsforhold.InntektArbeidYtelseTjeneste;
import no.nav.foreldrepenger.domene.iay.modell.AktørYtelse;
import no.nav.foreldrepenger.domene.iay.modell.YtelseFilter;
import no.nav.foreldrepenger.domene.mappers.til_kalkulus.MapTilYtelsegrunnlagDto;
import no.nav.foreldrepenger.domene.modell.BeregningAktivitetAggregat;
import no.nav.foreldrepenger.domene.modell.Beregningsgrunnlag;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.modell.BesteberegningGrunnlag;
import no.nav.foreldrepenger.domene.modell.kodeverk.FaktaOmBeregningTilfelle;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningAktiviteter;
import no.nav.foreldrepenger.domene.opptjening.OpptjeningForBeregningTjeneste;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.kalkulus.kontrakt.request.input.foreldrepenger.besteberegning.Ytelsegrunnlag;

@ApplicationScoped
public class BesteberegningFødendeKvinneTjeneste {
    private static final Set<FamilieHendelseType> FØDSEL_HENDELSER = Set.of(FamilieHendelseType.FØDSEL,
        FamilieHendelseType.TERMIN);
    private static final Set<OpptjeningAktivitetType> GODKJENT_FOR_AUTOMATISK_BEREGNING = Set.of(OpptjeningAktivitetType.ARBEID,
        OpptjeningAktivitetType.SYKEPENGER, OpptjeningAktivitetType.DAGPENGER, OpptjeningAktivitetType.FORELDREPENGER, OpptjeningAktivitetType.SVANGERSKAPSPENGER);
    // Hvis avvik er likt eller større enn grensen skal det bli manuell kontroll av besteberegningen
    private static final BigDecimal AVVIKSGRENSE_FOR_MANUELL_KONTROLL = BigDecimal.valueOf(50_000);

    private FamilieHendelseRepository familieHendelseRepository;
    private OpptjeningForBeregningTjeneste opptjeningForBeregningTjeneste;
    private InntektArbeidYtelseTjeneste inntektArbeidYtelseTjeneste;
    private BeregningTjeneste beregningTjeneste;
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
                                               BeregningTjeneste beregningTjeneste,
                                               BehandlingRepository behandlingRepository,
                                               BeregningsresultatRepository beregningsresultatRepository,
                                               FagsakRepository fagsakRepository) {
        this.familieHendelseRepository = familieHendelseRepository;
        this.opptjeningForBeregningTjeneste = opptjeningForBeregningTjeneste;
        this.inntektArbeidYtelseTjeneste = inntektArbeidYtelseTjeneste;
        this.beregningTjeneste = beregningTjeneste;
        this.behandlingRepository = behandlingRepository;
        this.beregningsresultatRepository = beregningsresultatRepository;
        this.fagsakRepository = fagsakRepository;
    }

    public boolean brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp) {
        if (!erFødendeKvinneSomSøkerForeldrepenger(behandlingReferanse)) {
            return false;
        }

        var opptjeningForBeregning = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(behandlingReferanse, stp,
            inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId()));
        return opptjeningForBeregning.map(
            opptjeningAktiviteter -> brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse, stp,
                opptjeningAktiviteter)).orElse(false);
    }

    boolean erFødendeKvinneSomSøkerForeldrepenger(BehandlingReferanse behandlingReferanse) {
        if (!gjelderForeldrepenger(behandlingReferanse)) {
            return false;
        }
        var familiehendelse = familieHendelseRepository.hentAggregatHvisEksisterer(
            behandlingReferanse.behandlingId()).map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon);
        var familiehendelseType = familiehendelse.map(FamilieHendelseEntitet::getType)
            .orElseThrow(() -> new IllegalStateException(
                "Mangler FamilieHendelse#type for behandling: " + behandlingReferanse.behandlingId()));
        return erFødendeKvinne(behandlingReferanse.relasjonRolle(), familiehendelseType);
    }

    public boolean kvalifisererTilAutomatiskBesteberegning(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp) {
        if (!brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse, stp)) {
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

        // Foreløpig besteberegner vi ikke saker med frilans eller næring automatisk.
        return harAktiviteterSomErGodkjentForAutomatiskBeregning(behandlingReferanse, stp);
    }

    private boolean erDagpengerManueltFjernetFraBeregningen(BehandlingReferanse behandlingReferanse) {
        var bgGrunnlag = beregningTjeneste.hent(behandlingReferanse);
        var harDPFraRegister = dagpengerLiggerIAktivitet(bgGrunnlag.map(BeregningsgrunnlagGrunnlag::getRegisterAktiviteter));
        var harDPIGjeldendeAggregat = dagpengerLiggerIAktivitet(bgGrunnlag.map(BeregningsgrunnlagGrunnlag::getGjeldendeAktiviteter));
        return harDPFraRegister && !harDPIGjeldendeAggregat;
    }

    private boolean dagpengerLiggerIAktivitet(Optional<BeregningAktivitetAggregat> aggregat) {
        return aggregat.map(BeregningAktivitetAggregat::getBeregningAktiviteter)
            .orElse(Collections.emptyList())
            .stream()
            .anyMatch(akt -> OpptjeningAktivitetType.DAGPENGER.equals(akt.getOpptjeningAktivitetType()));
    }

    private boolean beregningsgrunnlagErOverstyrt(BehandlingReferanse behandlingReferanse) {
        var bg = beregningTjeneste.hent(behandlingReferanse).flatMap(
            BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        return bg.map(Beregningsgrunnlag::isOverstyrt).orElse(false);
    }

    private boolean erBesteberegningManueltVurdert(BehandlingReferanse ref) {
        var beregningsgrunnlagEntitet = beregningTjeneste.hent(ref).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        return beregningsgrunnlagEntitet.map(Beregningsgrunnlag::getFaktaOmBeregningTilfeller)
            .orElse(Collections.emptyList()).stream().anyMatch(tilf ->tilf.equals(FaktaOmBeregningTilfelle.VURDER_BESTEBEREGNING));
    }

    private boolean harAktiviteterSomErGodkjentForAutomatiskBeregning(BehandlingReferanse ref, Skjæringstidspunkt stp) {
        var iay = inntektArbeidYtelseTjeneste.hentGrunnlag(ref.behandlingId());
        var opptjening = opptjeningForBeregningTjeneste.hentOpptjeningForBeregning(ref, stp, iay);
        var opptjeningAktiviteter = opptjening.map(OpptjeningAktiviteter::getOpptjeningPerioder).orElse(Collections.emptyList());
        return opptjeningAktiviteter.stream().allMatch(a -> GODKJENT_FOR_AUTOMATISK_BEREGNING.contains(a.opptjeningAktivitetType()));
    }

    private static boolean gjelderForeldrepenger(BehandlingReferanse behandlingReferanse) {
        return FagsakYtelseType.FORELDREPENGER.equals(behandlingReferanse.fagsakYtelseType());
    }

    static boolean erFødendeKvinne(RelasjonsRolleType relasjonsRolleType, FamilieHendelseType type) {
        var erMoren = RelasjonsRolleType.MORA.equals(relasjonsRolleType);
        return erMoren && FØDSEL_HENDELSER.contains(type);
    }

    private boolean brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp,
                                                                         OpptjeningAktiviteter opptjeningAktiviteter) {
        var skjæringstidspunkt = stp.getUtledetSkjæringstidspunkt();
        var ytelser = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId())
            .getAktørYtelseFraRegister(behandlingReferanse.aktørId())
            .map(AktørYtelse::getAlleYtelser)
            .orElse(Collections.emptyList());
        return DagpengerGirBesteberegning.harDagpengerPåEllerIntillSkjæringstidspunkt(opptjeningAktiviteter, ytelser,
            skjæringstidspunkt);
    }
    public boolean trengerManuellKontrollAvAutomatiskBesteberegning(BehandlingReferanse behandlingReferanse) {
        var besteberegnetAvvik = beregningTjeneste.hent(behandlingReferanse)
            .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag)
            .flatMap(Beregningsgrunnlag::getBesteberegningGrunnlag)
            .flatMap(BesteberegningGrunnlag::getAvvik);
        if (besteberegnetAvvik.isEmpty()) {
            return false;
        }
        return besteberegnetAvvik.get().compareTo(AVVIKSGRENSE_FOR_MANUELL_KONTROLL) >= 0;
    }

    /**
     * Metode som lager besteberegningsgrunnlag for bruk i kalkulus applikasjon. Erstatter #lagBesteberegningYtelseinput
     * @param behandlingReferanse
     * @param stp
     * @return liste over ytelsegrunnlag til bruk i besteberegning
     */

    public List<Ytelsegrunnlag> lagYtelsegrunnlagKalkulus(BehandlingReferanse behandlingReferanse, Skjæringstidspunkt stp) {
        var iayGrunnlag = inntektArbeidYtelseTjeneste.hentGrunnlag(behandlingReferanse.behandlingId());
        var ytelseFilter = new YtelseFilter(iayGrunnlag.getAktørYtelseFraRegister(behandlingReferanse.aktørId()));
        var periodeYtelserKanVæreRelevantForBB = stp.getSkjæringstidspunktHvisUtledet()
            .map(t -> DatoIntervallEntitet.fraOgMedTilOgMed(t.minusMonths(12), t));
        if (periodeYtelserKanVæreRelevantForBB.isEmpty() || !brukerOmfattesAvBesteBeregningsRegelForFødendeKvinne(behandlingReferanse, stp)) {
            return Collections.emptyList();
        }
        List<Ytelsegrunnlag> grunnlag = new ArrayList<>();

        // Obs: Før flere ytelser legges til her, validerer med produkteier hvordan
        // inntekten skal fordeles under besteberegning og sjekk om eksiterende kode støtter dette
        MapTilYtelsegrunnlagDto.mapEksterneYtelserTilBesteberegningYtelsegrunnlag(periodeYtelserKanVæreRelevantForBB.get(), ytelseFilter,
                RelatertYtelseType.SYKEPENGER)
            .ifPresent(grunnlag::add);
        MapTilYtelsegrunnlagDto.mapEksterneYtelserTilBesteberegningYtelsegrunnlag(periodeYtelserKanVæreRelevantForBB.get(), ytelseFilter,
                RelatertYtelseType.PLEIEPENGER_NÆRSTÅENDE)
            .ifPresent(grunnlag::add);
        MapTilYtelsegrunnlagDto.mapEksterneYtelserTilBesteberegningYtelsegrunnlag(periodeYtelserKanVæreRelevantForBB.get(), ytelseFilter,
                RelatertYtelseType.PLEIEPENGER_SYKT_BARN)
            .ifPresent(grunnlag::add);
        MapTilYtelsegrunnlagDto.mapEksterneYtelserTilBesteberegningYtelsegrunnlag(periodeYtelserKanVæreRelevantForBB.get(), ytelseFilter,
                RelatertYtelseType.OPPLÆRINGSPENGER)
            .ifPresent(grunnlag::add);

        var saksnumreSomMåHentesFraFpsak = MapTilYtelsegrunnlagDto.saksnummerSomMåHentesFraFpsak(periodeYtelserKanVæreRelevantForBB.get(), ytelseFilter);
        grunnlag.addAll(hentOgMapFpsakYtelserTilKalkulusDto(saksnumreSomMåHentesFraFpsak));
        return grunnlag;
    }

    private List<Ytelsegrunnlag> hentOgMapFpsakYtelserTilKalkulusDto(List<Saksnummer> saksnummer) {
        List<Ytelsegrunnlag> resultater = new ArrayList<>();
        saksnummer.forEach(sak -> {
            var fagsak = fagsakRepository.hentSakGittSaksnummer(sak);
            fagsak.flatMap(fag -> behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fag.getId()))
                .flatMap(beh -> beregningsresultatRepository.hentUtbetBeregningsresultat(beh.getId()))
                .flatMap(br -> MapTilYtelsegrunnlagDto.mapFpsakYtelseTilYtelsegrunnlag(br, fagsak.get().getYtelseType()))
                .ifPresent(resultater::add);
        });
        return resultater;
    }

}
