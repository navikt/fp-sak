package no.nav.foreldrepenger.web.app.tjenester.behandling.vedtak.aksjonspunkt;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterParameter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.AksjonspunktOppdaterer;
import no.nav.foreldrepenger.behandling.aksjonspunkt.DtoTilServiceAdapter;
import no.nav.foreldrepenger.behandling.aksjonspunkt.OppdateringResultat;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.AksjonspunktDefinisjon;
import no.nav.foreldrepenger.behandlingslager.behandling.aksjonspunkt.VurderÅrsak;
import no.nav.foreldrepenger.behandlingslager.kodeverk.Kodeverdi;
import no.nav.foreldrepenger.domene.vedtak.VedtakAksjonspunktData;
import no.nav.foreldrepenger.domene.vedtak.impl.FatterVedtakAksjonspunkt;
import no.nav.foreldrepenger.web.app.tjenester.behandling.aksjonspunkt.FatterVedtakAksjonspunktDto;

@ApplicationScoped
@DtoTilServiceAdapter(dto = FatterVedtakAksjonspunktDto.class, adapter = AksjonspunktOppdaterer.class)
public class FatterVedtakAksjonspunktOppdaterer implements AksjonspunktOppdaterer<FatterVedtakAksjonspunktDto> {

    private FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt;

    public FatterVedtakAksjonspunktOppdaterer() {
        // for CDI proxy
    }

    @Inject
    public FatterVedtakAksjonspunktOppdaterer(FatterVedtakAksjonspunkt fatterVedtakAksjonspunkt) {
        this.fatterVedtakAksjonspunkt = fatterVedtakAksjonspunkt;
    }

    @Override
    public OppdateringResultat oppdater(FatterVedtakAksjonspunktDto dto, AksjonspunktOppdaterParameter param) {
        Collection<AksjonspunktGodkjenningDto> aksjonspunktGodkjenningDtoList =
            dto.getAksjonspunktGodkjenningDtos() != null ? dto.getAksjonspunktGodkjenningDtos() : Collections.emptyList();

        var aksjonspunkter = aksjonspunktGodkjenningDtoList.stream().map(a -> {
            // map til VedtakAksjonsonspunktData fra DTO
            var aksDef = AksjonspunktDefinisjon.fraKode(a.getAksjonspunktKode());
            return new VedtakAksjonspunktData(aksDef, a.isGodkjent(), a.getBegrunnelse(), fraDto(a.getArsaker()));
        }).collect(Collectors.toSet());

        fatterVedtakAksjonspunkt.oppdater(param.getRef(), aksjonspunkter);

        return OppdateringResultat.utenOverhopp();
    }

    private Collection<String> fraDto(Collection<VurderÅrsak> arsaker) {
        return Optional.ofNullable(arsaker).orElse(List.of()).stream().map(Kodeverdi::getKode).collect(Collectors.toSet());
    }
}
