package no.nav.foreldrepenger.behandling.revurdering.felles;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandling.DekningsgradTjeneste;
import no.nav.foreldrepenger.behandling.revurdering.RevurderingFeil;
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
import no.nav.foreldrepenger.behandlingslager.fagsak.Fagsak;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.modell.BeregningsgrunnlagGrunnlag;
import no.nav.foreldrepenger.domene.prosess.BeregningTjeneste;
import no.nav.foreldrepenger.domene.uttak.ForeldrepengerUttak;
import no.nav.foreldrepenger.domene.uttak.Uttak;
import no.nav.foreldrepenger.domene.uttak.UttakTjeneste;
import no.nav.foreldrepenger.regler.uttak.UttakParametre;

@ApplicationScoped
public class RevurderingBehandlingsresultatutlederFelles {

    private BeregningTjeneste beregningTjeneste;
    private MedlemTjeneste medlemTjeneste;

    private BehandlingRepository behandlingRepository;
    private BehandlingVedtakRepository behandlingVedtakRepository;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BeregningsresultatRepository beregningsresultatRepository;
    private FamilieHendelseRepository familieHendelseRepository;
    private NesteSakRepository nesteSakRepository;
    private DekningsgradTjeneste dekningsgradTjeneste;
    private UttakTjeneste uttakTjeneste;

    RevurderingBehandlingsresultatutlederFelles() {
        // for CDI proxy
    }

    @Inject
    public RevurderingBehandlingsresultatutlederFelles(BehandlingRepositoryProvider repositoryProvider,
                                                       BehandlingGrunnlagRepositoryProvider grunnlagRepositoryProvider,
                                                       BeregningTjeneste beregningTjeneste,
                                                       MedlemTjeneste medlemTjeneste,
                                                       DekningsgradTjeneste dekningsgradTjeneste,
                                                       UttakTjeneste uttakTjeneste) {

        this.beregningTjeneste = beregningTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.behandlingVedtakRepository = repositoryProvider.getBehandlingVedtakRepository();
        this.medlemTjeneste = medlemTjeneste;
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.beregningsresultatRepository = repositoryProvider.getBeregningsresultatRepository();
        this.familieHendelseRepository = repositoryProvider.getFamilieHendelseRepository();
        this.nesteSakRepository = grunnlagRepositoryProvider.getNesteSakRepository();
        this.dekningsgradTjeneste = dekningsgradTjeneste;
        this.uttakTjeneste = uttakTjeneste;
    }

    public Behandlingsresultat bestemBehandlingsresultatForRevurdering(BehandlingReferanse revurderingRef, boolean erVarselOmRevurderingSendt) {
        var revurdering = behandlingRepository.hentBehandling(revurderingRef.behandlingId());

        var originalBehandlingId = revurdering.getOriginalBehandlingId()
            .orElseThrow(() -> RevurderingFeil.revurderingManglerOriginalBehandling(revurdering.getId()));

        var revurderingUttak = uttakTjeneste.hentHvisEksisterer(revurderingRef.behandlingId());
        var originalBehandlingUttak = uttakTjeneste.hentHvisEksisterer(originalBehandlingId);

        return bestemBehandlingsresultatForRevurderingCore(revurdering, behandlingRepository.hentBehandling(originalBehandlingId), revurderingUttak,
            originalBehandlingUttak, erVarselOmRevurderingSendt);
    }

    private Behandlingsresultat bestemBehandlingsresultatForRevurderingCore(Behandling revurdering,
                                                                            Behandling originalBehandling,
                                                                            Optional<Uttak> uttakRevurdering,
                                                                            Optional<Uttak> uttakOriginal,
                                                                            boolean erVarselOmRevurderingSendt) {
        if (!revurdering.getType().equals(BehandlingType.REVURDERING)) {
            throw new IllegalStateException("Utviklerfeil: Skal ikke kunne havne her uten en revurderingssak");
        }
        var behandlingId = revurdering.getId();

        var behandlingsresultatRevurdering = behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElseThrow();
        var behandlingsresultatOriginal = finnBehandlingsresultatPåOriginalBehandling(originalBehandling.getId()).orElseThrow();

        if (erAvslagPåAvslag(behandlingsresultatRevurdering, behandlingsresultatOriginal)) {
            return buildBehandlingsresultat(revurdering, behandlingsresultatRevurdering, BehandlingResultatType.INGEN_ENDRING,
                RettenTil.HAR_IKKE_RETT_TIL_FP, Vedtaksbrev.INGEN, List.of(KonsekvensForYtelsen.INGEN_ENDRING));
        } else if (SpesialBehandling.erOppsagtUttak(revurdering) || (erAnnulleringAvUttak(uttakRevurdering) && uttakOriginal.isPresent())) {
            return buildBehandlingsresultat(revurdering, behandlingsresultatRevurdering, BehandlingResultatType.FORELDREPENGER_SENERE,
                RettenTil.HAR_RETT_TIL_FP, Vedtaksbrev.AUTOMATISK, List.of(KonsekvensForYtelsen.ENDRING_I_UTTAK));
        }

        if (behandlingsresultatRevurdering.isInngangsVilkårAvslått()) {
            return opphør(revurdering, behandlingsresultatRevurdering);
        }

        //Opphør i løpet av uttaket, vilkår er fortsatt oppfylt
        var opphørsdatoRevurdering = uttakRevurdering.flatMap(Uttak::opphørsdato);
        var opphørsdatoOriginal = uttakOriginal.flatMap(Uttak::opphørsdato);
        if (opphørsdatoRevurdering.isPresent() && !Objects.equals(opphørsdatoRevurdering, opphørsdatoOriginal)) {
            var avslagsårsak = medlemTjeneste.hentAvslagsårsak(behandlingId);
            behandlingsresultatRevurdering.setAvslagsårsak(avslagsårsak.orElse(null));
            return opphør(revurdering, behandlingsresultatRevurdering);
        }

        var revurderingRef = BehandlingReferanse.fra(revurdering);
        var erEndringIUttak = erEndringIUttak(uttakRevurdering, uttakOriginal, revurderingRef);
        if (erEndringIUttak && uttakRevurdering.map(Uttak::erOpphør).orElse(false)) {
            // Endret ifm TFP-5356 la bruker søke på restdager av minsterett også etter ny stønadsperiode
            // Aktuell kode for TFP-5360 - håndtering av søknad som gir både innvilget og avslått/opphør-perioder
            var opphør = !uttakOriginal.orElseThrow().harOpphørsUttakNyeInnvilgetePerioder(uttakRevurdering.orElseThrow()) || !totette(revurdering);
            if (opphør) {
                return opphør(revurdering, behandlingsresultatRevurdering);
            }
        }
        var revurderingsGrunnlagOpt = beregningTjeneste.hent(BehandlingReferanse.fra(revurdering))
            .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);
        var originalGrunnlagOpt = beregningTjeneste.hent(BehandlingReferanse.fra(originalBehandling))
            .flatMap(BeregningsgrunnlagGrunnlag::getBeregningsgrunnlag);

        var erEndringISkalHindreTilbaketrekk = erEndringISkalHindreTilbaketrekk(revurdering, originalBehandling);
        var erEndringIBeregning = ErEndringIBeregning.vurder(revurderingsGrunnlagOpt, originalGrunnlagOpt);
        var erKunEndringIFordelingAvYtelsen = ErKunEndringIFordelingAvYtelsen.vurder(erEndringIBeregning, erEndringIUttak, revurderingsGrunnlagOpt,
            originalGrunnlagOpt, erEndringISkalHindreTilbaketrekk);

        return fastsettResultatVedEndringer(revurdering, uttakOriginal, erEndringIBeregning, erEndringIUttak, erVarselOmRevurderingSendt,
            erKunEndringIFordelingAvYtelsen, harInnvilgetIkkeOpphørtVedtak(revurdering.getFagsak()));
    }

    static boolean erAnnulleringAvUttak(Optional<Uttak> uttak) {
        if (uttak.isEmpty()) {
            return true;
        }
        return uttak
            .filter(ForeldrepengerUttak.class::isInstance)
            .map(ForeldrepengerUttak.class::cast)
            .map(RevurderingBehandlingsresultatutlederFelles::erUttakTomt)
            .orElse(false);
    }

    private static boolean erUttakTomt(ForeldrepengerUttak uttak) {
        if (uttak == null) {
            return true;
        }
        return uttak.getGjeldendePerioder().stream().noneMatch(periode -> periode.harUtbetaling() || periode.harTrekkdager());
    }

    private boolean erEndringIUttak(Optional<Uttak> uttakRevurdering, Optional<Uttak> uttakOriginal, BehandlingReferanse revurderingRef) {
        if (uttakRevurdering.isPresent() && uttakOriginal.isPresent()) {
            return uttakOriginal.get().harUlikUttaksplan(uttakRevurdering.get()) || uttakOriginal.get()
                .harUlikKontoEllerMinsterett(uttakRevurdering.get()) || dekningsgradTjeneste.behandlingHarEndretDekningsgrad(revurderingRef);
        }
        return !Objects.equals(uttakOriginal, uttakRevurdering);
    }

    private static Behandlingsresultat opphør(Behandling revurdering, Behandlingsresultat behandlingsresultatRevurdering) {
        return buildBehandlingsresultat(revurdering, behandlingsresultatRevurdering, BehandlingResultatType.OPPHØR, RettenTil.HAR_IKKE_RETT_TIL_FP,
            Vedtaksbrev.AUTOMATISK, List.of(KonsekvensForYtelsen.FORELDREPENGER_OPPHØRER));
    }

    static boolean erAvslagPåAvslag(Behandlingsresultat resRevurdering, Behandlingsresultat resOriginal) {
        return resRevurdering.isVilkårAvslått() && resOriginal.isBehandlingsresultatAvslått();
    }

    private Optional<Behandlingsresultat> finnBehandlingsresultatPåOriginalBehandling(Long originalBehandlingId) {
        var behandlingsresultatOriginal = behandlingsresultatRepository.hentHvisEksisterer(originalBehandlingId);
        if (behandlingsresultatOriginal.isPresent()) {
            // Dersom originalBehandling er et beslutningsvedtak må vi lete videre etter det
            // faktiske resultatet for å kunne vurdere avslag på avslag
            if (BehandlingResultatType.INGEN_ENDRING.equals(behandlingsresultatOriginal.get().getBehandlingResultatType())) {
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
        var sisteInnvilgede = behandlingRepository.finnSisteAvsluttedeIkkeHenlagteBehandling(fagsak.getId()).orElse(null);
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
        return vedtak -> BehandlingResultatType.OPPHØR.equals(vedtak.getBehandlingsresultat().getBehandlingResultatType());
    }

    private boolean erEndringISkalHindreTilbaketrekk(Behandling revurdering, Behandling originalBehandling) {
        var beregningsresultatFPAggregatEntitet = beregningsresultatRepository.hentBeregningsresultatAggregat(revurdering.getId());
        var orginalBeregningsresultatFPAggregatEntitet = beregningsresultatRepository.hentBeregningsresultatAggregat(originalBehandling.getId());

        if (beregningsresultatFPAggregatEntitet.isPresent() && orginalBeregningsresultatFPAggregatEntitet.isPresent()) {
            return !beregningsresultatFPAggregatEntitet.get()
                .skalHindreTilbaketrekk()
                .equals(orginalBeregningsresultatFPAggregatEntitet.get().skalHindreTilbaketrekk());
        }
        return false;
    }

    public Behandlingsresultat fastsettResultatVedEndringer(Behandling revurdering,
                                                            Optional<Uttak> uttakFraOriginalBehandling,
                                                            boolean erEndringIBeregning,
                                                            boolean erEndringIUttak,
                                                            boolean erVarselOmRevurderingSendt,
                                                            boolean erKunEndringIFordelingAvYtelsen,
                                                            boolean erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør) {
        var konsekvenserForYtelsen = utledKonsekvensForYtelsen(erEndringIBeregning, erEndringIUttak);

        if (uttakFraOriginalBehandling.isEmpty() || erUttakOpphørtFørDagensDato(uttakFraOriginalBehandling.get())) {
            return fastsettForIkkeEtablertYtelse(revurdering, konsekvenserForYtelsen);
        }

        if (!erMinstEnInnvilgetBehandlingUtenPåfølgendeOpphør) {
            return fastsettForIkkeEtablertYtelse(revurdering, konsekvenserForYtelsen);
        }

        if (erKunEndringIFordelingAvYtelsen) {
            return ErKunEndringIFordelingAvYtelsen.fastsett(revurdering, behandlingsresultatRepository.hent(revurdering.getId()),
                erVarselOmRevurderingSendt);
        }
        var vedtaksbrev = utledVedtaksbrev(konsekvenserForYtelsen, erVarselOmRevurderingSendt);
        var behandlingResultatType = utledBehandlingResultatType(konsekvenserForYtelsen);
        return buildBehandlingsresultat(revurdering, behandlingsresultatRepository.hent(revurdering.getId()), behandlingResultatType,
            RettenTil.HAR_RETT_TIL_FP, vedtaksbrev, konsekvenserForYtelsen);
    }

    private boolean erUttakOpphørtFørDagensDato(Uttak uttak) {
        return uttak.opphørsdato().stream().anyMatch(od -> od.isBefore(LocalDate.now()));
    }

    private Behandlingsresultat fastsettForIkkeEtablertYtelse(Behandling revurdering, List<KonsekvensForYtelsen> konsekvenserForYtelsen) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(revurdering.getId()).orElse(null);
        return buildBehandlingsresultat(revurdering, behandlingsresultat, BehandlingResultatType.INNVILGET, RettenTil.HAR_RETT_TIL_FP,
            Vedtaksbrev.AUTOMATISK, konsekvenserForYtelsen);
    }

    private Vedtaksbrev utledVedtaksbrev(List<KonsekvensForYtelsen> konsekvenserForYtelsen, boolean erVarselOmRevurderingSendt) {
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

    private List<KonsekvensForYtelsen> utledKonsekvensForYtelsen(boolean erEndringIBeregning, boolean erEndringIUttak) {
        List<KonsekvensForYtelsen> konsekvensForYtelsen = new ArrayList<>();

        if (erEndringIBeregning) {
            konsekvensForYtelsen.add(KonsekvensForYtelsen.ENDRING_I_BEREGNING);
        }
        if (erEndringIUttak) {
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
            .map(FamilieHendelseEntitet::getSkjæringstidspunkt)
            .orElse(null);
        return FagsakYtelseType.FORELDREPENGER.equals(behandling.getFagsakYtelseType()) && gjeldendeFamilieHendelsedato != null
            && behandling.harBehandlingÅrsak(BehandlingÅrsakType.RE_ENDRING_FRA_BRUKER) && nesteSakRepository.hentGrunnlag(behandling.getId())
            .map(NesteSakGrunnlagEntitet::getHendelsedato)
            .filter(h -> h.isBefore(
                gjeldendeFamilieHendelsedato.plusDays(1).plusWeeks(UttakParametre.ukerMellomTetteFødsler(gjeldendeFamilieHendelsedato))))
            .isPresent();
    }


}
