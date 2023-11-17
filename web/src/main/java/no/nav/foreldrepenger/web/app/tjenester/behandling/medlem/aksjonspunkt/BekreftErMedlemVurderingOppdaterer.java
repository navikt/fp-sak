package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.Objects;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.BekreftErMedlemVurderingAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftErMedlemVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftErMedlemVurderingOppdaterer implements AksjonspunktOppdaterer<BekreftErMedlemVurderingDto> {
    private MedlemskapAksjonspunktTjeneste medlemTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private MedlemskapRepository medlemskapRepository;

    BekreftErMedlemVurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftErMedlemVurderingOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                              HistorikkTjenesteAdapter historikkAdapter,
                                              MedlemskapAksjonspunktTjeneste medlemTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.medlemTjeneste = medlemTjeneste;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    @Override
    public OppdateringResultat oppdater(BekreftErMedlemVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();

        var bekreftedeDto = dto.getBekreftedePerioder().stream().findFirst();
        if (bekreftedeDto.isEmpty()) {
            return OppdateringResultat.utenOverhopp();
        }
        var bekreftet = bekreftedeDto.get();
        var medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        var vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);

        var originalVurdering = vurdertMedlemskap.map(VurdertMedlemskap::getMedlemsperiodeManuellVurdering).orElse(null);
        var bekreftetVurdering = bekreftet.getMedlemskapManuellVurderingType();
        var begrunnelseOrg = vurdertMedlemskap.map(VurdertMedlemskap::getBegrunnelse).orElse(null);

        var begrunnelse = bekreftet.getBegrunnelse();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, !Objects.equals(begrunnelse, begrunnelseOrg))
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP);

        var erEndret = !Objects.equals(originalVurdering, bekreftetVurdering);
        if (erEndret) {
            historikkAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.GYLDIG_MEDLEM_FOLKETRYGDEN, originalVurdering, bekreftetVurdering);
        }

        var adapter = new BekreftErMedlemVurderingAksjonspunktDto(bekreftet.getMedlemskapManuellVurderingType().getKode(),
            bekreftet.getBegrunnelse());
        medlemTjeneste.aksjonspunktBekreftMeldlemVurdering(behandlingId, adapter);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).build();
    }
}
