package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
import no.nav.foreldrepenger.behandling.revurdering.felles.UttakResultatHolder.VurderOpphørFørDagensDato;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingÅrsakType;
import no.nav.foreldrepenger.behandlingslager.behandling.KonsekvensForYtelsen;
import no.nav.foreldrepenger.behandlingslager.behandling.RettenTil;
import no.nav.foreldrepenger.behandlingslager.behandling.SpesialBehandling;
import no.nav.foreldrepenger.behandlingslager.behandling.beregning.BeregningsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.nestesak.NesteSakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingGrunnlagRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtak;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.BehandlingVedtakRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.vedtak.Vedtaksbrev;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.OpphørUttakTjeneste;
import no.nav.foreldrepenger.regler.uttak.UttakParametre;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

public abstract class RevurderingBehandlingsresultatutlederFelles {

    private BeregningTjeneste beregningTjeneste;
    private MedlemTjeneste medlemTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private OpphørUttakTjeneste opphørUttakTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private FamilieHendelseRepository familieHendelseRepository;
    private NesteSakRepository nesteSakRepository;
    private DekningsgradTjeneste dekningsgradTjeneste;

    protected RevurderingBehandlingsresultatutlederFelles() {
        // for CDI proxy
    }

    public RevurderingBehandlingsresultatutlederFelles(BehandlingRepositoryProvider repositoryProvider,
                                                       BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                                       BeregningTjeneste beregningTjeneste,
                                                       MedlemTjeneste medlemTjeneste,
                                                       OpphørUttakTjeneste opphørUttakTjeneste,
                                                       SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                                                       DekningsgradTjeneste dekningsgradTjeneste) {

        this.beregningTjeneste = beregningTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.opphørUttakTjeneste = opphørUttakTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.nesteSakRepository = grunnlagRepositoryProvider.getNesteSakRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
    }

    public Behandlingsresultat bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef,
                                                                       boolean erVarselOmRevurderingSendt) {
        var revurdering = behandlingRepository.hentBehandling(revurderingRef.behandlingId());

        var originalBehandlingId = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> RevurderingFeil.revurderingManglerOriginalBehandling(revurdering.getId()));

        var revurderingUttak = getUttakResultat(revurderingRef.behandlingId());
        var originalBehandlingUttak = getUttakResultat(originalBehandlingId);

        return bestemBehandlingsresultatForRevurderingCore(revurderingRef, revurdering,
            behandlingRepository.hentBehandling(originalBehandlingId), revurderingUttak, originalBehandlingUttak,
            erVarselOmRevurderingSendt);
    }

    protected abstract UttakResultatHolder getUttakResultat(Long behandlingId);

    private Behandlingsresultat bestemBehandlingsresultatForRevurderingCore(BehandlingReferanse revurderingRef,
                                                                            Behandling revurdering,
                                                                            Behandling originalBehandling,
                                                                            UttakResultatHolder uttakresultatRevurderingOpt,
                                                                            UttakResultatHolder uttakresultatOriginalOpt,
                                                                            boolean erVarselOmRevurderingSendt) {
        if (!revurdering.getType().equals(BehandlingType.REVURDERING)) {
            throw new IllegalStateException("Utviklerfeil: Skal ikke kunne havne her uten en revurderingssak");
        }
        validerReferanser(revurderingRef, revurdering.getId());
        var behandlingId = revurderingRef.behandlingId();

        var behandlingsresultatRevurdering = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        var behandlingsresultatOriginal = finnBehandlingsresultatPåOriginalBehandling(originalBehandling.getId());

        if (SpesialBehandling.erOppsagtUttak(revurdering)) {
            return buildBehandlingsresultat(revurdering, behandlingsresultatRevurdering.orElse(null),
                BehandlingResultatType.FORELDREPENGER_SENERE, RettenTil.HAR_RETT_TIL_FP,
                Vedtaksbrev.AUTOMATISK, List.of(KonsekvensForYtelsen.ENDRING_I_UTTAK));
        }

        if (FastsettBehandlingsresultatVedAvslagPåAvslag.vurder(behandlingsresultatRevurdering,
            behandlingsresultatOriginal, originalBehandling.getType())) {
            /* 2b */
            return FastsettBehandlingsresultatVedAvslagPåAvslag.fastsett(revurdering,
                behandlingsresultatRevurdering.orElse(null));
        }

        if (OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.vurder(behandlingsresultatRevurdering.orElse(null))) {
            /* 2c */
            return OppfyllerIkkeInngangsvilkårPåSkjæringstidsspunkt.fastsett(revurdering,
                behandlingsresultatRevurdering.orElse(null));
        }

        var utfall = medlemTjeneste.utledVilkårUtfall(revurdering);
        if (!utfall.vilkårUtfallType().equals(VilkårUtfallType.OPPFYLT)) {
            var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
            behandlingsresultat.setAvslagsårsak(utfall.avslagsårsak());
            return buildBehandlingsresultat(revurdering, behandlingsresultat,
                BehandlingResultatType.OPPHØR, RettenTil.HAR_IKKE_RETT_TIL_FP,
                Vedtaksbrev.AUTOMATISK, List.of(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER));
        }

        var erEndringIUttakFraEndringstidspunkt = uttakresultatOriginalOpt.harUlikUttaksplan(uttakresultatRevurderingOpt) ||
            uttakresultatOriginalOpt.harUlikKontoEllerMinsterett(uttakresultatRevurderingOpt) ||
            dekningsgradTjeneste.behandlingHarEndretDekningsgrad(revurderingRef);
        if (erEndringIUttakFraEndringstidspunkt
            && uttakresultatRevurderingOpt.kontrollerErSisteUttakAvslåttMedÅrsak()) {
            // Endret ifm TFP-5356 la bruker søke på restdager av minsterett også etter ny stønadsperiode
            // Aktuell kode for TFP-5360 - håndtering av søknad som gir både innvilget og avslått/opphør-perioder
            var opphør = !uttakresultatOriginalOpt.harOpphørsUttakNyeInnvilgetePerioder(uttakresultatRevurderingOpt) || !totette(revurdering);
            if (opphør) {
                return SettOpphørOgIkkeRett.fastsett(revurdering, behandlingsresultatRevurdering.orElse(null), Vedtaksbrev.AUTOMATISK);
            }
        }
        var revurderingsGrunnlagOpt = beregningTjeneste.hent(BehandlingReferanse.fra(revurdering)).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        var originalGrunnlagOpt = beregningTjeneste.hent(BehandlingReferanse.fra(originalBehandling)).flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);

        var erEndringISkalHindreTilbaketrekk = erEndringISkalHindreTilbaketrekk(revurdering, originalBehandling);
        var erEndringIBeregning = ErEndringIBeregning.vurder(revurderingsGrunnlagOpt, originalGrunnlagOpt);
        var erKunEndringIFordelingAvYtelsen = ErKunEndringIFordelingAvYtelsen.vurder(erEndringIBeregning,
            erEndringIUttakFraEndringstidspunkt, revurderingsGrunnlagOpt, originalGrunnlagOpt,
            erEndringISkalHindreTilbaketrekk);

        return fastsettResultatVedEndringer(revurdering, uttakresultatOriginalOpt, erOpphørtFørDagensDato(),
            erEndringIBeregning, erEndringIUttakFraEndringstidspunkt, erVarselOmRevurderingSendt,
            erKunEndringIFordelingAvYtelsen, harInnvilgetIkkeOpphørtVedtak(revurdering.getFagsak()));
    }

    private Optional<Behandlingsresultat> finnBehandlingsresultatPåOriginalBehandling(Long originalBehandlingId) {
        var behandlingsresultatOriginal = behandlingsresultatRepository.hentHvisEksisterer(originalBehandlingId);
        if (behandlingsresultatOriginal.isPresent()) {
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det
            // faktiske resultatet for å kunne vurdere avslag på avslag
            if (BehandlingResultatType.INGEN_ENDRING.equals(
                behandlingsresultatOriginal.get().getBehandlingResultatType())) {
                var forrigeBehandlingId = behandlingRepository.hentBehandling(originalBehandlingId)
                    .getOriginalBehandlingId()
                    .orElseThrow(() -> new IllegalStateException(
                        "Utviklerfeil: Kan ikke ha BehandlingResultatType.INGEN_ENDRING uten original behandling. BehandlingId="
                            + originalBehandlingId));
                return finnBehandlingsresultatPåOriginalBehandling(forrigeBehandlingId);
            }
            return behandlingsresultatOriginal;
        }
        return Optional.empty();
    }

    private boolean harInnvilgetIkkeOpphørtVedtak(Fagsak fagsak) {
        var sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId())
            .orElse(null);
        if (sisteInnvilgede == null) {
            return false;
        }
        var behandlinger = behandlingRepository.hentAbsoluttAlleBehandlingerForSaksnummer(fagsak.getSaksnummer());
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
        return behandlingVedtakRepository.hentForBehandlingHvisEksisterer(b.getId()).orElse(null);
    }

    private boolean erFattetEtter(Behandling sisteInnvilget, BehandlingVedtak vedtak) {
        var sistInnvilgetVedtak = behandlingVedtakRepository.hentForBehandling(sisteInnvilget.getId());
        var sistInnvilgetVedtaksdato = sistInnvilgetVedtak.getVedtaksdato();
        return vedtak != null && vedtak.getVedtaksdato().isAfter(sistInnvilgetVedtaksdato);
    }

    private Predicate<BehandlingVedtak> opphørvedtak() {
        return vedtak -> BehandlingResultatType.OPPHØR.equals(
            vedtak.getBehandlingsresultat().getBehandlingResultatType());
    }

    private VurderOpphørFørDagensDato erOpphørtFørDagensDato() {
        return resultat -> {
            var ref = BehandlingReferanse.fra(behandlingRepository.hentBehandling(resultat.getBehandlingId()));
            var stp = skjæringstidspunktTjeneste.getSkjæringstidspunkter(resultat.getBehandlingId());
            return opphørUttakTjeneste.getOpphørsdato(ref, stp, resultat).orElse(LocalDate.MAX).isBefore(LocalDate.now());
        };
    }

    private void validerReferanser(BehandlingReferanse ref, Long behandlingId) {
        if (!Objects.equals(ref.behandlingId(), behandlingId)) {
            throw new IllegalStateException(
                "BehandlingReferanse [" + ref.behandlingId() + "] matcher ikke forventet [" + behandlingId + "]");
        }
    }

    private boolean erEndringISkalHindreTilbaketrekk(Behandling revurdering, Behandling originalBehandling) {
        var beregningsresultatFPAggregatEntitet = beregningsresultatRepository.hentBeregningsresultatAggregat(
            revurdering.getId());
        var orginalBeregningsresultatFPAggregatEntitet = beregningsresultatRepository.hentBeregningsresultatAggregat(
            originalBehandling.getId());

        if (beregningsresultatFPAggregatEntitet.isPresent() && orginalBeregningsresultatFPAggregatEntitet.isPresent()) {
            return !beregningsresultatFPAggregatEntitet.get()
                .skalHindreTilbaketrekk()
                .equals(orginalBeregningsresultatFPAggregatEntitet.get().skalHindreTilbaketrekk());
        }
        return false;
    }

    public Behandlingsresultat fastsettResultatVedEndringer(Behandling revurdering,
                                                            UttakResultatHolder uttakresultatFraOriginalBehandling,
                                                            VurderOpphørFørDagensDato opphørFørDagensDato,
                                                            boolean erEndringIBeregning,
                                                            boolean erEndringIUttakFraEndringstidspunkt,
                                                            boolean erVarselOmRevurderingSendt,
                                                            boolean erKunEndringIFordelingAvYtelsen,
                                                            boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør) {
        var konsekvenserForYtelsen = utledKonsekvensForYtelsen(erEndringIBeregning,
            erEndringIUttakFraEndringstidspunkt);

        if (!harUttakIkkeOpphørt(uttakresultatFraOriginalBehandling, opphørFørDagensDato)) {
            return fastsettForIkkeEtablertYtelse(revurdering, konsekvenserForYtelsen);
        }

        if (!erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør) {
            return fastsettForIkkeEtablertYtelse(revurdering, konsekvenserForYtelsen);
        }

        if (erKunEndringIFordelingAvYtelsen) {
            return ErKunEndringIFordelingAvYtelsen.fastsett(revurdering,
                behandlingsresultatRepository.hent(revurdering.getId()), erVarselOmRevurderingSendt);
        }
        var vedtaksbrev = utledVedtaksbrev(konsekvenserForYtelsen, erVarselOmRevurderingSendt);
        var behandlingResultatType = utledBehandlingResultatType(konsekvenserForYtelsen);
        return buildBehandlingsresultat(revurdering, behandlingsresultatRepository.hent(revurdering.getId()),
            behandlingResultatType, RettenTil.HAR_RETT_TIL_FP, vedtaksbrev, konsekvenserForYtelsen);
    }

    private boolean harUttakIkkeOpphørt(UttakResultatHolder uttakResultatHolder,
                                        VurderOpphørFørDagensDato opphørFørDagensDato) {
        if (!uttakResultatHolder.eksistererUttakResultat()) {
            return false;
        }
        // Ikke avslått eller opphørt
        var ikkeLøpende = Set.of(BehandlingResultatType.AVSLÅTT, BehandlingResultatType.OPPHØR);
        var opphørtFørDagensDato = uttakResultatHolder.getBehandlingVedtak()
            .map(BehandlingVedtak::getBehandlingsresultat)
            .filter(bres -> ikkeLøpende.contains(bres.getBehandlingResultatType()))
            .map(opphørFørDagensDato::test)
            .orElse(false);
        return !opphørtFørDagensDato;
    }

    private Behandlingsresultat fastsettForIkkeEtablertYtelse(Behandling revurdering,
                                                              List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(revurdering.getId()).orElse(null);
        return buildBehandlingsresultat(revurdering, behandlingsresultat,
            BehandlingResultatType.INNVILGET, RettenTil.HAR_RETT_TIL_FP,
            Vedtaksbrev.AUTOMATISK, konsekvenserForYtelsen);
    }

    private Vedtaksbrev utledVedtaksbrev(List<KonsekvensForYtelsen> konsekvenserForYtelsen,
                                         boolean erVarselOmRevurderingSendt) {
        if (!erVarselOmRevurderingSendt && konsekvenserForYtelsen.contains(KonsekvensForYtelsen.INGEN_ENDRING)) {
            return Vedtaksbrev.INGEN;
        }
        return Vedtaksbrev.AUTOMATISK;
    }

    private BehandlingResultatType utledBehandlingResultatType(List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        if (konsekvenserForYtelsen.contains(KonsekvensForYtelsen.INGEN_ENDRING)) {
            return BehandlingResultatType.INGEN_ENDRING;
        }
        return BehandlingResultatType.FORELDREPENGER_ENDRET;
    }

    private List<KonsekvensForYtelsen> utledKonsekvensForYtelsen(boolean erEndringIBeregning,
                                                                 boolean erEndringIUttakFraEndringstidspunkt) {
        List<KonsekvensForYtelsen> konsekvensForYtelsen = new ArrayList<>();

        if (erEndringIBeregning) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.ENDRING_I_BEREGNING);
        }
        if (erEndringIUttakFraEndringstidspunkt) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.ENDRING_I_UTTAK);
        }
        if (konsekvensForYtelsen.isEmpty()) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.INGEN_ENDRING);
        }
        return konsekvensForYtelsen;
    }

    static Behandlingsresultat buildBehandlingsresultat(Behandling revurdering,
                                                        Behandlingsresultat behandlingsresultat,
                                                        BehandlingResultatType behandlingResultatType,
                                                        RettenTil rettenTil,
                                                        Vedtaksbrev vedtaksbrev,
                                                        List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        var behandlingsresultatBuilder = Behandlingsresultat.builderEndreEksisterende(behandlingsresultat);
        behandlingsresultatBuilder.medBehandlingResultatType(behandlingResultatType);
        behandlingsresultatBuilder.medVedtaksbrev(vedtaksbrev);
        behandlingsresultatBuilder.medRettenTil(rettenTil);
        konsekvenserForYtelsen.forEach(behandlingsresultatBuilder::leggTilKonsekvensForYtelsen);
        return behandlingsresultatBuilder.buildFor(revurdering);
    }

    private boolean totette(Behandling behandling) {
        var gjeldendeFamilieHendelsedato = familieHendelseRepository.hentAggregatHvisEksisterer(behandling.getId())
            .map(FamilieHendelseGrunnlagEntitet::getGjeldendeVersjon)
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt).orElse(null);
        return FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) && gjeldendeFamilieHendelsedato != null &&
            behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER) &&
            nesteSakRepository.hentGrunnlag(behandling.getId()).map(NesteSakGrunnlagEntitet::getHendelsedato)
                .filter(h -> h.isBefore(gjeldendeFamilieHendelsedato.plusDays(1)
                    .plusWeeks(UttakParametre.ukerMellomTetteFødsler(gjeldendeFamilieHendelsedato))))
                .isPresent();
    }


}
