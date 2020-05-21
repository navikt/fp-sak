package no.nav.foreldrepenger.domene.medlem;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.BehandlingReferanse;
import no.nav.foreldrepenger.behandlingslager.aktør.AdresseType;
import no.nav.foreldrepenger.behandlingslager.aktør.PersonstatusType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatDiff;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.personopplysning.PersonopplysningerAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.diff.DiffResult;
import no.nav.foreldrepenger.behandlingslager.geografisk.Region;
import no.nav.foreldrepenger.behandlingslager.kodeverk.BasisKodeverdi;
import no.nav.foreldrepenger.domene.medlem.api.EndringsresultatPersonopplysningerForMedlemskap;
import no.nav.foreldrepenger.domene.medlem.api.EndringsresultatPersonopplysningerForMedlemskap.EndretAttributt;
import no.nav.foreldrepenger.domene.medlem.api.FinnMedlemRequest;
import no.nav.foreldrepenger.domene.medlem.api.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.medlem.api.VurderMedlemskap;
import no.nav.foreldrepenger.domene.medlem.api.VurderingsÅrsak;
import no.nav.foreldrepenger.domene.medlem.impl.HentMedlemskapFraRegister;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;
import no.nav.vedtak.util.FPDateUtil;
import no.nav.vedtak.util.Tuple;

@ApplicationScoped
public class MedlemTjeneste {

    private static Map<MedlemResultat, AksjonspunktDefinisjon> mapMedlemResulatTilAkDef = new EnumMap<>(MedlemResultat.class);

    static {
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OM_ER_BOSATT, AksjonspunktDefinisjon.AVKLAR_OM_ER_BOSATT);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE, AksjonspunktDefinisjon.AVKLAR_GYLDIG_MEDLEMSKAPSPERIODE);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_LOVLIG_OPPHOLD, AksjonspunktDefinisjon.AVKLAR_LOVLIG_OPPHOLD);
        mapMedlemResulatTilAkDef.put(MedlemResultat.AVKLAR_OPPHOLDSRETT, AksjonspunktDefinisjon.AVKLAR_OPPHOLDSRETT);
        mapMedlemResulatTilAkDef.put(MedlemResultat.VENT_PÅ_FØDSEL, AksjonspunktDefinisjon.VENT_PÅ_FØDSEL);
    }

    private MedlemskapRepository medlemskapRepository;
    private HentMedlemskapFraRegister hentMedlemskapFraRegister;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private PersonopplysningTjeneste personopplysningTjeneste;
    private UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerTjeneste;
    private VurderMedlemskapTjeneste vurderMedlemskapTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;

    MedlemTjeneste() {
        // CDI
    }

    @Inject
    public MedlemTjeneste(BehandlingRepositoryProvider repositoryProvider,
                          HentMedlemskapFraRegister hentMedlemskapFraRegister,
                          MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository,
                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                          PersonopplysningTjeneste personopplysningTjeneste,
                          UtledVurderingsdatoerForMedlemskapTjeneste utledVurderingsdatoerForMedlemskapTjeneste,
                          VurderMedlemskapTjeneste vurderMedlemskapTjeneste) {
        this.hentMedlemskapFraRegister = hentMedlemskapFraRegister;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.medlemskapVilkårPeriodeRepository = medlemskapVilkårPeriodeRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.personopplysningTjeneste = personopplysningTjeneste;
        this.utledVurderingsdatoerTjeneste = utledVurderingsdatoerForMedlemskapTjeneste;
        this.vurderMedlemskapTjeneste = vurderMedlemskapTjeneste;
    }

    /**
     * Finn medlemskapsperioder i MEDL2 register for en person.
     *
     * @param finnMedlemRequest Inneholder fødselsnummer, start-/slutt- dato for søket, og behandling-/fagsak- ID.
     * @return Liste av medlemsperioder funnet
     */
    public List<Medlemskapsperiode> finnMedlemskapPerioder(FinnMedlemRequest finnMedlemRequest) {
        return hentMedlemskapFraRegister.finnMedlemskapPerioder(finnMedlemRequest);
    }

    public List<Medlemskapsperiode> finnMedlemskapPerioder(FinnMedlemRequest finnMedlemRequest, AktørId aktørId) {
        return hentMedlemskapFraRegister.finnMedlemskapPerioder(finnMedlemRequest, aktørId);
    }

    public Optional<MedlemskapAggregat> hentMedlemskap(Long behandlingId) {
        return medlemskapRepository.hentMedlemskap(behandlingId);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        Optional<Long> funnetId = medlemskapRepository.hentIdPåAktivMedlemskap(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(MedlemskapAggregat.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(MedlemskapAggregat.class));
    }

    /**
     * Sjekker endringer i personopplysninger som tilsier at bruker 'ikke er'/'skal miste' medlemskap.
     * Sjekker statsborgerskap (kun mht endring i {@link Region}, ikke land),
     * {@link PersonstatusType}, og {@link AdresseType}
     * for intervall { max(seneste vedtatte medlemskapsperiode, skjæringstidspunkt), nå}.
     * <p>
     * Metoden gjelder revurdering foreldrepenger
     */
    // TODO Diamant (Denne gjelder kun revurdering og foreldrepenger, bør eksponeres som egen tjeneste for FP + BT004)
    public EndringsresultatPersonopplysningerForMedlemskap søkerHarEndringerIPersonopplysninger(Behandling revurderingBehandling) {

        EndringsresultatPersonopplysningerForMedlemskap.Builder builder = EndringsresultatPersonopplysningerForMedlemskap.builder();
        if (revurderingBehandling.erRevurdering() && revurderingBehandling.getFagsakYtelseType().gjelderForeldrepenger()) {
            AktørId aktørId = revurderingBehandling.getAktørId();
            Long behandlingId = revurderingBehandling.getId();
            DatoIntervallEntitet intervall = DatoIntervallEntitet.fraOgMedTilOgMed(finnStartdato(revurderingBehandling), FPDateUtil.iDag());
            Optional<PersonopplysningerAggregat> historikkAggregat = personopplysningTjeneste
                .hentGjeldendePersoninformasjonForPeriodeHvisEksisterer(behandlingId, aktørId, intervall);

            historikkAggregat.ifPresent(historikk -> {
                sjekkEndringer(historikk.getStatsborgerskapFor(aktørId).stream()
                        .map(e -> new ElementMedGyldighetsintervallWrapper<>(e.getStatsborgerskap(), e.getPeriode())), builder,
                    EndretAttributt.StatsborgerskapRegion);

                sjekkEndringer(historikk.getPersonstatuserFor(aktørId).stream()
                    .map(e -> new ElementMedGyldighetsintervallWrapper<>(e.getPersonstatus(), e.getPeriode())), builder, EndretAttributt.Personstatus);

                sjekkEndringer(historikk.getAdresserFor(aktørId).stream()
                    .map(e -> new ElementMedGyldighetsintervallWrapper<>(e.getAdresseType(), e.getPeriode())), builder, EndretAttributt.Adresse);
            });
        }
        return builder.build();
    }

    public Map<LocalDate, VurderMedlemskap> utledVurderingspunkterMedAksjonspunkt(BehandlingReferanse ref) {
        final Map<LocalDate, Set<VurderingsÅrsak>> vurderingsdatoer = utledVurderingsdatoerTjeneste.finnVurderingsdatoerMedÅrsak(ref.getBehandlingId());
        final HashMap<LocalDate, VurderMedlemskap> map = new HashMap<>();
        for (Map.Entry<LocalDate, Set<VurderingsÅrsak>> entry : vurderingsdatoer.entrySet()) {
            LocalDate vurderingsdato = entry.getKey();
            final Set<MedlemResultat> vurderinger = vurderMedlemskapTjeneste.vurderMedlemskap(ref, vurderingsdato);
            if (!vurderinger.isEmpty()) {
                map.put(vurderingsdato, mapTilVurderMeldemspa(vurderinger, entry.getValue()));
            }
        }
        return map;
    }

    private VurderMedlemskap mapTilVurderMeldemspa(Set<MedlemResultat> vurderinger, Set<VurderingsÅrsak> vurderingsÅrsaks) {
        final Set<AksjonspunktDefinisjon> aksjonspunkter = vurderinger.stream()
            .map(vu -> Optional.ofNullable(mapMedlemResulatTilAkDef.get(vu)).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return new VurderMedlemskap(aksjonspunkter, vurderingsÅrsaks);
    }

    public DiffResult diffResultat(EndringsresultatDiff idDiff, boolean kunSporedeEndringer) {
        Objects.requireNonNull(idDiff.getGrunnlagId1(), "kan ikke diffe når id1 ikke er oppgitt");
        Objects.requireNonNull(idDiff.getGrunnlagId2(), "kan ikke diffe når id2 ikke er oppgitt");

        return medlemskapRepository.diffResultat((Long) idDiff.getGrunnlagId1(), (Long) idDiff.getGrunnlagId2(), kunSporedeEndringer);
    }

    /**
     * Sjekker både medlemskapsvilkåret og løpende medlemskapsvilkår
     * Tar hensyn til overstyring
     *
     * @param behandling
     * @return opphørsdato
     */
    public Optional<LocalDate> hentOpphørsdatoHvisEksisterer(Behandling behandling) {
        Optional<Behandlingsresultat> behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
        if (behandlingsresultatOpt.isEmpty() || behandlingsresultatOpt.get().getVilkårResultat() == null) {
            return Optional.empty();
        }
        Behandlingsresultat behandlingsresultat = behandlingsresultatOpt.get();
        Optional<Vilkår> medlemskapsvilkåret = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET)).findFirst();

        if (medlemskapsvilkåret.isPresent()) {
            Vilkår medlem = medlemskapsvilkåret.get();
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT)) {
                Optional<Vilkår> medlemLøpendeOpt = behandlingsresultat.getVilkårResultat()
                    .getVilkårene()
                    .stream()
                    .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE))
                    .findFirst();

                if (medlemLøpendeOpt.isPresent()) {
                    Vilkår medlemLøpende = medlemLøpendeOpt.get();
                    if (medlemLøpende.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT)) {
                        return Optional.empty();
                    } else {
                        return medlemskapVilkårPeriodeRepository.hentOpphørsdatoHvisEksisterer(behandling);
                    }
                }
                return Optional.empty();
            }
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandling.getId()).getSkjæringstidspunktHvisUtledet();
            }
        }
        return Optional.empty();
    }

    private <T extends BasisKodeverdi> void sjekkEndringer(Stream<ElementMedGyldighetsintervallWrapper<T>> elementer,
                                                      EndringsresultatPersonopplysningerForMedlemskap.Builder builder, EndretAttributt endretAttributt) {
        List<ElementMedGyldighetsintervallWrapper<T>> endringer = elementer
            .sorted(Comparator.comparing(ElementMedGyldighetsintervallWrapper::sortPeriode))
            .distinct().collect(Collectors.toList());

        leggTilEndringer(endringer, builder, endretAttributt);
    }

    private <T extends BasisKodeverdi> void leggTilEndringer(List<ElementMedGyldighetsintervallWrapper<T>> endringer,
                                                        EndringsresultatPersonopplysningerForMedlemskap.Builder builder, EndretAttributt endretAttributt) {
        if (endringer != null && endringer.size() > 1) {
            for (int i = 0; i < endringer.size() - 1; i++) {
                String endretFra = endringer.get(i).element.getNavn();
                String endretTil = endringer.get(i + 1).element.getNavn();
                DatoIntervallEntitet periode = endringer.get(i + 1).gylidghetsintervall;
                builder.leggTilEndring(endretAttributt, periode, endretFra, endretTil);
            }
        }
    }

    private LocalDate finnStartdato(Behandling revurderingBehandling) {

        Optional<MedlemskapVilkårPeriodeGrunnlagEntitet> medlemskapsvilkårPeriodeGrunnlag = medlemskapVilkårPeriodeRepository
            .hentAggregatHvisEksisterer(revurderingBehandling.getOriginalBehandling().get());

        LocalDate startDato = skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurderingBehandling.getId()).getUtledetSkjæringstidspunkt();
        if (medlemskapsvilkårPeriodeGrunnlag.isPresent()) {
            LocalDate date = medlemskapsvilkårPeriodeGrunnlag.get()
                .getMedlemskapsvilkårPeriode()
                .getPerioder()
                .stream().map(MedlemskapsvilkårPerioderEntitet::getFom)
                .max(LocalDate::compareTo)
                .get();

            if (startDato.isBefore(date)) {
                startDato = date;
            }
        }

        return startDato.isAfter(FPDateUtil.iDag()) ? FPDateUtil.iDag() : startDato;
    }

    public Tuple<VilkårUtfallType, Avslagsårsak> utledVilkårUtfall(Behandling revurdering) {
        Behandlingsresultat behandlingsresultat = behandlingsresultatRepository.hent(revurdering.getId());
        Optional<Vilkår> medlemOpt = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();

        if (medlemOpt.isPresent()) {
            Vilkår medlem = medlemOpt.get();
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                return new Tuple<>(medlem.getGjeldendeVilkårUtfall(), Avslagsårsak.fraKode(medlem.getVilkårUtfallMerknad().getKode()));
            } else {
                Optional<Vilkår> løpendeOpt = behandlingsresultat.getVilkårResultat()
                    .getVilkårene()
                    .stream()
                    .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE))
                    .findFirst();
                if (løpendeOpt.isPresent()) {
                    Vilkår løpende = løpendeOpt.get();
                    if (løpende.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT) && !løpende.erOverstyrt()) {
                        return new Tuple<>(VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.fraKode(løpende.getVilkårUtfallMerknad().getKode()));
                    } else if (løpende.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT) && løpende.erOverstyrt()) {
                        Avslagsårsak avslagsårsak = løpende.getAvslagsårsak();
                        return new Tuple<>(VilkårUtfallType.IKKE_OPPFYLT, avslagsårsak);
                    }
                }
            }
            return new Tuple<>(VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT);
        }
        throw new IllegalStateException("Kan ikke utlede vilkår utfall type når medlemskapsvilkåret ikke finnes");
    }

    private static final class ElementMedGyldighetsintervallWrapper<T> {
        private final T element;
        private final DatoIntervallEntitet gylidghetsintervall;

        private ElementMedGyldighetsintervallWrapper(T element, DatoIntervallEntitet gylidghetsintervall) {
            Objects.requireNonNull(element);
            Objects.requireNonNull(gylidghetsintervall);
            this.element = element;
            this.gylidghetsintervall = gylidghetsintervall;
        }

        private static Long sortPeriode(ElementMedGyldighetsintervallWrapper<?> e) {
            return e.gylidghetsintervall.getFomDato().toEpochDay();
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            if (obj instanceof ElementMedGyldighetsintervallWrapper<?>) {
                ElementMedGyldighetsintervallWrapper<?> other = (ElementMedGyldighetsintervallWrapper<?>) obj;
                return element.equals(other.element);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(element, gylidghetsintervall);
        }
    }
}
