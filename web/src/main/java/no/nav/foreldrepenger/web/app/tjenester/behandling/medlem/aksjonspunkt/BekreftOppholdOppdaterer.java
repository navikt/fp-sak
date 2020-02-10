package no.nav.foreldrepenger.web.app.tjenester.behandling.medlem.aksjonspunkt;

import java.util.Objects;
import java.util.Optional;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.MedlemskapAggregat;
import no.nav.foreldrepenger.behandlingslager.behandling.medlemskap.VurdertMedlemskap;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.domene.medlem.MedlemTjeneste;
import no.nav.foreldrepenger.domene.medlem.MedlemskapAksjonspunktTjeneste;
import no.nav.foreldrepenger.domene.medlem.api.BekreftOppholdVurderingAksjonspunktDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

public abstract class BekreftOppholdOppdaterer implements AksjonspunktOppdaterer<BekreftedePerioderMalDto> {

    private MedlemTjeneste medlemTjeneste;
    private MedlemskapAksjonspunktTjeneste medlemskapAksjonspunktTjeneste;
    private HistorikkTjenesteAdapter historikkAdapter;

    protected BekreftOppholdOppdaterer() {
        // for CDI proxy
    }

    protected BekreftOppholdOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                       MedlemTjeneste medlemTjeneste,
                                       MedlemskapAksjonspunktTjeneste medlemskapAksjonspunktTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.medlemTjeneste = medlemTjeneste;
        this.medlemskapAksjonspunktTjeneste = medlemskapAksjonspunktTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(BekreftedePerioderMalDto dto, AksjonspunktOppdaterParameter param) {
        Long behandlingId = param.getBehandlingId();

        Optional<BekreftedePerioderDto> bekreftedeDto = dto.getBekreftedePerioder().stream().findFirst();
        if (bekreftedeDto.isEmpty()) {
            return OppdateringResultat.utenOveropp();
        }
        BekreftedePerioderDto bekreftet = bekreftedeDto.get();
        Optional<MedlemskapAggregat> medlemskap = medlemTjeneste.hentMedlemskap(behandlingId);
        Optional<VurdertMedlemskap> vurdertMedlemskap = medlemskap.flatMap(MedlemskapAggregat::getVurdertMedlemskap);

        Boolean orginalOppholdsrettBool = vurdertMedlemskap.map(VurdertMedlemskap::getOppholdsrettVurdering).orElse(null);
        HistorikkEndretFeltVerdiType orginalOppholdsrett = mapTilOppholdsrettVerdiKode(orginalOppholdsrettBool);
        HistorikkEndretFeltVerdiType bekreftetOppholdsrett = mapTilOppholdsrettVerdiKode(bekreftet.getOppholdsrettVurdering());

        Boolean orginalLovligOppholdBool = vurdertMedlemskap.map(VurdertMedlemskap::getLovligOppholdVurdering).orElse(null);
        HistorikkEndretFeltVerdiType originalLovligOpphold = mapTilLovligOppholdVerdiKode(orginalLovligOppholdBool);
        HistorikkEndretFeltVerdiType bekreftetLovligOpphold = mapTilLovligOppholdVerdiKode(bekreftet.getLovligOppholdVurdering());

        String begrunnelseOrg = vurdertMedlemskap.map(VurdertMedlemskap::getBegrunnelse).orElse(null);

        boolean erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.OPPHOLDSRETT_EOS, orginalOppholdsrett, bekreftetOppholdsrett);
        erEndret = oppdaterVedEndretVerdi(HistorikkEndretFeltType.OPPHOLDSRETT_IKKE_EOS, originalLovligOpphold, bekreftetLovligOpphold)
            || erEndret;

        String begrunnelse = bekreftet.getBegrunnelse();
        historikkAdapter.tekstBuilder()
            .medBegrunnelse(begrunnelse, Objects.equals(begrunnelse, begrunnelseOrg))
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_MEDLEMSKAP);

        final BekreftOppholdVurderingAksjonspunktDto adapter = new BekreftOppholdVurderingAksjonspunktDto(bekreftet.getOppholdsrettVurdering(),
            bekreftet.getLovligOppholdVurdering(), bekreftet.getErEosBorger(), bekreftet.getBegrunnelse());

        medlemskapAksjonspunktTjeneste.aksjonspunktBekreftOppholdVurdering(behandlingId, adapter);

        return OppdateringResultat.utenTransisjon().medTotrinnHvis(erEndret).build();
    }

    private HistorikkEndretFeltVerdiType mapTilLovligOppholdVerdiKode(Boolean harLovligOpphold) {
        if (harLovligOpphold == null) {
            return null;
        }
        return harLovligOpphold ? HistorikkEndretFeltVerdiType.LOVLIG_OPPHOLD : HistorikkEndretFeltVerdiType.IKKE_LOVLIG_OPPHOLD;
    }

    private HistorikkEndretFeltVerdiType mapTilOppholdsrettVerdiKode(Boolean harOppholdsrett) {
        if (harOppholdsrett == null) {
            return null;
        }
        return harOppholdsrett ? HistorikkEndretFeltVerdiType.OPPHOLDSRETT : HistorikkEndretFeltVerdiType.IKKE_OPPHOLDSRETT;
    }

    private boolean oppdaterVedEndretVerdi(HistorikkEndretFeltType historikkEndretFeltType, HistorikkEndretFeltVerdiType original, HistorikkEndretFeltVerdiType bekreftet) {
        if (!Objects.equals(bekreftet, original)) {
            if (bekreftet != null) {
                historikkAdapter.tekstBuilder().medEndretFelt(historikkEndretFeltType, original, bekreftet);
            }
            return true;
        }
        return false;
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = BekreftLovligOppholdVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class BekreftLovligOppholdVurderingOppdaterer extends BekreftOppholdOppdaterer {

        BekreftLovligOppholdVurderingOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public BekreftLovligOppholdVurderingOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                                       MedlemTjeneste medlemTjeneste,
                                                       MedlemskapAksjonspunktTjeneste medlemskapAksjonspunktTjeneste) {
            super(historikkAdapter, medlemTjeneste, medlemskapAksjonspunktTjeneste);
        }
    }

    @ApplicationScoped
    @DtoTilServiceAdapter(dto = BekreftOppholdsrettVurderingDto.class, adapter = AksjonspunktOppdaterer.class)
    public static class BekreftOppholdsrettVurderingOppdaterer extends BekreftOppholdOppdaterer {

        BekreftOppholdsrettVurderingOppdaterer() {
            // for CDI proxy
        }

        @Inject
        public BekreftOppholdsrettVurderingOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                                      MedlemTjeneste medlemTjeneste,
                                                      MedlemskapAksjonspunktTjeneste medlemskapAksjonspunktTjeneste) {
            super(historikkAdapter, medlemTjeneste, medlemskapAksjonspunktTjeneste);
        }
    }
}
