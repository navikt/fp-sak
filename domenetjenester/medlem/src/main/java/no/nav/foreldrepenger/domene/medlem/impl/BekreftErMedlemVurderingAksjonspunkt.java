package no.nav.foreldrepenger.domene.medlem.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapManuellVurderingType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.medlem.api.BekreftErMedlemVurderingAksjonspunktDto;

public class BekreftErMedlemVurderingAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;

    public BekreftErMedlemVurderingAksjonspunkt(BehandlingRepositoryProvider repositoryProvider) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    public void oppdater(Long behandlingId, BekreftErMedlemVurderingAksjonspunktDto adapter) {

        var medlemskapManuellVurderingType = MedlemskapManuellVurderingType.fraKode(adapter.getManuellVurderingTypeKode());

        var vurdertMedlemskap = medlemskapRepository.hentVurdertMedlemskap(behandlingId);

        var nytt = new VurdertMedlemskapBuilder(vurdertMedlemskap).medMedlemsperiodeManuellVurdering(medlemskapManuellVurderingType)
            .medBegrunnelse(adapter.getBegrunnelse())
            .build();

        medlemskapRepository.lagreMedlemskapVurdering(behandlingId, nytt);
    }

}
