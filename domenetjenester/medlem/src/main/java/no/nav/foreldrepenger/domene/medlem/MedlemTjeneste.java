package no.nav.foreldrepenger.domene.medlem;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapBehandlingsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOpphør;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.AvslagsårsakMapper;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.domene.medlem.medl2.HentMedlemskapFraRegister;
import no.nav.foreldrepenger.domene.medlem.medl2.Medlemskapsperiode;
import no.nav.foreldrepenger.domene.typer.AktørId;
import no.nav.foreldrepenger.skjæringstidspunkt.SkjæringstidspunktTjeneste;

@ApplicationScoped
public class MedlemTjeneste {

    private MedlemskapRepository medlemskapRepository;
    private HentMedlemskapFraRegister hentMedlemskapFraRegister;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private BehandlingRepository behandlingRepository;
    private VilkårMedlemskapRepository vilkårMedlemskapRepository;

    MedlemTjeneste() {
        // CDI
    }

    @Inject
    public MedlemTjeneste(BehandlingRepositoryProvider repositoryProvider,
                          HentMedlemskapFraRegister hentMedlemskapFraRegister,
                          MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository,
                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                          VilkårMedlemskapRepository vilkårMedlemskapRepository) {
        this.hentMedlemskapFraRegister = hentMedlemskapFraRegister;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.medlemskapVilkårPeriodeRepository = medlemskapVilkårPeriodeRepository;
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.vilkårMedlemskapRepository = vilkårMedlemskapRepository;
    }

    /**
     * Finn medlemskapsperioder i MEDL2 register for en person.
     *
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
        return funnetId.map(id -> EndringsresultatSnapshot.medSnapshot(MedlemskapAggregat.class, id))
            .orElse(EndringsresultatSnapshot.utenSnapshot(MedlemskapAggregat.class));
    }

    public Optional<LocalDate> hentOpphørsdatoHvisEksisterer(Long behandlingId) {
        var behandlingsresultatOpt = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingsresultatOpt.isEmpty() || behandlingsresultatOpt.get().getVilkårResultat() == null) {
            return Optional.empty();
        }
        var behandlingsresultat = behandlingsresultatOpt.get();
        var medlemskapsvilkåret = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();
        if (medlemskapsvilkåret.isEmpty()) {
            return Optional.empty();
        }
        if (medlemskapsvilkåret.get().erIkkeOppfylt()) {
            skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getSkjæringstidspunktHvisUtledet();
        }
        return vilkårMedlemskapRepository.hentHvisEksisterer(behandlingId)
            .flatMap(VilkårMedlemskap::getOpphør)
            .map(MedlemskapOpphør::fom);
    }

    private Optional<Vilkår> finnVilkår(Behandlingsresultat behandlingsresultat, VilkårType vilkårType) {
        return behandlingsresultat.getVilkårResultat().getVilkårene().stream()
            .filter(vt -> vt.getVilkårType().equals(vilkårType))
            .findFirst();
    }

    public Optional<Avslagsårsak> hentAvslagsårsak(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hent(behandlingId);
        var medlemskapsvilkåret = behandlingsresultat.getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkår -> vilkår.getVilkårType().gjelderMedlemskap())
            .findFirst();
        if (medlemskapsvilkåret.isEmpty()) {
            return Optional.empty();
        }
        if (medlemskapsvilkåret.get().getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
            return Optional.of(medlemskapsvilkåret.get().getAvslagsårsak());
        }
        return vilkårMedlemskapRepository.hentHvisEksisterer(behandlingId)
            .flatMap(VilkårMedlemskap::getOpphør)
            .map(MedlemskapOpphør::årsak);
    }

    public Optional<LocalDate> hentMedlemFomDato(Long behandlingId) {
        var behandlingsresultat = behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
        if (behandlingsresultat.isEmpty()) {
            return Optional.empty();
        }
        var ikkeOppfyltVilkår = behandlingsresultat.get().getVilkårResultat()
            .getVilkårene()
            .stream()
            .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET_FORUTGÅENDE))
            .filter(vilkår -> VilkårUtfallType.erFastsatt(vilkår.getGjeldendeVilkårUtfall()))
            .filter(vilkår -> VilkårUtfallType.IKKE_OPPFYLT.equals(vilkår.getGjeldendeVilkårUtfall()))
            .findFirst();
        if (ikkeOppfyltVilkår.isEmpty()) {
            return Optional.empty();
        }
        return vilkårMedlemskapRepository.hentHvisEksisterer(behandlingId)
            .flatMap(VilkårMedlemskap::getMedlemFom);
    }

    public record VilkårUtfallMedÅrsak(VilkårUtfallType vilkårUtfallType, Avslagsårsak avslagsårsak) {
    }

    public VilkårUtfallMedÅrsak utledVilkårUtfall(Behandling revurdering) {
        var behandlingsresultat = behandlingsresultatRepository.hent(revurdering.getId());
        var medlemOpt = finnVilkår(behandlingsresultat, VilkårType.MEDLEMSKAPSVILKÅRET);

        if (medlemOpt.isPresent()) {
            var medlem = medlemOpt.get();
            if (medlem.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT)) {
                return new VilkårUtfallMedÅrsak(medlem.getGjeldendeVilkårUtfall(),
                    AvslagsårsakMapper.fraVilkårUtfallMerknad(medlem.getVilkårUtfallMerknad()));
            }
            var løpendeOpt = finnVilkår(behandlingsresultat, VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE);
            if (løpendeOpt.isPresent()) {
                var løpende = løpendeOpt.get();
                if (løpende.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT) && !løpende.erOverstyrt()) {
                    return new VilkårUtfallMedÅrsak(VilkårUtfallType.IKKE_OPPFYLT,
                        AvslagsårsakMapper.fraVilkårUtfallMerknad(løpende.getVilkårUtfallMerknad()));
                }
                if (løpende.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.IKKE_OPPFYLT) && løpende.erOverstyrt()) {
                    return new VilkårUtfallMedÅrsak(VilkårUtfallType.IKKE_OPPFYLT, løpende.getAvslagsårsak());
                }
            }
            return new VilkårUtfallMedÅrsak(VilkårUtfallType.OPPFYLT, Avslagsårsak.UDEFINERT);
        }
        throw new IllegalStateException("Kan ikke utlede vilkår utfall type når medlemskapsvilkåret ikke finnes");
    }

    public Optional<MedlemskapBehandlingsgrunnlagEntitet> hentGrunnlagPåId(Long grunnlagId) {
        return medlemskapRepository.hentGrunnlagPåId(grunnlagId);
    }
}
