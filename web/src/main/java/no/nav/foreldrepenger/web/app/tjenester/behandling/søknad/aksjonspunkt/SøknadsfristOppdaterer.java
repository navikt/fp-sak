package no.nav.foreldrepenger.web.app.tjenester.behandling.søknad.aksjonspunkt;

import static no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagLinjeBuilder.tilNullable;
import static no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårType.SØKNADSFRISTVILKÅRET;

import java.util.Optional;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdateringTransisjon;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkAktør;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.Historikkinnslag;
import no.nav.foreldrepenger.behandlingslager.behandling.historikk.HistorikkinnslagRepository;
import no.nav.foreldrepenger.behandlingslager.behandling.skjermlenke.SkjermlenkeType;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.Avslagsårsak;
import no.nav.foreldrepenger.behandlingslager.behandling.vilkår.VilkårUtfallType;
import no.nav.vedtak.exception.FunksjonellException;

@ApplicationScoped
@DtoTilServiceAdapter(dto = SoknadsfristAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class SøknadsfristOppdaterer implements AksjonspunktOppdaterer<SoknadsfristAksjonspunktDto> {

    private HistorikkinnslagRepository historikkinnslagRepository;

    SøknadsfristOppdaterer() {
    }

    @Inject
    public SøknadsfristOppdaterer(HistorikkinnslagRepository historikkinnslagRepository) {
        this.historikkinnslagRepository = historikkinnslagRepository;
    }

    @Override
    public OppdateringResultat oppdater(SoknadsfristAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        lagreHistorikk(dto, param);

        if (dto.getErVilkarOk()) {
            return new OppdateringResultat.Builder().leggTilManueltOppfyltVilkår(SØKNADSFRISTVILKÅRET).build();
        }

        var avslagsårsak = Avslagsårsak.fraDefinertKode(dto.getAvslagskode())
            .orElseThrow(() -> new FunksjonellException("FP-MANGLER-ÅRSAK", "Ugyldig avslagsårsak", "Velg gyldig avslagsårsak"));

        return OppdateringResultat.utenTransisjon()
            .medFremoverHopp(AksjonspunktOppdateringTransisjon.AVSLAG_VILKÅR)
            .leggTilManueltAvslåttVilkår(SØKNADSFRISTVILKÅRET, avslagsårsak)
            .build();
    }

    private void lagreHistorikk(SoknadsfristAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        var avslagsårsak = Optional.ofNullable(dto.getAvslagskode()).flatMap(Avslagsårsak::fraDefinertKode).map(Avslagsårsak::getNavn).orElse(null);
        var utfall = dto.getErVilkarOk() ? VilkårUtfallType.OPPFYLT.getNavn() : VilkårUtfallType.IKKE_OPPFYLT.getNavn();
        var historikkinnslag = new Historikkinnslag.Builder().medFagsakId(param.getFagsakId())
            .medBehandlingId(param.getBehandlingId())
            .medTittel(SkjermlenkeType.SOEKNADSFRIST)
            .medAktør(HistorikkAktør.SAKSBEHANDLER)
            .addLinje(tilNullable(SØKNADSFRISTVILKÅRET.getNavn(), utfall))
            .addLinje(tilNullable("Avslagsårsak", avslagsårsak))
            .addLinje(dto.getBegrunnelse())
            .build();
        historikkinnslagRepository.lagre(historikkinnslag);
    }
}
