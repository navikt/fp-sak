package no.nav.foreldrepenger.behandling.steg.medlemskap;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandlingskontroll.BehandleStegResultat;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingSteg;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingStegRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingTypeRef;
import no.nav.foreldrepenger.behandlingskontroll.BehandlingskontrollKontekst;
import no.nav.foreldrepenger.behandlingskontroll.FagsakYtelseTypeRef;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandling;
import no.nav.foreldrepenger.behandlingslager.behandling.Behandlingsresultat;
import no.nav.foreldrepenger.behandlingslager.behandling.BehandlingsresultatRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeGrunnlagEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapVilkårPeriodeRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPeriodeEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapsvilkårPerioderEntitet;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingLås;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallMerknad;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.foreldrepenger.inngangsvilkaar.VilkårData;
import no.nav.foreldrepenger.inngangsvilkaar.medlemskap.VurderLøpendeMedlemskap;
import no.nav.vedtak.util.Tuple;

@BehandlingStegRef(kode = "VULOMED")
@BehandlingTypeRef("BT-004")
@FagsakYtelseTypeRef("FP")
@ApplicationScoped
public class VurderLøpendeMedlemskapSteg implements BehandlingSteg {

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
        //CDI
    }

    @Override
    public BehandleStegResultat utførSteg(BehandlingskontrollKontekst kontekst) {
        Long behandlingId = kontekst.getBehandlingId();
        if (skalVurdereLøpendeMedlemskap(behandlingId)) {
            Map<LocalDate, VilkårData> vurderingsTilDataMap = vurderLøpendeMedlemskap.vurderLøpendeMedlemskap(behandlingId);
            if (!vurderingsTilDataMap.isEmpty()) {
                Behandling behandling = behandlingRepository.hentBehandling(behandlingId);

                MedlemskapVilkårPeriodeGrunnlagEntitet.Builder builder = medlemskapVilkårPeriodeRepository.hentBuilderFor(behandling);
                MedlemskapsvilkårPeriodeEntitet.Builder perioderBuilder = builder.getPeriodeBuilder();

                vurderingsTilDataMap.forEach((vurderingsdato, vilkårData) -> {
                    MedlemskapsvilkårPerioderEntitet.Builder periodeBuilder = perioderBuilder.getBuilderForVurderingsdato(vurderingsdato);
                    periodeBuilder.medVurderingsdato(vurderingsdato);
                    periodeBuilder.medVilkårUtfall(vilkårData.getUtfallType());
                    Optional.ofNullable(vilkårData.getVilkårUtfallMerknad()).ifPresent(periodeBuilder::medVilkårUtfallMerknad);
                    perioderBuilder.leggTil(periodeBuilder);
                });
                builder.medMedlemskapsvilkårPeriode(perioderBuilder);
                medlemskapVilkårPeriodeRepository.lagreMedlemskapsvilkår(behandling, builder);

                Tuple<VilkårUtfallType, VilkårUtfallMerknad> resultat = medlemskapVilkårPeriodeRepository.utledeVilkårStatus(behandling);
                VilkårResultat.Builder vilkårBuilder = VilkårResultat.builderFraEksisterende(getBehandlingsresultat(behandlingId).getVilkårResultat());
                Avslagsårsak avslagsårsak = null;
                if (VilkårUtfallType.IKKE_OPPFYLT.equals(resultat.getElement1())) {
                    avslagsårsak = Avslagsårsak.fraKode(resultat.getElement2().getKode());
                }
                vilkårBuilder.leggTilVilkårResultat(VilkårType.MEDLEMSKAPSVILKÅRET_LØPENDE, resultat.getElement1(), resultat.getElement2(), null, avslagsårsak, false, false, null, null);

                BehandlingLås lås = kontekst.getSkriveLås();
                behandlingRepository.lagre(vilkårBuilder.buildFor(behandling), lås);
            }
        }
        return BehandleStegResultat.utførtUtenAksjonspunkter();
    }

    private boolean skalVurdereLøpendeMedlemskap(Long behandlingId) {
        Optional<Behandlingsresultat> behandlingsresultat = Optional.ofNullable(getBehandlingsresultat(behandlingId));
        return behandlingsresultat.map(b -> b.getVilkårResultat().getVilkårene()).orElse(Collections.emptyList())
            .stream()
            .anyMatch(v -> v.getVilkårType().equals(VilkårType.MEDLEMSKAPSVILKÅRET) && v.getGjeldendeVilkårUtfall().equals(VilkårUtfallType.OPPFYLT));
    }

    private Behandlingsresultat getBehandlingsresultat(Long behandlingId) {
        return behandlingsresultatRepository.hentHvisEksisterer(behandlingId).orElse(null);
    }
}
