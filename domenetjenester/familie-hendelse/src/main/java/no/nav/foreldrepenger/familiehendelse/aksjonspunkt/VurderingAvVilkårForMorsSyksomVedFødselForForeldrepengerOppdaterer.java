package no.nav.foreldrepenger.familiehendelse.aksjonspunkt;

import java.util.Objects;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagType;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.familiehendelse.FamilieHendelseTjeneste;
import no.nav.foreldrepenger.familiehendelse.aksjonspunkt.dto.VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto.class, adapter = AksjonspunktOppdaterer.class)
public class VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerOppdaterer implements AksjonspunktOppdaterer<VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto> {

    private HistorikkTjenesteAdapter historikkAdapter;
    private FamilieHendelseTjeneste familieHendelseTjeneste;

    VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerOppdaterer() {
    }

    @Inject
    public VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerOppdaterer(HistorikkTjenesteAdapter historikkAdapter,
                                                                              FamilieHendelseTjeneste familieHendelseTjeneste) {
        this.historikkAdapter = historikkAdapter;
        this.familieHendelseTjeneste = familieHendelseTjeneste;
    }

    @Override
    public OppdateringResultat oppdater(VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = param.getBehandling();

        lagreHistorikkinnslag(dto, param);

        final var oppdatertOverstyrtHendelse = familieHendelseTjeneste.opprettBuilderFor(behandling);
        oppdatertOverstyrtHendelse.medErMorForSykVedFødsel(dto.getErMorForSykVedFodsel());
        familieHendelseTjeneste.lagreOverstyrtHendelse(behandling, oppdatertOverstyrtHendelse);

        return OppdateringResultat.utenOveropp();
    }

    private void lagreHistorikkinnslag(VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto dto, AksjonspunktOppdaterParameter param) {
        var familieHendelseGrunnlag = familieHendelseTjeneste.hentAggregat(param.getBehandlingId());
        var original = familieHendelseGrunnlag.getGjeldendeVersjon().erMorForSykVedFødsel();
        boolean bekreftet = dto.getErMorForSykVedFodsel();

        if (!Objects.equals(bekreftet, original)) {
            lagHistorikkinnslag(dto, param, original, bekreftet);
            historikkAdapter.opprettHistorikkInnslag(param.getBehandlingId(), HistorikkinnslagType.FAKTA_ENDRET);
        }
    }

    private void lagHistorikkinnslag(VurderingAvVilkårForMorsSyksomVedFødselForForeldrepengerDto dto, AksjonspunktOppdaterParameter param, Boolean original, boolean bekreftet) {
        var tilVerdi = bekreftet ? HistorikkEndretFeltVerdiType.DOKUMENTERT : HistorikkEndretFeltVerdiType.IKKE_DOKUMENTERT;
        var fraVerdi = fraVerdi(original);
        historikkAdapter.tekstBuilder()
            .medEndretFelt(HistorikkEndretFeltType.SYKDOM, fraVerdi, tilVerdi)
            .medBegrunnelse(dto.getBegrunnelse(), param.erBegrunnelseEndret())
            .medSkjermlenke(SkjermlenkeType.FAKTA_OM_FOEDSEL);
    }

    private HistorikkEndretFeltVerdiType fraVerdi(Boolean original) {
        if (original == null) {
            return null;
        }
        return original ? HistorikkEndretFeltVerdiType.DOKUMENTERT : HistorikkEndretFeltVerdiType.IKKE_DOKUMENTERT;
    }
}
