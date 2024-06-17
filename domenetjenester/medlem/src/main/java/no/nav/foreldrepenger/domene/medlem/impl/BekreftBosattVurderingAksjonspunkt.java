package no.nav.foreldrepenger.domene.medlem.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.medlem.api.BekreftBosattVurderingAksjonspunktDto;

public class BekreftBosattVurderingAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;

    public BekreftBosattVurderingAksjonspunkt(BehandlingRepositoryProvider repositoryProvider) {
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    public void oppdater(Long behandlingId, BekreftBosattVurderingAksjonspunktDto adapter) {
        var vurdertMedlemskap = medlemskapRepository.hentVurdertMedlemskap(behandlingId);

        var nytt = new VurdertMedlemskapBuilder(vurdertMedlemskap).medBosattVurdering(adapter.getBosattVurdering())
            .medBegrunnelse(adapter.getBegrunnelse())
            .build();

        medlemskapRepository.lagreMedlemskapVurdering(behandlingId, nytt);
    }
}
