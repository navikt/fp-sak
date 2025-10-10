package no.nav.foreldrepenger.behandling.steg.inngangsvilkår;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKARET_FORELDREPENGER;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.ADOPSJONSVILKÅRET_ENGANGSSTØNAD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FORELDREANSVARSVILKÅRET_2_LEDD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FORELDREANSVARSVILKÅRET_4_LEDD;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_FAR_MEDMOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.FØDSELSVILKÅRET_MOR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OMSORGSOVERTAKELSEVILKÅR;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.OMSORGSVILKÅRET;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingResultatType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.familiehendelse.FamilieHendelseRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;

class RyddVilkårTyper {

    private static final Map<VilkårType, Consumer<RyddVilkårTyper>> OPPRYDDER_FOR_AVKLARTE_DATA = new EnumMap<>(VilkårType.class);

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
        OPPRYDDER_FOR_AVKLARTE_DATA.put(OMSORGSOVERTAKELSEVILKÅR,
            r -> r.familieGrunnlagRepository.slettAvklarteData(r.behandling.getId(), r.kontekst.getSkriveLås()));
    }

    private final BehandlingRepository behandlingRepository;
    private final FamilieHendelseRepository familieGrunnlagRepository;
    private final BehandlingsresultatRepository behandlingsresultatRepository;
    private final Behandling behandling;

    private final BehandlingskontrollKontekst kontekst;

    RyddVilkårTyper(BehandlingRepositoryProvider repositoryProvider, Behandling behandling, BehandlingskontrollKontekst kontekst) {
        this.behandlingRepository = repositoryProvider.getBehandlingRepository();
        this.familieGrunnlagRepository = repositoryProvider.getFamilieHendelseRepository();
        this.behandlingsresultatRepository = repositoryProvider.getBehandlingsresultatRepository();
        this.behandling = behandling;
        this.kontekst = kontekst;
    }

    void ryddVedOverhoppFramover(List<VilkårType> vilkårTyper) {
        slettAvklarteFakta(vilkårTyper);
        nullstillVilkår(vilkårTyper, false);
    }

    void ryddVedTilbakeføring(List<VilkårType> vilkårTyper, boolean nullstillManueltAvklartVilkår) {
        nullstillVilkår(vilkårTyper, nullstillManueltAvklartVilkår);
        nullstillVedtaksresultat();
    }

    private void nullstillVedtaksresultat() {
        var behandlingsresultat = getBehandlingsresultat(behandling);
        if (behandlingsresultat.isEmpty() ||
            behandlingsresultat.filter(r -> BehandlingResultatType.IKKE_FASTSATT.equals(r.getBehandlingResultatType())).isPresent()) {
            return;
        }

        Behandlingsresultat.builderEndreEksisterende(behandlingsresultat.get())
                .medBehandlingResultatType(BehandlingResultatType.IKKE_FASTSATT);
        behandlingRepository.lagre(behandling, kontekst.getSkriveLås());
    }

    private Optional<Behandlingsresultat> getBehandlingsresultat(Behandling behandling) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandling.getId());
    }

    private void slettAvklarteFakta(List<VilkårType> vilkårTyper) {
        vilkårTyper.forEach(vilkårType -> {
            var ryddVilkårConsumer = OPPRYDDER_FOR_AVKLARTE_DATA.get(vilkårType);
            if (ryddVilkårConsumer != null) {
                ryddVilkårConsumer.accept(this);
            }
        });
    }

    private void nullstillVilkår(List<VilkårType> vilkårTyper, boolean nullstillManueltAvklartVilkår) {
        getBehandlingsresultat(behandling)
            .map(Behandlingsresultat::getVilkårResultat)
            .ifPresent(vilkårResultat -> {
                var vilkårSomSkalNullstilles = vilkårResultat.getVilkårene().stream()
                    .filter(v -> vilkårTyper.contains(v.getVilkårType()))
                    .filter(v -> !v.erOverstyrt())
                    .toList();
                if (!vilkårSomSkalNullstilles.isEmpty()) {
                    var builder = VilkårResultat.builderFraEksisterende(vilkårResultat);
                    vilkårSomSkalNullstilles.forEach(v -> builder.nullstillVilkår(v, nullstillManueltAvklartVilkår));
                    builder.buildFor(behandling);
                }
            });
    }

}
