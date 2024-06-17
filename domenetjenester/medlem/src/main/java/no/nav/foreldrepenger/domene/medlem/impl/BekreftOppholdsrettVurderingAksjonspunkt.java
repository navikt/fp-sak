package no.nav.foreldrepenger.domene.medlem.impl;

import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskapBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.domene.medlem.api.BekreftOppholdVurderingAksjonspunktDto;

public class BekreftOppholdsrettVurderingAksjonspunkt {

    private MedlemskapRepository medlemskapRepository;

    public BekreftOppholdsrettVurderingAksjonspunkt(BehandlingRepositoryProvider repositoryProvider) {
        medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    public void oppdater(Long behandlingId, BekreftOppholdVurderingAksjonspunktDto adapter) {
        var medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        var vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);

        var nytt = new VurdertMedlemskapBuilder(vurdertMedlemskap).medOppholdsrettVurdering(adapter.getOppholdsrettVurdering())
            .medLovligOppholdVurdering(adapter.getLovligOppholdVurdering())
            .medErEosBorger(adapter.getErEosBorger())
            .medBegrunnelse(adapter.getBegrunnelse())
            .build();

        medlemskapRepository.lagreMedlemskapVurdering(behandlingId, nytt);
    }
}
