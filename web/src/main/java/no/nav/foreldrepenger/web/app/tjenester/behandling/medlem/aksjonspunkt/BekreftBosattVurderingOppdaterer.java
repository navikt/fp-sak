package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepositoryProvider;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.BekreftBosattVurderingAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftBosattVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftBosattVurderingOppdaterer implements AksjonspunktOppdaterer<BekreftBosattVurderingDto> {

    private MedlemskapAksjonspunktTjeneste medlemTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;
    private MedlemskapRepository medlemskapRepository;

    BekreftBosattVurderingOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftBosattVurderingOppdaterer(BehandlingRepositoryProvider repositoryProvider,
                                            HistorikkTjenesteAdapter historikkAdapter,
                                            MedlemskapAksjonspunktTjeneste medlemTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.medlemTjeneste = medlemTjeneste;
        this.medlemskapRepository = repositoryProvider.getMedlemskapRepository();
    }

    @Override
    public OppdateringResultat oppdater(BekreftBosattVurderingDto dto, AksjonspunktOppdaterParameter param) {
        var behandlingId = param.getBehandlingId();

        var bekreftedeDto = dto.getBekreftedePerioder().stream().findFirst();
        if (bekreftedeDto.isEmpty()) {
            return OppdateringResultat.utenOveropp();
        }
        var bekreftet = bekreftedeDto.get();
        var totrinn = håndterEndringHistorikk(bekreftet, behandlingId);
        medlemTjeneste.aksjonspunktBekreftBosattVurdering(behandlingId, new BekreftBosattVurderingAksjonspunktDto(bekreftet.getBosattVurdering(), bekreftet.getBegrunnelse()));

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(totrinn).build();
    }

    private boolean håndterEndringHistorikk(BekreftedePerioderDto bekreftet, Long behandlingId) {
        var bosattVurdering = bekreftet.getBosattVurdering();
        var begrunnelse = bekreftet.getBegrunnelse();
        var medlemskap = medlemskapRepository.hentMedlemskap(behandlingId);
        var originalBosattBool = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap)
            .map(VurdertMedlemskap::getBosattVurdering).orElse(null);
        var begrunnelseOrg = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap)
            .map(VurdertMedlemskap::getBegrunnelse).orElse(null);

        var originalBosatt = mapTilBosattVerdiKode(originalBosattBool);
        var bekreftetBosatt = mapTilBosattVerdiKode(bosattVurdering);

        var erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.ER_SOKER_BOSATT_I_NORGE, originalBosatt, bekreftetBosatt);

        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, !Objects.equals(begrunnelse, begrunnelseOrg))
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP);

        return erEndret;
    }

    private HistorikkEndretFeltVerdiType mapTilBosattVerdiKode(Boolean bosattBool) {
        if (bosattBool == null) {
            return null;
        }
        return bosattBool ? HistorikkEndretFeltVerdiType.BOSATT_I_NORGE : HistorikkEndretFeltVerdiType.IKKE_BOSATT_I_NORGE;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, HistorikkEndretFeltVerdiType original, HistorikkEndretFeltVerdiType bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            return true;
        }
        return false;
    }
}
