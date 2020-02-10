package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.felles.FastsettBehandlingsresultatVedEndring.Betingelser;
import no.nav.foreldrepenger.behandling.revurdering.felles.HarEtablertYtelse.VurderOpphørDagensDato;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BehandlingBeregningsresultatEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.domene.MÅ_LIGGE_HOS_FPSAK.HentOgLagreBeregningsgrunnlagTjeneste;
import no.nav.foreldrepenger.domene.SKAL_FLYTTES_TIL_KALKULUS.BeregningsgrunnlagEntitet;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.typer.Saksnummer;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.util.FPDateUtil;
import no.nav.vedtak.util.Tuple;

public abstract class RevurderingBehandlingsresultatutlederFellesImpl implements RevurderingBehandlingsresultatutlederFelles {

    private HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste;
    private MedlemTjeneste medlemTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OpphørUttakTjeneste opphørUttakTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private HarEtablertYtelse harEtablertYtelse;
    private ErEndringIUttakFraEndringsdato erEndringIUttakFraEndringsdato;
    private ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;

    protected RevurderingBehandlingsresultatutlederFellesImpl() {
        // for CDI proxy
    }

    public RevurderingBehandlingsresultatutlederFellesImpl(BehandlingRepositoryProvider repositoryProvider,
                                                           HentOgLagreBeregningsgrunnlagTjeneste beregningsgrunnlagTjeneste,
                                                           MedlemTjeneste medlemTjeneste,
                                                           OpphørUttakTjeneste opphørUttakTjeneste,
                                                           HarEtablertYtelse harEtablertYtelse,
                                                           ErEndringIUttakFraEndringsdato erEndringIUttakFraEndringsdato,
                                                           ErSisteUttakAvslåttMedÅrsakOgHarEndringIUttak erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak,
                                                           SkjæringstidspunktTjeneste skjæringstidspunktTjeneste) {

        this.beregningsgrunnlagTjeneste = beregningsgrunnlagTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.opphørUttakTjeneste = opphørUttakTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.harEtablertYtelse = harEtablertYtelse;
        this.erEndringIUttakFraEndringsdato = erEndringIUttakFraEndringsdato;
        this.erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak = erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
    }

    @Override
    public Behandlingsresultat bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef, boolean erVarselOmRevurderingSendt) {
        Behandling revurdering = behandlingRepository.hentBehandling(revurderingRef.getBehandlingId());

        Behandling originalBehandling = revurdering.getOriginalBehandling()
            .orElseThrow(() -> RevurderingFeil.FACTORY.revurderingManglerOriginalBehandling(revurdering.getId()).toException());

        UttakResultatHolder revurderingUttak = getUttakResultat(revurderingRef.getBehandlingId());
        UttakResultatHolder originalBehandlingUttak = getUttakResultat(originalBehandling.getId());
        UttakResultatHolder annenpartUttak = getAnnenPartUttak(revurderingRef.getSaksnummer());

        return bestemBehandlingsresultatForRevurderingCore(revurderingRef, revurdering, originalBehandling, revurderingUttak, originalBehandlingUttak,
            annenpartUttak, erVarselOmRevurderingSendt);
    }

    protected abstract UttakResultatHolder getAnnenPartUttak(Saksnummer saksnummer);

    protected abstract UttakResultatHolder getUttakResultat(Long behandlingId);

    private Behandlingsresultat bestemBehandlingsresultatForRevurderingCore(BehandlingReferanse revurderingRef,
                                                                            Behandling revurdering,
                                                                            Behandling originalBehandling,
                                                                            UttakResultatHolder uttakresultatRevurderingOpt,
                                                                            UttakResultatHolder uttakresultatOriginalOpt,
                                                                            UttakResultatHolder uttakresultatAnnenPartOpt, boolean erVarselOmRevurderingSendt) {
        if (!revurdering.getType().equals(BehandlingType.REVURDERING)) {
            throw new IllegalStateException("Utviklerfeil: Skal ikke kunne havne her uten en revurderingssak");
        }
        validerReferanser(revurderingRef, revurdering.getId());
        Long behandlingId = revurderingRef.getBehandlingId();

        Optional<Behandlingsresultat> behandlingsresultatRevurdering = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        Optional<Behandlingsresultat> behandlingsresultatOriginal = finnBehandlingsresultatPåOriginalBehandling(originalBehandling);
        if (FastsettBehandlingsresultatVedAvslagPåAvslag.vurder(behandlingsresultatRevurdering, behandlingsresultatOriginal)) {
            /* 2b */
            return FastsettBehandlingsresultatVedAvslagPåAvslag.fastsett(revurdering);
        }

        if (OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.vurder(revurdering)) {
            /* 2c */
            return OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.fastsett(revurdering);
        }

        LocalDate endringsdato = finnEndringsdato(revurderingRef);



        Tuple<VilkårUtfallType, Avslagsårsak> utfall = medlemTjeneste.utledVilkårUtfall(revurdering);
        if (!utfall.getElement1().equals(VilkårUtfallType.OPPFYLT)) {
            Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
            Behandlingsresultat.Builder behandlingsresultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
            behandlingsresultatBuilder.medBehandlingResultatType(BehandlingResultatType.OPPHØR);
            behandlingsresultatBuilder.medRettenTil(RettenTil.HAR_IKKE_RETT_TIL_FP);
            behandlingsresultatBuilder.leggTilKonsekvensForYtelsen(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER);
            behandlingsresultatBuilder.medVedtaksbrev(Vedtaksbrev.AUTOMATISK);
            behandlingsresultat.setAvslagsårsak(utfall.getElement2());
            return behandlingsresultatBuilder.buildFor(revurdering);
        }

        boolean erEndringIUttakFraEndringstidspunkt = erEndringIUttakFraEndringsdato.vurder(endringsdato, uttakresultatRevurderingOpt,
            uttakresultatOriginalOpt);
        if (erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak.vurder(uttakresultatRevurderingOpt, erEndringIUttakFraEndringstidspunkt)) {
            return erSisteUttakAvslåttMedÅrsakOgHarEndringIUttak.fastsett(revurdering);
        }
        Optional<BeregningsgrunnlagEntitet> revurderingsGrunnlagOpt = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(revurdering.getId());
        Optional<BeregningsgrunnlagEntitet> originalGrunnlagOpt = beregningsgrunnlagTjeneste.hentBeregningsgrunnlagEntitetForBehandling(originalBehandling.getId());

        boolean erEndringISkalHindreTilbaketrekk = erEndringISkalHindreTilbaketrekk(revurdering, originalBehandling);
        boolean erEndringIBeregning = ErEndringIBeregning.vurder(revurderingsGrunnlagOpt, originalGrunnlagOpt);
        boolean erKunEndringIFordelingAvYtelsen = ErKunEndringIFordelingAvYtelsen.vurder(erEndringIBeregning, erEndringIUttakFraEndringstidspunkt,
            revurderingsGrunnlagOpt, originalGrunnlagOpt, erEndringISkalHindreTilbaketrekk);

        Betingelser betingelser = Betingelser.fastsett(erEndringIBeregning, erEndringIUttakFraEndringstidspunkt,
            erVarselOmRevurderingSendt, erKunEndringIFordelingAvYtelsen,
            harInnvilgetIkkeOpphørtVedtak(revurdering.getFagsak()), gittOpphørFørEllerEtterDagensDato());

        return FastsettBehandlingsresultatVedEndring.fastsett(revurdering, betingelser,
            uttakresultatOriginalOpt, uttakresultatAnnenPartOpt, harEtablertYtelse, erSluttPåStønadsdager(originalBehandling));
    }

    protected abstract boolean erSluttPåStønadsdager(Behandling behandling);

    protected abstract LocalDate finnEndringsdato(BehandlingReferanse revurderingRef);

    private Optional<Behandlingsresultat> finnBehandlingsresultatPåOriginalBehandling(Behandling originalBehandling) {
        Optional<Behandlingsresultat> behandlingsresultatOriginal = behandlingsresultatRepository.hentHvisEksisterer(originalBehandling.getId());
        if (behandlingsresultatOriginal.isPresent()) {
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det faktiske resultatet for å kunne vurdere avslag på avslag
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultatOriginal.get().getBehandlingResultatType())) {
                return finnBehandlingsresultatPåOriginalBehandling(originalBehandling.getOriginalBehandling()
                    .orElseThrow(
                        () -> new IllegalStateException("Utviklerfeil: Kan ikke ha BehandlingResultatType.INGEN_ENDRING uten original behandling. BehandlingId="
                            + originalBehandling.getId())));
            } else {
                return behandlingsresultatOriginal;
            }
        }
        return Optional.empty();
    }

    private boolean harInnvilgetIkkeOpphørtVedtak(Fagsak fagsak) {
        Behandling sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
        if (sisteInnvilgede == null) {
            return false;
        }
        List<Behandling> behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer());
        return behandlinger.stream()
            .filter(this::erAvsluttetRevurdering)
            .map(this::tilBehandlingvedtak)
            .filter(vedtak -> erFattetEtter(sisteInnvilgede, vedtak))
            .noneMatch(opphørvedtak());
    }

    private boolean erAvsluttetRevurdering(Behandling behandling) {
        return behandling.erRevurdering() && behandling.erSaksbehandlingAvsluttet();
    }

    private BehandlingVedtak tilBehandlingvedtak(Behandling b) {
        return behandlingVedtakRepository.hentBehandlingvedtakForBehandlingId(b.getId()).orElse(null);
    }

    private boolean erFattetEtter(Behandling sisteInnvilget, BehandlingVedtak vedtak) {
        return vedtak != null && vedtak.getVedtaksdato().isAfter(sisteInnvilget.getOriginalVedtaksDato());
    }

    private Predicate<BehandlingVedtak> opphørvedtak() {
        return vedtak -> BehandlingResultatType.OPPHØR.equals(vedtak.getBehandlingsresultat().getBehandlingResultatType());
    }

    private VurderOpphørDagensDato gittOpphørFørEllerEtterDagensDato() {
        return (resultat) -> {
            LocalDate dagensDato = FPDateUtil.iDag();
            var ref = lagRef(resultat.getBehandling());
            return opphørUttakTjeneste.getOpphørsdato(ref, resultat).orElse(LocalDate.MAX).isBefore(dagensDato);
        };
    }

    private BehandlingReferanse lagRef(Behandling behandling) {
        Long behandlingId = behandling.getId();
        return BehandlingReferanse.fra(behandling, skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId));
    }

    private void validerReferanser(BehandlingReferanse ref, Long behandlingId) {
        if (!Objects.equals(ref.getBehandlingId(), behandlingId)) {
            throw new IllegalStateException(
                "BehandlingReferanse [" + ref.getBehandlingId() + "] matcher ikke forventet [" + behandlingId + "]");
        }
    }

    private boolean erEndringISkalHindreTilbaketrekk(Behandling revurdering, Behandling originalBehandling) {
        Optional<BehandlingBeregningsresultatEntitet> beregningsresultatFPAggregatEntitet = beregningsresultatRepository
            .hentBeregningsresultatAggregat(revurdering.getId());
        Optional<BehandlingBeregningsresultatEntitet> orginalBeregningsresultatFPAggregatEntitet = beregningsresultatRepository
            .hentBeregningsresultatAggregat(originalBehandling.getId());

        if (beregningsresultatFPAggregatEntitet.isPresent() && orginalBeregningsresultatFPAggregatEntitet.isPresent()) {
            return !beregningsresultatFPAggregatEntitet.get().skalHindreTilbaketrekk()
                .equals(orginalBeregningsresultatFPAggregatEntitet.get().skalHindreTilbaketrekk());
        }
        return false;
    }

}
