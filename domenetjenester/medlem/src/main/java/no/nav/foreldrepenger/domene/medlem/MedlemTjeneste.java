package no.nav.foreldrepenger.domene.medlem;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.EndringsresultatSnapshot;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapBehandlingsgrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapOpphør;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VilkårMedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
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
    private SkjæringstidspunktTjeneste skjæringstidspunktTjeneste;
    private BehandlingsresultatRepository behandlingsresultatRepository;
    private VilkårMedlemskapRepository vilkårMedlemskapRepository;

    MedlemTjeneste() {
        // CDI
    }

    @Inject
    public MedlemTjeneste(BehandlingRepositoryProvider repositoryProvider,
                          HentMedlemskapFraRegister hentMedlemskapFraRegister,
                          SkjæringstidspunktTjeneste skjæringstidspunktTjeneste,
                          VilkårMedlemskapRepository vilkårMedlemskapRepository) {
        this.hentMedlemskapFraRegister = hentMedlemskapFraRegister;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.skjæringstidspunktTjeneste = skjæringstidspunktTjeneste;
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
        var medlemskapsvilkåret = hentVilkårStream(behandlingId)
            .filter(vilkår -> vilkår.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET))
            .findFirst();
        if (medlemskapsvilkåret.isEmpty()) {
            return Optional.empty();
        }
        if (medlemskapsvilkåret.filter(Vilkår::erIkkeOppfylt).isPresent()) {
            skjæringstidspunktTjeneste.getSkjæringstidspunkter(behandlingId).getSkjæringstidspunktHvisUtledet();
        }
        return vilkårMedlemskapRepository.hentHvisEksisterer(behandlingId).flatMap(VilkårMedlemskap::getOpphør).map(MedlemskapOpphør::fom);
    }

    public Optional<Avslagsårsak> hentAvslagsårsak(Long behandlingId) {
        var medlemskapsvilkåret =hentVilkårStream(behandlingId)
            .filter(vilkår -> vilkår.getVilkårType().gjelderMedlemskap())
            .findFirst();
        if (medlemskapsvilkåret.isEmpty()) {
            return Optional.empty();
        }
        if (medlemskapsvilkåret.filter(Vilkår::erIkkeOppfylt).isPresent()) {
            return Optional.of(medlemskapsvilkåret.get().getAvslagsårsak());
        }
        return vilkårMedlemskapRepository.hentHvisEksisterer(behandlingId)
            .flatMap(VilkårMedlemskap::getOpphør)
            .map(MedlemskapOpphør::årsak);
    }

    public Optional<LocalDate> hentMedlemFomDato(Long behandlingId) {
        var ikkeOppfyltVilkår = hentVilkårStream(behandlingId)
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

    public Optional<MedlemskapBehandlingsgrunnlagEntitet> hentGrunnlagPåId(Long grunnlagId) {
        return medlemskapRepository.hentGrunnlagPåId(grunnlagId);
    }

    private Stream<Vilkår> hentVilkårStream(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId)
            .map(Behandlingsresultat::getVilkårResultat)
            .map(VilkårResultat::getVilkårene)
            .orElseGet(List::of)
            .stream();
    }
}
