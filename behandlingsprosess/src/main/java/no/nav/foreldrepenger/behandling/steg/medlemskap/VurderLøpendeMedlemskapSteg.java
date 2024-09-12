package no.nav.foreldrepenger.behandling.steg.medlemskap;

import java.util.Collections;
import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingStegType;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingType;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.behandlingslager.fagsak.FagsakYtelseType;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.VurderLøpendeMedlemskap;
import no.nav.foreldrepenger.konfig.Environment;

@BehandlingStegRef(BehandlingStegType.VULOMED)
@BehandlingTypeRef(BehandlingType.REVURDERING)
@FagsakYtelseTypeRef(FagsakYtelseType.FORELDREPENGER)
@ApplicationScoped
public class VurderLøpendeMedlemskapSteg implements BehandlingSteg {

    private static final Environment ENV = Environment.current(); // TODO medlemskap2 sanere etter omlegging

    private BehandlingsresultatRepository behandlingsresultatRepository;
    private VurderLøpendeMedlemskap vurderLøpendeMedlemskap;
    private MedlemskapVilkårPeriodeRepository medlemskapVilkårPeriodeRepository;
    private BehandlingRepository behandlingRepository;

    @Inject
    public VurderLøpendeMedlemskapSteg(VurderLøpendeMedlemskap vurderLøpendeMedlemskap,
            BehandlingRepositoryProvider provider) {
        this.vurderLøpendeMedlemskap = vurderLøpendeMedlemskap;
        this.medlemskapVilkårPeriodeRepository = provider.getMedlemskapVilkårPeriodeRepository();
        this.behandlingRepository = provider.getBehandlingRepository();
        this.behandlingsresultatRepository = provider.getBehandlingsresultatRepository();
    }

    VurderLøpendeMedlemskapSteg() {
        // CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        var behandlingId = kontekst.getBehandlingId();
        if (skalVurdereLøpendeMedlemskap(behandlingId)) {
            var vurderingsTilDataMap = vurderLøpendeMedlemskap.vurderLøpendeMedlemskap(behandlingId);
            if (!vurderingsTilDataMap.isEmpty()) {
                var behandling = behandlingRepository.hentBehandling(behandlingId);

                var builder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
                var perioderBuilder = builder.getPeriodeBuilder();

                vurderingsTilDataMap.forEach((vurderingsdato, vilkårData) -> {
                    var periodeBuilder = perioderBuilder.getBuilderForVurderingsdato(vurderingsdato);
                    periodeBuilder.medVurderingsdato(vurderingsdato);
                    periodeBuilder.medVilkårUtfall(vilkårData.utfallType());
                    Optional.ofNullable(vilkårData.vilkårUtfallMerknad()).ifPresent(periodeBuilder::medVilkårUtfallMerknad);
                    perioderBuilder.leggTil(periodeBuilder);
                });
                builder.medMedlemskapsvilkårPeriode(perioderBuilder);
                medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, builder);

                var resultat = medlemskapVilkårPeriodeRepository.utledeVilkårStatus(behandling);
                var vilkårResultatBuilder = VilkårResultat
                        .builderFraEksisterende(getBehandlingsresultat(behandlingId).map(Behandlingsresultat::getVilkårResultat).orElse(null));
                var vilkårBuilder = vilkårResultatBuilder.getVilkårBuilderFor(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE);
                if (VilkårUtfallType.IKKE_OPPFYLT.equals(resultat.vilkårUtfallType())) {
                    vilkårBuilder.medVilkårUtfall(resultat.vilkårUtfallType(), resultat.vilkårUtfallMerknad());
                } else {
                    vilkårBuilder.medVilkårUtfall(resultat.vilkårUtfallType(), VilkårUtfallMerknad.UDEFINERT);
                }
                vilkårResultatBuilder.leggTilVilkår(vilkårBuilder);
                var lås = kontekst.getSkriveLås();
                behandlingRepository.lagre(vilkårResultatBuilder.buildFor(behandling), lås);
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalVurdereLøpendeMedlemskap(Long behandlingId) {
        if (!ENV.isProd()) {
            return false;
        }
        return getBehandlingsresultat(behandlingId)
            .map(b -> b.getVilkårResultat().getVilkårene()).orElse(Collections.emptyList())
            .stream()
            .anyMatch(v -> v.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET) && v.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT));
    }

    private Optional<Behandlingsresultat> getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId);
    }
}
