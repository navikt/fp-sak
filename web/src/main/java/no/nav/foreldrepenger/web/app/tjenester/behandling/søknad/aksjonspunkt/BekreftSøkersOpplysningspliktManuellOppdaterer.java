package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingskontroll.transisjoner.FellesTransisjoner;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltType;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkEndretFeltVerdiType;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;
import no.nav.foreldrepenger.historikk.HistorikkTjenesteAdapter;

@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSokersOpplysningspliktManuDto.class, adapter=AksjonspunktOppdaterer.class)
public class BekreftSøkersOpplysningspliktManuellOppdaterer implements AksjonspunktOppdaterer<BekreftSokersOpplysningspliktManuDto> {

    private HistorikkTjenesteAdapter historikkTjenesteAdapter;
    private BehandlingRepository behandlingRepository;

    protected BekreftSøkersOpplysningspliktManuellOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftSøkersOpplysningspliktManuellOppdaterer(HistorikkTjenesteAdapter historikkTjenesteAdapter,
                                                          BehandlingRepository behandlingRepository) {
        this.historikkTjenesteAdapter = historikkTjenesteAdapter;
        this.behandlingRepository = behandlingRepository;
    }

    @Override
    public OppdateringResultat oppdater(BekreftSokersOpplysningspliktManuDto dto, AksjonspunktOppdaterParameter param) {
        var behandling = behandlingRepository.hentBehandling(param.getBehandlingId());
        var erVilkårOk = dto.getErVilkarOk() && dto.getInntektsmeldingerSomIkkeKommer()
            .stream()
            .filter(imelding -> !imelding.isBrukerHarSagtAtIkkeKommer())
            .toList()
            .isEmpty();
        leggTilEndretFeltIHistorikkInnslag(dto.getBegrunnelse(), erVilkårOk);

        var åpneAksjonspunkter = behandling.getÅpneAksjonspunkter();
        var resultatBuilder = OppdateringResultat.utenTransisjon();
        if (erVilkårOk) {
            resultatBuilder.leggTilManueltOppfyltVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT);
            return resultatBuilder.build();
        } else {
            // Hoppe rett til foreslå vedtak uten totrinnskontroll
            åpneAksjonspunkter.stream()
                .filter(a -> !a.getAksjonspunktDefinisjon().equals(dto.getAksjonspunktDefinisjon())) // Ikke seg selv
                .forEach(a -> resultatBuilder.medEkstraAksjonspunktResultat(a.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));

            return resultatBuilder
                .medFremoverHopp(FellesTransisjoner.FREMHOPP_VED_AVSLAG_VILKÅR)
                .leggTilManueltAvslåttVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, Avslagsårsak.MANGLENDE_DOKUMENTASJON)
                .build();
        }
    }

    private void leggTilEndretFeltIHistorikkInnslag(String begrunnelse, Boolean vilkårOppfylt) {
        var tilVerdi = Boolean.TRUE.equals(vilkårOppfylt) ? HistorikkEndretFeltVerdiType.VILKAR_OPPFYLT : HistorikkEndretFeltVerdiType.VILKAR_IKKE_OPPFYLT;

        if (begrunnelse != null) {
            historikkTjenesteAdapter.tekstBuilder().medBegrunnelse(begrunnelse);
        }
        historikkTjenesteAdapter.tekstBuilder().medEndretFelt(HistorikkEndretFeltType.SOKERSOPPLYSNINGSPLIKT, null, tilVerdi)
            .medSkjermlenke(SkjermlenkeType.OPPLYSNINGSPLIKT);
    }
}
