package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKNADSFRISTVILKÅRET;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag2Repository;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SoknadsfristAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class SøknadsfristOppdaterer implements AksjonspunktOppdaterer<SoknadsfristAksjonspunktDto> {

    private Historikkinnslag2Repository historikkinnslag2Repository;

    SøknadsfristOppdaterer() {
    }

    @Inject
    public SøknadsfristOppdaterer(Historikkinnslag2Repository historikkinnslag2Repository) {
        this.historikkinnslag2Repository = historikkinnslag2Repository;
    }

    @Override
    public OppdateringResultat oppdater(SoknadsfristAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        lagreHistorikk(dto, param);

        if (dto.getErVilkarOk()) {
            return new OppdateringResultat.Builder().leggTilManueltOppfyltVilkår(SØKNADSFRISTVILKÅRET).build();
        }
        return OppdateringResultat.utenTransisjon()
            .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
            .leggTilManueltAvslåttVilkår(SØKNADSFRISTVILKÅRET, Avslagsårsak.SØKT_FOR_SENT)
            .build();
    }

    private void lagreHistorikk(SoknadsfristAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var tilTekst = dto.getErVilkarOk() ? "oppfylt" : "ikke oppfylt";
        var historikkinnslag = new Historikkinnslag2.Builder().medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getBehandlingId())
            .medTittel(SkjermlenkeType.SOEKNADSFRIST)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .addLinje(new HistorikkinnslagLinjeBuilder().til("Søknadsfristvilkåret", tilTekst))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslag2Repository.lagre(historikkinnslag);
    }
}
