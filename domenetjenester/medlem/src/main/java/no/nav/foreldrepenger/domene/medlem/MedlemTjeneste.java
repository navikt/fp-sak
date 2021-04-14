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
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapBehandlingsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.behandlingslager.kodeverk.BasisKodeverdi;
import no.nav.foreldrepenger.domene.medlem.api.EndringsresultatPersonopplysningerForMedlemskap;
import no.nav.foreldrepenger.domene.medlem.api.EndringsresultatPersonopplysningerForMedlemskap.EndretAttributt;
import no.nav.foreldrepenger.domene.medlem.api.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.medlem.api.VurderMedlemskap;
import no.nav.foreldrepenger.domene.medlem.api.VurderingsÅrsak;
import no.nav.foreldrepenger.domene.medlem.impl.HentMedlemskapFraRegister;
import no.nav.foreldrepenger.domene.medlem.impl.MedlemResultat;
import no.nav.foreldrepenger.domene.personopplysning.PersonopplysningTjeneste;
import no.nav.foreldrepenger.domene.tid.DatoIntervallEntitet;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

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
    private BehandlingRepository behandlingRepository;

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
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
    }

    /**
     * Finn medlemskapsperioder i MEDL2 register for en person.
     * @return Liste av medlemsperioder funnet
     */
    public List<Medlemskapsperiode> finnMedlemskapPerioder(AktørId aktørId, LocalDate fom, LocalDate tom) {
        return hentMedlemskapFraRegister.finnMedlemskapPerioder(aktørId, fom, tom);
    }

    public Optional<MedlemskapAggregat> hentMedlemskap(Long behandlingId) {
        return medlemskapRepository.hentMedlemskap(behandlingId);
    }

    public EndringsresultatSnapshot finnAktivGrunnlagId(Long behandlingId) {
        var funnetId = medlemskapRepository.hentIdPåAktivMedlemskap(behandlingId);
        return funnetId
            .map(id -> EndringsresultatSnapshot.medSnapshot(MedlemskapAggregat.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(MedlemskapAggregat.class));
    }

    /**
     * Sjekker endringer i personopplysninger som tilsier at bruker 'ikke er'/'skal miste' medlemskap.
     * Sjekker statsborgerskap (kun mht endring i Region, ikke land),
     * {@link PersonstatusType}, og {@link AdresseType}
     * for intervall { max(seneste vedtatte medlemskapsperiode, skjæringstidspunkt), nå}.
     * <p>
     * Metoden gjelder revurdering foreldrepenger
     */
    // TODO Diamant (Denne gjelder kun revurdering og foreldrepenger, bør eksponeres som egen tjeneste for FP + BT004)
    public EndringsresultatPersonopplysningerForMedlemskap søkerHarEndringerIPersonopplysninger(Behandling revurderingBehandling) {

        var builder = EndringsresultatPersonopplysningerForMedlemskap.builder();
        if (revurderingBehandling.erRevurdering() && FagsakYtelseType.FORELDREPENGER.equals(revurderingBehandling.getFagsakYtelseType())) {
            var aktørId = revurderingBehandling.getAktørId();
            var behandlingId = revurderingBehandling.getId();
            var intervall = DatoIntervallEntitet.fraOgMedTilOgMed(finnStartdato(revurderingBehandling), LocalDate.now());
            var historikkAggregat = personopplysningTjeneste
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
        final var vurderingsdatoer = utledVurderingsdatoerTjeneste.finnVurderingsdatoerMedÅrsak(ref);
        final var map = new HashMap<LocalDate, VurderMedlemskap>();
        for (var entry : vurderingsdatoer.entrySet()) {
            var vurderingsdato = entry.getKey();
            final var vurderinger = vurderMedlemskapTjeneste.vurderMedlemskap(ref, vurderingsdato);
            if (!vurderinger.isEmpty()) {
                map.put(vurderingsdato, mapTilVurderMeldemspa(vurderinger, entry.getValue()));
            }
        }
        return map;
    }

    private VurderMedlemskap mapTilVurderMeldemspa(Set<MedlemResultat> vurderinger, Set<VurderingsÅrsak> vurderingsÅrsaks) {
        final var aksjonspunkter = vurderinger.stream()
            .map(vu -> Optional.ofNullable(mapMedlemResulatTilAkDef.get(vu)).orElse(null))
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
        return new VurderMedlemskap(aksjonspunkter, vurderingsÅrsaks);
    }

    /**
     * TODO: Sjekk denne mot det som hentes til MedlemV2Dto .... Denne brukes som UttakInput
     *
     * Sjekker både medlemskapsvilkåret og løpende medlemskapsvilkår
     * Tar hensyn til overstyring
     *
     * @param behandling
     * @return opphørsdato
     */
    public Optional<LocalDate> hentOpphørsdatoHvisEksisterer(Long behandlingId) {
        var behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingsresultatOpt.isEmpty() || behandlingsresultatOpt.get().getVilkårResultat() == null) {
            return Optional.empty();
        }
        var behandlingsresultat = behandlingsresultatOpt.get();
        var medlemskapsvilkåret = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET)).findFirst();

        if (medlemskapsvilkåret.isPresent()) {
            var medlem = medlemskapsvilkåret.get();
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT)) {
                var medlemLøpendeOpt = behandlingsresultat.getVilkårResultat()
                    .getVilkårene()
                    .stream()
                    .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE))
                    .findFirst();

                if (medlemLøpendeOpt.isPresent()) {
                    var medlemLøpende = medlemLøpendeOpt.get();
                    if (medlemLøpende.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT)) {
                        return Optional.empty();
                    }
                    var behandling = behandlingRepository.hentBehandling(behandlingId);
                    return medlemskapVilkårPeriodeRepository.hentOpphørsdatoHvisEksisterer(behandling);
                }
                return Optional.empty();
            }
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                return skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getSkjæringstidspunktHvisUtledet();
            }
        }
        return Optional.empty();
    }

    private <T extends BasisKodeverdi> void sjekkEndringer(Stream<ElementMedGyldighetsintervallWrapper<T>> elementer,
                                                      EndringsresultatPersonopplysningerForMedlemskap.Builder builder, EndretAttributt endretAttributt) {
        var endringer = elementer
            .sorted(Comparator.comparing(ElementMedGyldighetsintervallWrapper::sortPeriode))
            .distinct().collect(Collectors.toList());

        leggTilEndringer(endringer, builder, endretAttributt);
    }

    private <T extends BasisKodeverdi> void leggTilEndringer(List<ElementMedGyldighetsintervallWrapper<T>> endringer,
                                                        EndringsresultatPersonopplysningerForMedlemskap.Builder builder, EndretAttributt endretAttributt) {
        if (endringer != null && endringer.size() > 1) {
            for (var i = 0; i < endringer.size() - 1; i++) {
                var endretFra = endringer.get(i).element.getNavn();
                var endretTil = endringer.get(i + 1).element.getNavn();
                var periode = endringer.get(i + 1).gylidghetsintervall;
                builder.leggTilEndring(endretAttributt, periode, endretFra, endretTil);
            }
        }
    }

    private LocalDate finnStartdato(Behandling revurderingBehandling) {

        var medlemskapsvilkårPeriodeGrunnlag = revurderingBehandling.getOriginalBehandlingId()
            .map(behandlingRepository::hentBehandling)
            .flatMap(medlemskapVilkårPeriodeRepository::hentAggregatHvisEksisterer);

        var startDato = skjæringstidspunktTjeneste.getSkjæringstidspunkter(revurderingBehandling.getId()).getUtledetSkjæringstidspunkt();
        if (medlemskapsvilkårPeriodeGrunnlag.isPresent()) {
            var date = medlemskapsvilkårPeriodeGrunnlag.get()
                .getMedlemskapsvilkårPeriode()
                .getPerioder()
                .stream().map(MedlemskapsvilkårPerioderEntitet::getFom)
                .max(LocalDate::compareTo)
                .get();

            if (startDato.isBefore(date)) {
                startDato = date;
            }
        }

        return startDato.isAfter(LocalDate.now()) ? LocalDate.now() : startDato;
    }

    public static record VilkårUtfallMedÅrsak(VilkårUtfallType vilkårUtfallType, Avslagsårsak avslagsårsak) {}

    public VilkårUtfallMedÅrsak utledVilkårUtfall(Behandling revurdering) {
        var behandlingsresultat = behandlingsresultatRepository.hent(revurdering.getId());
        var medlemOpt = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();

        if (medlemOpt.isPresent()) {
            var medlem = medlemOpt.get();
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                return new VilkårUtfallMedÅrsak(medlem.getGjeldendeVilkårUtfall(), Avslagsårsak.fraKode(medlem.getVilkårUtfallMerknad().getKode()));
            }
            var løpendeOpt = behandlingsresultat.getVilkårResultat()
                .getVilkårene()
                .stream()
                .filter(vilkårType -> vilkårType.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE))
                .findFirst();
            if (løpendeOpt.isPresent()) {
                var løpende = løpendeOpt.get();
                if (løpende.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT) && !løpende.erOverstyrt()) {
                    return new VilkårUtfallMedÅrsak(VilkårUtfallType.IKKE_OPPFYLT, Avslagsårsak.fraKode(løpende.getVilkårUtfallMerknad().getKode()));
                }
                if (løpende.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT) && løpende.erOverstyrt()) {
                    var avslagsårsak = løpende.getAvslagsårsak();
                    return new VilkårUtfallMedÅrsak(VilkårUtfallType.IKKE_OPPFYLT, avslagsårsak);
                }
            }
            return new VilkårUtfallMedÅrsak(VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT);
        }
        throw new IllegalStateException("Kan ikke utlede vilkår utfall type når medlemskapsvilkåret ikke finnes");
    }

    public Optional<MedlemskapBehandlingsgrunnlagEntitet> hentGrunnlagPåId(Long grunnlagId) {
        return medlemskapRepository.hentGrunnlagPåId(grunnlagId);
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
                var other = (ElementMedGyldighetsintervallWrapper<?>) obj;
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
