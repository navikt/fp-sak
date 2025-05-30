package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktStatus;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.repository.BehandlingRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType;


@ApplicationScoped
@DtoTilServiceAdapter(dto = BekreftSokersOpplysningspliktManuDto.class, adapter = AksjonspunktOppdaterer.class)
public class BekreftSøkersOpplysningspliktManuellOppdaterer implements AksjonspunktOppdaterer<BekreftSokersOpplysningspliktManuDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;

    private BehandlingRepository behandlingRepository;

    protected BekreftSøkersOpplysningspliktManuellOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public BekreftSøkersOpplysningspliktManuellOppdaterer(HistorikkinnslagRepository historikkinnslagRepository,
                                                          BehandlingRepository behandlingRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
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
        leggTilEndretFeltIHistorikkInnslag(param, dto.getBegrunnelse(), erVilkårOk);

        var åpneAksjonspunkter = behandling.getÅpneAksjonspunkter();
        var resultatBuilder = OppdateringResultat.utenTransisjon();
        if (erVilkårOk) {
            resultatBuilder.leggTilManueltOppfyltVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT);
            return resultatBuilder.build();
        } else {
            // Hoppe rett til foreslå vedtak uten totrinnskontroll
            åpneAksjonspunkter.stream().filter(a -> !a.getAksjonspunktDefinisjon().equals(dto.getAksjonspunktDefinisjon())) // Ikke seg selv
                .forEach(a -> resultatBuilder.medEkstraAksjonspunktResultat(a.getAksjonspunktDefinisjon(), AksjonspunktStatus.AVBRUTT));

            return resultatBuilder.medFremoverHopp(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR)
                .leggTilManueltAvslåttVilkår(VilkårType.SØKERSOPPLYSNINGSPLIKT, Avslagsårsak.MANGLENDE_DOKUMENTASJON)
                .build();
        }
    }

    private void leggTilEndretFeltIHistorikkInnslag(AksjonspunktOppdaterParameter param, String begrunnelse, Boolean vilkårOppfylt) {

        var tilVerdi = vilkårOppfylt ? "Vilkåret er oppfylt" : "Vilkåret er ikke oppfylt";
        var historikkinnslag = new Historikkinnslag.Builder().medAktør(HistorikkAktør.SAKSBEHANDLER)
            .medBehandlingId(param.getBehandlingId())
            .medFagsakId(param.getFagsakId())
            .medTittel(SkjermlenkeType.OPPLYSNINGSPLIKT)
            .addLinje(new HistorikkinnslagLinjeBuilder().til("Søkers opplysningsplikt", tilVerdi))
            .addLinje(begrunnelse);

        historikkinnslagRepository.lagre(historikkinnslag.build());
    }
}
