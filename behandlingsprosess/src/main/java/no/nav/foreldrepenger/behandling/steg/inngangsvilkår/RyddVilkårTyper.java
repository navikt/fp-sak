package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import static java.util.stream.Collectors.toList;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultatType.IKKE_FASTSATT;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKARET_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_MOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.MEDLEMSKAPSVILKÅRET;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OMSORGSVILKÅRET;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Vilkår;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;

class RyddVilkårTyper {

    private BehandlingRepository behandlingRepository;
    private final Behandling behandling;
    private final BehandlingskontrollKontekst kontekst;

    static Map<VilkårType, Consumer<RyddVilkårTyper>> OPPRYDDER_FOR_AVKLARTE_DATA = new HashMap<>();
    private FamilieHendelseRepository familieGrunnlagRepository;
    private MedlemskapRepository medlemskapRepository;

    static {
        OPPRYDDER_FOR_AVKLARTE_DATA.put(FØDSELSVILKÅRET_MOR,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(FØDSELSVILKÅRET_FAR_MEDMOR,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(ADOPSJONSVILKÅRET_ENGANGSSTØNAD,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(ADOPSJONSVILKARET_FORELDREPENGER,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(OMSORGSVILKÅRET,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(FORELDREANSVARSVILKÅRET_2_LEDD,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(FORELDREANSVARSVILKÅRET_4_LEDD,
                r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
        OPPRYDDER_FOR_AVKLARTE_DATA.put(MEDLEMSKAPSVILKÅRET,
                r -> r.medlemskapRepository.slettAvklarteMedlemskapsdata(r.behandling.getId(), r.kontekst.getSkriveLås()));
    }

    RyddVilkårTyper(BehandlingRepositoryProvider repositoryProvider, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
        this.behandling = behandling;
        this.kontekst = kontekst;
    }

    void ryddVedOverhoppFramover(List<VilkårType> vilkårTyper) {
        slettAvklarteFakta(vilkårTyper);
        nullstillVilkår(vilkårTyper, true);
    }

    void ryddVedTilbakeføring(List<VilkårType> vilkårTyper) {
        nullstillInngangsvilkår();
        nullstillVilkår(vilkårTyper, false);
        nullstillVedtaksresultat();
    }

    private void nullstillVedtaksresultat() {
        Behandlingsresultat behandlingsresultat = getBehandlingsresultat(behandling);
        if ((behandlingsresultat == null) ||
                Objects.equals(behandlingsresultat.getBehandlingResultatType(), BehandlingResultatType.IKKE_FASTSATT)) {
            return;
        }

        Behandlingsresultat.builderEndreEksisterende(getBehandlingsresultat(behandling))
                .medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private Behandlingsresultat getBehandlingsresultat(Behandling behandling) {
        return behandling.getBehandlingsresultat();
    }

    private void slettAvklarteFakta(List<VilkårType> vilkårTyper) {
        vilkårTyper.forEach(vilkårType -> {
            Consumer<RyddVilkårTyper> ryddVilkårConsumer = OPPRYDDER_FOR_AVKLARTE_DATA.get(vilkårType);
            if (ryddVilkårConsumer != null) {
                ryddVilkårConsumer.accept(this);
            }
        });
    }

    private void nullstillInngangsvilkår() {
        Optional<VilkårResultat> vilkårResultatOpt = Optional.ofNullable(getBehandlingsresultat(behandling))
                .map(Behandlingsresultat::getVilkårResultat)
                .filter(inng -> !inng.erOverstyrt());
        if (!vilkårResultatOpt.isPresent()) {
            return;
        }

        VilkårResultat vilkårResultat = vilkårResultatOpt.get();
        VilkårResultat.Builder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        if (!vilkårResultat.getVilkårResultatType().equals(IKKE_FASTSATT)) {
            builder.medVilkårResultatType(IKKE_FASTSATT);
        }
        builder.buildFor(behandling);
    }

    private void nullstillVilkår(List<VilkårType> vilkårTyper, boolean nullstillOverstyring) {
        Optional<VilkårResultat> vilkårResultatOpt = Optional.ofNullable(getBehandlingsresultat(behandling))
                .map(Behandlingsresultat::getVilkårResultat);
        if (!vilkårResultatOpt.isPresent()) {
            return;
        }
        VilkårResultat vilkårResultat = vilkårResultatOpt.get();

        List<Vilkår> vilkårSomSkalNullstilles = vilkårResultat.getVilkårene().stream()
                .filter(v -> vilkårTyper.contains(v.getVilkårType()))
                .collect(toList());
        if (vilkårSomSkalNullstilles.isEmpty()) {
            return;
        }

        VilkårResultat.Builder builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
        vilkårSomSkalNullstilles.stream()
                .filter(it -> !it.erOverstyrt() || nullstillOverstyring)
                .forEach(vilkår -> builder.nullstillVilkår(vilkår.getVilkårType(),
                        !nullstillOverstyring ? vilkår.getVilkårUtfallOverstyrt() : VilkårUtfallType.UDEFINERT));
        builder.buildFor(behandling);
    }

}
